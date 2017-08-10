/*
 *  JDBCCorpus.java
 *
 * Copyright (c) 2010, Austrian Research Institute for
 * Artificial Intelligence (OFAI)
 *
 * This file is free
 * software, licenced under the GNU General Public License,
 *
 *  Johann Petrak, 30/8/2010
 *
 *  $Id: JDBCCorpus.java 124 2014-04-24 18:23:51Z johann.petrak $
 */

package at.ofai.gate.virtualcorpus;

import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

import gate.*;
import gate.corpora.CorpusImpl;
import gate.corpora.DocumentImpl;
import gate.creole.*;
import gate.creole.metadata.*;
import gate.event.CorpusEvent;
import gate.event.CorpusListener;
import gate.event.CreoleEvent;
import gate.event.CreoleListener;
import gate.persist.PersistenceException;
import gate.util.*;
import gate.util.persistence.PersistenceManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;

/** 
 * A Corpus LR that mirrors documents stored in a JDBC database table field.
 * 
 * The table must have a unique id field which will serve as the document
 * name and it must have a field that contains the actual document in some
 * format that can be both read, and if readonly is not true, also written by 
 * GATE using the currently loaded
 * plugins. The format used by default is gate XML, however it is possible
 * to specify a different format by specifying a mime type when the corpus
 * is created.
 * <p>
 * NOTE: this corpus is immutable, none of the methods to add or remove documents
 * is supported!
 * <p>
 * This corpus LR automatically uses a "dummy datastore" internally.
 * This datastore is created and removed automatically when the corpus LR is
 * created and removed. This datastore cannot be used for anything useful, it
 * does not allow listing of resources or storing of anything but documents
 * that are already in the corpus. It is mainly here because GATE assumes that
 * documents are either transient or from a datastore. To avoid documents from
 * a JDBCCorpus to get treated as transient documents, their DataStore is
 * set to this dummy DataStore.
 * 
 * @author Johann Petrak
 */

@CreoleResource(
    name = "JDBCCorpus",
    interfaceName = "gate.Corpus", 
    icon = "corpus", 
    helpURL = "http://code.google.com/p/gateplugin-virtualcorpus/wiki/JDBCCorpusUsage",
    comment = "A corpus backed by GATE documents stored in a JDBC table")
public class JDBCCorpus  
  extends VirtualCorpus
  implements Corpus, CreoleListener
  {

  //*****
  // Fields
  //******
  
  /**
   * 
   */
  private static final long serialVersionUID = -8485133333415382902L;

  // for accessing document name by index
  protected List<String> documentNames = new ArrayList<String>();
  // for checking if ith document is loaded
  protected List<Boolean> isLoadeds = new ArrayList<Boolean>();
  // for finding index for document name
  //REMOVE protected Map<String,Integer> documentIndexes = new HashMap<String,Integer>();
  
  protected Map<String,Document> loadedDocuments = new HashMap<String,Document>();
  
  protected List<CorpusListener> listeners = new ArrayList<CorpusListener>();
  
  

  protected String jdbcDriver = "org.h2.Driver";
  @CreoleParameter(
          comment = "The JDBC driver to use",
          defaultValue = "org.h2.Driver")
  // other values: com.mysql.jdbc.Driver
  public void setJdbcDriver(String driver) { jdbcDriver = driver; }
  public String getJdbcDriver() { return jdbcDriver; }

  
  protected String jdbcUrl = "";
  @CreoleParameter(
          comment = "The JDBC URL, may contain $prop{name} or $env{name} or ${relpath}",
          defaultValue = "jdbc:h2:${dbdirectory}/YOURDBPREFIX")
  // other values: jdbc:mysql://localhost:3306/database?user=user&password=pass
  public void setJdbcUrl(String url) { jdbcUrl = url; }
  public String getJdbcUrl() { return jdbcUrl; }
  
  protected String jdbcUser = "";
  @Optional
  @CreoleParameter(comment = "The JDBC user id",defaultValue = "")
  public void setJdbcUser(String user) { jdbcUser = user; }
  public String getJdbcUser() { return jdbcUser; }
  
  protected String jdbcPassword = "";
  @Optional
  @CreoleParameter(comment = "The JDBC password",defaultValue = "")
  public void setJdbcPassword(String pw) { jdbcPassword = pw; }
  public String getJdbcPassword() { return jdbcPassword; }
  
  protected URL dbDirectoryUrl = null;
  @Optional
  @CreoleParameter(
          comment = "The location of where a file database is stored. This is not used directly but can be used to replace the ${dbdirectory} variable in the jdbcUrl parameter", defaultValue="file://.")
  public void setDbDirectoryUrl(URL dir) {dbDirectoryUrl = dir;}
  public URL getDbDirectoryUrl() {return dbDirectoryUrl;}
  

  /**
   */
  @CreoleParameter(comment="The database table name")
  public void setTableName(String name) { tableName = name; }
  public String getTableName() { return tableName;}
  protected String tableName;

  
  @CreoleParameter(comment = "The document id/name field name")
  public void setDocumentNameField(String name) { documentNameField = name; }
  public String getDocumentNameField() { return documentNameField; }
  protected String documentNameField;

  @CreoleParameter(comment = "The document content field name")
  public void setDocumentContentField(String name) { documentContentField = name; }
  public String getDocumentContentField() { return documentContentField; }
  protected String documentContentField;

  @Optional
  @CreoleParameter(
    comment = "Mime type of content, if empty, GATE XML is assumed", defaultValue = "")
  public void setMimeType(String type) { mimeType = type; }
  public String getMimeType() { return mimeType; }
  protected String mimeType = "";


  @CreoleParameter(comment = "SQL Query for selecting the set of document ids/names",
    defaultValue = "SELECT ${documentNameField} from ${tableName}")
  @Optional
  public void setSelectSQL(String sql) {
    this.selectSQL = sql;
  }
  /**
   * @return 
   */
  public String getSelectSQL() {
    return this.selectSQL;
  }
  protected String selectSQL = "SELECT ${documentNameField} from ${tableName}";


  protected DummyDataStore4JDBCCorp ourDS = null;
  protected Connection dbConnection = null;
  protected PreparedStatement getContentStatement = null;
  protected PreparedStatement updateContentStatement = null;

  private static final String DEFAULT_MIME_TYPE = "application/xml";
  String encoding = "utf-8";

  @Override
  /**
   * Initializes the JDBCCorpus LR
   */
  public Resource init() 
    throws ResourceInstantiationException {
    if(getTableName() == null || getTableName().equals("")) {
      throw new ResourceInstantiationException("tableName must not be empty");
    }
    if(getDocumentNameField() == null || getDocumentNameField().equals("")) {
      throw new ResourceInstantiationException("documentNameField must not be empty");
    }
    if(getDocumentContentField() == null || getDocumentContentField().equals("")) {
      throw new ResourceInstantiationException("documentContentField must not be empty");
    }
    if(getSelectSQL() == null || getSelectSQL().equals("")) {
      throw new ResourceInstantiationException("selectSQL must not be empty");
    }
    String query = getSelectSQL(); // this contains the ${tableName} and ${documentNameField} vars
    query = query.replaceAll(Pattern.quote("${tableName}"), getTableName());
    query = query.replaceAll(Pattern.quote("${documentNameField}"), getDocumentNameField());
    String expandedUrl = "";
    try {
      Class.forName(getJdbcDriver());
      String dbdirectory = "";
      if(getDbDirectoryUrl().getProtocol().equals("file")) {
        dbdirectory = getDbDirectoryUrl().getPath();
        dbdirectory = new File(dbdirectory).getAbsolutePath();
      } else {
        throw new GateRuntimeException("The database directory URL is not a file URL");
      }
      Map<String,String> dbdirectoryMap = new HashMap<String,String>();
      dbdirectoryMap.put("dbdirectory", dbdirectory);
      
      expandedUrl = 
        gate.Utils.replaceVariablesInString(jdbcUrl, dbdirectoryMap, this);
      String expandedUser = 
        gate.Utils.replaceVariablesInString(jdbcUser, dbdirectoryMap, this);
      String expandedPassword = 
        gate.Utils.replaceVariablesInString(jdbcPassword, dbdirectoryMap, this);
      
      System.out.println("Using JDBC URL: "+expandedUrl);
      dbConnection = DriverManager.getConnection(expandedUrl, expandedUser, expandedPassword);
    } catch (Exception ex) {
      throw new ResourceInstantiationException("Could not get driver/connection",ex);
    }
    Statement stmt = null;
    try {
      stmt = dbConnection.createStatement();
      ResultSet rs = null;
      rs = stmt.executeQuery(query);
      int i = 0;
      while(rs.next()) {
        String docName = rs.getString(getDocumentNameField());
        documentNames.add(docName);
        isLoadeds.add(false);
        documentIndexes.put(docName, i);
        i++;
      }
    } catch(SQLException ex) {
      throw new ResourceInstantiationException("Problem accessing database",ex);
    }
    try {
      PersistenceManager.registerPersistentEquivalent(
          at.ofai.gate.virtualcorpus.JDBCCorpus.class,
          at.ofai.gate.virtualcorpus.JDBCCorpusPersistence.class);
    } catch (PersistenceException e) {
      throw new ResourceInstantiationException(
              "Could not register persistence",e);
    }
      try {
        // TODO: use more fields or a hash to make this unique?
        ourDS =
          (DummyDataStore4JDBCCorp) Factory.createDataStore(
                "at.ofai.gate.virtualcorpus.DummyDataStore4JDBCCorp", 
                expandedUrl + "//" + getTableName());
        ourDS.setName("DummyDS4_" + this.getName());
        ourDS.setComment("Dummy DataStore for JDBCCorpus " + this.getName());
        ourDS.setCorpus(this);
        //System.err.println("Created dummy corpus: "+ourDS+" with name "+ourDS.getName());
      } catch (Exception ex) {
        throw new ResourceInstantiationException(
          "Could not create dummy data store", ex);
      }
    Gate.getCreoleRegister().addCreoleListener(this);


    // create all the prepared statements we need for accessing stuff in the db
    try {
      query = "SELECT "+getDocumentContentField()+" FROM "+
        getTableName()+" WHERE "+getDocumentNameField()+" = ?";
      System.out.println("Preparing get document statement: "+query);
      getContentStatement = dbConnection.prepareStatement(query);
      String outfield = getDocumentContentField();

    } catch (SQLException ex) {
      throw new ResourceInstantiationException("Could not prepare statement",ex);
    }


    return this;
  }
  
  /**
   * Test is the document with the given index is loaded. If an index is 
   * specified that is not in the corpus, a GateRuntimeException is thrown.
   * 
   * @param index 
   * @return true if the document is loaded, false otherwise. 
   */
  public boolean isDocumentLoaded(int index) {
    if(index < 0 || index >= isLoadeds.size()) {
      throw new GateRuntimeException("Document number "+index+
              " not in corpus "+this.getName()+" of size "+isLoadeds.size());
    }
    //System.out.println("isDocumentLoaded called: "+isLoadeds.get(index));
    return isLoadeds.get(index);
  }

  public boolean isDocumentLoaded(Document doc) {
    String docName = doc.getName();
    //System.out.println("DirCorp: called unloadDocument: "+docName);
    Integer index = documentIndexes.get(docName);
    if(index == null) {
      throw new RuntimeException("Document "+docName+
              " is not contained in corpus "+this.getName());
    }
    return isDocumentLoaded(index);
  }

  /**
   * Unload a document from the corpus. When a document is unloaded it
   * is automatically stored in GATE XML format to the directory where it
   * was read from or to the directory specified for the outDirectoryURL
   * parameter. If saveDocuments is false, nothing is saved at all.
   * if the document is not part of the corpus, a GateRuntimeException is
   * thrown.
   *
   * @param doc
   */
  public void unloadDocument(Document doc) {
    String docName = doc.getName();
    //System.out.println("DirCorp: called unloadDocument: "+docName);
    Integer index = documentIndexes.get(docName);
    if(index == null) {
      throw new RuntimeException("Document "+docName+
              " is not contained in corpus "+this.getName());
    }
    if(isDocumentLoaded(index)) {
      // if saveOnUnload is set, save the document
      /* TEMPORARY 
      if(saveDocuments) {
        try {
          saveDocument(doc);
        } catch (Exception ex) {
          throw new GateRuntimeException("Problem saving document "+docName,ex);
        }
      }
      */
      loadedDocuments.remove(docName);
      isLoadeds.set(index, false);
      //System.err.println("Document unloaded: "+docName);
    } // else silently do nothing
  }
  
  
  public void removeCorpusListener(CorpusListener listener) {
    listeners.remove(listener);
  }
  public void addCorpusListener(CorpusListener listener) {
    listeners.add(listener);
  }

  /**
   * Get the list of document names in this corpus.
   *
   * @return the list of document names 
   */
  public List<String> getDocumentNames() {
    List<String> newList = new ArrayList<String>(documentNames);
    return newList;
  }

  /**
   * Return the name of the document with the given index from the corpus. 
   *
   * @param i the index of the document to return
   * @return the name of the document with the given index
   */
  public String getDocumentName(int i) {
    return documentNames.get(i);
  }

  /**
   * This method is not implemented and throws a
   * gate.util.MethodNotImplementedException.
   * 
   * @param directory
   * @param filter
   * @param encoding
   * @param recurseDirectories
   */
  public void populate(
      URL directory, FileFilter filter, 
      String encoding, boolean recurseDirectories) {
      populate(directory, filter, encoding, null, recurseDirectories);
  }

  /**
   * This method is not implemented and throws a
   * gate.util.MethodNotImplementedException.
   *
   * @param directory
   * @param filter
   * @param encoding
   * @param mimeType
   * @param recurseDirectories
   */
  public void populate (
      URL directory, FileFilter filter, 
      String encoding, String mimeType, 
      boolean recurseDirectories) {
    /* TEMPORARY
    if(isTransientCorpus) {
      throw new GateRuntimeException("Cannot populate a transient JDBC corpus");
    } else {
      try {
        CorpusImpl.populate(this, directory, filter, encoding, mimeType, recurseDirectories);
      } catch (IOException ex) {
        throw new GateRuntimeException("IO error",ex);
      }
    }
    */
  }

  /**
   * @return 
   */
  public DataStore getDataStore() {
      return ourDS;
  }

  /**
   * This always throws a PersistenceException as this kind of corpus cannot
   * be saved to a datastore.
   * 
   * @param ds
   * @throws PersistenceException
   */
  public void setDataStore(DataStore ds) throws PersistenceException {
    throw new PersistenceException("Corpus "+this.getName()+
            " cannot be saved to a datastore");
  }

  /**
   * This follows the convention for transient corpus objects and always
   * returns false.
   * 
   * @return always false
   */
  public boolean isModified() {
    return false;
  }

  @Override
  public void sync() {
    // TODO: save document!?!?!?
  }


  @Override
  public void cleanup() {
      // TODO:
      // deregister our listener for resources of type document
      //
    try {
      if(dbConnection != null && !dbConnection.isClosed()) {
        dbConnection.close();
      }
    } catch (SQLException ex) {
      // TODO: log, but otherwise ignore
    }
      Gate.getDataStoreRegister().remove(ourDS);
  }

  @Override
  public void setName(String name) {
    super.setName(name);
    if(ourDS != null) {
      ourDS.setName("DummyDS4_"+this.getName());
      ourDS.setComment("Dummy DataStore for JDBCCorpus "+this.getName());
    }
  }


  // Methods to be implemented from List

  /**
   * Add a document to the corpus. If the document has a name that is already
   * in the list of documents, return false and do not add the document.
   * Note that only the name is checked!
   * If the name of the document added is not ending in ".xml", a 
   * GateRuntimeException is thrown.
   * If the document is already adopted by some data store throw an exception.
   */
  public boolean add(Document doc) {
    /* TEMPORARY
    if(!saveDocuments) {
      return false;
    }
    //System.out.println("JDBCCorp: called add(Object): "+doc.getName());
    String docName = doc.getName();
    Integer index = documentIndexes.get(docName);
    if(index != null) {
      return false;  // if that name is already in the corpus, do not add
    } else {
      if(doc.getDataStore() != null) {
        throw new GateRuntimeException("Cannot add "+doc.getName()+" which belongs to datastore "+doc.getDataStore().getName());
      }
      try {
        insertDocument(doc);
      } catch (Exception ex) {
        throw new GateRuntimeException("Problem inserting document "+docName,ex);
      }
      int i = documentNames.size();
      documentNames.add(docName);
      documentIndexes.put(docName, i);
      isLoadeds.add(false);
      if(!isTransientCorpus) {
        adoptDocument(doc);
      }
      fireDocumentAdded(new CorpusEvent(
          t    }
his, doc, i, CorpusEvent.DOCUMENT_ADDED));
      
      return true;
    }
    */ 
    return true;
  }



  /**
   * This removes all documents from the corpus. Note that this does nothing
   * when the saveDocuments parameter is set to false.
   * If the outDirectoryURL parameter was set, this method will throw
   * a GateRuntimeException.
   */
  public void clear() {
    /** TEMPORARY
    if(!saveDocuments) {
      return;
    }
    for(int i=documentNames.size()-1; i>=0; i--) {
      remove(i);
    }
    */
  }
  
  /**
   * This checks if a document with the same name as the document
   * passed is already in the corpus. The content is not considered 
   * for this.
   */
  public boolean contains(Object docObj) {
    Document doc = (Document)docObj;
    String docName = doc.getName();
    return (documentIndexes.get(docName) != null);
  }
  

  /**
   * Return the document for the given index in the corpus.
   * An IndexOutOfBoundsException is thrown when the index is not contained
   * in the corpus.
   * The document will be read from the file only if it is not already loaded.
   * If it is already loaded a reference to that document is returned.
   * 
   * @param index
   * @return 
   */
  public Document get(int index) {
    //System.out.println("DirCorp: called get(index): "+index);
    if(index < 0 || index >= documentNames.size()) {
      throw new IndexOutOfBoundsException(
          "Index "+index+" not in corpus "+this.getName()+
          " of size "+documentNames.size());
    }
    String docName = documentNames.get(index);
    if(isDocumentLoaded(index)) {
      Document doc = loadedDocuments.get(docName);
      //System.out.println("Returning loaded document "+doc);
      return doc;
    }
    //System.out.println("Document not loaded, reading");
    Document doc;
    try {
      doc = readDocument(docName);
    } catch (Exception ex) {
      throw new GateRuntimeException("Problem retrieving document data for "+docName,ex);
    }
    loadedDocuments.put(docName, doc);
    isLoadeds.set(index, true);
      adoptDocument(doc);
    return doc;
  }

  /**
   * Returns the index of the document with the same name as the given document
   * in the corpus. The content of the document is not considered for this.
   * 
   * @param docObj
   * @return
   */
  public int indexOf(Object docObj) {
    Document doc = (Document)docObj;
    String docName = doc.getName();
    Integer index = documentIndexes.get(docName);
    if(index == null) {
      return -1;
    } else {
      return index;
    }
  }

  /**
   * Check if the corpus is empty.
   *
   * @return true if the corpus is empty
   */
  public boolean isEmpty() {
    return (documentNames.size() == 0);
  }

  /**
   * Returns an iterator to iterate through the documents of the
   * corpus. The iterator does not allow modification of the corpus.
   * 
   * @return
   */
  public Iterator<Document> iterator() {
    return new JDBCCorpusIterator();
  }

  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   * 
   * @param docObj
   * @return
   */
  public int lastIndexOf(Object docObj) {
    throw new MethodNotImplementedException(
            notImplementedMessage("lastIndexOf(Object)"));
  }

  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   *
   * @return
   */
  public ListIterator<Document> listIterator() {
    throw new MethodNotImplementedException(
            notImplementedMessage("listIterator"));
  }

  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   *
   *
   * @param i
   * @return
   */
  public ListIterator<Document> listIterator(int i) {
    throw new MethodNotImplementedException(
            notImplementedMessage("listIterator(int)"));
  }

  /**
   * 
   * @param index
   * @return the document that was just removed from the corpus
   */
  public Document remove(int index) {
    Document doc = (Document)get(index);
    String docName = documentNames.get(index);
    documentNames.remove(index);
    if(isLoadeds.get(index)) {
      loadedDocuments.remove(docName);
    }
    isLoadeds.remove(index);
    documentIndexes.remove(docName);
    removeDocument(docName);
      try {
        doc.setDataStore(null);
      } catch (PersistenceException ex) {
        // this should never happen
      }
    fireDocumentRemoved(new CorpusEvent(
        this, doc,
        index, CorpusEvent.DOCUMENT_REMOVED));
    return doc;
  }

  /**
   * Removes a document with the same name as the given document
   * from the corpus. This is not
   * supported and throws a GateRuntimeException if the outDirectoryURL
   * was specified for this corpus. If the saveDocuments parameter is false
   * for this corpus, this method does nothing and always returns false.
   * If the a document with the same name as the given document is not
   * found int the corpus, this does nothing and returns false.
   * 
   * @param docObj
   * @return true if a document was removed from the corpus
   */
  public boolean remove(Object docObj) {
    int index = indexOf(docObj);
    if(index == -1) {
      return false;
    }
    String docName = documentNames.get(index);
    documentNames.remove(index);
    isLoadeds.remove(index);
    documentIndexes.remove(docName);
    removeDocument(docName);  
    Document doc = isDocumentLoaded(index) ? (Document)get(index) : null;
      try {
        doc.setDataStore(null);
      } catch (PersistenceException ex) {
        // this should never happen
      }
    fireDocumentRemoved(new CorpusEvent(
        this, doc,
        index, CorpusEvent.DOCUMENT_REMOVED));
    return true;
  }

  /**
   * Remove all the documents in the collection from the corpus.
   *
   * @param coll
   * @return true if any document was removed
   */
  public boolean removeAll(Collection coll) {
    boolean ret = false;
    for(Object docObj : coll) {
      ret = ret || remove(docObj);
    }
    return ret;
  }

  public int size() {
    return documentNames.size();
  }

  //****** 
  //Listener methods
  //***********
  protected void fireDocumentAdded(CorpusEvent e) {
    for(CorpusListener listener : listeners) {
      listener.documentAdded(e);
    }
  }

  protected void fireDocumentRemoved(CorpusEvent e) {
    for(CorpusListener listener : listeners) {
      listener.documentRemoved(e);
    }
  }

  public void resourceLoaded(CreoleEvent e) {
    //System.out.println("DirCorp: Resource loaded");
  }

  public void resourceRenamed(
          Resource resource,
          String oldName,
          String newName) {
    // if one of our documents gets renamed, rename it back and
    // write an error message
    if(resource instanceof Document) {
      Document doc = (Document)resource;
      if(loadedDocuments.containsValue(doc)) {
        System.err.println("ERROR: documents from a JDBC corpus cannot be renamed!");
        doc.setName(oldName);
      }
    }
  }

  public void resourceUnloaded(CreoleEvent e) {
    Resource res = e.getResource();
    if(res instanceof Document) {
      Document doc = (Document)res;
      // check if this document has actually been loaded by us
      if(loadedDocuments.containsValue(doc)) {
        unloadDocument(doc);
      } // else: its not ours, ignore
    } else if(res == this) {
      Gate.getCreoleRegister().removeCreoleListener(this);
    }
  }

  public void datastoreClosed(CreoleEvent ev) {
  }
  
  public void datastoreCreated(CreoleEvent ev) {
    
  }
  
  public void datastoreOpened(CreoleEvent ev) {
    
  }
  
  //**************************
  // helper methods
  // ************************
  protected void saveDocument(Document doc) throws ResourceInstantiationException, IOException, SQLException {
    /* TEMPORARY
    if(!getSaveDocuments()) {
      return;
    }
    String docContent = doc.toXml();
    String docName = doc.getName();
    updateContentStatement.setString(2, docName);
    if (getUseCompression() || getCompressOnCopy()) {
      String docEncoding = (String) doc.getParameterValue("encoding");
      String usedEncoding = getActiveEncoding(docEncoding);
      InputStream iscomp = getGZIPCompressedInputStream(docContent, usedEncoding);
      updateContentStatement.setBinaryStream(1, iscomp);
      updateContentStatement.execute();
      iscomp.close();
    } else {
      updateContentStatement.setString(1, docContent);
      updateContentStatement.execute();
    }
    */
  }
  protected void insertDocument(Document doc) throws SQLException, ResourceInstantiationException, IOException {
    /* TEMPORARY
    if (!getSaveDocuments()) {
      return;
    }
    String docContent = doc.toXml();
    String docName = doc.getName();
    String docEncoding = (String) doc.getParameterValue("encoding");
    String usedEncoding = getActiveEncoding(docEncoding);
    
    insertContentStatement.setString(1, docName);
    String docMimeType = (String)doc.getParameterValue("mimeType");
    // when we have  encoding and/or mime type fields, set them!
    if(haveEncodingField) {
      if(haveMimeTypeField) {
        insertContentStatement.setString(3, usedEncoding);
        insertContentStatement.setString(4, docMimeType); 
      } else {
        insertContentStatement.setString(3, usedEncoding);
      }
    } else {
      if(haveMimeTypeField) {
        insertContentStatement.setString(3, docMimeType);         
      } else {
        // neither encoding, nor mime type, nothing needs to be done
      }
    }
    if (getUseCompression() || getCompressOnCopy()) {
      InputStream iscomp = getGZIPCompressedInputStream(docContent, usedEncoding);
      insertContentStatement.setBinaryStream(2, iscomp);
      insertContentStatement.execute();
      iscomp.close();
    } else {
      insertContentStatement.setString(2, docContent);
      insertContentStatement.execute();
    }
    */
  }
  
  protected InputStream getGZIPCompressedInputStream(String theString, String theEncoding) 
    throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    GZIPOutputStream gos = new GZIPOutputStream(baos);
    gos.write(theString.getBytes(theEncoding));
    gos.close();
    ByteArrayInputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
    return inputStream;
  }
  
  
  protected Document readDocument(String docName) throws SQLException, IOException {
    //System.out.println("JDBCCorp: read doc "+docName);
    Document doc = null;

    ResultSet rs = null;
    /* TEMPORARY
    String docEncoding = encoding;
    if (haveEncodingField) {
      getEncodingStatement.setString(1, docName);
      rs = getEncodingStatement.executeQuery();
      if(!rs.first()) {
        throw new GateRuntimeException("Could not retrieve encoding for "+docName);
      }
      if(!rs.last()) {
        throw new GateRuntimeException("More than one match for document "+docName);
      }
      docEncoding = rs.getString(1);
    }
    
    //System.out.println("Trying to get content for "+docName);
    getContentStatement.setString(1, docName);
    //System.out.println("After setString: "+getContentStatement);
    rs = getContentStatement.executeQuery();
    if (!rs.next()) {
      throw new GateRuntimeException("Document not found int the DB table: " + docName);
    }
    if (!rs.isLast()) {
      throw new GateRuntimeException("More than one row found for document name " + docName);
    }


    String content = null;
    if (getUseCompression()) {
      InputStream is = rs.getBinaryStream(1);
      InputStream isdec = null;
      isdec = new GZIPInputStream(is);
      String usedEncoding = getActiveEncoding(docEncoding);
      content = IOUtils.toString(isdec, usedEncoding);
      isdec.close();
      is.close();
    } else {
      content = rs.getString(1);
    }
    String docMimeType = mimeType;
    if (haveMimeTypeField) {
      getMimeTypeStatement.setString(1, docName);
      rs = getMimeTypeStatement.executeQuery();
      rs.first();
      mimeType = rs.getString(1);
    }
    FeatureMap params = Factory.newFeatureMap();
    params.put(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME, content);
    params.put(Document.DOCUMENT_ENCODING_PARAMETER_NAME, docEncoding);
    params.put(Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, docMimeType);
    try {
      doc =
        (Document) Factory.createResource(DocumentImpl.class.getName(),
        params, null, docName);
    } catch (Exception ex) {
      throw new GateRuntimeException("Exception creating the document", ex);
    }
    */
    return doc;
  }
  
  protected String getActiveEncoding(String docEncoding) {
    String usedEncoding = "UTF-8";  // if all else fails, use UTF8
    if(docEncoding != null && !docEncoding.isEmpty()) {
      usedEncoding = docEncoding;  // encoding from or for document overwrites all else
    } else if (encoding != null && !encoding.isEmpty()) { 
      usedEncoding = encoding; // use LR's encoding parameter
    } else if (System.getProperty("file.encoding") != null) {
      usedEncoding = System.getProperty("file.encoding");
    }
    return usedEncoding;
  }
  
  
  protected void removeDocument(String docName) {
    /* TEMPORARY
    if(getRemoveDocuments() && getSaveDocuments()) {
      try {
        deleteRowStatement.execute();
      } catch (SQLException ex) {
        throw new GateRuntimeException("Problem when trying to delete table row for document "+docName,ex);
      }
    }
    */
  }
  
  
  protected void adoptDocument(Document doc) {
    try {
      doc.setDataStore(ourDS);
      //System.err.println("Adopted document "+doc.getName());
    } catch (PersistenceException ex) {
      System.err.println("Got exception when adopting: "+ex);
    }
  }
  
  protected class JDBCCorpusIterator implements Iterator<Document> {
    int nextIndex = 0;
    @Override
    public boolean hasNext() {
      return (documentNames.size() > nextIndex);
    }
    @Override
    public Document next() {
      if(hasNext()) {
        return get(nextIndex++);
      } else {
        return null;
      }
    }
    @Override
    public void remove() {
      throw new MethodNotImplementedException();
    }    
  }
  
} // class JDBCCorpus

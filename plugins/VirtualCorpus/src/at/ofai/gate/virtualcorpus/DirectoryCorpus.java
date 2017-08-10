/*
 *  DirectoryCorpus.java
 *
 *
 * Copyright (c) 2010, Austrian Research Institute for
 * Artificial Intelligence (OFAI)
 *
 * This file is free
 * software, licenced under the GNU General Public License, Version 2
 *
 *  Johann Petrak, 30/8/2010
 *
 *  $Id: DirectoryCorpus.java 124 2014-04-24 18:23:51Z johann.petrak $
 */

package at.ofai.gate.virtualcorpus;

import java.io.IOException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collection;

import gate.*;
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
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

// TODO: use DocumentFormat.getSupportedFileSuffixes() to get the list of 
// supported input file extensions, unless the user limits those through 
// a parameter. 
// If we enable gzip-compression then we also add all the above extensions
// with .gz appended. 
// We allow to save back the files in the following formats: .xml .xml.gz and,
// if the plugin is loaded and finf is supported, finf. 
// QUESTION: is it possible to use a runtime-generated list as a default list
// for an init parameter to choose from / correct?

// BIGGER change: by default only support formats which can be written back,
// which would be xml, xml.gz and finf. In that case we may want to just 
// write back to the same file as we read from, no matter what.
// But if additional read/only extensions are specified, then we may want 
// to give the format to use for writing back? 
// OR: create different directory corpora: read only corpus which supports 
// all formats, but must save with "Save" option and ReadWrite corpus which 
// only supports the formats which can be used both for reading and writing.
// We could merge in the code from convertFormat to seperate out the 
// format conversion functionality!



/** 
 * A Corpus LR that mirrors files in a directory. In the default configuration,
 * just the <code>directoryURL</code> parameter is specified at creation and
 * all files that have a file extension of ".xml" and are not hidden are
 * accessible as documents through that corpus and automatically written back
 * to the directory when sync'ed or when unloaded (which does an implicit sync).
 * If the parameter <code>outDirectoryURL</code>
 * is also specified, the corpus reflects all the files from the 
 * <code>directoryURL</code> directory but writes any changed documents into
 * the directory <code>outDirectoryURL</code>. If the parameter 
 * <code>saveDocuments</code> is set to false, nothing is ever written
 * to either of the directories.
 * <p>
 * The main purpose of this Corpus implementation is that through it
 * a serial controller
 * can directly read and write from files stored in a directory. 
 * This makes it much easier to share working pipelines between pipeline
 * developers, especially when the pipeline files are checked into SCS.
 * <p>
 * This LR does not implement the following methods: 
 * <ul>
 * <li>toArray: none of the toArray methods is implemented. 
 * </ul>
 * If the parameter "transientCorpus" is false,
 * this corpus LR automatically uses a "dummy datastore" internally.
 * This datastore is created and removed automatically when the corpus LR is
 * created and removed. This datastore cannot be used for anything useful, it
 * does not allow listing of resources or storing of anything but documents
 * that are already in the corpus. It is mainly here because GATE assumes that
 * documents are either transient or from a datastore. To avoid documents from
 * a DirectoryCorpus to get treated as transient documents, their DataStore is
 * set to this dummy DataStore.
 * <p>
 * Documents will always get saved to either the original file or to a file
 * in the outDocumentURL directory whenever the document is synced or unloaded.
 * <p>
 * NOTE: If you use the "Save as XML" option from the LR's context menu, be
 * careful not specify the directory where the corpus saves documents as 
 * the target directory for the "Save as XML" function -- this might produce
 * unexpected results. Even if a different directory is specified, the 
 * "Save as XML" function will still also re-save the documents in the 
 * corpus directory unless the <code>saveDocuments</code> option is set to 
 * false.
 * 
 * @author Johann Petrak
 */
@CreoleResource(
    name = "DirectoryCorpus",
    interfaceName = "gate.Corpus", 
    icon = "corpus", 
    helpURL = "http://code.google.com/p/gateplugin-virtualcorpus/wiki/DirectoryCorpusUsage",
    comment = "A corpus backed by GATE documents in a directory or directory tree")
public class DirectoryCorpus  
  extends VirtualCorpus
  implements CreoleListener
  {

  //*****
  // Fields
  //******
  
  /**
   * 
   */
  private static final long serialVersionUID = -8485161260415382902L;

  // for accessing document name by index
  protected List<String> documentNames = new ArrayList<String>();
  // for checking if ith document is loaded
  protected List<Boolean> isLoadeds = new ArrayList<Boolean>();
  // for finding index for document name
  //REMOVE Map<String,Integer> documentIndexes = new HashMap<String,Integer>();
  
  protected Map<String,Document> loadedDocuments = new HashMap<String,Document>();
  
  protected File backingDirectoryFile;
  
  protected List<CorpusListener> listeners = new ArrayList<CorpusListener>();
  
  //***************
  // Parameters
  //***************
  
  /**
   * Setter for the <code>directoryURL</code> LR initialization parameter.
   * @param dirURL The URL of the directory where the files for the corpus will
   * be read
   * from. If the <code>outDirectoryURL</code> is left empty the documents
   * will be written back to the original files in this directory when
   * unloaded (except when <code>saveDocuments</code> is set to false).
   */
  @CreoleParameter(comment = "The directory URL where files will be read from")
  public void setDirectoryURL(URL dirURL) {
    this.directoryURL = dirURL;
  }
  /**
   * Getter for the <code>directoryURL</code> LR initialization parameter.
   *
   * @return The directory URL where files are read from and (and saved to
   * if unloaded when outDirectoryURL is not specified and saveDocuments
   * is true).
   */
  public URL getDirectoryURL() {
    return this.directoryURL;
  }
  protected URL directoryURL = null;

  /**
   * File extensions to use for loading document.
   * If this is not empty, then only files with that extension will be visible
   * in the corpus. If it is left empty, the file extensions supported by
   * the currently loaded document formats will be visible. 
   * Note that in both cases, any extension which does not have a document
   * exporter which supports that extension is ignored. 
   * The PR will check for each extension at init time, for which of those
   * there is a registered document exporter for saving and will only use
   * that exporter for any saving. 
   * 
   * @param extensions 
   */
  @Optional
  @CreoleParameter(comment = "A list of file extensions which will be loaded into the corpus. If not specified, all supported file extensions. ")
  public void setExtensions(List<String> extensions) {
    this.extensions = extensions;
  }
  public List<String> getExtensions() { return extensions; }
  protected List<String> extensions;

  @Optional
  @CreoleParameter(comment = "Recursively get files from the directory (default: false)",defaultValue="false")
  public void setRecurseDirectory(Boolean value) {
    this.recurseDirectory = value;
  }
  public Boolean getRecurseDirectory() { return recurseDirectory; }
  protected Boolean recurseDirectory;
  
  DummyDataStore4DirCorp ourDS = null;
  
  Map<String,DocumentExporter> extension2Exporter = new HashMap<String,DocumentExporter>();
  public static final Logger logger = Logger.getLogger(DirectoryCorpus.class);
  
  /**
   * Initializes the DirectoryCorpus LR
   * @return 
   * @throws ResourceInstantiationException
   */
  @Override
  public Resource init() 
    throws ResourceInstantiationException {
    logger.info("DirectoryCorpus: calling init");
    if(directoryURL == null) {
      throw new ResourceInstantiationException("directoryURL must be set");
    }
    // first of all, create a map that contains all the supported extensions
    // as keys and the corresponding documente exporter as value. 
    
    // First, get all the supported extensions for reading files
    Set<String> readExtensions = DocumentFormat.getSupportedFileSuffixes();
    logger.info("DirectoryCorpus/init readExtensions="+readExtensions);
    Set<String> supportedExtensions = new HashSet<String>();
    
    // if we also want to write, we have to limit the supported extensions
    // to those where we have an exporter and also we need to remember which
    // exporter supports which extensions
    if (!getReadonly()) {
      List<Resource> des = null;
      try {
        // Now get all the Document exporters
        des = Gate.getCreoleRegister().
                getAllInstances("gate.DocumentExporter");
      } catch (GateException ex) {
        throw new ResourceInstantiationException("Could not get the document exporters", ex);
      }
      for (Resource r : des) {
        DocumentExporter d = (DocumentExporter) r;
        if (readExtensions.contains(d.getDefaultExtension())) {
          extension2Exporter.put(d.getDefaultExtension(), d);
          supportedExtensions.add(d.getDefaultExtension());
        }
      }
    } else {
      supportedExtensions.addAll(readExtensions);
    }
    logger.info("DirectoryCorpus/init supportedExtensions=" + readExtensions);

    // now check if an extension list was specified by the user. If no, nothing
    // needs to be done. If yes, remove all the extensions from the extnesion2Exporter
    // map which were not specified and warn about all the extensions specified
    // for which we do not have an entry. Also remove them from the supportedExtensions set
    if(getExtensions() != null && !getExtensions().isEmpty()) {
      logger.info("DirectoryCorpu/init getExtgension is not empty: "+getExtensions());
      for(String ext : getExtensions()) {
        if(!supportedExtensions.contains(ext)) {
          logger.warn("DirectoryCorpus warning: extension is not supported: "+ext);
        }
      }
      // now remove all the extensions which are not specified
      Iterator<String> it = supportedExtensions.iterator();
      while(it.hasNext()) {
        String ext = it.next();
        logger.info("DirectoryCorpus/init checking supported extension: "+ext);
        if(!getExtensions().contains(ext)) {
          logger.info("DirectoryCorpus/init removing extension: "+ext);
          it.remove();
          extension2Exporter.remove(ext);
        }
      }
    }
    logger.info("DirectoryCorpus/init supportedExtensions after parms: "+supportedExtensions);
    logger.info("DirectoryCorpus/init exporter map: "+extension2Exporter);

    if(supportedExtensions.isEmpty()) {
      throw new ResourceInstantiationException("DirectoryCorpus could not be created, no file format supported or loaded");
    }
    
    backingDirectoryFile = Files.fileFromURL(directoryURL);
    try {
      backingDirectoryFile = backingDirectoryFile.getCanonicalFile();
    } catch (IOException ex) {
      throw new ResourceInstantiationException(
              "Cannot get canonical file for "+backingDirectoryFile,ex);
    }
    if(!backingDirectoryFile.isDirectory()) {
      throw new ResourceInstantiationException(
              "Not a directory "+backingDirectoryFile);
    }
    
    try {
        ourDS =
          (DummyDataStore4DirCorp) Factory.createDataStore("at.ofai.gate.virtualcorpus.DummyDataStore4DirCorp", backingDirectoryFile.getAbsoluteFile().toURI().toURL().toString());
        ourDS.setName("DummyDS4_" + this.getName());
        ourDS.setComment("Dummy DataStore for DirectoryCorpus " + this.getName());
        ourDS.setCorpus(this);
        //System.err.println("Created dummy corpus: "+ourDS+" with name "+ourDS.getName());
    } catch (Exception ex) {
        throw new ResourceInstantiationException(
          "Could not create dummy data store", ex);
    }
    logger.info("DirectoryCorpus/init: ds created: "+ourDS.getName());

    
    
    Iterator<File> fileIt = 
            FileUtils.iterateFiles(backingDirectoryFile, 
            supportedExtensions.toArray(new String[0]), getRecurseDirectory());
    int i = 0;
    while(fileIt.hasNext()) {
      File file = fileIt.next();
      // if recursion was specified, we need to get the relative file path
      // relative to the root directory. This is done by getting the canonical
      // full path name for both the directory and the file and then 
      // relativizing the path.
      String filename = file.getName();
      // TODO: first check if this file should be ignored (hidden files?)
      if(!filename.startsWith(".")) {
        if(getRecurseDirectory()) {
          try {
            file = file.getCanonicalFile();
          } catch (IOException ex) {
            throw new ResourceInstantiationException("Could not get canonical path for "+file);
          }
          filename = backingDirectoryFile.toURI().relativize(file.toURI()).getPath();
        }
        documentNames.add(filename);
        isLoadeds.add(false);
        documentIndexes.put(filename, i);
        i++;
      }
    }
    if(i==0) {
      logger.warn("DirectoryCorpus warning: empty immutable corpus created, no files found");
    }
    try {
      PersistenceManager.registerPersistentEquivalent(
          at.ofai.gate.virtualcorpus.DirectoryCorpus.class,
          at.ofai.gate.virtualcorpus.DirectoryCorpusPersistence.class);
    } catch (PersistenceException e) {
      throw new ResourceInstantiationException(
              "Could not register persistence",e);
    }
    Gate.getCreoleRegister().addCreoleListener(this);
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
   * Unload a document from the corpus. 
   * This mimics what SerialCorpusImpl does: the document gets synced which
   * in turn will save the document, then it gets removed from memory.
   * Syncing will make our dummy datastore to invoke our own saveDocument
   * method. The saveDocument method determines if the document should really
   * be saved and how.
   *
   * @param doc
   */
  @Override
  public void unloadDocument(Document doc) {
    unloadDocument(doc,true);
  }
  
  // NOTE: unfortunately this method, like the unloadDocument(int) methods
  // is not in the Corpus interface. 
  public void unloadDocument(Document doc, boolean sync) {
    String docName = doc.getName();
    System.out.println("DirCorp: called unloadDocument: "+docName);
    Integer index = documentIndexes.get(docName);
    if(index == null) {
      throw new RuntimeException("Document "+docName+
              " is not contained in corpus "+this.getName());
    }
    if(isDocumentLoaded(index)) {
      if(sync) { 
        try { 
          doc.sync();
        } catch (Exception ex) {
          throw new GateRuntimeException("Problem syncing document "+doc.getName(),ex);
        }
      }
      loadedDocuments.remove(docName);
      isLoadeds.set(index, false);
      //System.err.println("Document unloaded: "+docName);
    } // else silently do nothing
  }
  
  
  @Override
  public void removeCorpusListener(CorpusListener listener) {
    listeners.remove(listener);
  }
  @Override
  public void addCorpusListener(CorpusListener listener) {
    listeners.add(listener);
  }

  /**
   * Get the list of document names in this corpus.
   * This returns a list of the document names contained in the corpus.
   * Modifying this list will have no effect on the corpus.
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
  @Override
  public String getDocumentName(int i) {
    return documentNames.get(i);
  }

  /**
   * @return
   */
  @Override
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
  @Override
  public void setDataStore(DataStore ds) throws PersistenceException {
    throw new PersistenceException("Corpus "+this.getName()+
            " cannot be saved to a datastore");
  }

  /**
   * This returns false because the corpus itself cannot be in a modified,
   * unsaved state.
   * 
   * For now all DirectoryCorpus objects are immutable: the list of documents
   * cannot be changed. Therefore, there is no way to modfy the corpus LR.
   * However, even if documents can be added or removed at some point, 
   * these changes will be immediately reflected in the backing directory,
   * so there is no way to modify the corpus and not have these changes 
   * "saved" or "synced". 
   * The bottom line is that this will always return false.
   * 
   * @return always false
   */
  @Override
  public boolean isModified() {
    return false;
  }

  /**
   * Syncing the corpus does nothing.
   * For an immutable corpus, there is nothing that would ever need to 
   * get synced (saved) and for a mutable corpus, all changes are saved
   * immediately so "syncing" is never necessary.
   * NOTE: syncing the corpus itself does not affect and should not affect
   * any documents which still may be modified and not synced.
   */
  @Override
  public void sync() {
    // do nothing.
  }


  @Override
  public void cleanup() {
    // TODO:
    // deregister our listener for resources of type document
    //
    Gate.getDataStoreRegister().remove(ourDS);
  }

  /**
   * Set the name of the DirectoryCorpus.
   * Note that this can be called by the factory before init is run!
   * @param name 
   */
  @Override
  public void setName(String name) {
    logger.info("DirectoryCorpus: calling setName with "+name);
    super.setName(name);
    // If we get called before init, there will be no DS yet, so no need
    // to rename it!
    if(ourDS != null) {
      ourDS.setName("DummyDS4_"+this.getName());
      ourDS.setComment("Dummy DataStore for DirectoryCorpus "+this.getName());
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
   * IMPORTANT: this is NOT IMPLEMENTED at the moment!
   */
  @Override
  public boolean add(Document doc) {
    throw new MethodNotImplementedException(notImplementedMessage("add(Document doc)"));
  }
  /*
  public boolean add(Document doc) {
    if(!saveDocuments) {
      return false;
    }
    //System.out.println("DocCorp: called add(Object): "+doc.getName());
    String docName = doc.getName();
    ensureValidDocumentName(docName,true);
    Integer index = documentIndexes.get(docName);
    if(index != null) {
      return false;  // if that name is already in the corpus, do not add
    } else {
      // for now, we do not allow any document to be added that is already
      // adopted by a datastore.
      if(doc.getDataStore() != null) {
        throw new GateRuntimeException("Cannot add "+doc.getName()+" which belongs to datastore "+doc.getDataStore().getName());
      }
      saveDocument(doc);
      int i = documentNames.size();
      documentNames.add(docName);
      documentIndexes.put(docName, i);
      isLoadeds.add(false);
      if(!isTransientCorpus) {
        adoptDocument(doc);
      }
      fireDocumentAdded(new CorpusEvent(
          this, doc, i, CorpusEvent.DOCUMENT_ADDED));
      
      return true;
    }
  }
  */


  /**
   * This removes all documents from the corpus. Note that this does nothing
   * when the saveDocuments parameter is set to false.
   * If the outDirectoryURL parameter was set, this method will throw
   * a GateRuntimeException.
   * IMPORTANT: this is not implemented at the moment!!
   */
  @Override
  public void clear() {
    throw new MethodNotImplementedException(notImplementedMessage("clear()"));
  }
  /*
  public void clear() {
    if(!saveDocuments) {
      return;
    }
    if(outDirectoryURL != null) {
      throw new GateRuntimeException(
              "clear method not supported when outDirectoryURL is set for "+
              this.getName());
    }
    for(int i=documentNames.size()-1; i>=0; i--) {
      remove(i);
    }
  }
  */
  
  /**
   * This checks if a document with the same name as the document
   * passed is already in the corpus. 
   * IMPORTANT: The content is not considered 
   * for this, only the name is relevant!
   */
  @Override
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
  @Override
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
    Document doc = readDocument(docName);
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
  @Override
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
  @Override
  public boolean isEmpty() {
    return (documentNames.isEmpty());
  }

  /**
   * Returns an iterator to iterate through the documents of the
   * corpus. The iterator does not allow modification of the corpus.
   * 
   * @return
   */
  @Override
  public Iterator<Document> iterator() {
    return new DirectoryCorpusIterator();
  }

  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   * 
   * @param docObj
   * @return
   */
  @Override
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
  @Override
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
  @Override
  public ListIterator<Document> listIterator(int i) {
    throw new MethodNotImplementedException(
            notImplementedMessage("listIterator(int)"));
  }

  /**
   * Removes the document with the given index from the corpus. This is not
   * supported and throws a GateRuntimeException if the outDirectoryURL
   * was specified for this corpus. If the saveDocuments parameter is false
   * for this corpus, this method does nothing.
   * A document which is removed from the corpus will have its dummy
   * datastore removed and look like a transient document again. 
   * 
   * IMPORTANT: this is NOT IMPLEMENTED yet!
   * 
   * @param index
   * @return the document that was just removed from the corpus
   */
  @Override
  public Document remove(int index) {
    throw new MethodNotImplementedException(notImplementedMessage("remove(int index)"));
  }
  /*
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
    if (!isTransientCorpus) {
      try {
        doc.setDataStore(null);
      } catch (PersistenceException ex) {
        // this should never happen
      }
    }
    fireDocumentRemoved(new CorpusEvent(
        this, doc,
        index, CorpusEvent.DOCUMENT_REMOVED));
    return doc;
  }
  */

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
  @Override
  public boolean remove(Object docObj) {
    throw new MethodNotImplementedException(notImplementedMessage("remove(Object docObj)"));
  }
  /*
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
    if (!isTransientCorpus) {
      try {
        doc.setDataStore(null);
      } catch (PersistenceException ex) {
        // this should never happen
      }
    }
    fireDocumentRemoved(new CorpusEvent(
        this, doc,
        index, CorpusEvent.DOCUMENT_REMOVED));
    return true;
  }
  */

  /**
   * Remove all the documents in the collection from the corpus.
   *
   * @param coll
   * @return true if any document was removed
   */
  @Override
  public boolean removeAll(Collection coll) {
    throw new MethodNotImplementedException(notImplementedMessage("removeAll(Collection coll)"));
  }
  /*
  public boolean removeAll(Collection coll) {
    boolean ret = false;
    for(Object docObj : coll) {
      ret = ret || remove(docObj);
    }
    return ret;
  }
  */

  
  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   * 
   * @param index
   * @param obj
   * @return
   */
  @Override
  public Document set(int index, Document obj) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("set(int,Object)"));
  }
  
  @Override
  public int size() {
    return documentNames.size();
  }

  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   *
   * @param i1
   * @param i2
   * @return
   */
  @Override
  public List<Document> subList(int i1, int i2) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("subList(int,int)"));
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

  @Override
  public void resourceLoaded(CreoleEvent e) {
  }

  @Override
  public void resourceRenamed(
          Resource resource,
          String oldName,
          String newName) {
    // if one of our documents gets renamed, rename it back and
    // write an error message
    if(resource instanceof Document) {
      Document doc = (Document)resource;
      if(loadedDocuments.containsValue(doc)) {
        System.err.println("ERROR: documents from a directory corpus cannot be renamed!");
        doc.setName(oldName);
      }
    }
  }

  @Override
  public void resourceUnloaded(CreoleEvent e) {
    Resource res = e.getResource();
    if(res instanceof Document) {
      Document doc = (Document)res;
      // check if this document has actually been loaded by us
      if(loadedDocuments.containsValue(doc)) {
        unloadDocument(doc);
      } // else: its not ours, ignore
    } else if(res == this) {
      // if this corpus object gets unloaded, what should we do with any 
      // of the documents which are currently loaded?
      // TODO!!!!
      // Also should we not do the cleanup in the cleanup code?
      Gate.getCreoleRegister().removeCreoleListener(this);
    }
  }

  @Override
  public void datastoreClosed(CreoleEvent ev) {
  }
  
  @Override
  public void datastoreCreated(CreoleEvent ev) {
    
  }
  
  @Override
  public void datastoreOpened(CreoleEvent ev) {
    
  }
  
  //**************************
  // helper methods
  // ************************
  
  // This method should only get called by the datastore when a document
  // is synced. This will happen automatically when a document is unloaded
  // or when a document is deliberately synced via its datastore. 
  protected void saveDocument(Document doc) {
    //System.out.println("DirCorp: save doc "+doc.getName());
    // If the corpus is read-only, nothing gets saved
    if(getReadonly()) {
      return;
    }
    String docName = doc.getName();
    // get the extension and then look up the document exporter for that
    // extension which will be used to do the actual saving.
    int extDotPos = docName.lastIndexOf(".");
    if(extDotPos <= 0) {
      throw new GateRuntimeException("Did not find a file name extensions when trying to save document "+docName);
    }
    String ext = docName.substring(extDotPos+1);
    if(ext.isEmpty()) {
      throw new GateRuntimeException("Encountered empty extension when trying to save document "+docName);
    }
    DocumentExporter de = extension2Exporter.get(ext);
    logger.info("DirectoryCorpus/saveDocument exit is "+ext+" exporter "+de);
    File docFile = new File(backingDirectoryFile, docName);
    try {
      logger.info("DirectoryCorpus/saveDocument trying to save document "+doc.getName()+" using exporter "+de);
      de.export(doc, docFile);
      logger.info("DirectoryCorpus/saveDocument saved: "+doc.getName());
    } catch (IOException ex) {
      throw new GateRuntimeException("Could not save file: "+docFile,ex);
    }
  }
  
  protected Document readDocument(String docName) {
    //System.out.println("DirCorp: read doc "+docName);
    File docFile = new File(backingDirectoryFile, docName);
    URL docURL;
    Document doc = null;
    try {
      docURL = docFile.toURI().toURL();
    } catch (MalformedURLException ex) {
      throw new GateRuntimeException(
              "Could not create URL for document name "+docName,ex);
    }
    FeatureMap params = Factory.newFeatureMap();
    params.put(Document.DOCUMENT_URL_PARAMETER_NAME,docURL);
    try {
       doc =
          (Document) Factory.createResource(
            DocumentImpl.class.getName(),
            params, null, docName);
    } catch (ResourceInstantiationException ex) {
        throw new GateRuntimeException(
          "Could not create Document from file " + docFile, ex);
    }
    return doc;
  }

  // NOTE: not used at the moment, our corpus is always immutable so far!
  /*
  protected void removeDocument(String docName) {
    File docFile = new File(backingDirectoryFile, docName);
    docFile.delete();
  }
  */
  

  protected void adoptDocument(Document doc) {
    try {
      doc.setDataStore(ourDS);
      //System.err.println("Adopted document "+doc.getName());
    } catch (PersistenceException ex) {
      //System.err.println("Got exception when adopting: "+ex);
    }
  }
  
  protected class DirectoryCorpusIterator implements Iterator<Document> {
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


} // class DirectoryCorpus

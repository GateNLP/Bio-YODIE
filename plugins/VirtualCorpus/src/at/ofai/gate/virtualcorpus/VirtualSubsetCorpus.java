/*
 *  VirtualSubsetCorpus.java
 *
 *
 * Copyright (c) 2010, Austrian Research Institute for
 * Artificial Intelligence (OFAI)
 * http://www.ofai.at
 *
 * This file is free
 * software, licenced under the GNU General Public License, Version 2
 *
 *  Johann Petrak, 30/8/2010
 *
 *  $Id: DirectoryCorpus.java 42 2011-02-07 12:56:46Z johann.petrak $
 */

package at.ofai.gate.virtualcorpus;

import java.io.FileFilter;
import java.net.URL;
import java.util.*;

import gate.*;
import gate.creole.*;
import gate.event.CorpusEvent;
import gate.event.CorpusListener;
import gate.event.CreoleEvent;
import gate.event.CreoleListener;
import gate.persist.PersistenceException;
import gate.util.*;

/** 
 * A corpus that contains a subset of the documents of an existing
 * VirtualCorpus (its "parent corpus"). 
 * 
 * Only documents that are in the
 * parent corpus can be added, and removing a document from the SubsetCorpus
 * will not remove any documents from the parent corpus, nor will it remove the
 * original file or database table row which backs the document. 
 * A Subset Corpus is essentially
 * just a view into the parent corpus.
 * Its main purpose is to support the Learning plugin
 * with a VirtualCorpus and other situations where one wants to create 
 * corpora from documents in an existing corpus for subsetting, sampling etc.
 * <p>
 * NOTE: all non-subset corpora are currently immutable!
 * <p>
 * Removing a SubsetCorpus will not remove the datastore. The normal way
 * to create a SubsetCorpus is by adopting a new and empty transient corpus
 * to the datastore of an existing JDBCCorpus. However, it can also
 * be instantiated directly (the only required parameter is an existing
 * JDBCCorpus).
 * 
 * @author Johann Petrak
 */
public abstract class VirtualSubsetCorpus  extends AbstractLanguageResource
  implements Corpus, CreoleListener
  {

  //*****
  // Fields
  //******
  
  /**
   * 
   */

  // for accessing document name by index
  protected List<String> documentNames = new ArrayList<String>();
  protected List<Integer> originalIndexes = new ArrayList<Integer>();
  // for checking if ith document is loaded
  //protected List<Boolean> isLoadeds = new ArrayList<Boolean>();
  // for finding index for document name
  protected Map<String,Integer> documentIndexes = new HashMap<String,Integer>();



  //protected Map<String,Document> loadedDocuments = new HashMap<String,Document>();
  
  protected List<CorpusListener> listeners = new ArrayList<CorpusListener>();

  protected VirtualCorpus virtualCorpus;
  
  /**
   * Initializes the JDBCSubsetCorpus LR
   * @return 
   * @throws ResourceInstantiationException
   */
  @Override
  public Resource init() 
    throws ResourceInstantiationException {
    if(virtualCorpus == null) {
      throw new ResourceInstantiationException("jdbcCorpus must be set");
    }
    if(virtualCorpus.getDataStore() == null) {
      throw new ResourceInstantiationException("jdbcCorpus must not be transient");
    }
    // listen for events on this corpus
    // TODO: listen for events on the parent corpus too!
    Gate.getCreoleRegister().addCreoleListener(this);
    return this;
  }
  
  /**
   * Test if the document with the given index is loaded. If an index is
   * specified that is not in the corpus, a GateRuntimeException is thrown.
   * <p>
   * This is handled entirely by the parent JDBCCorpus but
   * is valid only if requested for a document that had been added
   * to this corpus.
   * @param index 
   * @return true if the document is loaded, false otherwise. 
   */
  public boolean isDocumentLoaded(int index) {
    if(index < 0 || index >= originalIndexes.size()) {
      throw new GateRuntimeException("Document number "+index+
              " not in corpus "+this.getName()+" of size "+originalIndexes.size());
    }    
    return virtualCorpus.isDocumentLoaded(originalIndexes.get(index));
  }

  public boolean isDocumentLoaded(Document doc) {
    String docName = doc.getName();
    //System.out.println("DirCorp: called unloadDocument: "+docName);
    Integer index = documentIndexes.get(docName);
    if(index == null) {
      throw new RuntimeException("Document "+docName+
              " is not contained in corpus "+this.getName());
    }
    return this.isDocumentLoaded(index);
  }

  /**
   * @param doc
   */
  public void unloadDocument(Document doc) {

    String docName = doc.getName();
    Integer index = documentIndexes.get(docName);
    if(index == null) {
      throw new RuntimeException("Document "+docName+
              " is not contained in corpus "+this.getName());
    }
    virtualCorpus.unloadDocument(doc);
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

  public void populate(
      URL directory, FileFilter filter,
      String encoding, boolean recurseDirectories) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("populate(URL, FileFilter, boolean)"));
  }


   public void populate (
      URL directory, FileFilter filter,
      String encoding, String mimeType,
      boolean recurseDirectories) {
   throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("populate(URL, FileFilter, String, String, boolean"));
  }

  public long populate(
      URL trecFile, String encoding, int numberOfDocumentsToExtract) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("populate(URL, String, int"));
  }

  public long populate(URL url, String docRoot, String encoding, int nrdocs,
      String docNamePrefix, String mimetype, boolean includeroot) {
    throw new gate.util.MethodNotImplementedException(
        notImplementedMessage("populate(URL, String, String, int, String, String, boolean"));
  }


  /**
   * @return
   */
  public DataStore getDataStore() {
    //System.out.println("getDataStore called, returning: "+directoryCorpus.getDataStore());
    return virtualCorpus.getDataStore();
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
   * This follows the convention for transient corpus objects and always
   * returns false.
   * 
   * @return always false
   */
  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void sync() {
    // TODO: save the document!?!?
  }


  @Override
  public void cleanup() {
    // TODO:
    // deregister our listener for resources of type document
    //
    Gate.getCreoleRegister().removeCreoleListener(this);
  }


  // Methods to be implemented from List

  /**
   * This method is not implemented and throws 
   * a gate.util.MethodNotImplementedException
   * 
   * @param index
   * @param docObj
   */
  public void add(int index, Document docObj) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("add(int,Object)"));
  }
  /**
   * Add a document to the corpus. If the document has a name that is already
   * in the list of documents, return false and do not add the document.
   * Note that only the name is checked!
   * Otherwise check if the document is in the parent corpus, if yes, add
   * it to this corpus by adding the name and the original index.
   */
  public boolean add(Document doc) {
    //System.out.println("DocCorp: called add(Object): "+doc.getName());
    String docName = doc.getName();
    Integer index = documentIndexes.get(docName);
    if(index != null) {
      return false;  // if that name is already in the corpus, do not add
    } else {
      // get the index of this document in the parent corpus
      Integer parentIndex = virtualCorpus.documentIndexes.get(docName);
      if(parentIndex == null) {
        throw new GateRuntimeException(
          "Attempt to add a document to JDBCSubsetCorpus "+
          this.getName()+
          " but no document with that name is in the parent jdbc corpus "+
          virtualCorpus.getName());
      }
      int i = documentNames.size();
      documentNames.add(docName);
      originalIndexes.add(parentIndex);
      documentIndexes.put(docName, i);
      fireDocumentAdded(new CorpusEvent(
          this, doc, i, CorpusEvent.DOCUMENT_ADDED));
      
      return true;
    }
  }

  /**
   * This method is not implemented and throws 
   * a gate.util.MethodNotImplementedException
   * 
   * @param index
   * @param c
   * @return
   */
  public boolean addAll(int index, Collection c) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("addAll(int,Collection)"));
  }

  /**
   * Add all documents in the collection to the end of the corpus.
   * Documents with a name that is already in the corpus are not added.
   * 
   * @param c a collection of documents
   * @return true if any document from the corpus was added.
   */
  public boolean addAll(Collection c) {
    boolean ret = false;
    for(Object obj : c) {
      ret = ret || this.add((Document)obj);
    }
    return ret;
  }

  /**
   * This removes all documents from the corpus. Note that this does nothing
   * when the saveDocuments parameter is set to false.
   */
  public void clear() {
    for(int i=documentNames.size()-1; i>=0; i--) {
      remove(i);
    }
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
   * This method is not implemented and throws
   * a gate.util.MethodNotImplementedException
   * 
   * @param c
   * @return
   */
  public boolean containsAll(Collection c) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("containsAll(Collection)"));
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
    return virtualCorpus.get(originalIndexes.get(index));
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
    return (documentNames.isEmpty());
  }

  /**
   * Returns an iterator to iterate through the documents of the
   * corpus. The iterator does not allow modification of the corpus.
   * 
   * @return
   */
  public Iterator<Document> iterator() {
    return new VirtualSubsetCorpusIterator();
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
   * Remove a document from this corpus. This will just remove it
   * from the view but always leave the files and documents in the
   * parent corpus untouched!
   * <p>
   * NOTE: the document removed is not unloaded either!
   * <p>
   * CAUTION: for now returns null, not the original document!!!
   * <p>
   * @param index
   * @return the document that was just removed from the corpus
   */
  public Document remove(int index) {
    if(index < 0 || index >= documentNames.size()) {
      throw new GateRuntimeException("Attempt to remove document with index "+
        index+
        "from JDBCSubsetCorpus "+this.getName()+
        " of size "+documentNames.size());
    }
    String docName = documentNames.get(index);
    documentNames.remove(index);
    originalIndexes.remove(index);
    documentIndexes.remove(docName);
    fireDocumentRemoved(new CorpusEvent(
        this, null,  // can we get away with using null instead of doc?
        index, CorpusEvent.DOCUMENT_REMOVED));
    return null; // can we get away with using null instead of doc???
  }

  /**
   * 
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
    originalIndexes.remove(index);
    documentIndexes.remove(docName);
    fireDocumentRemoved(new CorpusEvent(
        this, null,
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

  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   *
   * @param coll
   * @return
   */
  public boolean retainAll(Collection coll) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("retainAll(Collection)"));
  }
  
  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   * 
   * @param index
   * @param obj
   * @return
   */
  public Document set(int index, Document obj) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("set(int,Object)"));
  }
  
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
  public List<Document> subList(int i1, int i2) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("subList(int,int)"));
  }

  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   *
   * @return
   */
  public Object[] toArray() {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("toArray()"));
  }

  /**
   * This method is not implemented and always throws a
   * MethodNotImplementedException.
   *
   * @param a
   * @return
   */
  public Object[] toArray(Object[] x) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("toArray()"));
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
  }

  public void resourceRenamed(
          Resource resource,
          String oldName,
          String newName) {
    // do nothing and let the parent corpus handle this!
  }

  public void resourceUnloaded(CreoleEvent e) {
    Resource res = e.getResource();
    if(res == this) {
      Gate.getCreoleRegister().removeCreoleListener(this);
    }
  }

  public void datastoreClosed(CreoleEvent ev) {
  }
  
  public void datastoreCreated(CreoleEvent ev) {
    
  }
  
  public void datastoreOpened(CreoleEvent ev) {
    
  }
    
  protected class VirtualSubsetCorpusIterator implements Iterator<Document> {
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
  
  protected String notImplementedMessage(String methodName) {
    return "Method "+methodName+" not implemented for corpus "+
            this.getName()+" of class "+this.getClass();
  }  
  
} // class JDBCCorpus

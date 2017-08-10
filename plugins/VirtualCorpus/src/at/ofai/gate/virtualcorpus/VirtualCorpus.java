package at.ofai.gate.virtualcorpus;


import gate.Corpus;
import gate.Document;
import gate.creole.*;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.HiddenCreoleParameter;
import gate.creole.metadata.Optional;
import java.io.FileFilter;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 
 * 
 * @author Johann Petrak
 */
public abstract class VirtualCorpus
  extends AbstractLanguageResource implements Corpus
{

  /**
   * Setter for the <code>immutable</code> LR initialization parameter.
   *
   * @param immutable If set to true, the corpus list cannot be changed, i.e.
   * documents cannot be removed or deleted. All methods which would otherwise
   * change the corpus content are silently ignored.
   * 
   * NOTE: For now this is hidden and all instances of virtual corpora are
   * immutable! This parameter may get removed in the future and all VirtualCorpus
   * instances may forever remain immutable!
   *
   */
  @Optional
  @HiddenCreoleParameter
  @CreoleParameter(comment="if true, the corpus content cannot be changed, documents cannot be added or removed",
    defaultValue="true")
  public void setImmutable(Boolean immutable) {
    this.immutable = immutable;
  }
  public Boolean getRemoveDocuments() {
    return this.immutable;
  }
  protected Boolean immutable = true;

  /**
   * Setter for the <code>readonly</code> LR initialization parameter.
   *
   * @param readonly If set to true, documents will never be saved back. All
   * methods which would otherwise cause a document to get saved are silently
   * ignored.
   */
  @Optional
  @CreoleParameter(comment="If true, documents will never be saved",
    defaultValue="false")
  public void setReadonly(Boolean readonly) {
    this.readonly = readonly;
  }
  public Boolean getReadonly() {
    return this.readonly;
  }
  protected Boolean readonly = true;

  
  public void populate( // OK
      URL directory, FileFilter filter,
      String encoding, boolean recurseDirectories) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("populate(URL, FileFilter, boolean)"));
  } 


  
  public long populate(URL url, String docRoot, String encoding, int nrdocs,
      String docNamePrefix, String mimetype, boolean includeroot) {
    throw new gate.util.MethodNotImplementedException(
        notImplementedMessage("populate(URL, String, String, int, String, String, boolean"));
  }


   public void populate ( // OK
      URL directory, FileFilter filter,
      String encoding, String mimeType,
      boolean recurseDirectories) {
   throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("populate(URL, FileFilter, String, String, boolean"));
  }

  /**
   * @param trecFile
   * @param encoding
   * @param numberOfDocumentsToExtract
   * @return
   */
  public long populate(
      URL trecFile, String encoding, int numberOfDocumentsToExtract) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("populate(URL, String, int"));
  }


  protected String notImplementedMessage(String methodName) {
    return "Method "+methodName+" not supported for VirtualCorpus corpus "+
            this.getName()+" of class "+this.getClass();
  }

  /**
   * For mapping document names to document indices.
   */
  Map<String,Integer> documentIndexes = new HashMap<String,Integer>();

  @Override
  public abstract boolean isDocumentLoaded(int index);
  @Override
  public abstract void unloadDocument(Document doc);
  @Override
  public abstract Document get(int index);
  
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
   * @return
   */
  public Object[] toArray(Object[] x) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("toArray()"));
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
      if(obj instanceof Document) {
        ret = ret || this.add((Document)obj);
      } 
    }
    return ret;
  }

  /**
   * Not implemented.
   * @param i
   * @param c
   * @return 
   */
  public boolean addAll(int i, Collection c) {
    throw new gate.util.MethodNotImplementedException(
            notImplementedMessage("set(int,Object)"));
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
  
  
} // abstract class VirtualCorpus

package at.ofai.gate.virtualcorpus;

import gate.DataStore;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.persistence.LRPersistence;

/**
 * Persistence for the DirectoryCorpus LR.
 * The standard Corpus Persistence won't do as it either expects a persistent
 * corpus in which case it must have a Datastore, or a transient corpus in
 * which case all the documents are serialized too. We do not want either and
 * just serialize the initialization parameters so the LR will be recreated
 * in an identical way when loaded.
 * 
 * @author Johann Petrak
 */
public class DirectoryCorpusPersistence extends LRPersistence {
  public static final long serialVersionUID = 1L;
  /**
   * Populates this Persistence with the data that needs to be stored from the
   * original source object.
   */
  @Override
  public void extractDataFromSource(Object source)
    throws PersistenceException{
    if(! (source instanceof DirectoryCorpus)){
      throw new UnsupportedOperationException(
                getClass().getName() + " can only be used for " +
                DirectoryCorpus.class.getName() +
                " objects!\n" + source.getClass().getName() +
                " is not a " + DirectoryCorpus.class.getName());
    }

    DirectoryCorpus corpus = (DirectoryCorpus)source;
    DataStore ds = corpus.getDataStore();
    super.extractDataFromSource(source);
    corpus.setDataStore(ds);
  }


  /**
   * Creates a new object from the data contained. This new object is supposed
   * to be a copy for the original object used as source for data extraction.
   */
  @Override
  public Object createObject()throws PersistenceException,
                                     ResourceInstantiationException{
    DirectoryCorpus corpus = (DirectoryCorpus)super.createObject();
    return corpus;
  }
}


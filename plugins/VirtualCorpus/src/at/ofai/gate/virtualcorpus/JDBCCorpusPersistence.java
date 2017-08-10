/*
 *  JDBCCorpusPersistence.java
 *
 * Copyright (c) 2010, Austrian Research Institute for
 * Artificial Intelligence (OFAI)
 *
 * This file is free
 * software, licenced under the GNU General Public License, Version 2
 *
 *  Johann Petrak, 30/8/2010
 *
 *  $Id: JDBCCorpusPersistence.java 12 2011-02-06 00:03:07Z johann.petrak $
 */
package at.ofai.gate.virtualcorpus;

import gate.DataStore;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.persistence.LRPersistence;

/**
 * Persistence for the JDBCCorpus LR.
 * The standard Corpus Persistence won't do as it either expects a persistent
 * corpus in which case it must have a Datastore, or a transient corpus in
 * which case all the documents are serialized too. We do not want either and
 * just serialize the initialization parameters so the LR will be recreated
 * in an identical way when loaded.
 * 
 * @author Johann Petrak
 */
public class JDBCCorpusPersistence extends LRPersistence {
  public static final long serialVersionUID = 2L;
  /**
   * Populates this Persistence with the data that needs to be stored from the
   * original source object.
   */
  public void extractDataFromSource(Object source)
    throws PersistenceException{
    if(! (source instanceof JDBCCorpus)){
      throw new UnsupportedOperationException(
                getClass().getName() + " can only be used for " +
                JDBCCorpus.class.getName() +
                " objects!\n" + source.getClass().getName() +
                " is not a " + JDBCCorpus.class.getName());
    }

    JDBCCorpus corpus = (JDBCCorpus)source;
    // Fake that this LR does not have a DS, that will save it without
    // DS persistence information which will make it possible to restore it
    // without a DS and create the dummy DS ourselves at init time.
    DataStore ds = corpus.getDataStore();
    corpus.setDataStore(null);
    super.extractDataFromSource(source);
    corpus.setDataStore(ds);
  }


  /**
   * Creates a new object from the data contained. This new object is supposed
   * to be a copy for the original object used as source for data extraction.
   */
  public Object createObject()throws PersistenceException,
                                     ResourceInstantiationException{
    JDBCCorpus corpus = (JDBCCorpus)super.createObject();
    return corpus;

  }
}


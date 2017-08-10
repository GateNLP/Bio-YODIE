/*
 *  DummyDataStore4Virtuals.java
 *
 * Copyright (c) 2010, Austrian Research Institute for
 * Artificial Intelligence (OFAI)
 *
 * This file is free
 * software, licenced under the GNU General Public License, Version 2
 *
 *  Johann Petrak, 30/8/2010
 *
 *  $Id: DummyDataStore4Virtuals.java 123 2014-04-24 17:03:56Z johann.petrak $
 */

package at.ofai.gate.virtualcorpus;

import gate.DataStore;
import gate.FeatureMap;
import gate.LanguageResource;
import gate.event.DatastoreListener;
import gate.persist.PersistenceException;
import java.util.List;

/**
 * 
 * @author johann
 */
public abstract class DummyDataStore4Virtuals
implements DataStore {

  String storageURL = "";
  String myName = "";
  FeatureMap myFeatures = null;

  public void setStorageUrl(String storageUrl) throws PersistenceException {
    this.storageURL = storageUrl;
  }

  public String getStorageUrl() {
    return storageURL;
  }

  /**
   * This does nothing.
   * @throws PersistenceException
   * @throws UnsupportedOperationException
   */
  public void create() throws PersistenceException, UnsupportedOperationException {
    //
  }

  /**
   * This does nothing.
   * @throws PersistenceException
   */
  public void open() throws PersistenceException {
  }

  /**
   * This does nothing (a dummy datastore cannot be closed ... it exists
   * as long as the LR it was created for exists).
   * 
   * @throws PersistenceException
   */
  public void close() throws PersistenceException {
  }

  /**
   * This does nothing, see close.
   * @throws PersistenceException
   * @throws UnsupportedOperationException
   */
  public void delete() throws PersistenceException, UnsupportedOperationException {
  }

  /**
   * This does nothing. The dummy datastore does not actually hold or manage
   * any resources. If deleting is supported at all, it will be implemented
   * by the resource that created this dummy datastore.
   * 
   * @param lrClassName
   * @param lrId
   * @throws PersistenceException
   */
  public void delete(String lrClassName, Object lrId) throws PersistenceException {
    if(lrClassName.equals("at.ofai.gate.virtualcorpus.DirectorySubsetCorpus") ||
       lrClassName.equals("at.ofai.gate.virtualcorpus.JDBCSubsetCorpus")) {
      // ignore this
    } else {
      throw new UnsupportedOperationException(
        "Datastore "+this.getName()+
        " delete not supported for class="+lrClassName+" id="+lrId);
    }
  }

  /**
   * This will pass on the resquest to save the language resource to the
   * object that created this dummy datastore, if the language resource
   * comes from that object. Otherwise a Persistence Exception is thrown.
   * 
   * @param lr
   * @throws PersistenceException
   */
  public abstract void sync(LanguageResource lr) throws PersistenceException;

  /**
   * This does nothing.
   * 
   * @param autoSaving
   * @throws UnsupportedOperationException
   * @throws PersistenceException
   */
  public void setAutoSaving(boolean autoSaving) throws UnsupportedOperationException, PersistenceException {
  }

  /**
   * This always returns false as autosaving is not supported at all.
   * @return
   */
  public boolean isAutoSaving() {
    return false;
  }

  /**
   * If the object that created this dummy datastore can handle new LRs,
   * the document will be added to the object. The LR will always be a
   * document. If adding a new document is not supported a persistence exception
   * is thrown.
   * 
   * @param lr
   * @param secInfo
   * @return
   * @throws PersistenceException
   */
  public abstract LanguageResource adopt(LanguageResource lr) throws PersistenceException;

  public LanguageResource getLr(String lrClassName, Object lrId) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  public List getLrTypes() throws PersistenceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  public List getLrIds(String lrType) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  public List getLrNames(String lrType) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  public List findLrIds(List constraints) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  public List findLrIds(List constraints, String lrType) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  public String getLrName(Object lrId) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  /**
   * This does nothing at all. The dummy datastore does not fire any events.
   * @param l
   */
  public void addDatastoreListener(DatastoreListener l) {
  }

  /**
   * This does nothing at all. The dummy datastore does not fire any events.
   * @param l
   */
  public void removeDatastoreListener(DatastoreListener l) {    
  }

  public String getIconName() {
    return "datastore";
  }

  public String getComment() {
    return "Dummy DataStore";
  }

  public boolean canReadLR(Object lrID) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public boolean canWriteLR(Object lrID) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public boolean lockLr(LanguageResource lr) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void unlockLr(LanguageResource lr) throws PersistenceException {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public FeatureMap getFeatures() {
    return myFeatures;
  }

  public void setFeatures(FeatureMap features) {
    myFeatures = features;
  }

  public void setName(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

}

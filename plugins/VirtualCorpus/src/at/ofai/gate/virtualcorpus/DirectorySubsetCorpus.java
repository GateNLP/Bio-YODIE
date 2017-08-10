/*
 *  DirectorySubsetCorpus.java
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
 *  $Id: DirectoryCorpus.java 42 2011-02-07 12:56:46Z johann.petrak $
 */

package at.ofai.gate.virtualcorpus;


import gate.*;
import gate.creole.metadata.*;
import gate.event.CreoleListener;

/** 
 * A directory corpus that contains a subset of the documents of an existing
 * DirectoryCorpus (its "parent corpus"). Only documents that are in the
 * parent corpus can be added, and removing a document from the SubsetCorpus
 * will not delete any files in the directory. This corpus is essentially
 * just a view into the parent corpus.
 * Its main purpose is to support the Learning plugin
 * with a DirectoryCorpus.
 * <p>
 * NOTE: for now, only a non-transient DirectoryCorpus can have a
 * DirectorySubsetCorpus.
 * <p>
 * NOTE: for now, events on the parent corpus are not all handled correctly,
 * e.g. if a document gets removed from the parent, this corpus might not
 * adapt to it. For now, a subset corpus should only be used while the
 * parent corpus stays unchanged!!!!!
 * <p>
 * Removing a SubsetCorpus will not remove the datastore. The normal way
 * to create a SubsetCorpus is by adopting a new and empty transient corpus
 * to the datastore of an existing DirectoryCorpus. However, it can also
 * be instantiated directly (the only required parameter is an existing
 * DirectoryCorpus).
 * 
 * @author Johann Petrak
 */
@CreoleResource(
    name = "DirectorySubsetCorpus",
    interfaceName = "gate.Corpus", 
    icon = "corpus", 
    helpURL = "http://code.google.com/p/gateplugin-virtualcorpus/wiki/DirectoryCorpusUsage",
    comment = "A corpus that provides a view of a subset of the documents in an existing DirectoryCorpus")
public class DirectorySubsetCorpus  extends VirtualSubsetCorpus
  implements Corpus, CreoleListener
  {

  //*****
  // Fields
  //******
  
  /**
   * 
   */
  private static final long serialVersionUID = -8485199876515382902L;
  
  //***************
  // Parameters
  //***************
  
  /**
   * @param corpus 
   */
  @CreoleParameter(
    comment = "The DirectoryCorpus for which to create this corpus",
    defaultValue = "")
  public void setDirectoryCorpus(DirectoryCorpus corpus) {
    this.virtualCorpus = corpus;
  }
  /**
   * @return
   */
  public DirectoryCorpus getDirectoryCorpus() {
    return (DirectoryCorpus)this.virtualCorpus;
  }
  
} // class DirectorySubsetCorpus

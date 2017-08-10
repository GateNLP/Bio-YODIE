package com.jpetrak.gate.stringannotation.extendedgazetteer;

/**
 * A visitor for some GazStore implementation. The actual visitor returned by 
 * a concrete GazStore implementation is a subclass of this class.
 * 
 * @author Johann Petrak
 *
 */
public abstract class Visitor {
  /**
   * Reset the visitor object for a new match attempt.
   */
  public abstract void reset();
  
  /**
   * Try to match another character and return if the match was successful or failed.
   * @param c
   * @return
   */
  public abstract boolean match(char c);
  
  /**
   * Check if the current matched state is a final state and thus represents a match
   * of the current prefix.
   * @return
   */
  public abstract boolean isFinal();
  
  // TODO: how to best represent the list of "Lookups" in a generic way?
  // If the state matches and is final, there will be a payload that is a set/list
  // of (gazetteerListIndex,entryFeatures).
  
  /**
   * Return an object that represents a matching state. That way, a client can 
   * remember several matching situations before starting to act on one of them.
   * @return
   */
  public abstract Match getMatch();
  
}

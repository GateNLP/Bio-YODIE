package com.jpetrak.gate.stringannotation.extendedgazetteer.trie;

import java.io.Serializable;

/**
 * The base class of the implementations for the two stages of storing 
 * CharMaps: first stage stores the whole charmap in a single char[] but each 
 * char[] chunk is stored as a separate object. Second stage stores all char[]
 * chunks in a single char[] array.
 * 
 * @author Johann Petrak
 *
 */
public abstract class StoreCharMapBase implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -1162248412493478826L;
  /**
   * Adds a new state for the given char to the char map. If mapIndex less than zero,
   * then the charmap will be created and its new index returned, otherwise, the charmap
   * at the given index will be used and the given index returned back. 
   * @param mapIndex
   * @param key
   * @param value
   * @return
   */
  //public abstract int putOld(int mapIndex, char key, State state);
  public abstract int put(int mapIndex, char key, int state);
  //public abstract void replaceOld(int mapIndex, char key, State newState, State oldState);
  public abstract void replace(int mapIndex, char key, int newState, int oldState);
  /**
   * Return the state index.
   * @param mapIndex
   * @param chr
   * @return
   */
  //public abstract State nextOld(int mapIndex, char chr);
  public abstract int next(int mapIndex, char chr);
}

package com.jpetrak.gate.stringannotation.extendedgazetteer.trie;

import com.jpetrak.gate.stringannotation.utils.StoreArrayOfCharArrays;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.lang.management.ManagementFactory;

import org.junit.Test;

import gate.util.GateRuntimeException;
import org.apache.log4j.Logger;

/**
 * This class is used to store states as elements in the character store instead of
 * as individual objects. The class is associated with a character store and its 
 * methods essentially perform the same functions as for State, but using integer
 * numbers to represent States. Negative numbers represent null.
 * 
 * @author Johann Petrak
 *
 */
public class StoreStates implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 7967567872004912624L;
  // we keep around some statistics about the number of nodes, kinds of nodes etc.
  public int nrNodes = 0;
  public int mapNodes = 0;
  public int charNodes = 0;
  public int changedNodes = 0; // changed from char to map
  public int finalNodes = 0;
  public int nrChars = 0;
  public int nrInput = 0;
  
  public String statsString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Total nodes:           ").append(nrNodes).append("\n");
    sb.append("CharMap nodes:         ").append(mapNodes).append("\n");
    sb.append("SingleChar nodes:      ").append(charNodes).append("\n");
    sb.append("Map to Single changes: ").append(changedNodes).append("\n");
    sb.append("Final nodes:           ").append(finalNodes).append("\n");
    //sb.append("Number of characters:           ").append(nrChars).append("\n");
    sb.append("Input characters:      ").append(nrInput).append("\n");
    return sb.toString();
  }
  
  public int initialState = -1;
  
  protected transient Logger logger;
  
  
  protected StoreCharMapBase charMapStore = null;
  protected StoreArrayOfCharArrays dataStore  = null; 
  
  public StoreStates(StoreArrayOfCharArrays store) {
    dataStore = store;
    charMapStore = new StoreCharMapPhase1(store);
    initialState = newCharMapState();
  }
  
  public StoreStates() {
    logger = Logger.getLogger(this.getClass().getName());
  }
  
  public void compact() {
    if(charMapStore instanceof StoreCharMapPhase2) {
      // alsready compacted, do nothing
    } else {
      // make sure we have a logger even after de-serialization!
      if(logger==null) {
        logger = Logger.getLogger(this.getClass().getName());
      }
      logger.info("Compacting states");
      System.gc();
      long startTime = System.currentTimeMillis();
      long before = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
      charMapStore = new StoreCharMapPhase2(charMapStore);
      long endTime = System.currentTimeMillis();
      long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
      logger.info("Compacting finished in (secs):        "+((endTime-startTime)/1000.0));
      logger.info("Heap memory increase (estimate,MB):   "+
        String.format("%01.3f", ((after-before)/(1024.0*1024.0))));
    }
  }
  
  // ************************************* public methods
  
  /**
   * Create and add a new, empty char map state to the store and return its index.
   * A char map state consists of the following fields (total of 5 chars) 
   * <ul>
   * <li>The lookup index, i.e. the payload (two chars minus one bit)
   * <li>The type=char map: one bit stored in the two chars that also hold the lookup index
   * <li>The index of the char map (two chars)
   * <li>One char padding to make the size equal to single char state
   * </ul>
   * 
   * @return the index representing the new state
   */
  public int newCharMapState() {
    char[] chunk = new char[5];
    setLookupIntoChars(chunk, -1);
    setCharMapIntoChars(chunk, -1);
    setIsCharMapState(chunk, true);
    nrNodes++;
    mapNodes++;
    return dataStore.addFixedLengthData(chunk);
  }
  
  /**
   * Create a new char map state by replacing the existing single char state. This will
   * return the same state index as it gets because it will always re-use the data of the
   * single char state.
   * 
   * @param singleCharState
   * @return
   */
  public int newCharMapState(int singleCharState) {
    char[] chunk = dataStore.getFixedLengthData(singleCharState, 5);
    // the lookup index stays the same, but we need to change the state type
    setIsCharMapState(chunk, true);
    // instead of the next state and character we have the index to a charmap 
    // and just padding. The existing char and next state get added to the charmap.
    int nextState = getNextStateFromChars(chunk);
    char chr = chunk[4];
    int charmapindex = charMapStore.put(-1, chr, nextState);
    setCharMapIntoChars(chunk, charmapindex);
    mapNodes++;
    charNodes--;
    changedNodes++;
    dataStore.replaceFixedLengthData(singleCharState, chunk);
    return singleCharState;
  }
  
  /**
   * Create and add a new, empty single char state to the store and return its index.
   * A single char state consists of the following fields (5 chars)
   * <ul>
   * <li>The lookup index (two chars minus one bit)
   * <li>The type=single char: one bit stored in the two chars that also hold the lookup index
   * <li>The index of the next state (two chars)
   * <li>The character (one char)
   * </ul>
   * @return
   */
  public int newSingleCharState() {
    char[] chunk = new char[5];
    setLookupIntoChars(chunk, -1);
    setNextStateIntoChars(chunk, -1);
    setIsCharMapState(chunk, false);
    chunk[4] = 0;
    nrNodes++;
    charNodes++;
    return dataStore.addFixedLengthData(chunk);
  }
  
  /**
   * Add a new edge to a state: for the given character, add a transition to the 
   * given state (value).
   * This may internally replace an existing single char state with a char map state.
   *   
   * @param state
   * @param key
   * @param value
   */
  public void put(int state, char key, int to_state) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    if(getIsSingleCharState(chunk)) {
      char chr = chunk[4];
      if(chr != 0) {
        throw new GateRuntimeException("Trying to put into a non-empty single char state");
      }
      chunk[4] = key;
      setNextStateIntoChars(chunk, to_state);
      dataStore.replaceFixedLengthData(state, chunk);
    } else {
      int charmapIndex = getCharMapFromChars(chunk);
      int newIndex = charMapStore.put(charmapIndex, key, to_state);
      if(charmapIndex < 0) {  // charmap did not exist
        setCharMapIntoChars(chunk, newIndex);
        dataStore.replaceFixedLengthData(state, chunk);
      }
    }
  }
  
  
  /**
   * Check if the given state is final.
   * @param state
   * @return
   */
  public boolean isFinal(int state) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    int l = getLookupFromChars(chunk);
    return l >= 0;
  }
  
  /**
   * Return the state for the given character or a negative index if no such state exists.
   * @param state
   * @param chr
   * @return
   */
  public int next(int state, char chr) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    if(getIsSingleCharState(chunk)) {
      char storedchr = chunk[4];
      if(storedchr == chr) {
        int ret = getNextStateFromChars(chunk);
        //logger.info("Returning "+ret);
        return ret;
      } else {
        //logger.info("Returning -1 because storedchr="+storedchr+" and char="+chr);
        return -1;
      }
    } else { // charmap state
      int charmapindex = getCharMapFromChars(chunk);
      int ret =  charMapStore.next(charmapindex, chr);
      //logger.info("Returning from charmapindex: "+charmapindex+" for "+chr+": "+ret);
      return ret;
    }
  }
  

  public int getLookupIndex(int state) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    return getLookupFromChars(chunk);
  }
  
  public void setLookupIndex(int state, int lookup) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    int oldLookup = getLookupFromChars(chunk);
    setLookupIntoChars(chunk, lookup);
    if(oldLookup < 0 && lookup >= 0) {
      finalNodes++;
    } else if(lookup < 0 && oldLookup >= 0) {
      finalNodes--;
    } 
    dataStore.replaceFixedLengthData(state,chunk);
  }
  
  public boolean getIsCharMapState(int state) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    return getIsCharMapState(chunk);
  }
  
  public boolean getIsSingleCharState(int state) {
    return !getIsCharMapState(state);
  }
  
  public char getSingleCharStateChar(int state) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    char ret = 0;
    if(getIsSingleCharState(chunk)) {
      return chunk[4];
    } else {
      throw new GateRuntimeException("Not a single char state");
    }
  }
  
  public void setSingleCharStateChar(int state, char chr) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    if(getIsSingleCharState(chunk)) {
      chunk[4] = chr;
      dataStore.replaceFixedLengthData(state, chunk);
    } else {
      throw new GateRuntimeException("Not a single char state");
    }
  }
  
  public int getCharMapIndex(int state) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    if(getIsCharMapState(chunk)) {
      return getCharMapFromChars(chunk);
    } else {
      throw new GateRuntimeException("Not a char map state");
    }    
  }
  
  public void setCharMapIndex(int state, int mapIndex) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    if(getIsCharMapState(chunk)) {
      setCharMapIntoChars(chunk, mapIndex);
      dataStore.replaceFixedLengthData(state, chunk);
    } else {
      throw new GateRuntimeException("Not a char map state");
    }    
  }
  
  public String toString(int state) {
    char[] chunk = dataStore.getFixedLengthData(state, 5);
    StringBuilder sb = new StringBuilder();
    if(getIsCharMapState(chunk)) {
      sb.append("CharMap,mapindex=");
      sb.append(getCharMapFromChars(chunk));
    } else {
      sb.append("SingleChar,char=");
      sb.append(chunk[4]);
      sb.append(",next=");
      sb.append(getNextStateFromChars(chunk));
    }
    return sb.toString();
  }
  
  public void test() {
    char[] chunk = new char[5];
    setLookupIntoChars(chunk, 123);
    int ret = getLookupFromChars(chunk);
    assertEquals(123,ret);
    setIsCharMapState(chunk, true);
    boolean is = getIsCharMapState(chunk);
    assertEquals(true,is);
    ret = getLookupFromChars(chunk);
    assertEquals(123,ret);
    is = getIsCharMapState(chunk);
    assertEquals(true,is);
    setIsCharMapState(chunk, false);
    is = getIsCharMapState(chunk);
    assertEquals(false,is);
    ret = getLookupFromChars(chunk);
    assertEquals(123,ret);
    setLookupIntoChars(chunk, -1);
    ret = getLookupFromChars(chunk);
    assertEquals(-1,ret);
    is = getIsCharMapState(chunk);
    assertEquals(false,is);
    setIsCharMapState(chunk, true);
    is = getIsCharMapState(chunk);
    assertEquals(true,is);
    ret = getLookupFromChars(chunk);
    assertEquals(-1,ret);    
  }
  
  // ************************************** helper methods
  
  protected void setLookupIntoChars(char[] chunk, int lookup) {
    int storedLookup = lookup << 1;
    int flag = chunk[1] & 0x1;
    Utils.setTwoCharsFromInt(storedLookup+flag, chunk, 0);
  }
  
  protected int getLookupFromChars(char[] chunk) {
    int tmp = (chunk[0] << 16) + chunk[1];
    tmp = tmp & 0xfffffffe;
    return tmp / 2;
  }
  
  protected void setCharMapIntoChars(char[] chunk, int charmapindex) {
    Utils.setTwoCharsFromInt(charmapindex, chunk, 2);
  }
  
  protected int getCharMapFromChars(char[] chunk) {
    return (chunk[2] << 16) + chunk[3];
  }
  
  protected void setIsCharMapState(char[] chunk, boolean trueorfalse) {
    // set the leftmost (high) bit of the 3rd character if yes, otherwise clear it
    if(trueorfalse) {
      chunk[1] = (char)(chunk[1] | 0x1);
    } else {
      chunk[1] = (char)(chunk[1] & 0xfffe);
    }
  }
  
  protected boolean getIsCharMapState(char[] chunk) {
    // return true if the leftmost (high) bit of the 3rd character is set
    return (chunk[1] & 0x1) != 0;
  }

  protected boolean getIsSingleCharState(char[] chunk) {
    return !getIsCharMapState(chunk);
  }
  
  protected void setNextStateIntoChars(char[] chunk, int nextState) {
    Utils.setTwoCharsFromInt(nextState, chunk, 2);
  }
  
  protected int getNextStateFromChars(char[] chunk) {
    return (chunk[2] << 16) + chunk[3];
  }

  
  
}

package com.jpetrak.gate.stringannotation.extendedgazetteer.trie;

import com.jpetrak.gate.stringannotation.utils.StoreArrayOfCharArrays;
import gate.util.GateRuntimeException;

import java.util.ArrayList;
import java.util.Arrays;

public class StoreCharMapPhase1 extends StoreCharMapBase {

  protected StoreArrayOfCharArrays store = null;
  
  protected ArrayList<char[]>  storeKeys   = new ArrayList<char[]>();
  protected ArrayList<int[]> storeStates = new ArrayList<int[]>();
  
  protected ArrayList<char[]> storeKeysAndStates = new ArrayList<char[]>();
  
  
  // methods necessary to access keys and states from the same array: each entry
  // in the char[] array now really consists of 3 chars: the first is the key as char,
  // the second and third encode the int for the state index.
    
  public StoreCharMapPhase1(StoreArrayOfCharArrays lookupStore) {
    store = lookupStore;
  }
  
  // Kryo needds this!
  public StoreCharMapPhase1() {} 
  
  /**
   * Add a new State to the charmap stored at mapIndex, but if mapIndex < 0,
   * create a new mapIndex first and return its index. 
   */
  @Override
  public int put(int mapIndex, char key, int value) {
    int currentIndex = mapIndex;
    if(mapIndex < 0) {
      storeKeysAndStates.add(null);
      currentIndex = storeKeysAndStates.size() - 1;
    }
    char[] entries = storeKeysAndStates.get(currentIndex);
    if (entries == null)
    {
        entries = new char[3];
        entries[0] = key;
        char[] c2 = Utils.int2TwoChars(value);
        entries[1] = c2[0];
        entries[2] = c2[1];
        storeKeysAndStates.set(currentIndex, entries);
        return currentIndex;
    }// if first time
    int index = binarySearchInEntries(entries, key);
    if (index<0)
    {   //System.out.println("Insert point returned "+index);
        index = ~index;
        int newsz = entries.length + 3;
        char[] tempEntries = new char[newsz];
        //System.out.println("Trying to copy: "+index+" entries="+entries.length+" tmp="+tempEntries.length);
        System.arraycopy(entries, 0, tempEntries, 0, index);
        System.arraycopy(entries, index, tempEntries, index + 3, newsz - index - 3);
        entries = tempEntries;
        entries[index] = key;
        char[] c2 = Utils.int2TwoChars(value);
        entries[index+1] = c2[0];
        entries[index+2] = c2[1];
        storeKeysAndStates.set(currentIndex, entries);
    }
    return currentIndex;    
  }

  // @Override
  public int putOld(int mapIndex, char key, int value) {
    int currentIndex = mapIndex;
    if(mapIndex < 0) {
      storeKeys.add(null);
      storeStates.add(null);
      currentIndex = storeKeys.size() - 1;
    }
    char[] itemsKeys = storeKeys.get(currentIndex);
    int[] itemsObjs = storeStates.get(currentIndex);
    if (itemsKeys == null)
    {
        itemsKeys = new char[1];
        itemsKeys[0] = key;
        itemsObjs = new int[1];
        itemsObjs[0] = value;
        storeKeys.set(currentIndex, itemsKeys);
        storeStates.set(currentIndex,itemsObjs);
        return currentIndex;
    }// if first time
    int index = Arrays.binarySearch(itemsKeys, key);
    if (index<0)
    {   
        index = ~index;
        int newsz = itemsKeys.length + 1;
        char[] tempKeys = new char[newsz];
        int[] tempObjs = new int[newsz];
        System.arraycopy(itemsKeys, 0, tempKeys, 0, index);
        System.arraycopy(itemsObjs, 0, tempObjs, 0, index);
        System.arraycopy(itemsKeys, index, tempKeys, index + 1, newsz - index - 1);
        System.arraycopy(itemsObjs, index, tempObjs, index + 1, newsz - index - 1);
        itemsKeys = tempKeys;
        itemsObjs = tempObjs;
        itemsKeys[index] = key;
        itemsObjs[index] = value;
        storeKeys.set(currentIndex, itemsKeys);
        storeStates.set(currentIndex,itemsObjs);
    }
    return currentIndex;    
  }
  
  
  @Override
  public void replace(int mapIndex, char key, int newState, int oldState) {
    char[] entries = storeKeysAndStates.get(mapIndex);
    int index = binarySearchInEntries(entries, key);
    if(index<0) {
      throw new GateRuntimeException("CharMapState: should have key but not found: "+key);
    }
    if(entries[index] != oldState) {
      throw new GateRuntimeException("CharMapState: old states differ!");
    }
    char[] c2 = Utils.int2TwoChars(newState);
    entries[index+1] = c2[0];
    entries[index+2] = c2[1];
  }

  // @Override
  public void replaceOld(int mapIndex, char key, int newState, int oldState) {
    char[] itemsKeys = storeKeys.get(mapIndex);
    int[] itemsObjs = storeStates.get(mapIndex);
    int index = Arrays.binarySearch(itemsKeys, key);
    if(index<0) {
      throw new GateRuntimeException("CharMapState: should have key but not found: "+key);
    }
    if(itemsObjs[index] != oldState) {
      throw new GateRuntimeException("CharMapState: old states differ!");
    }
    itemsObjs[index] = newState;
  }
  
  // @Override
  public int nextOld(int mapIndex, char chr) {
    if(mapIndex < 0) {
      return -1;
    }
    char[] itemsKeys = storeKeys.get(mapIndex);
    int[] itemsObjs = storeStates.get(mapIndex);
    if (itemsKeys == null) return -1;
    int index = Arrays.binarySearch(itemsKeys, chr);
    if (index<0)
        return -1;
    return itemsObjs[index];    
  }
  
  @Override
  public int next(int mapIndex, char chr) {
    if(mapIndex < 0) {
      return -1;
    }
    char[] entries = storeKeysAndStates.get(mapIndex);
    if (entries == null) return -1;
    int index = binarySearchInEntries(entries,chr);
    if (index<0)
        return -1;
    int ret = Utils.twoChars2Int(entries[index+1],entries[index+2]);
    return ret;    
  }
 
  // A modification of binary search that only looks at the indices 
  // 0, 3, 6, .... in the array
  // This searches the entries array to find the key in one of these positions
  // and returns the index (>= 0) if found or the insertion point as a negative int
  // if not found.
  protected int binarySearchInEntries(char[] entries, char chr) {
    int nrentries = entries.length / 3;
    int low = 0;
    int high = nrentries - 1;

    while (low <= high) {
        int mid = (low + high) >>> 1;
        char midVal = entries[mid*3];

        if (midVal < chr)
            low = mid + 1;
        else if (midVal > chr)
            high = mid - 1;
        else
            return mid*3; // key found
    }
    return 3*(-low)-1;  // key not found.    
  }
  
}

package com.jpetrak.gate.stringannotation.extendedgazetteer.trie;

import com.jpetrak.gate.stringannotation.utils.StoreArrayOfCharArrays;
import gate.util.GateRuntimeException;

public class StoreCharMapPhase2 extends StoreCharMapBase {

  protected StoreArrayOfCharArrays store = null;
  
  // The chunkIndices array is used after the whole trie is created and when we 
  // want to store the char map tables in the store too. The chunkIndices array
  // has as many entries as there are charMaps, i.e. as many entries as the 
  // storeKeysAndStates arraylist. For each index 0..storeKeysAndStates.size(), the
  // chunkIndices array contains the index of the chunk as stored in the store.
  protected int[] chunkIndices = null;

  public StoreCharMapPhase2(StoreCharMapBase previousStore) {
    if (previousStore instanceof StoreCharMapPhase1) {
      StoreCharMapPhase1 oldStore = (StoreCharMapPhase1) previousStore;
      store = oldStore.store;
      chunkIndices = new int[oldStore.storeKeysAndStates.size()];
      for (int i = 0; i < chunkIndices.length; i++) {
        char[] chunk = oldStore.storeKeysAndStates.get(i);
        int index = store.addData(chunk);
        chunkIndices[i] = index;
      }
    } else {
      throw new GateRuntimeException(
          "Can only compact once, or from a Phase1 store!");
    }
  }
  
  // Kryo needs this!
  public StoreCharMapPhase2() {
    
  }

  @Override
  public int put(int mapIndex, char key, int state) {
    throw new GateRuntimeException("put cannot be called after compaction!");
  }

  @Override
  public void replace(int mapIndex, char key, int newState, int oldState) {
    throw new GateRuntimeException("replace cannot be called after compaction!");
  }


  @Override
  public int next(int mapIndex, char chr) {
    if(mapIndex < 0) {
      return -1;
    }
    char[] entries = store.getData(chunkIndices[mapIndex]);
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

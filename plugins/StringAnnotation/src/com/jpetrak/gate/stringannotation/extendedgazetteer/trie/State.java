package com.jpetrak.gate.stringannotation.extendedgazetteer.trie;


public abstract class State extends com.jpetrak.gate.stringannotation.extendedgazetteer.State {
  public static int nrNodes = 0;
  public static int mapNodes = 0;
  public static int charNodes = 0;
  public static int finalNodes = 0;
  public static int nrChars = 0;
  public static int nrInput = 0;
  
  static StoreCharMapBase store = null;
  
  public abstract void put(char key, State value);
  

  protected int lookupIndex = -1;
  public int getLookupIndex() {
    return lookupIndex;
  }

  
  public void addLookup(int index) {
    lookupIndex = index;
  }
  
  public boolean isFinal() {
    return lookupIndex >= 0;
  }    
  
  public abstract State next(char chr);
  
  public abstract void replace(char key, State newState, State oldState);
}

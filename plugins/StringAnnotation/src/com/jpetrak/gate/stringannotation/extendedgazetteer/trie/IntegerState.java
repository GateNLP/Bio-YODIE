package com.jpetrak.gate.stringannotation.extendedgazetteer.trie;


/**
 * A lightweight wrapper to make the integer-index based approach to states compatible
 * with the old object-based approach.
 * Once/if we abandon all old object-based implementations, this can be removed and 
 * StoreState used directly to avoid some obejct creation overhead.
 * @author Johann Petrak
 *
 */
public class IntegerState extends State {

  StoreStates store = null;
  int index = -1;
  
  public IntegerState(StoreStates store, int index) {
    this.store = store;
    this.index = index;
  }
  
  @Override
  public boolean isFinal() {
    return store.isFinal(this.index);
  }

  @Override
  public State next(char c) {
    int next = store.next(index,c);
    if(next < 0) {
      return null;
    } else {
      return new IntegerState(store,next);
    }
  }

  @Override
  public void put(char key, State value) {
    IntegerState st = (IntegerState)value;
    store.put(index, key, st.index);
  }

  @Override
  public void replace(char key, State newState, State oldState) {
    //
  }
  
  @Override
  public int getLookupIndex() {
    return store.getLookupIndex(index);
  }

  public String toString() {
    return "IntegerState:"+index+"("+store.toString(index)+")";
  }
  
}

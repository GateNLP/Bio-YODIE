package com.jpetrak.gate.stringannotation.extendedgazetteer;

/**
 * As long as the state is visible and we do not yet have a visitor to hide it,
 * this class is the common parent of all State implementations. This is necessart
 * so the GazStore.getLookups(State) works for all state implementations
 * 
 * @author Johann Petrak
 *
 */
public abstract class State {
  public abstract boolean isFinal();
  public abstract State next(char c);
}

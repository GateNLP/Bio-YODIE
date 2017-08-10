package com.jpetrak.gate.stringannotation.extendedgazetteer;

import java.util.List;

/**
 * Store information about a successful match.
 * 
 * @author Johann Petrak
 *
 */
public abstract class Match {  
  public abstract List<Lookup> getLookups();
}

package com.jpetrak.gate.stringannotation.extendedgazetteer;

import java.io.Serializable;

import gate.FeatureMap;

public class ListInfo implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 5810465533584791597L;
  private String annotationType;
  private String sourceURL;
  private FeatureMap features;
  
  // make Kryo happy
  public ListInfo() {
    
  }
  public ListInfo(String type, String source, FeatureMap features) {
    annotationType = type;
    sourceURL = source;
    this.features = features;
  }
  public String getAnnotationType() {
    return annotationType;
  }
  public String getSourceURL() {
    return sourceURL;
  }
  public FeatureMap getFeatures() {
    return features;
  }

}

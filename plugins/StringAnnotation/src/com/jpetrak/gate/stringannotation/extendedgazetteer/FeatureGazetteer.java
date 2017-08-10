/*
 * ExtendedGazeteer.java
 * 
 * 
 *
 */
package com.jpetrak.gate.stringannotation.extendedgazetteer;


import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Utils;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.GateRuntimeException;
import gate.util.InvalidOffsetException;

import java.util.Iterator;

import org.apache.log4j.Logger;

//TODO: contemplate features:
// = processIfNoMatch: instead of processing annotations where there is a match, do for non-matches
// = what to do: add features, add Annotation (all, just the ones not already there), 
//   remove Annotation


/**
 * See document in the wiki: https://github.com/johann-petrak/gateplugin-stringannotation/wiki/Feature-Gazetteer
 *
 *  @author Johann Petrak
 */
@CreoleResource(
  name = "Feature Gazetteer",
  comment = "Run the gazetteer on individual feature values and add features, delete or annotate",
  icon="shefGazetteer.gif",
  helpURL="https://github.com/johann-petrak/gateplugin-stringannotation/wiki/Feature-Gazetteer"
)
@SuppressWarnings("javadoc")
public class FeatureGazetteer extends GazetteerBase
{


  // constants
  private static boolean debug = false;
  
  
  // *************************************************************************
  // PR Parameters
  // *************************************************************************
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The input annotation set",
  defaultValue = "")
  public void setInputAnnotationSet(String ias) {
    this.inputAnnotationSet = ias;
  }
  public String getInputAnnotationSet() {
    return inputAnnotationSet;
  }
  private String inputAnnotationSet = "";
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The word annotation set type",
  defaultValue = "Token")
  public void setWordAnnotationType(String val) {
    this.wordAnnotationType = val;
  }
  public String getWordAnnotationType() {
    return wordAnnotationType;
  }
  private String wordAnnotationType = "";
  
  @RunTime
  @CreoleParameter(
    comment = "The feature from the word annotation to use as text, required.",
  defaultValue = "")
  public void setTextFeature(String val) {
    this.textFeature = val;
  }
  public String getTextFeature() {
    return textFeature;
  }
  private String textFeature = "";
  
  
  @RunTime
  @Optional
  @CreoleParameter(
    comment = "The containing annotation set type",
  defaultValue = "")
  public void setContainingAnnotationType(String val) {
    this.containingAnnotationType = val;
  }
  public String getContainingAnnotationType() {
    return containingAnnotationType;
  }
  private String containingAnnotationType = "";
  

  @CreoleParameter(comment = "Are matches required to start at the beginning of the feature value?",
    defaultValue = "true")
  @RunTime
  public void setMatchAtStartOnly(Boolean yesno) {
    matchAtStartOnly = yesno;
  }
  public Boolean getMatchAtStartOnly() {
    return matchAtStartOnly;
  }
  private boolean matchAtStartOnly;
  
  @CreoleParameter(comment = "Are matches required to end at the end of the feature value?",
      defaultValue = "true")
    @RunTime
    public void setMatchAtEndOnly(Boolean yesno) {
      matchAtEndOnly = yesno;
    }
    public Boolean getMatchAtEndOnly() {
      return matchAtEndOnly;
    }
    private boolean matchAtEndOnly;
    
  @CreoleParameter(comment = "Processing mode", defaultValue = "AddFeatures")
  @RunTime
  public void setProcessingMode(FeatureGazetteerProcessingMode mode) {
    processingMode = mode;
  }
  public FeatureGazetteerProcessingMode getProcessingMode() {
    return processingMode;
  }
  FeatureGazetteerProcessingMode processingMode;
  
  @CreoleParameter(comment = "Output annotation set if mode is AddNewAnnotation", defaultValue = "")
  @RunTime
  @Optional
  public void setOutputAnnotationSet(String name) {
    outputAnnotationSet = name;
  }
  public String getOutputAnnotationSet() {
    return outputAnnotationSet;
  }
  protected String outputAnnotationSet = "";
  
  @CreoleParameter(comment = "The output annotation type, overwriting the type from the list or the default, Lookup", defaultValue = "")
  @RunTime
  @Optional
  public void setOutputAnnotationType(String name) {
    outputAnnotationType = name;
  }
  public String getOutputAnnotationType() {
    return outputAnnotationType;
  }
  protected String outputAnnotationType = "";
  
  @CreoleParameter(comment = "The output feature prefix, added to the start of all feature names added", defaultValue = "")
  @RunTime
  @Optional
  public void setOutputFeaturePrefix(String name) {
    outputFeaturePrefix = name;
  }
  public String getOutputFeaturePrefix() {
    return outputFeaturePrefix;
  }
  protected String outputFeaturePrefix = "";
  
  
  
  // ************************************************************************
  // other class fields 
  // ************************************************************************


  public FeatureGazetteer() {
    logger = Logger.getLogger(this.getClass().getName());
  }

   @Override
  public void execute() throws ExecutionException{
    doExecute(document); // delegate so that a subclass can overwrite execute() and still use doExecute
   }

   public void doExecute(Document theDocument) throws ExecutionException {
    interrupted = false;
    //check the input
    if(theDocument == null) {
      throw new ExecutionException(
        "No document to process!"
      );
    }

    AnnotationSet inputAS = null; 
    if(inputAnnotationSet == null ||
       inputAnnotationSet.equals("")) inputAS = theDocument.getAnnotations();
    else inputAS = theDocument.getAnnotations(inputAnnotationSet);

    AnnotationSet processAnns = null;
    if(wordAnnotationType == null || wordAnnotationType.isEmpty()) {
      throw new GateRuntimeException("Word annotation type must not be empty!");
    } 
    processAnns = inputAS.get(wordAnnotationType);
    
    AnnotationSet containingAnns = null;
    if(containingAnnotationType == null || containingAnnotationType.isEmpty()) {
      // leave the containingAnns null to indicate we do not use containing annotations
    } else {
      containingAnns = inputAS.get(containingAnnotationType);
      //System.out.println("DEBUG: got containing annots: "+containingAnns.size()+" type is "+containingAnnotationType);
    }
    
    AnnotationSet outputAS = document.getAnnotations(outputAnnotationSet);
    
    fireStatusChanged("Performing look-up in " + theDocument.getName() + "...");

    if(containingAnns == null) {
      // go through all word annotations 
      for(Annotation ann : processAnns) {
        Iterator<Lookup> ret = doMatch(featureAsString(ann,textFeature),matchAtStartOnly,matchAtEndOnly);
        if(ret != null) {
          processMatch(ann,ret, inputAS, outputAS);
        } else {
          processNonMatch(ann,ret,inputAS,outputAS);
        }
      }
    } else {
      for(Annotation containingAnn : containingAnns) {
        AnnotationSet containedAnns = Utils.getContainedAnnotations(processAnns, containingAnn);
        for(Annotation ann : containedAnns) {
          Iterator<Lookup> ret = doMatch(featureAsString(ann,textFeature),matchAtStartOnly,matchAtEndOnly);
          if(ret != null) {
            processMatch(ann,ret, inputAS, outputAS);
          } else {
            processNonMatch(ann,ret,inputAS,outputAS);
          }
        }
      }
    }

    fireProcessFinished();
    fireStatusChanged("Look-up complete!");
  } // execute

  protected void processMatch(Annotation ann, Iterator<Lookup> lookups, 
      AnnotationSet inputAS, AnnotationSet outputAS) {
    if(getProcessingMode().equals(FeatureGazetteerProcessingMode.AddFeatures)) {
      addLookupsToAnn(ann, lookups, false);
    } else if(getProcessingMode().equals(FeatureGazetteerProcessingMode.OverwriteFeatures)) {
      addLookupsToAnn(ann, lookups, true);
    } else if(getProcessingMode().equals(FeatureGazetteerProcessingMode.RemoveAnnotation)) {
      inputAS.remove(ann);
    } else if(getProcessingMode().equals(FeatureGazetteerProcessingMode.AddNewAnnotation)) {
      addNewAnnotation(ann, lookups, inputAS, outputAS);
    }
  }
  protected void processNonMatch(Annotation ann, Iterator<Lookup> lookups, 
      AnnotationSet inputAS, AnnotationSet outputAS) {
    if(getProcessingMode().equals(FeatureGazetteerProcessingMode.KeepAnnotation)) {
      inputAS.remove(ann);
    }
  }
   
  protected void addNewAnnotation(Annotation ann, Iterator<Lookup> lookups, 
      AnnotationSet inputAS, AnnotationSet outputAS) {
    int from = ann.getStartNode().getOffset().intValue();
    int to = ann.getEndNode().getOffset().intValue();
    //System.out.println("Trying to add annotation from "+from+" to "+to+" lookups="+lookups+"Annotation is "+ann);
    while(lookups.hasNext()) {
      Lookup lookup = lookups.next();
      String type = gazStore.getLookupType(lookup);
      type = getAnnotationTypeName(type);
      FeatureMap fm = Factory.newFeatureMap();
      gazStore.addLookupListFeatures(fm, lookup);
      gazStore.addLookupEntryFeatures(fm, lookup);
      try {
        outputAS.add(new Long(from), new Long(to), type, fm);
      } catch (InvalidOffsetException ex) {
        throw new GateRuntimeException("Invalid offset exception - doclen/from/to="
          + document.getContent().size() + "/" + from + "/" + to + " / ", ex);
      }
    }
  }
  protected String featureAsString(Annotation ann, String key) {
    Object value = ann.getFeatures().get(key);
    if(value == null) {
      return "";
    } else {
      return value.toString();
    }
  }

  protected void addLookupsToAnn(Annotation ann, Iterator<Lookup> lookups, boolean overwrite) {
    FeatureMap fm = ann.getFeatures();
    while(lookups.hasNext()) {
      Lookup lookup = lookups.next();
      FeatureMap newFm = Factory.newFeatureMap();
      gazStore.addLookupListFeatures(newFm, lookup);
      gazStore.addLookupEntryFeatures(newFm, lookup);
      if(getOutputFeaturePrefix() != null && !getOutputFeaturePrefix().isEmpty()) {
        FeatureMap newFm2 = Factory.newFeatureMap();
        for(Object key : newFm.keySet()) {
          newFm2.put(getOutputFeaturePrefix()+key.toString(), newFm.get(key));
        }
        newFm = newFm2;
      }
      if(overwrite) {        
        fm.putAll(newFm);
        fm.putAll(newFm);
      } else {
        // now add only features not already in fm
        for(Object key : newFm.keySet()) {
          if(fm.get(key) == null) {
            fm.put(key, newFm.get(key));
          }
        }
      }
    }
  }
  
  public Iterator<Lookup> doMatch(String theString, boolean matchAtStartOnly, boolean matchAtEndOnly)
      throws ExecutionException {
    interrupted = false;
    int length = theString.length();
    char currentChar;
    State currentState = gazStore.getInitialState();

    //System.out.println("Trying match for "+theString);
    // an empty string never matches
    if (theString == null || theString.isEmpty()) {
      return null;
    }

    // if the match is required to start at the begging, we set the
    // upper index of where we try to start from (matchfrom) to 0 otherwise
    // we try all from 0 to the last.
    // if the match is requried to end at the end, we set the minimim
    // index a final state must reach (matchto) to the last index.
    // otherwise it can end anywhere from offset 0 and up.
    int matchfrom = 0;
    if(!matchAtStartOnly) {
      matchfrom = length-1;
    }
    int matchto = length-1;
    if(!matchAtEndOnly) {
      matchto = 0;
    }
    for (int pos = 0; pos <= matchfrom; pos++) {
      for (int i = pos; i < length; i++) {
        currentChar = theString.charAt(i);
        currentChar = caseSensitive.booleanValue() ? currentChar : Character
            .toUpperCase(currentChar);
        currentState = currentState.next(currentChar);
        if (currentState == null) {
          currentState = gazStore.getInitialState();
          break;
        }
        if (currentState.isFinal()) {
          //System.out.println("MATCH");
          if(i >= matchto) {
            //System.out.println("CHECK");
            return gazStore.getLookups(currentState);
          }
        }
      }
    }
    return null;
  } // doMatch  
  

  // in the program, we use only this method to find the annotation type
  // name to assign: if the annotation type is set as a runtime parameter,
  // always use that, otherwise use the one we get from the gazetteer list
  // specification in the def file.
  
  public String getAnnotationTypeName(String defaultType) {
    if(outputAnnotationType == null || outputAnnotationType.isEmpty()) {
      return defaultType;
    } else {
      return outputAnnotationType;
    }
  }
  

} // FeatureGazetteer


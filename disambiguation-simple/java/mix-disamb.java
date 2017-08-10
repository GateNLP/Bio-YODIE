/*
 * mix-disamb.java
 *
 * Copyright (c) 1995-2016, The University of Sheffield. See the file
 * COPYRIGHT.txt in the software or at
 * http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 * This file is part of Bio-YODIE (see 
 * https://gate.ac.uk/applications/bio-yodie.html), and is free 
 * software, licenced under the GNU Affero General Public License
 * Version 3, 19 November 2007
 *
 * A copy of this licence is included in the distribution in the file
 * LICENSE-AGPL3.html, and is also available at
 * http://www.gnu.org/licenses/agpl-3.0-standalone.html
 *
 * G. Gorrell, 26 September 2016
 */

import gate.trendminer.lodie.utils.LodieUtils;
import gate.Utils;
import gate.*;
import gate.util.GateRuntimeException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

//List<String> featureNames = new ArrayList<String>(
//  Arrays.asList("scPageRank", "scCRISCUINorm", "scCRISLabelCUINorm", "scStringSimilarity"));
//List<Double> featureWeights = new ArrayList<Double>(Arrays.asList(1.0, 1.0, 1.0, 1.0));
List<String> featureNames = new ArrayList<String>(
  Arrays.asList("scPageRank", "scStringSimilarity"));
List<Double> featureWeights = new ArrayList<Double>(Arrays.asList(1.0, 1.0));
Double minScore = 0.00;
boolean createNilMentions = false;

@Override
public void init() {
  // Override the feature name from the configuration property, 
  // if the property exists;
  String conf = System.getProperty("lodie.disambiguation-simple.mix-disamb.featureNames");
  if(conf != null) {
    String[] fn = conf.split(",");
    featureNames = new ArrayList<String>();
    for(int i=0;i<fn.length;i++){
      featureNames.add(fn[i].trim());
    }
  }

  String weights = System.getProperty("lodie.disambiguation-simple.mix-disamb.featureWeights");
  if(weights != null) {
    String[] fw = weights.split(",");
    featureWeights = new ArrayList<Double>();
    for(int i=0;i<fw.length;i++){
      featureWeights.add(Double.parseDouble(fw[i].trim()));
    }
  }

  if(featureNames.size()!=featureWeights.size()){
    System.out.println("WARNING: wrong number of weights for feature selection. Using equal weights.");
    featureWeights = new ArrayList<Double>();
    for(int i=0;i<featureNames.size();i++){
      featureWeights.add(1.0);
    }
  }

  String minScoreString = System.getProperty("lodie.disambiguation-simple.mix-disamb.minScore");
  if(minScoreString != null) {
    try {
      minScore = Double.parseDouble(minScoreString);
      System.out.println("INFO: using simple-disamb.minScore="+minScore);
    } catch (Exception ex) {
      throw new GateRuntimeException("Problem converting the minScore setting: "+minScoreString,ex);
    }
  }

  String createNilMentionsString = System.getProperty("lodie.disambiguation-simple.mix-disamb.createNilMentions");
  if(createNilMentionsString != null) {
    if(createNilMentionsString.trim().toLowerCase().equals("false")) {
      createNilMentions = false;
    } else if(createNilMentionsString.trim().toLowerCase().equals("true")) {
      createNilMentions = true;      
    } else {
      throw new GateRuntimeException("Property createNilMentions is neither 'true' nor 'false': "+createNilMentionsString);
    }
  }
  System.out.println("INFO: creating explicit NIL mentions: "+createNilMentions);
}

@Override
public void execute() {
  for(Annotation listAnn : inputAS.get("LookupList")) {
    FeatureMap bestFm = null;
    Double bestScore = 0.0;
    List<FeatureMap> cands = LodieUtils.getCandidateList(inputAS,listAnn);
    for(FeatureMap lookupfm: cands){
      Double score = 0.0;
      for(int i=0;i<featureNames.size();i++){
        String feat = featureNames.get(i);
        if(lookupfm.get(feat) instanceof Number){
          score+=((Number)lookupfm.get(feat)).doubleValue()*featureWeights.get(i);
        } else {
          //System.out.println("WARNING: skipping " + feat + " " + lookupfm.get(feat) + "; not a number.");
        }
      }
      if(score>bestScore || bestFm==null){
        bestFm = lookupfm;
        bestScore = score;
      }
    }
    
    boolean isNull = false;
    if(bestScore < minScore) {
      isNull = true;
    }

    if(isNull || bestFm == null) {
      // we have to create a Mention annotation which is a Nil or 
      // a NilMention annotation (if we do not want explicit NIL Mention annotations)
      String type="Mention";
      if(createNilMentions) {
        type = "NilMention";
      }
      FeatureMap newFm = Factory.newFeatureMap();
      if(bestFm == null) {
        newFm.put("inst","");
        newFm.put("nilReason","nothing found"); 
      } else {
        newFm.putAll(bestFm);
        newFm.put("bestInst",newFm.get("inst"));
        newFm.put("inst","");
        newFm.put("nilThreshold",minScore);
        newFm.put("nilReason","score<minscore");
      }
      Utils.addAnn(outputAS,listAnn,type,newFm);
    } else {
      Utils.addAnn(outputAS,listAnn,"Mention",Utils.toFeatureMap(bestFm));
    }
  }
}

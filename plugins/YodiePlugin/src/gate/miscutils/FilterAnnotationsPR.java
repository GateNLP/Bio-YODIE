/* 
 * Copyright (C) 2015-2016 The University of Sheffield.
 *
 * This file is part of YodiePlugin.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package gate.miscutils;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Controller;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.Utils;
import gate.annotation.AnnotationSetImpl;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.GateRuntimeException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 *
 * @author Johann Petrak
 */
@CreoleResource(name = "FilterAnnotations",
        comment = "Filter annotatios by how the do(not) overlap with other annotations.",
        helpURL="")
public class FilterAnnotationsPR  extends AbstractLanguageAnalyser 
  implements ProcessingResource, ControllerAwarePR
{
  private static final long serialVersionUID = 1L;
  
  //****************************
  // PARAMETERS
  //****************************
  
  /**
   * Annotation set in which the filtering should be done.
   * If left empty, the default annotation set is used.
   */
  public String toFilterAS;
  @CreoleParameter(comment="Annotation set in which to filter annotations (default annotation set if left empty)",defaultValue="")
  @RunTime
  @Optional  
  public void setToFilterAS(String value) { toFilterAS = value; }
  public String getToFilterAS() { return toFilterAS; }
     
  public List<String> toFilterTypes;
  @CreoleParameter(comment="Annotation types to filter, if empty filter all annotations in the set",defaultValue="")
  @RunTime
  @Optional
  public void setToFilterTypes(List<String> value) { toFilterTypes = value; }
  public List<String> getToFilterTypes() { return toFilterTypes; }

  public String byAS;
  @CreoleParameter(comment="Annotation set that contains the annotation to filter by",defaultValue="")
  @RunTime
  @Optional  
  public void setByAS(String value) { byAS = value; }
  public String getByAS() { return byAS; }
     
  
  // if type is specified, those annotations are used
  // if type and feature name are specified, only annotations where that feature is null/missing are used
  // if type and feature name and "==" but no value are specified, annotations where that features
  // is empty (toString is the empty string) are used.
  // if everything is specified, the toString of the value of the feature must match exactly. 
  
  public List<String> byAnnotationSpecs;
  @CreoleParameter(comment="Specifications of which annotations to filter by. Format Type[.feature==value]",defaultValue="")
  @RunTime
  @Optional  
  public void setByAnnotationSpecs(List<String> value) { byAnnotationSpecs = value; }
  public List<String> getByAnnotationSpecs() { return byAnnotationSpecs; }
  
  
  public FilterCriterion filterCriterion;
  @CreoleParameter(comment="How to filter; containing means the toFilter annotation contains a byFilter annotation",defaultValue="OVERLAPPING")
  @RunTime
  public void setFilterCriterion(FilterCriterion value) { filterCriterion = value; }
  public FilterCriterion getFilterCriterion() { return filterCriterion; }
     
  public String outPutSet;
  @CreoleParameter(comment="If this is non-empty, the name of a set where copies of the deleted annotations get stored, with the _filteredBy feature added.",defaultValue="")
  @RunTime
  @Optional  
  public void setOutputSet(String value) { outPutSet = value; }
  public String getOutputSet() { return outPutSet; }
        
  
  
  
  // *******************************************
  // CLASS FIELDS
  // *******************************************
  
  
  protected static final Logger logger = Logger
          .getLogger(FilterAnnotationsPR.class);
  
  List<AnnotationSpecification> annotationSpecifications;  
  
  //*******************************************************
  // STANDARD PR METHODS
  //*******************************************************
  
  // not needed for now ...
  //@Override
  //public Resource init() throws ResourceInstantiationException {
  //  super.init();
  //  return this;
  //}
  
  //@Override
  //public void reInit() {}

  
  //@Override
  //public void cleanup() {
  //}

  //@Override
  //public void interrupt() {
  //}
  
  
  //***********************************************************************
  // EXECUTE
  //***********************************************************************
  
  @Override
  public void execute() {
    AnnotationSet bySet = getDocument().getAnnotations(getByAS());
    AnnotationSetImpl byAnns = new AnnotationSetImpl(bySet.getDocument());
    for(AnnotationSpecification annSpec : annotationSpecifications) {
      byAnns.addAll(getAnnotationsForSpec(bySet,annSpec));
    }
    AnnotationSet outputSet = null;
    if(getOutputSet() != null && !getOutputSet().isEmpty()) {
      outputSet = getDocument().getAnnotations(getOutputSet());
    }
    AnnotationSet toFilter = null;
    if(getToFilterTypes() == null || getToFilterTypes().isEmpty()) {
      toFilter = getDocument().getAnnotations(getToFilterAS());
      //System.out.println("Annotations to filter, no type: "+toFilter.size());
    } else {
      Set<String> types = new HashSet<String>();
      types.addAll(getToFilterTypes());
      toFilter = getDocument().getAnnotations(getToFilterAS()).get(types);
      //System.out.println("Annotations to filter, has types: "+toFilter.size());
    }
    //System.out.println("Filter by annotations: "+byAnns.size());
    Set<Annotation> toDelete = new HashSet<Annotation>();
    // TODO: we could optimize here: if the criterion is not a negative one and we do not have 
    // any by annotations, then the whole loop can be skipped because we will never find anything to
    // filter!
    for(Annotation ann : toFilter) {
      AnnotationSet checkSet;
      if(getFilterCriterion() == FilterCriterion.COEXTENSIVE || getFilterCriterion() == FilterCriterion.NOT_COEXTENSIVE) {
        checkSet = Utils.getCoextensiveAnnotations(byAnns, ann);
      } else if(getFilterCriterion() == FilterCriterion.OVERLAPPING || getFilterCriterion() == FilterCriterion.NOT_OVERLAPPING) {
        checkSet = Utils.getOverlappingAnnotations(byAnns, ann);
      } else if(getFilterCriterion() == FilterCriterion.CONTAINED || getFilterCriterion() == FilterCriterion.NOT_CONTAINED) {
        // here we need to find out of the annotation ann is contained by any of the annotation in byAnn
        checkSet = Utils.getCoveringAnnotations(byAnns, ann);
      } else if(getFilterCriterion() == FilterCriterion.CONTAINING || getFilterCriterion() == FilterCriterion.NOT_CONTAINING) {
        checkSet = Utils.getContainedAnnotations(byAnns, ann);
      } else {
        throw new GateRuntimeException("Logic stopped working");
      }
      // now if the filter criterion is "NOT_", filter if checkSet has no annotations, otherwise
      // filter if checkSet does have any annotations
      //System.out.println("Processing ann="+ann+" found "+checkSet.size());
      boolean removeIt = false;
      if(getFilterCriterion() == FilterCriterion.NOT_COEXTENSIVE || getFilterCriterion() == FilterCriterion.NOT_CONTAINED ||
         getFilterCriterion() == FilterCriterion.NOT_CONTAINING  || getFilterCriterion() == FilterCriterion.NOT_OVERLAPPING) {
        removeIt = (checkSet.size() == 0);
      } else {
        removeIt = (checkSet.size() > 0);
      }
      if(removeIt) {
        toDelete.add(ann);
      }
    }
    if(outputSet != null) {
      for(Annotation ann : toDelete) {
        FeatureMap fm = Utils.toFeatureMap(ann.getFeatures());
        fm.put("_filteredBy", this.getName());
        Utils.addAnn(outputSet, Utils.start(ann), Utils.end(ann), ann.getType(), fm);
      }
    }
    getDocument().getAnnotations(getToFilterAS()).removeAll(toDelete);
  }  
  
  //*********************************************************
  // CONTROLLER CALLBACKS
  //*********************************************************
  
  @Override
  public void controllerExecutionStarted(Controller c)
      throws ExecutionException {
    // check runtime parameters and convert the annotation specifications
    // into actual specification objects.
    if(getByAnnotationSpecs() == null || getByAnnotationSpecs().isEmpty()) {
      throw new GateRuntimeException("Parameter byAnnotationSpecs must not be empty");
    }
    annotationSpecifications = createAnnotationSpecifications(getByAnnotationSpecs());
  }  
  
  @Override
  public void controllerExecutionFinished(Controller c)
      throws ExecutionException {
  }  
  @Override
  public void controllerExecutionAborted(Controller c, Throwable th)
      throws ExecutionException {
  }  
  
  static public List<AnnotationSpecification> createAnnotationSpecifications(List<String> asString) {
    List<AnnotationSpecification> annotationSpecifications = new ArrayList<AnnotationSpecification>();
    for(String annSpecString : asString) {
      //String annSpecStringOrig = annSpecString;
      AnnotationSpecification annSpec = new AnnotationSpecification();
        int dotIndex = annSpecString.indexOf('.');
        if(dotIndex < 0) {
          annSpec.typeName = annSpecString;
        } else {
          annSpec.typeName = annSpecString.substring(0,dotIndex);
          if(annSpecString.length() == dotIndex+1) {
            annSpec.featureName = "";
          } else {
            annSpecString = annSpecString.substring(dotIndex+1);
            int eqindex = annSpecString.indexOf("==");
            if(eqindex<0) {
              annSpec.featureName = annSpecString;
            } else {
              annSpec.featureName = annSpecString.substring(0,eqindex);
              if(annSpecString.length() == eqindex+2) {
                annSpec.featureValueString = "";
              } else {
                annSpec.featureValueString = annSpecString.substring(eqindex+2);
              }
            }
          }
        }
      annotationSpecifications.add(annSpec);
      //System.out.println("DEBUG: converted "+annSpecStringOrig+" into "+annSpec);
    }
    return annotationSpecifications;
  }
  
  
  /**
   * Return annotations from the set that match the given sepcification. 
   * @param set
   * @param spec
   * @return 
   */
  static public AnnotationSet getAnnotationsForSpec(AnnotationSet set, AnnotationSpecification spec) {
    if(spec.typeName != null) {
      set = set.get(spec.typeName);
    }
    AnnotationSetImpl ret = new AnnotationSetImpl(set.getDocument());
    if(spec.featureName != null) {
      for(Annotation ann : set) {
        Object val = ann.getFeatures().get(spec.featureName);
        if(spec.featureValueString == null) {
          if(val == null) { ret.add(ann); }
        } else {
          String valString = null;
          if(val != null) { valString = val.toString(); }
          if(spec.featureValueString.equals(valString)) {
            ret.add(ann);
          }
        }
      }
    } else {
      ret.addAll(set);
    }
    System.out.println("Set for "+spec+" contains "+ret.size());
    return ret;
  }
  
  
  
  public static enum FilterCriterion {
    OVERLAPPING,
    COEXTENSIVE,
    CONTAINED,
    CONTAINING,
    NOT_OVERLAPPING,
    NOT_COEXTENSIVE,
    NOT_CONTAINED,
    NOT_CONTAINING
  }
  
  public static class AnnotationSpecification {
    public String typeName = null;
    public String featureName = null;
    public String featureValueString = null;    
    @Override
    public String toString() {
      return ("AnnSpec:type="+typeName+",fName="+featureName+",fValue="+featureValueString);
    }
  }
  
}


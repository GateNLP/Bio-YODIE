/*
 * mergeCoextensive.java
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

import gate.Utils;
import gate.*;
import gate.util.GateRuntimeException;
import java.util.*;
import gate.trendminer.lodie.utils.LodieUtils;
import gate.trendminer.lodie.utils.Pair;

// For each LookupList annotation in the inputAS, find all coextensive annotations
// and merge them into a single annotation

// NOTE: in theory it should not matter which list gets merged into which, but 
// because of a bug in preparation as of 2015-03, we may get slightly different
// candidates for the same inst in two different lists. 
// In that case, a different candidate may remain, depending on which list 
// we merge into (which was non-deterministic in the original version of this
// program). We therefore try to make sure that we will process coextensive
// annotations in the same order always. 
// Strategy to do this for now:
// Instead of collecting pairs of list annotations to merge, collect list by offset.
// Sort the list by minorType and string and then merge the 2nd, 3rd ... into the first in that order

@Override
public void execute() {
  AnnotationSet lls = inputAS.get("LookupList");
  Set<Annotation> seen = new HashSet<Annotation>();
  Map<Pair<Long,Long>,Set<Annotation>> toDo = new HashMap<Pair<Long,Long>,Set<Annotation>>();
  for(Annotation ll : lls) {
    //logger.info("Checking: "+ll);
    if(seen.contains(ll)) {
      continue;
    }
    
    AnnotationSet coexts = Utils.getCoextensiveAnnotations(lls,ll);
    if(coexts.size() > 1) {
      seen.addAll(coexts);
      Long start = ll.getStartNode().getOffset();
      Long end   = ll.getEndNode().getOffset();
      toDo.put(new Pair(start,end),coexts);
    }
  }
  // now we have a map with all sets of coextensive annotations. We now want to 
  // merge them into a single annotation, but in a deterministic way.
  // we do this by sorting all annotations according to their minorType and string feature values
  
  
  for(Pair<Long,Long> range : toDo.keySet()) {
    // get the set of coextensive annotations and convert it into a list
    List<Annotation> list = new ArrayList<Annotation>();
    list.addAll(toDo.get(range));
    // System.out.println("DEBUG: offsets: "+range+" list is "+list);
    // sort the list by the values of the minorType and string features
    Collections.sort(list,sorter);
    // System.out.println("DEBUG: after sorting, list is "+list);
    //logger.info("Merge: "+pair.value1+"/"+pair.value2);
    Annotation target = list.get(0);
    for(int i=0; i<list.size(); i++) {
      LodieUtils.mergeListAnns(inputAS,target,list.get(i),true,"inst","label");
    }
    // System.out.println("DEBUG: after merge: "+target);
    //logger.info("Merge after: "+pair.value1);
  }
  
}

private Sorter sorter;

@Override
public void init() {
  sorter = new Sorter();
}

private class Sorter implements Comparator<Annotation> {
  @Override
  public int compare(Annotation ann1, Annotation ann2) {
    String mt1 = (String)ann1.getFeatures().get("minorType");
    String mt2 = (String)ann1.getFeatures().get("minorType");
    String st1 = (String)ann1.getFeatures().get("string");
    String st2 = (String)ann1.getFeatures().get("string");
    if(mt1==null) { mt1 = ""; }
    if(mt2==null) { mt2 = ""; }
    if(st1==null) { st1 = ""; }
    if(st2==null) { st2 = ""; }
    return(st1.compareTo(st2));
  }
}

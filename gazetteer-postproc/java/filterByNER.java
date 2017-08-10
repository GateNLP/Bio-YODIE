/*
 * filterByNER.java
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
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collections;


// Simple filter which removes all Lookup annotations from the input set 
// (and adds a copy to the output set)
// which do not overlap with one of the NER annotation types 
// (either from ANNIE or StanfordNER or any of the two) in the default set.
// TODO This should be a config some time, but at the moment we always check
// for either!

HashSet<String> types = new HashSet<String>();

@Override
public void initPr() {
  types.add("Person");
  types.add("PERSON");
  types.add("Location");
  types.add("LOCATION");
  types.add("Organization");
  types.add("ORGANIZATION");
  types.add("MISC");
}


@Override
public void execute() {
  AnnotationSet ners = doc.getAnnotations().get(types);
  ArrayList<Annotation> toDelete = new ArrayList<Annotation>();
  for(Annotation lookup : inputAS.get("Lookup")) {
    // check if the current annotation overlaps with any of the NERs
    AnnotationSet overlaps = gate.Utils.getOverlappingAnnotations(ners,lookup);
    if(overlaps.size() == 0) {
      // no overlaps, remove but also add to the output set a copy!
      outputAS.get(gate.Utils.addAnn(outputAS,lookup,lookup.getType(),lookup.getFeatures())).getFeatures().put("deletedBecause","notOverlapsWithNE");
      toDelete.add(lookup);
    }
  }
  inputAS.removeAll(toDelete);
}
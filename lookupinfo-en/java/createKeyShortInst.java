/*
 * createKeyShortInst.java
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

import java.util.*;
import gate.*;
import gate.util.*;


@Override
public void execute() {
  AnnotationSet keyshort = doc.getAnnotations("KeyShortInst");
  keyshort.clear();
  AnnotationSet keys = doc.getAnnotations("Key").get("Mention");
  for(Annotation ann : keys) {
    // copy the annotation to the key4list set, and change the inst feature value first
    FeatureMap fm = Utils.toFeatureMap(ann.getFeatures());
    String inst = (String)fm.get("inst");
    inst=inst.replaceAll("http://dbpedia\\.org/resource/","");
    inst=inst.replaceAll("http://[a-z]+\\.dbpedia\\.org/resource/","");
    fm.put("inst",inst);
    Utils.addAnn(keyshort,ann,"Mention",fm);
  }
}

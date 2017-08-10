/*
 * type-filter.java
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

import gate.*;
import java.util.HashSet;

String typeList = 
   "Acquired Abnormality;"
 + "Anatomical Abnormality;"
 + "Antibiotic;"
 + "Body Location;"
 + "Region;"
 + "Body Part, Organ, or Organ Component;"
 + "Body Space or Junction;"
 + "Body System;"
 + "Cell or Molecular Dysfunction;"
 + "Clinical Drug;"
 + "Congenital Abnormality;"
 + "Diagnostic Procedure;"
 + "Disease or Syndrome;"
 + "Experimental Model of Disease;"
 //+ "Finding;"
 + "Injury or Poisoning;"
 + "Laboratory Procedure;"
 + "Laboratory or Test Result;"
 + "Mental or Behavioral Dysfunction;"
 //+ "Mental Process;"
 + "Molecular Biology Research Technique;"
 + "Neoplastic Process;"
 + "Pathologic Function;"
 + "Pharmacologic Substance;"
 + "Research Activity;"
 + "Sign or Symptom;"
 + "Tissue;"
 + "Individual Behavior;"
 + "Clinical Attribute;"
 + "Anatomical Structure;"
 + "Health Care Activity;"
 + "Health Care Related Organization;"
 + "Self-help or Relief Organization;"
 + "Therapeutic or Preventive Procedure;"
 + "Temporal Concept";

String[] types = null;

@Override
public void init() {
  String conf = System.getProperty("lodie.finalize.type-filter.typeList");
  if(conf != null) {
    typeList = conf;
    System.out.println("INFO: using type list "+ conf);
  }
  types = typeList.split(";");
}

@Override
public void execute() {
  HashSet<Annotation> toremove = new HashSet<Annotation>();
  for(Annotation ann : inputAS.get("Mention")){
   String stys = ann.getFeatures().get("STY").toString();
   boolean found = false;
   if(stys!=null){
    for(String type : types){
     if(stys.contains(type)){
      found = true;
      break;
     }
    }
   }
   if(!found) toremove.add(ann);
  }
  for(Annotation ann : toremove){
    inputAS.remove(ann);
  }
}

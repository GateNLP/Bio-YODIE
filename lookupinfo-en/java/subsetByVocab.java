/*
 * subsetByVocab.java
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
 * G. Gorrell, 13 February 2017
 */

import gate.Utils;
import gate.*;
import gate.util.GateRuntimeException;
import java.util.*;
import gate.trendminer.lodie.utils.LodieUtils;
import gate.trendminer.lodie.utils.Pair;


@Override
public void execute() {
  if(parms.get("VOCABS")!=null){
    AnnotationSet lls = inputAS.get("LookupList");
    String[] vocabs = parms.get("VOCABS").toString().split(";");
    Set<Annotation> toremove = new HashSet<Annotation>();
    AnnotationSet debug = doc.getAnnotations("deleted-vocabs");
    for(Annotation ll:lls){
      ArrayList<Integer> ids = (ArrayList<Integer>)ll.getFeatures().get("ids");
      ArrayList<Integer> idstoremove = new ArrayList<Integer>();
      for(Integer id:ids){
        boolean tokeep = false;
        Annotation lookup = inputAS.get(id);
        String[] vocabsOnAnn = lookup.getFeatures().get("CUIVOCABS").toString().split(",");
        for(int i=0;i<vocabs.length;i++){
          for(int j=0;j<vocabsOnAnn.length;j++){
            if(vocabsOnAnn[j].equals(vocabs[i])){
              tokeep = true;
            }
          }
        }
        if(!tokeep){
          toremove.add(lookup);
          idstoremove.add(id);
        }
      }
      for(Integer id:idstoremove){
        ids.remove(id);
      }
      if(((ArrayList<Integer>)ll.getFeatures().get("ids")).size()==0) toremove.add(ll);
    }
    for(Annotation rem:toremove){
      debug.add(rem);
      inputAS.remove(rem);
    }
  }
}

@Override
public void init() {
}

/*
 * adaptStanford.java
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
 * 26 September 2016
 */

import java.util.*;
import gate.*;


@Override
public void execute() {
  // first map all the POS tags we are interested in from STTS to Penn  
  // this is a hack and not really possible properly!!! 
  // SSTS does not care about singular plural whie penn does but ssts is much more
  // detailed about verbs than penn

  for(Annotation token : inputAS.get("Token")) {
    FeatureMap fm = token.getFeatures();
    String cat = (String)fm.get("category");
    if(cat != null) {
      // normal noun
      if(cat.startsWith("nc")) {
        // use PENN
        cat = "NN";
      } else if(cat.startsWith("np")) {
        cat = "NNP";
      } 
      // TODO: add as needed or change in some other way!
    }
    fm.put("category",cat);
  }
  Set<Annotation> toDelete = new HashSet<Annotation>();
  for(Annotation a : inputAS.get("PERS")) { 
    toDelete.add(a);
    Utils.addAnn(outputAS,a,"PERSON",Utils.toFeatureMap(a.getFeatures()));
  }
  for(Annotation a : inputAS.get("LUG")) { 
    toDelete.add(a);
    Utils.addAnn(outputAS,a,"LOCATION",Utils.toFeatureMap(a.getFeatures()));
  }
  for(Annotation a : inputAS.get("ORG")) { 
    toDelete.add(a);
    Utils.addAnn(outputAS,a,"ORGANIZATION",Utils.toFeatureMap(a.getFeatures()));
  }
  for(Annotation a : inputAS.get("OTROS")) { 
    toDelete.add(a);
    Utils.addAnn(outputAS,a,"MISC",Utils.toFeatureMap(a.getFeatures()));
  }
  inputAS.removeAll(toDelete);
}

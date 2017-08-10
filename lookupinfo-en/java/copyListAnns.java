/*
 * copyListAnns.java
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

// Copy the LookupList and the referenced Lookup annotations 
// from the inputAS to the outputAS.
// This step is done so we can evaluate the max recall later,
// based on ALL candidates we initially have, not just the ones
// that remain at a later step.

import gate.*;
import gate.trendminer.lodie.utils.LodieUtils;

@Override
public void execute() {
  LodieUtils.copyListAnns(inputAS, outputAS, "LookupList");
}

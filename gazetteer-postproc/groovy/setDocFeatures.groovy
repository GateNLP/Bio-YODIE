/*
 * setDocFeatures.groovy
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

// Set document features from config properties


void beforeCorpus(c) {
  // we expect the system property "lodie.home" to be set to the root of the 
  // application directory
  if(!binding.hasVariable("config")) {    
    config = new Properties()
    String home = System.getProperty("lodie.home")
    if(home == null) {
      throw new GateRuntimeException("System property lodie.home is not set!")
    }
    // now see if there is a config file called "lodie.config.properties" in the lodie.home directory 
    File configFile = new File(new File(home),"lodie.config.properties")
    if(configFile.exists()) {
      // read the config file
      config.load(new FileInputStream(configFile))
    }
    // try to get the property which represents the run-removeLookups feature value
    String parm = config.getProperty("lodie.gazetteer-postproc.run-removeLookups")
    if(parm != null) {
      // doc.getFeatures.put("run-removeLookups",parm)
      featureRunRemoveLookups = parm
    } else {
      println("No config setting lodie.gazetteer-postproc.run-removeLookups not present, using true")
      // doc.getFeatures.put("run-removeLookups","true") // DEFAULT
      featureRunRemoveLookups = "true"
    }
  } // binding config does not exist
} // beforeCorpus

doc.getFeatures().put("run-removeLookups",featureRunRemoveLookups)


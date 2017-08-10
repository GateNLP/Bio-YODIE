package com.jpetrak.gate.stringannotation.extendedgazetteer;

import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.util.GateException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;


/**
 *
 * @author Johann Petrak
 */
public class GenerateCache {
    public static void main(String[] args) {
      if(!(args.length == 3)) {
        System.err.println("Need 3 parameters: config/def file URL, case-sensitive, locale");
        System.err.println("Where case sensitive is true or false");
        System.exit(1);
      }
      java.net.URL url = null;
      String deffileName = args[0];
      File deffile = new File(args[0]);
      String cacheFileName = deffileName.replaceAll("\\.[a-z]+$",".gazbin");
      File cacheFile = new File(cacheFileName);
      if(cacheFile.exists()) {
        System.err.println("Cache file exists: "+cacheFileName);
        if(cacheFile.delete()) {
          System.err.println("Cache file successfully deleted");
        } else {
          System.err.println("Could not delete cache file something is wrong");
          System.exit(1);
        }
      } else {
        System.err.println("Cache file does not yet exist: "+cacheFileName);
      }
      try {
        url = deffile.toURI().toURL();
      } catch (MalformedURLException ex) {
        System.err.println("Could not convert file path into a URL");
        ex.printStackTrace(System.err);
        System.exit(1);
      }
      String locale = args[2];
      boolean caseSensitive = Boolean.parseBoolean(args[1]);
      
      String pluginDir = System.getProperty("pluginDir");
      if(pluginDir == null) {
        System.err.println("Property 'pluginDir' not set");
        System.exit(1);
      }
      String gateHome = System.getProperty("gate.home");
      if(gateHome == null) {
        System.err.println("Property 'gate.home' not set");
        System.exit(1);
      }
      Gate.setGateHome(new File(gateHome));
      Gate.runInSandbox(true);
      try {
        Gate.init();
      } catch (GateException ex) {
        System.err.println("Could not initialize Gate");
        ex.printStackTrace(System.err);
        System.exit(1);
      }
      URL pluginURL = null;
      try {
        pluginURL = new File(pluginDir).toURI().toURL();
      } catch (MalformedURLException ex) {
        System.err.println("Could not convert plugin directory to URL "+pluginDir);
        ex.printStackTrace(System.err);
        System.exit(1);
      }
      try {
        Gate.getCreoleRegister().registerDirectories(pluginURL);
      } catch (GateException ex) {
        System.err.println("Could not register plugin") ;
        ex.printStackTrace(System.err);
        System.exit(1);
      }
      
      FeatureMap parms = Factory.newFeatureMap();
      parms.put("configFileURL", url);
      parms.put("caseSensitive", caseSensitive);
      parms.put("caseConversionLanguage", locale);
      // parms.put("gazetteerFeatureSeparator","\\t");
      try {
        gate.ProcessingResource pr = 
          (gate.ProcessingResource) Factory.createResource(
                "com.jpetrak.gate.stringannotation.extendedgazetteer.ExtendedGazetteer",
                parms);
      } catch (Exception ex) {
        System.err.println("Error initializing the gazetteer PR");
        ex.printStackTrace(System.err);
        System.exit(1);
      }
      System.err.println("DONE");
    }
  
}

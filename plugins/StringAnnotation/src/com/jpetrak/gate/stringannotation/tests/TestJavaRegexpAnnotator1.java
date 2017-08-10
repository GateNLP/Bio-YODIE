package com.jpetrak.gate.stringannotation.tests;

import static org.junit.Assert.*;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jpetrak.gate.stringannotation.regexp.JavaRegexpAnnotator;
import com.jpetrak.gate.stringannotation.regexp.MatchPreference;

public class TestJavaRegexpAnnotator1 {

  private static boolean isInitialized = false; 
  private static File testingDir;
  @BeforeClass
  public static void init() throws GateException, MalformedURLException {
    if(!isInitialized) {
      System.out.println("Tests1: Inititalizing ...");
      isInitialized = true;
      Gate.runInSandbox(true);
      Gate.init();
      File pluginHome = new File(".");
      System.out.println("Plugin home directory is "+pluginHome.getAbsolutePath());
      Gate.getCreoleRegister().registerDirectories(
              pluginHome.toURI().toURL());
      testingDir = new File(pluginHome,"tests");
      assertTrue("Directory 'tests' does not exist",testingDir.exists());
    } else {
      System.out.println("Already initialized ...");
    }
  }
  
  @AfterClass
  public static void cleanup() throws Exception {
    System.out.println("Tests1: Cleaning up ...");
  }
  
  @Test
  public void testJavaRegexpAnnotator1() 
      throws MalformedURLException, ResourceInstantiationException, ExecutionException {
    System.out.println("Running JavaRegexpAnnotator test class 1  tester 1");
    FeatureMap parms = Factory.newFeatureMap();
    File rulesFile = new File(testingDir,"regexp1.rules");
    URL rulesURL = rulesFile.toURI().toURL();
    parms.put("patternFileURL", rulesURL);
    JavaRegexpAnnotator jra = (JavaRegexpAnnotator)Factory.createResource(
            "com.jpetrak.gate.stringannotation.regexp.JavaRegexpAnnotator", parms);
    // load the document
    parms = Factory.newFeatureMap();
    File docFile = new File(testingDir,"extgaz2docprep.xml");
    parms.put("sourceUrl",docFile.toURI().toURL());
    Document doc = (Document) 
         Factory.createResource("gate.corpora.DocumentImpl", parms);
    // check if we got the correct annotations
    AnnotationSet tokens = doc.getAnnotations().get("Token");
    assertEquals(46,tokens.size());
    AnnotationSet sentences = doc.getAnnotations().get("Sentence");
    assertEquals(4,sentences.size());

    AnnotationSet lookups = doc.getAnnotations("Out").get("Lookup");
    assertEquals(0,lookups.size());
        
    // set runtime parameters and run the PR on the document
    doc.getAnnotations("Out").removeAll(lookups);
    jra.setOverlappingMatches(true);
    jra.setOutputAnnotationSet("Out");
    jra.setMatchPreference(MatchPreference.FIRSTRULE);
    jra.setDocument(doc);    
    jra.execute();
    lookups = doc.getAnnotations("Out").get("Lookup");
    assertEquals(26,lookups.size());
    
    // set runtime parameters and run the PR on the document
    doc.getAnnotations("Out").removeAll(lookups);
    jra.setOverlappingMatches(false);
    jra.setOutputAnnotationSet("Out");
    jra.setMatchPreference(MatchPreference.FIRSTRULE);
    jra.setDocument(doc);    
    jra.execute();
    lookups = doc.getAnnotations("Out").get("Lookup");
    assertEquals(14,lookups.size());
    
    doc.getAnnotations("Out").removeAll(lookups);
    jra.setMatchPreference(MatchPreference.LONGEST_LASTRULE);
    jra.setDocument(doc);    
    jra.execute();
    lookups = doc.getAnnotations("Out").get("Lookup");
    assertEquals(12,lookups.size());
    
    // same with containing annotation set Sentence
    doc.getAnnotations("Out").removeAll(lookups);
    jra.setMatchPreference(MatchPreference.LONGEST_LASTRULE);
    jra.setContainingAnnotationType("Sentence");
    jra.setDocument(doc);    
    jra.execute();
    lookups = doc.getAnnotations("Out").get("Lookup");
    assertEquals(12,lookups.size());
    
    // same with indirect matching of the Token.kind feature, not containing annotation 
    doc.getAnnotations("Out").removeAll(lookups);
    jra.setMatchPreference(MatchPreference.LONGEST_LASTRULE);
    jra.setContainingAnnotationType("");
    jra.setInputAnnotationType("Token");
    jra.setSpaceAnnotationType("SpaceToken");
    jra.setTextFeature("kind");
    jra.setDocument(doc);    
    jra.execute();
    lookups = doc.getAnnotations("Out").get("Lookup");
    assertEquals(29,lookups.size());
    
    
    
    System.out.println("JavaRegexpAnnotator test class 1  tester 1 finished");
  }
}

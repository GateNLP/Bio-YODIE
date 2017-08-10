// This is the class from which all the objects generated
// for a script inherit
package com.jpetrak.gate.scala;

import gate.Document;
import gate.Controller;
import gate.AnnotationSet;
import gate.Corpus;
import gate.FeatureMap;
import gate.Resource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class ScalaScript {
  public static volatile ConcurrentMap<String,Object> globalsForAll = new ConcurrentHashMap<String,Object>();
  public void setGlobalsForAll(ConcurrentMap<String,Object> val) { globalsForAll = val; }
  public ConcurrentMap<String,Object> getGlobalsForAll() { return globalsForAll; }
  public volatile ConcurrentMap<String,Object> globalsForPr = null;  // will be set by JavaSriptingPR
  public Resource resource1 = null;
  public Resource resource2 = null;
  public Resource resource3 = null;
  public Document doc = null;
  public Controller controller = null;
  public Corpus corpus = null;
  public AnnotationSet inputAS = null;
  public String inputASName = null;
  public String outputASName = null;
  public AnnotationSet outputAS = null;
  public FeatureMap parms = null;
  public int duplicationId = 0;
  public void execute() { 
  }
  public void controllerStarted()  { }
  public void controllerFinished()  { }
  public void controllerAborted( Throwable throwable)  { }
  public void initPr() { }
  public void initAll() { }
  public void cleanupPr() { }
  
  void callExecute() {
    callInitAll();
    callInitPr();
    execute();
  }
  // This is an object that allows to serialize code so that only one instance
  // of several custom-duplicated ones will be run.
  // This will get set, by the PR which creates the first instance of this class
  // for a script. When that PR gets c
  public volatile Object lockForPr = null;
  public volatile boolean initializedForPr = false;
  private void callInitPr() { 
    if(initializedForPr) return;
    synchronized(lockForPr) {
      if(!initializedForPr) {
        initPr();
        initializedForPr = true;
      }
    }
  }
  // This is an object that loows to serialize code so that only one instance
  // of all JavaScripting instances in the VM will run. 
  public static volatile Object lockForAll = new Object();
  public static volatile boolean initializedForAll = false;
  private void callInitAll() { 
    if(initializedForAll) return;
    synchronized(lockForAll) {
      if(!initializedForAll) {
        initAll();
        initializedForAll = true;
      }
    }
  }
  
  public static void resetInitAll() {
    synchronized(lockForAll) {
      initializedForAll = false;
      globalsForAll = new ConcurrentHashMap<String,Object>();
    }
  }
  
}

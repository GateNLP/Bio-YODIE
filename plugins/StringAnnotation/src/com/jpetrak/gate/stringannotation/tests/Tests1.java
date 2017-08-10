package com.jpetrak.gate.stringannotation.tests;

import static org.junit.Assert.*;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.AnnotationDiffer;
import gate.util.GateException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jpetrak.gate.stringannotation.extendedgazetteer.ExtendedGazetteer;
import com.jpetrak.gate.stringannotation.extendedgazetteer.FeatureGazetteer;
import com.jpetrak.gate.stringannotation.extendedgazetteer.Lookup;
import com.jpetrak.gate.stringannotation.extendedgazetteer.State;
import com.jpetrak.gate.stringannotation.extendedgazetteer.trie.GazStoreTrie3;
import com.jpetrak.gate.stringannotation.utils.StoreArrayOfCharArrays;
import com.jpetrak.gate.stringannotation.extendedgazetteer.trie.StoreCharMapPhase1;
import com.jpetrak.gate.stringannotation.extendedgazetteer.trie.StoreStates;

public class Tests1 {

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
      FileFilter fileFilter = new WildcardFileFilter("*.gazbin");
      File[] files = testingDir.listFiles(fileFilter);
      for(File file : files) {
        file.delete();
      }
    } else {
      System.out.println("Already initialized ...");
    }
  }
  
  @AfterClass
  public static void cleanup() throws Exception {
    System.out.println("Tests1: Cleaning up ...");
  }
  

  @Test
  public void testStore()  {
    StoreArrayOfCharArrays as = new StoreArrayOfCharArrays();
    
    int i;
    char[] r;
    i = as.addData("asdfjk".toCharArray());
    assertEquals(0,i);
    i = as.addData("qwertyqwerty".toCharArray());
    assertEquals(8,i);
    r = as.getData(0);
    assertEquals("asdfjk",new String(r));
    r = as.getData(8);
    assertEquals("qwertyqwerty",new String(r));
    
    // test the list methods
    i = as.addListData("l1:4567890".toCharArray());
    assertEquals(22,i);
    int size = as.getListSize(i);
    assertEquals(1,size);
    r = as.getListData(i, 0);
    assertEquals("l1:4567890",new String(r));
    
    i = as.addListData(i,"l2:4567890".toCharArray());
    assertEquals(22,i);
    size = as.getListSize(i);
    assertEquals(2,size);
    r = as.getListData(i,1);
    assertEquals("l2:4567890",new String(r));
        
    // check if the first element (element 0) is still the same!
    r = as.getListData(i,0);
    assertEquals("l1:4567890",new String(r));
    
    i = as.addListData(i,"l3:4567890".toCharArray());
    assertEquals(22,i);
    size = as.getListSize(i);
    assertEquals(3,size);
    r = as.getListData(i,2);
    assertEquals("l3:4567890",new String(r));
    
    i = as.addListData("another".toCharArray());
    size = as.getListSize(i);
    assertEquals(1,size);
    r = as.getListData(i,0);
    assertEquals("another",new String(r));

    char[] f1 = "fixed data 01".toCharArray();
    int i1 = as.addFixedLengthData(f1);
    int i2 = as.addData("last".toCharArray());
    r = as.getFixedLengthData(i1, f1.length);
    assertNotNull(r);
    assertEquals(r.length,f1.length);
    assertEquals(new String(f1),new String(r));
    r = as.getData(i2);
    assertNotNull(r);
    assertEquals("last",new String(r));
    
    i = as.addListData("element1".toCharArray());
    as.addListData(i,"element2".toCharArray());
    int j = as.findListData(i, "element1".toCharArray());
    assertEquals(0,j);
    j = as.findListData(i, "element2".toCharArray());
    assertEquals(1,j);
  }
  
  
  @Test
  public void testLookupTrie3() {
    GazStoreTrie3 gs = new GazStoreTrie3();
    // delegate to the method in GazStoreTrie3 ...
    gs.runImplementationTests();
  }
  
  @Test
  public void testTrie3() {
    GazStoreTrie3 gs = new GazStoreTrie3();
    FeatureMap fm = Factory.newFeatureMap();
    fm.put("listFeature1","value1");
    fm.put("listFeature2","value2");
    int info1 = gs.addListInfo("Type1", "URL1", fm);
    String[] keyvals1 = new String[]{"key1","val1","key2","","k3","valuenumberthree"};
    
    gs.addLookup("asdf", info1, keyvals1);
    gs.addLookup("asdf", info1, keyvals1);
    gs.addLookup("asdf", info1, keyvals1);
    // TODO: check we have only added the lookup once!
    Iterator<Lookup> it = gs.match("asdf");
    assertNotNull(it);
    int nrLookups = 0;
    while(it.hasNext()) {
      it.next();
      nrLookups++;
    }
    assertEquals(1,nrLookups);
  }
  
  @Test
  public void testGazetteerApplication1BE3() 
      throws MalformedURLException, ResourceInstantiationException, ExecutionException {
    System.out.println("Running Gazetteer application test 1");
    FeatureMap parms = Factory.newFeatureMap();
    File defFile = new File(testingDir,"extgaz2.def");
    URL gazURL = defFile.toURI().toURL();
    parms.put("configFileURL", gazURL);
    ExtendedGazetteer eg = (ExtendedGazetteer)Factory.createResource(
            "com.jpetrak.gate.stringannotation.extendedgazetteer.ExtendedGazetteer", parms);
    // load the document
    parms = Factory.newFeatureMap();
    File docFile = new File(testingDir,"extgaz2docprep.xml");
    parms.put("sourceUrl",docFile.toURI().toURL());
    Document doc = (Document) 
         Factory.createResource("gate.corpora.DocumentImpl", parms);
    AnnotationSet lookups = doc.getAnnotations().get("OutType");
    assertEquals(0,lookups.size());
    // run the gazetteer on the document
    eg.setDocument(doc);
    eg.execute();
    // check if we got the correct annotations
    AnnotationSet tokens = doc.getAnnotations().get("Token");
    assertEquals(46,tokens.size());
    AnnotationSet sentences = doc.getAnnotations().get("Sentence");
    assertEquals(4,sentences.size());
    lookups = doc.getAnnotations().get("OutType");
    assertEquals(12,lookups.size());
    int i = 1;
    FeatureMap fm;
    long from;
    long to;
    for(Annotation ann : gate.Utils.inDocumentOrder(lookups)) {
      //System.out.println("Annotation: "+ann);
      fm = ann.getFeatures();
      String inst = (String)fm.get("inst");
      String string = (String)fm.get("_string");
      from = ann.getStartNode().getOffset();
      to = ann.getEndNode().getOffset();
      if(i == 1) {
        assertEquals(8,from);
        assertEquals(12,to);
        assertEquals("i1",inst);
        assertEquals("some",string);
      } else if(i == 5) {
        assertEquals(26,from);
        assertEquals(34,to);
        assertEquals("i11",inst);
        assertEquals("word and",string);
      }
      i++;
    }
    doc.getAnnotations().removeAll(lookups);
    eg.setMatchAtWordStartOnly(false);
    eg.setMatchAtWordEndOnly(false);
    eg.execute();
    lookups = doc.getAnnotations().get("OutType");
    assertEquals(22,lookups.size());
    doc.getAnnotations().removeAll(lookups);
    eg.setLongestMatchOnly(false);
    eg.execute();
    lookups = doc.getAnnotations().get("OutType");
    assertEquals(26,lookups.size());
    System.out.println("Gazetteer application test 1 finished");
  }
  
  public void testGazetteerApplication2BE3() 
      throws ResourceInstantiationException, ExecutionException, IOException {
    System.out.println("Running gazetteer application test 2 for news1pre");
    FeatureMap parms = Factory.newFeatureMap();
    File defFile = new File(testingDir,"annie/lists.def");
    URL gazURL = defFile.toURI().toURL();
    parms.put("configFileURL", gazURL);
    ExtendedGazetteer eg = (ExtendedGazetteer)Factory.createResource(
            "com.jpetrak.gate.stringannotation.extendedgazetteer.ExtendedGazetteer", parms);
    // load the document
    eg.setOutputAnnotationSet("EXT");
    parms = Factory.newFeatureMap();
    File docFile = new File(testingDir,"news1pre.xml");
    parms.put("sourceUrl",docFile.toURI().toURL());
    Document doc = (Document) 
         Factory.createResource("gate.corpora.DocumentImpl", parms);
    AnnotationSet lookups = doc.getAnnotations("EXT").get("Lookup");
    assertEquals(0,lookups.size());
    // run the gazetteer on the document
    eg.setDocument(doc);
    eg.execute();
    AnnotationDiffer differ = new AnnotationDiffer();
    differ.setSignificantFeaturesSet(new HashSet<String>());
    AnnotationSet keys = doc.getAnnotations().get("Lookup");
    System.out.println("Lookups old: "+keys.size());
    AnnotationSet responses = doc.getAnnotations("EXT").get("Lookup");
    System.out.println("Lookups new: "+responses.size());
    differ.calculateDiff(keys, responses);
    int correct = differ.getCorrectMatches();
    int falsePositives = differ.getFalsePositivesStrict();
    int missing = differ.getMissing();
    System.out.println("Diff: correct="+correct+" false positives="+falsePositives+" missing="+missing);
    File outFile = new File(testingDir,"news1pre_procBE.xml");
    FileUtils.writeStringToFile(outFile, doc.toXml(),"UTF-8");
    assertEquals(194,correct);
    assertEquals(33,falsePositives);    
    assertEquals(2,missing);
    System.out.println("Gazetteer application test 2 finished");
  }

  @Test
  public void testFeatureGazetteer1() throws MalformedURLException, ResourceInstantiationException, ExecutionException {
    System.out.println("Running FEATURE GAZETTEER application test");
    FeatureMap parms = Factory.newFeatureMap();
    File defFile = new File(testingDir,"extgaz2.def");
    URL gazURL = defFile.toURI().toURL();
    parms.put("configFileURL", gazURL);
    FeatureGazetteer eg = (FeatureGazetteer)Factory.createResource(
            "com.jpetrak.gate.stringannotation.extendedgazetteer.FeatureGazetteer", parms);
    // test matching directly
    Iterator<Lookup> ret = null;
    ret = eg.doMatch("some", true,true);
    System.out.println("Matching same: "+ret);
    // check that it works with a gazetteer list that does not have any features
    defFile = new File(testingDir,"extgaz3.def");
    gazURL = defFile.toURI().toURL();
    parms.put("configFileURL", gazURL);
    eg = (FeatureGazetteer)Factory.createResource(
        "com.jpetrak.gate.stringannotation.extendedgazetteer.FeatureGazetteer", parms);
    ret = eg.doMatch("word", true,true);
    System.out.println("Matching extgaz3 'word': "+eg.lookups2FeatureMaps(ret));
    ret = eg.doMatch("word", false,false);
    System.out.println("Matching extgaz3 'word': "+eg.lookups2FeatureMaps(ret));
    ret = eg.doMatch("word", true,true);
    System.out.println("Matching extgaz3 'word': "+eg.lookups2FeatureMaps(ret));
    ret = eg.doMatch("notthere", true,true);
    System.out.println("Matching extgaz3 'notthere': "+eg.lookups2FeatureMaps(ret));
    ret = eg.doMatch("test", true,true);
    System.out.println("Matching extgaz3 'test': "+eg.lookups2FeatureMaps(ret));
    ret = eg.doMatch("text", true,true);
    System.out.println("Matching extgaz3 'text': "+eg.lookups2FeatureMaps(ret));
    ret = eg.doMatch("thewordyes", false,false);
    System.out.println("Matching extgaz3 'thewordyes': "+eg.lookups2FeatureMaps(ret));
    
    System.out.println("Feature Gazetteer application test finished");
  }
  

  @Test
  public void testStoreCharMapPhase1() {
    StoreCharMapPhase1 store = new StoreCharMapPhase1(new StoreArrayOfCharArrays());
    System.out.println("Adding first:1");
    int i = store.put(-1, 'a', 1);
    assertEquals(0,i);
    System.out.println("Adding first:2");
    i = store.put(0,'b', 2);
    assertEquals(0,i);
    System.out.println("Adding second");
    i = store.put(-1,'a',3);
    assertEquals(1,i);
  }

  @Test
  public void testStoreStates1() {
    StoreArrayOfCharArrays backing = new StoreArrayOfCharArrays();
    StoreStates store = new StoreStates(backing);
    store.test();
    // creating a StoreStates object also creates the initial state, so the first
    // index must be 5
    int s1 = store.newCharMapState();
    assertEquals(5,s1);
    // also the initial state must have index 0
    assertEquals(0,store.initialState);
    int s2 = store.newCharMapState();
    assertEquals(10,s2);
    int s3 = store.newSingleCharState();
    assertEquals(15,s3);
    assertEquals(true,store.getIsCharMapState(s1));
    assertEquals(true,store.getIsCharMapState(s2));
    assertEquals(false,store.getIsCharMapState(s3));
    store.setLookupIndex(s1, 123);
    store.setLookupIndex(s2, 234);
    store.setLookupIndex(s3, 345);
    assertEquals(true,store.getIsCharMapState(s1));
    assertEquals(true,store.getIsCharMapState(s2));
    assertEquals(false,store.getIsCharMapState(s3));
    assertEquals(123,store.getLookupIndex(s1));
    assertEquals(234,store.getLookupIndex(s2));
    assertEquals(345,store.getLookupIndex(s3));
    assertEquals(-1,store.getCharMapIndex(s1));
    assertEquals(-1,store.getCharMapIndex(s2));
    assertEquals(0,store.getSingleCharStateChar(s3));
    store.put(store.initialState, 'a', s1);
    store.put(store.initialState, 'b', s2);
    store.put(store.initialState, 'c', s3);
    assertEquals(s1,store.next(store.initialState, 'a'));
    assertEquals(s2,store.next(store.initialState, 'b'));
    assertEquals(s3,store.next(store.initialState, 'c'));
  }

  @Test
  public void testGazStoreTrie3() throws IOException {
    System.out.println("GazStoreTrie3: *******************************");
    GazStoreTrie3 gs = new GazStoreTrie3();
    FeatureMap fm1 = Factory.newFeatureMap();
    fm1.put("feature1","value1");
    int info1 = gs.addListInfo("Type1", "TheFile", fm1);
    String[] kv1 = new String[2];
    kv1[0] = "key1of1";
    kv1[1] = "value1of1";
    gs.addLookup("entry", info1, kv1);
    String[] kv2 = new String[4];
    kv2[0] = "KEY1of2";
    kv2[1] = "VALUE1of2";
    kv2[2] = "KEY2of2";
    kv2[3] = "VALUE2of2";
    gs.addLookup("as", info1, kv2);
    State init = gs.getInitialState();
    System.out.println("Initial State: "+init);
    State s1 = init.next('a');
    System.out.println("State after a: "+s1);
    System.out.println("isFinal: "+s1.isFinal());
    State s2 = s1.next('s');
    System.out.println("State after s: "+s2);
    System.out.println("isFinal: "+s2.isFinal());
    Iterator<Lookup> lookupIter = gs.getLookups(s2);
    System.out.println("Have lookups: "+lookupIter.hasNext());
    while(lookupIter.hasNext()) {
      Lookup l = lookupIter.next();
      System.out.println("Have a lookup"+l);
    }
    File someFile = new File("tmp.gazbin");
    try {
      gs.save(someFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assertTrue("could not save trie", false);
      return;
    }
    GazStoreTrie3 gs2 = new GazStoreTrie3();
    try {
      gs2 = (GazStoreTrie3)gs2.load(someFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      assertTrue("could not load trie",false);
      return;
    }
    State init_2 = gs2.getInitialState();
    System.out.println("Initial State: "+init_2);
    State s1_2 = init_2.next('a');
    System.out.println("State after a: "+s1_2);
    System.out.println("isFinal: "+s1_2.isFinal());
    State s2_2 = s1_2.next('s');
    System.out.println("State after s: "+s2_2);
    System.out.println("isFinal: "+s2_2.isFinal());
    Iterator<Lookup> lookupIter_2 = gs2.getLookups(s2_2);
    System.out.println("Have lookups: "+lookupIter_2.hasNext());
    while(lookupIter_2.hasNext()) {
      Lookup l = lookupIter_2.next();
      System.out.println("Have a lookup"+l);
    }
    
    System.out.println("************* end ********************");
  }  
  
}

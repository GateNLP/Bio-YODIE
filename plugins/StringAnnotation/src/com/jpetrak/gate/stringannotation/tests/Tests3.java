package com.jpetrak.gate.stringannotation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jpetrak.gate.stringannotation.extendedgazetteer.ExtendedGazetteer;
import com.jpetrak.gate.stringannotation.extendedgazetteer.trie.GazStoreTrie3;
import com.jpetrak.gate.stringannotation.utils.StoreArrayOfCharArrays;

public class Tests3 {

  private static boolean isInitialized = false; 
  private static File testingDir;
  @BeforeClass
  public static void init() throws GateException, MalformedURLException {
    if(!isInitialized) {
      System.out.println("Tests3: Inititalizing ...");
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
    System.out.println("Tests3: Cleaning up ...");
  }
  
  public OutputStream openOutputStream(String filename, boolean compressed) 
      throws IOException {
    OutputStream out = new FileOutputStream(new File(filename));
    if(compressed) {
      out = new GZIPOutputStream(out);
    }
    return out;
  }
  public InputStream openInputStream(String filename, boolean compressed) 
      throws IOException {
    InputStream in = new FileInputStream(new File(filename));
    if(compressed) {
      in = new GZIPInputStream(in);
    }
    return in;
  }
  
  @Test
  public void testPersistLargeArray() throws IOException, ClassNotFoundException {
    StoreArrayOfCharArrays as = new StoreArrayOfCharArrays();
    char[] data = new char[]{'a', 's', 'd', 'f', 'g' };
    for(int i = 0; i< 20000000; i++) {
      as.addData(data);
    }
    ObjectOutputStream os = new ObjectOutputStream(openOutputStream("testout1.gazbin",true));
    long startTime = System.currentTimeMillis();
    os.writeObject(as);
    os.flush();
    os.close();
    long endTime = System.currentTimeMillis();
    System.out.println("Elapsed time for save: "+((endTime-startTime)/1000.0));
    System.gc();
    
    as = null;
    ObjectInputStream is = new ObjectInputStream(openInputStream("testout1.gazbin",true));
    startTime = System.currentTimeMillis();
    Object object = is.readObject();
    is.close();
    endTime = System.currentTimeMillis();
    System.out.println("Elapsed time for load: "+((endTime-startTime)/1000.0));
    assertTrue(object instanceof StoreArrayOfCharArrays);
    as = (StoreArrayOfCharArrays)object;
    char[] back = as.getData(0);
    assertEquals("asdfg",new String(back));
  }
  
  @Test
  public void testPersistenceBig1() 
      throws ResourceInstantiationException, ExecutionException, IOException {
    System.out.println("Running gazetteer persistence test");
    FeatureMap parms = Factory.newFeatureMap();
    File defFile = new File(testingDir,"pref_en_500K.def");
    URL gazURL = defFile.toURI().toURL();
    parms.put("configFileURL", gazURL);
    System.gc();
    long before = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    System.out.println("Memory used before loading gazetteer: "+before);
    long startTime = System.currentTimeMillis();
    ExtendedGazetteer eg = (ExtendedGazetteer)Factory.createResource(
            "com.jpetrak.gate.stringannotation.extendedgazetteer.ExtendedGazetteer", parms);
    long endTime = System.currentTimeMillis();
    System.gc();
    long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    System.out.println("Memory used after loading gazetteer: "+after);
    System.out.println("Elapsed time for loading from lst: "+((endTime-startTime)/1000.0));
    System.out.println("Memory used up in between: "+(after-before));
    System.out.println("Saving to test-big.gazbin");
    File save = new File("test-big.gazbin");
    startTime = System.currentTimeMillis();
    eg.save(save);
    endTime = System.currentTimeMillis();
    Factory.deleteResource(eg);
    System.out.println("Elapsed time for saving to gazbin: "+((endTime-startTime)/1000.0));
    eg = null;
    System.gc();
    System.out.println("Saving completed, trying to load into a new gaz store");
    before = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    startTime = System.currentTimeMillis();
    GazStoreTrie3 gs = new GazStoreTrie3();
    gs = (GazStoreTrie3)gs.load(save);
    endTime = System.currentTimeMillis();
    after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    System.out.println("Loading from GAZBIN completed");
    System.out.println("Elapsed time for loading from gazbin: "+((endTime-startTime)/1000.0));
    System.out.println("Memory used up for cache loading: "+(after-before));
    System.out.println("Big gazetteer application test finished");
  }


  
  
}

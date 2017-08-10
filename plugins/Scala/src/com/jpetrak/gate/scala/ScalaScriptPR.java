package com.jpetrak.gate.scala;

import com.jpetrak.gate.scala.gui.ScalaEditorVR;
import java.net.URL;

import gate.Resource;
import gate.Controller;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.GateConstants;

import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ControllerAwarePR;
import gate.creole.CustomDuplication;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.gui.ActionsPublisher;
import gate.gui.MainFrame;
import gate.util.GateClassLoader;
import gate.util.GateRuntimeException;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

// TODO: sync bug-fixes from Java plugin, e.g. updating the sources in the
// editor window on re-init!


@CreoleResource(
        name = "Scala Script PR",
        helpURL = "",
        comment = "Use a Scala program as a processing resource")
public class ScalaScriptPR
        extends AbstractLanguageAnalyser
        implements ControllerAwarePR, ScalaCodeDriven, CustomDuplication, ActionsPublisher {

  // ********* Parameters
  @CreoleParameter(comment = "The URL of the Scala program to run",suffixes = ".scala")
  public void setScalaProgramUrl(URL surl) {
    scalaProgramUrl = surl;
  }

  public URL getScalaProgramUrl() {
    return scalaProgramUrl;
  }
  protected URL scalaProgramUrl;

  @Optional
  @RunTime
  @CreoleParameter(comment = "The input annotation set", defaultValue = "")
  public void setInputAS(String asname) {
    inputAS = asname;
  }

  public String getInputAS() {
    return inputAS;
  }
  protected String inputAS;

  @Optional
  @RunTime
  @CreoleParameter(comment = "The output annotation set", defaultValue = "")
  public void setOutputAS(String asname) {
    outputAS = asname;
  }

  public String getOutputAS() {
    return outputAS;
  }
  protected String outputAS;

  @Optional
  @RunTime
  @CreoleParameter(comment = "The script parameters", defaultValue = "")
  public void setScriptParams(FeatureMap parms) {
    scriptParams = parms;
  }
  
  @Override
  @Optional
  @RunTime
  @CreoleParameter()
  public void setDocument(Document d) {
    document = d;
  }
    
  @CreoleParameter(comment = "which compiler to use, default is IMain", defaultValue="ReflectGlobal")
  public void setCompilerType(CompilerType parm) {
    compilerType = parm;
  }
  
  public CompilerType getCompilerType() { return compilerType; }
  protected CompilerType compilerType;

  public FeatureMap getScriptParams() {
    if (scriptParams == null) {
      scriptParams = Factory.newFeatureMap();
    }
    return scriptParams;
  }
  
  @Optional
  @RunTime
  @CreoleParameter()
  public void setResource1(Resource r) {
    resource1 = r;
  }
  public Resource getResource1() {
    return resource1;
  }
  protected Resource resource1;
  
  @Optional
  @RunTime
  @CreoleParameter()
  public void setResource2(Resource r) {
    resource2 = r;
  }
  public Resource getResource2() {
    return resource2;
  }
  protected Resource resource2;
  
  @Optional
  @RunTime
  @CreoleParameter()
  public void setResource3(Resource r) {
    resource3 = r;
  }
  public Resource getResource3() {
    return resource3;
  }
  protected Resource resource3;
  
  
  
  
  protected FeatureMap scriptParams;
  Controller controller = null;
  File scalaProgramFile = null;
  // this is used by the VR
  public File getScalaProgramFile() { return scalaProgramFile; }
  List<String> scalaProgramLines = null;
  ScalaScript scalaProgramClass = null;

  protected File getPluginDir() {
    URL creoleURL = Gate.getCreoleRegister().get(this.getClass().getName()).getXmlFileUrl();
    File pluginDir = gate.util.Files.fileFromURL(creoleURL).getParentFile();
    return pluginDir;
  }
  String fileProlog = "import com.jpetrak.gate.scala.ScalaScript";
  String classProlog = "class THECLASSNAME extends ScalaScript {";

  // NOTE: we store two different classloaders here for now, because each
  // of the two compilers uses and needs classloaders differently.
  // We have to figure out how to do this better!!
  GateClassLoader classloader = null;
  
  
  // NOTE: the epilog is specific to the compiler implementation we use
  // so we implement it as a method on the ScalaCompiler class
  
  // This will try and compile the script. 
  // This is done 
  // = at init() time
  // = at reInit() time
  // = when the "Use"  button is pressed in the VR
  // If the script fails compilation, our script object is set to null
  // and the PR will throw an exception when an attempt is made to run it
  public void tryCompileScript() {
    String scalaProgramSource;
    String className;
    if(classloader != null) {
      Gate.getClassLoader().forgetClassLoader(classloader);
    }
    classloader = 
            Gate.getClassLoader().getDisposableClassLoader(
              //"C"+java.util.UUID.randomUUID().toString().replaceAll("-", ""), 
              scalaProgramUrl.toExternalForm()+System.currentTimeMillis(),
              this.getClass().getClassLoader(), true);
    try {
      className = "ScalaScriptClass" + getNextId();
      StringBuilder sb = new StringBuilder();
      scalaProgramLines = new ArrayList<String>();
      scalaProgramLines.add(fileProlog);      
      scalaProgramLines.add(classProlog.replaceAll("THECLASSNAME", className));
      LineIterator it = FileUtils.lineIterator(scalaProgramFile, "UTF-8");
      try {
        while (it.hasNext()) {
          String line = it.nextLine();
          scalaProgramLines.add(line);
        }
      } finally {
        LineIterator.closeQuietly(it);
      }
      scalaProgramLines.add(scalaCompiler.getClassEpilog().replaceAll("THECLASSNAME", className));
      for (String line : scalaProgramLines) {
        sb.append(line);
        sb.append("\n");
      }
      scalaProgramSource = sb.toString();

      //System.out.println("Program Source: " + scalaProgramSource);
    } catch (IOException ex) {
      System.err.println("Problem reading program from " + scalaProgramUrl);
      ex.printStackTrace(System.err);
      return;
    }
    try {
      //System.out.println("Trying to compile ...");
      scalaProgramClass = scalaCompiler.compile(className,scalaProgramSource,classloader);
      //scalaProgramClass = (ScalaScript) Gate.getClassLoader().
      //        loadClass("scalascripting." + className).newInstance();
      scalaProgramClass.globalsForPr = globalsForPr;
      scalaProgramClass.lockForPr = new Object();
      if(registeredEditorVR != null) {
        registeredEditorVR.setCompilationOk();
      }
      scalaProgramClass.resource1 = resource1;
      scalaProgramClass.resource2 = resource2;
      scalaProgramClass.resource3 = resource3;
      isCompileError = false;
      scalaProgramClass.resetInitAll();
    } catch (Exception ex) {
      System.err.println("Problem compiling ScalaScript Class");
      printScalaProgram(System.err);
      ex.printStackTrace(System.err);
      if(classloader != null) {
        Gate.getClassLoader().forgetClassLoader(classloader);
        classloader = null;
      }
      isCompileError = true;
      scalaProgramClass = null;
      if(registeredEditorVR != null) {
        registeredEditorVR.setCompilationError();
      }
      return;
    }
  }
  
  private void printScalaProgram(PrintStream out) {
    int linenr = 0;
    for(String line : scalaProgramLines) {
      linenr++;
      out.println(linenr+" "+line);
    }
  }
  
  // We need this so that the VR can determine if the latest compile was
  // an error or ok. This is necessary if the VR gets activated after the
  // compilation.
  public boolean isCompileError;
  
  private ScalaEditorVR registeredEditorVR = null;

  public void registerEditorVR(ScalaEditorVR vr) {
    registeredEditorVR = vr;
  }
  // TODO: make this atomic so it works better in a multithreaded setting
  private static int idNumber = 0;

  private static synchronized String getNextId() {
    idNumber++;
    return ("" + idNumber);
  }


  @Override
  public Resource init() throws ResourceInstantiationException {
    if (getScalaProgramUrl() == null) {
      throw new ResourceInstantiationException("The scalaProgramUrl must not be empty");
    }
    scalaProgramFile = gate.util.Files.fileFromURL(getScalaProgramUrl());
    try {
      // just check if we can read the script here ... what we read is not actually 
      // ever used
      String tmp = FileUtils.readFileToString(scalaProgramFile, "UTF-8");
    } catch (IOException ex) {
      throw new ResourceInstantiationException("Could not read the scala program from " + getScalaProgramUrl(), ex);
    }
    tryInitCompiler(true);  // true=re-use any existing compiler 
    tryCompileScript();
    return this;
  }

  static ScalaCompiler scalaCompiler = null;  // guarded by ScalaScriptPR class
  static Object compilerSync = new Object();
  
  
  protected void tryInitCompiler(boolean reUse) {
    synchronized(compilerSync) {
    if(scalaCompiler == null || !reUse) {
      //System.out.println("Creating compiler instance");
      if(getCompilerType() == CompilerType.IMain) {
        scalaCompiler = new ScalaCompilerImpl1();
      } else if(getCompilerType() == CompilerType.ReflectGlobal) {
        scalaCompiler = new ScalaCompilerImpl2();
      } else {
        scalaCompiler = new ScalaCompilerImpl1();
        setCompilerType(CompilerType.IMain);  // explicitly set the value so we save it correctly (DEBUG?)
      }
      //System.out.println("Initializing compiler instance");
      scalaCompiler.init();
    } else {
      //System.out.println("Compiler already initialized, reusing instance");
    }
    }
  }
  
  // Add an action to the GUI to re-initialize the compiler
  private List<Action> actions;

  @Override
  public List<Action> getActions() {
    if (actions == null) {
      actions = new ArrayList<Action>();
      actions.add(
              new AbstractAction("Re-initialize the Scala Compiler") {
        {
          putValue(SHORT_DESCRIPTION,
                  "Throw away the existing Scala Compiler and create a new one");
          putValue(GateConstants.MENU_PATH_KEY,
                  new String[]{"Scala"});
        }
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent evt) {
          Runnable runnableAction = new Runnable() {
            @Override
            public void run() {
              try {
                MainFrame.lockGUI("Re-initializing the Scala compiler ...");
                tryInitCompiler(false);
              } finally {
                MainFrame.unlockGUI();
              }
            }
          };
          Thread thread = new Thread(runnableAction, "Scala Compiler Re-Initialization");
          thread.start();
        };
      });
    }
    return actions;
  }

  @Override
  public void reInit() throws ResourceInstantiationException {
    //System.out.println("ScalaScriptPR reinitializing ...");
    // We re-set the global initialization indicator so that re-init can be
    // used to test the global init method
    if (scalaProgramClass != null) {
      scalaProgramClass.cleanupPr();
      scalaProgramClass.resetInitAll();
    }
    if(registeredEditorVR != null) {
      registeredEditorVR.setFile(getScalaProgramFile());
    }
    init();
  }

  @Override
  public void cleanup() {
    super.cleanup();
    // make sure the generated class does not hold any references
    if (scalaProgramClass != null) {
      scalaProgramClass.cleanupPr();
      scalaProgramClass.doc = null;
      scalaProgramClass.controller = null;
      scalaProgramClass.corpus = null;
      scalaProgramClass.inputASName = null;
      scalaProgramClass.outputASName = null;
      scalaProgramClass.inputAS = null;
      scalaProgramClass.outputAS = null;
      scalaProgramClass.parms = null;
      scalaProgramClass.globalsForPr = null;
      scalaProgramClass.lockForPr = null;
      scalaProgramClass.resource1 = null;
      scalaProgramClass.resource2 = null;
      scalaProgramClass.resource3 = null;
    }
    if (classloader != null) {
      Gate.getClassLoader().forgetClassLoader(classloader);
      classloader = null;
    }
  }

  @Override
  public void execute() {
    if (scalaProgramClass != null) {
      try {
        scalaProgramClass.resource1 = getResource1();
        scalaProgramClass.resource2 = getResource2();
        scalaProgramClass.resource3 = getResource3();        
        scalaProgramClass.doc = document;
        scalaProgramClass.controller = controller;
        scalaProgramClass.corpus = corpus;
        scalaProgramClass.inputASName = getInputAS();
        scalaProgramClass.outputASName = getOutputAS();
        scalaProgramClass.inputAS =
                (document != null && getInputAS() != null) ? document.getAnnotations(getInputAS()) : null;
        scalaProgramClass.outputAS =
                (document != null && getOutputAS() != null) ? document.getAnnotations(getOutputAS()) : null;
        scalaProgramClass.parms = getScriptParams();
        scalaProgramClass.callExecute();
        scalaProgramClass.doc = null;
        scalaProgramClass.inputASName = null;
        scalaProgramClass.outputASName = null;
        scalaProgramClass.inputAS = null;
        scalaProgramClass.outputAS = null;
      } catch (Exception ex) {
        printScalaProgram(System.err);
        throw new GateRuntimeException("Could not run program ", ex);
      }
    } else {
      throw new GateRuntimeException("Cannot run script, compilation failed: "+getScalaProgramUrl());
    }
  }

  @Override
  public void controllerExecutionStarted(Controller controller) {
    this.controller = controller;
    if (scalaProgramClass != null) {
      scalaProgramClass.resource1 = getResource1();
      scalaProgramClass.resource2 = getResource2();
      scalaProgramClass.resource3 = getResource3();    
      scalaProgramClass.controller = controller;
      try {
        scalaProgramClass.controllerStarted();
      } catch (Exception ex) {
        System.err.println("Could not run controllerStarted method for script "+getName());
        printScalaProgram(System.err);
        ex.printStackTrace(System.err);
      }
    }
  }

  @Override
  public void controllerExecutionFinished(Controller controller) {
    this.controller = controller;
    if (scalaProgramClass != null) {
      scalaProgramClass.controller = controller;
      try {
        scalaProgramClass.controllerFinished();
      } catch (Exception ex) {
        System.err.println("Could not run controllerFinished method for script "+getName());
        printScalaProgram(System.err);
        ex.printStackTrace(System.err);
      }
      scalaProgramClass.controller = null;
      scalaProgramClass.corpus = null;
      scalaProgramClass.parms = null;
    }
  }

  @Override
  public void controllerExecutionAborted(Controller controller, Throwable throwable) {
    this.controller = controller;
    if (scalaProgramClass != null) {
      scalaProgramClass.controller = controller;
      try {
        scalaProgramClass.controllerAborted(throwable);
      } catch (Exception ex) {
        System.err.println("Could not run controllerAborted method for script "+getName());
        printScalaProgram(System.err);
        ex.printStackTrace(System.err);
      }
      scalaProgramClass.controller = null;
      scalaProgramClass.corpus = null;
      scalaProgramClass.parms = null;
    }
  }
  // This is how we share global data between the different copies created
  // by custom duplication: each ScalaScriptPR instance will initially 
  // get its initial globalForScript map instance. But when duplicate is 
  // executed, that map instance will be overridden by whatever the first PR
  // instance was. At the point where a new compiled script object is created,
  // the compiled script object's map field will get set to that map.
  protected ConcurrentMap<String, Object> globalsForPr =
          new ConcurrentHashMap<String, Object>();
  
  protected Object lockForPr;

  protected void updateShared() {
    scalaProgramClass.lockForPr = lockForPr;
    scalaProgramClass.globalsForPr = globalsForPr;
  }
  
  
  @Override
  public Resource duplicate(Factory.DuplicationContext dc) throws ResourceInstantiationException {
    ScalaScriptPR res = (ScalaScriptPR) Factory.defaultDuplicate(this, dc);
    // Now give the new instance access to the ScriptGlobal data structure
    res.globalsForPr = this.scalaProgramClass.globalsForPr;
    res.lockForPr = this.lockForPr;
    if(res.scalaProgramClass != null) {
      res.scalaProgramClass.duplicationId = this.scalaProgramClass.duplicationId + 1;
    }
    res.updateShared();
    return res;
  }
  
  public enum CompilerType {
    IMain, ReflectGlobal
  }
  
}

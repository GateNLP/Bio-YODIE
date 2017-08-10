package com.jpetrak.gate.scala

import gate.util.GateClassLoader
import gate.util.GateRuntimeException
import java.io.File
import gate.Gate
import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.IMain
import javax.script.CompiledScript
import javax.script.ScriptContext
import scala.collection.mutable.LinkedHashSet

/**
 * Compiler implementation based on the standard IMain class.
 * 
 * This is a compiler implementation which uses the same IMain class which
 * is also used by the Scala JSR223 scripting engine implemtation and in
 * the REPL. The advantages of this approach are:
 * <ul>
 * <li>Does not need to create class files on disk and load them during compilation
 * <li>Compiler instance can be re-used
 * <li>maybe: no limitations of the compiler with regard to opimization etc.
 * </ul>
 * 
 * The disadvantages are:
 * <ul>
 * <li>The compiler needs to know about the actual JAR files to use for compilation,
 * cannot just use a classloader
 * <li>The compiler cannot be made to use a specific classloader to load the compiled
 * class. This could lead to some class getting loaded from a different classloader
 * and ending up to be incompatible to an existing class, but this did not happen yet.
 * </ul>
 * 
 * @author: Johann Petrak
 * 
 */
class ScalaCompilerImpl1 extends ScalaCompiler {
  
  var compiler: IMain = null 
  
  def init() = {
    this.synchronized {
      println("Creating Scala compiler instance IMain")
      //println("Creating settings instance");
      val settings = new Settings();
      //println("Have settings: "+settings)
      //println("The original classpath: "+settings.classpath)
      //println("The bootclasspath: "+settings.bootclasspath)
      //println("The javabootclasspath: "+settings.javabootclasspath)
    
      settings.usejavacp.value = true
      var pluginDirName = 
        getPluginDir(this.getClass.getName()).getCanonicalPath()
      System.setProperty("scala.home",pluginDirName)
    
      // now set both the bootclasspath and the javaclasspath to include
      // all the URLs we found
      var knownJars = Set[String]()
      
         
      var cl = Gate.getClassLoader
      var gatecl: GateClassLoader = null
      if(cl.isInstanceOf[GateClassLoader]) {
        gatecl = cl.asInstanceOf[GateClassLoader]
      } else {
        throw new GateRuntimeException("Scala compiler init: Gate.getClassLoader is not a GateClassLoader")
      }
      
      var urls = List[String]()
      //urls = urls ++ getJarUrls4ClassLoader(classOf[ScalaScriptPR].getClassLoader)
      //urls = urls ++ getJarUrls4ClassLoader(gate.Gate.getClassLoader)
      //urls = urls ++ getJarUrls4ClassLoader(java.lang.Thread.currentThread.getContextClassLoader)
      
      urls = urls ++ getJarUrls4GateClassLoader(gatecl)

      //println("Urls from GATE classloader: "+urls)
      urls.foreach { url =>
        if(!knownJars.contains(url)) {
          //println("appending from GATE classloader to settings.classpath: "+url)
          settings.classpath.append(url) 
          settings.bootclasspath.append(url)
          knownJars += url
        } else {
          //println("GATE classloader - ignoring already known "+url)
        }
      }
      
      
      //println("Settings initialized, all settings: "+settings)
      //println("Settings.classpath is: "+settings.classpath)
      //println("Settings.bootclasspath is: "+settings.bootclasspath)
      
      //println("Creating actual compiler instance")
      compiler = new IMain(settings);
      //println("Compiler classpath is: "+compiler.compilerClasspath)

    }    

  }
  
  def getClassEpilog() = "}; new THECLASSNAME()"

  def compile(name: String, source: String, classloader: GateClassLoader): ScalaScript = {
    this.synchronized {
      //println("Trying to compile the source: "+source)
      var ret: ScalaScript = null
      var obj = compiler.compile(source)
      //println("Compilation gave me: "+obj)
      if(obj.isInstanceOf[CompiledScript]) {
        //println("YES, is instance of compiled script")
        val cs = obj.asInstanceOf[CompiledScript]
        val e = cs.eval(null.asInstanceOf[ScriptContext])
        //println("Got evaluation result: "+e+" of type "+e.getClass())
        if(e.isInstanceOf[ScalaScript]) {
          //println("YES is a ScalaScript")
          ret = e.asInstanceOf[ScalaScript]
        } else {
          //println("NO, not a scalaprocessingresource")
          throw new GateRuntimeException("Problem compiling script, did not get ScalaScript but "+cs.getClass())
        }
      } else {
        //println("NO is not instance, but "+ret.getClass())
        throw new GateRuntimeException("Problem compiling script, did not get a CompiledScript but "+obj.getClass())
      }
      ret
    }
  }
  
  /// UTILITY classes
  def getPluginDir(pluginClassName: String): java.io.File = {
    val creoleFileURL = 
      Gate.getCreoleRegister().get("com.jpetrak.gate.scala.ScalaScriptPR").getXmlFileUrl()
    gate.util.Files.fileFromURL(creoleFileURL).getParentFile();    
  }
  
  def getJarUrls4GateClassLoader(cl: GateClassLoader): List[String] = {
    var urlSet = new LinkedHashSet[String]()
    var tmpcl = cl.getParent
    //println("PARENT CL:"+tmpcl)
    var urls: List[String] = List[String]()
    if(tmpcl == null) {
      // do notghin 
    } else if(tmpcl.isInstanceOf[GateClassLoader]) {
      //urls = getJarUrls4GateClassLoader(tmpcl.asInstanceOf[GateClassLoader])
      urls = getJarUrls4ClassLoader(tmpcl)
    } else {
      urls = getJarUrls4ClassLoader(tmpcl)
    }
    //println("URLS FOR PARENT:"+urls)
    urlSet = urlSet ++ urls
    val children = cl.getChildren
    //println("Got children: "+children.size)
    children.toArray.foreach { child =>
      if(child.isInstanceOf[GateClassLoader]) {
        tmpcl = child.asInstanceOf[GateClassLoader]
        urls = getJarUrls4ClassLoader(tmpcl)
        //println("Child CL:"+tmpcl)
        //println("URLS FOR child:"+urls)
        urls.foreach { url =>
          if(!urlSet.contains(url)) { urlSet.add(url) }
        }
      } else {
        println("Found a child classloader which is not a GateClassLoader!")
      }
    }
    urlSet.toList
  }
  
  def getJarUrls4ClassLoader(cl: ClassLoader): List[String] = {
    val theUrls: List[String] = 
      cl match {
        case cl: java.net.URLClassLoader => 
          cl.getURLs.toList.
          map { _.toString }.
          filter { _.startsWith("file:") }.
          filter { _.endsWith(".jar") }.
          map { new java.net.URL(_) }.
          map { gate.util.Files.fileFromURL(_).getCanonicalPath } 
        case _ =>  
          println("Not a URL classloader when trying to get JAR files")
          List[String]()
      }
    theUrls
  }

  def getJarFileNames4pluginDir(dirname: String): List[String] = {
    val dir = new File(dirname)
    var list = dir.listFiles.toList.filter(
      f => f.toString.toLowerCase.endsWith(".jar")).map( _.getCanonicalPath )
    val libDir = new java.io.File(dir,"lib")
    if(libDir.exists) {
      list ++= libDir.listFiles.toList.filter( 
        f => f.toString.toLowerCase.endsWith(".jar")).map( _.getCanonicalPath )      
    }
    list
  }
  
  
  
  
}

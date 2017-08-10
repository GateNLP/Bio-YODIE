package com.jpetrak.gate.scala;

import gate.util.GateClassLoader;

public interface ScalaCompiler {
  public void init();
  /**
   * Compile a script.
   * 
   * The name should be the actual class name used for this compilation. 
   * the classloader should be a classloader that should get used to ultimately
   * load the class into.
   *
   * @param name
   * @param source
   * @param classloader
   * @return 
   */
  public ScalaScript compile(String name, String source, GateClassLoader classloader);
  
  public String getClassEpilog();
  
}

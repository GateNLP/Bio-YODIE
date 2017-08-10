import gate.Utils
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
//import gate.creole.ontology.impl.sesame._

var someField = "text";

override def execute() = {
  System.out.println("Hello World");

  System.out.println("The document is "+doc);
  System.out.println("The controller is "+controller);
  System.out.println("The corpus is "+corpus);
  System.out.println("The globalsForAll is: "+getGlobalsForAll);
  System.out.println("The globalsForScript is: "+globalsForPr);
  System.out.println("GATE initialized: "+gate.Gate.isInitialised());
}

override def initAll() = {
  System.out.println("Doing the global initialization");
  getGlobalsForAll.put("Key1","somestring")
  getGlobalsForAll.put("Key2",1.asInstanceOf[Integer])
  System.out.println("GlobalsForAll is now: "+getGlobalsForAll);
}

override def initPr() = {
  System.out.println("Doing the PR initialization");
}

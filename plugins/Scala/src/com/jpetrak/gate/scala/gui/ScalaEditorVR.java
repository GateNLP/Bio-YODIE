package com.jpetrak.gate.scala.gui;

import com.jpetrak.gate.scala.ScalaCodeDriven;
import com.jpetrak.gate.scala.ScalaScriptPR;
import gate.Resource;
import gate.creole.AbstractVisualResource;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.GuiType;
import java.awt.GridLayout;
import java.io.File;


/**
 *
 * @author johann
 */
@CreoleResource(
  name = "Scala Code Editor", 
  comment = "Editor for Java code", 
  guiType = GuiType.LARGE, 
  mainViewer = true, 
  resourceDisplayed = "com.jpetrak.gate.scala.ScalaCodeDriven")
public class ScalaEditorVR extends AbstractVisualResource 
{
  
  protected ScalaEditorPanel panel;
  protected ScalaCodeDriven theTarget;
  protected ScalaScriptPR pr = null;
  
  @Override
  public void setTarget(Object target) {
    if(target instanceof ScalaCodeDriven) {
      //System.out.println("Found a ScalaCodeDriven, activating panel");
      theTarget = (ScalaCodeDriven)target;
      panel = new ScalaEditorPanel();
      this.add(panel);
      this.setLayout(new GridLayout(1,1));
      // register ourselves as the EditorVR
      pr = (ScalaScriptPR)target;
      pr.registerEditorVR(this);
      panel.setPR(pr);
      panel.setFile(pr.getScalaProgramFile());
      if(pr.isCompileError) {
        panel.setCompilationError();
      } else {
        panel.setCompilationOk();
      }
    } else {
      //System.out.println("Not a ScalaCodeDriven: "+((Resource)target).getName());
    }
  }
  
  public void setFile(File file) {
    panel.setFile(file);
  }
  
  public void setCompilationError() {
    panel.setCompilationError();
  }
  public void setCompilationOk() {
    panel.setCompilationOk();
  }
  
}

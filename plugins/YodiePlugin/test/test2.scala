import gate.trendminer.lodie.utils.LodieUtils
import gate._
Gate.init()
import java.util._

var in = new ArrayList[FeatureMap]()
in.add(Utils.featureMap("k",(10.0).asInstanceOf[Number]))
in.add(Utils.featureMap("k",(1.0).asInstanceOf[Number]))
in.add(Utils.featureMap("k",(1.0).asInstanceOf[Number]))
in.add(Utils.featureMap("k",(1.0).asInstanceOf[Number]))
in.add(Utils.featureMap("k",(1.0).asInstanceOf[Number]))
in.add(Utils.featureMap("k",(0.2).asInstanceOf[Number]))
in.add(Utils.featureMap("k",(0.2).asInstanceOf[Number]))
in.add(Utils.featureMap("k",(13.1).asInstanceOf[Number]))
in.add(Utils.featureMap("k",(13.2).asInstanceOf[Number]))
in.add(Utils.featureMap("k",null))
in.add(Utils.featureMap("k",null))
in.add(Utils.featureMap("k",null))
in.add(Utils.featureMap("k",null))
in.add(Utils.featureMap("k",null))
in.add(Utils.featureMap("k",null))
in.add(Utils.featureMap("k",null))
in.add(Utils.featureMap("k",null))

LodieUtils.sortCandidatesDescOn(in,"k",2)
LodieUtils.sortCandidatesDescOn(in,"k",99)
LodieUtils.sortCandidatesDescOn(in,"k",99,true)

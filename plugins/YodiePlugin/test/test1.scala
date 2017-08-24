import gate._
Gate.init()
import gate.trendminer.lodie.utils._
var cands = new java.util.ArrayList[FeatureMap]()

cands.add(gate.Utils.featureMap("v",12.asInstanceOf[Integer])) 
cands.add(gate.Utils.featureMap("v",null))
cands.add(gate.Utils.featureMap("v",1.asInstanceOf[Integer]))
cands.add(gate.Utils.featureMap("v",null))
cands.add(gate.Utils.featureMap("v",12.asInstanceOf[Integer]))
cands.add(gate.Utils.featureMap("v",12.asInstanceOf[Integer]))
cands.add(gate.Utils.featureMap("v",14.asInstanceOf[Integer]))
cands.add(gate.Utils.featureMap("v",1.asInstanceOf[Integer]))
cands.add(gate.Utils.featureMap("v",null))
cands.add(gate.Utils.featureMap("v","ss"))
cands.add(gate.Utils.featureMap("v","123"))
cands.add(gate.Utils.featureMap("v",2.asInstanceOf[Integer]))
cands.add(gate.Utils.featureMap("v",null))

println("For  1: "+LodieUtils.sortCandidatesDescOn(cands,"v",1))
println("For  2: "+LodieUtils.sortCandidatesDescOn(cands,"v",2))
println("For  3: "+LodieUtils.sortCandidatesDescOn(cands,"v",3))
println("For  4: "+LodieUtils.sortCandidatesDescOn(cands,"v",4))
println("For 999: "+LodieUtils.sortCandidatesDescOn(cands,"v",999))



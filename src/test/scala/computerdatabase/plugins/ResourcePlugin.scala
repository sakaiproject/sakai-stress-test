package plugins

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure._

class ResourcePlugin extends SakaiSimulationPlugin {

	def name(): String = { "sakai-announcements" }
 
  	def runSimulation: ChainBuilder = exec(session => {
  		println(session)
  		session 
  	})

}

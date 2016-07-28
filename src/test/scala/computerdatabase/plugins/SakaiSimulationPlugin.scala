package plugins

import io.gatling.core.Predef._
import io.gatling.core.structure._

trait SakaiSimulationPlugin {
	def name: String
	def runSimulation: ChainBuilder
}

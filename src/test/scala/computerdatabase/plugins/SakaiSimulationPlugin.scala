package plugins

import io.gatling.core.Predef._
import io.gatling.core.structure._

trait SakaiSimulationPlugin {

	val headers = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
		"Accept-Encoding" -> "gzip, deflate, sdch, br",
		"Accept-Language" -> "es-ES,es;q=0.8,en;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Connection" -> "keep-alive",
		"Upgrade-Insecure-Requests" -> "1")
	  
	val successStatus: Int = 200

	def name: String
	def description: String
	def toolid: String
	def getSimulationChain: ChainBuilder
}

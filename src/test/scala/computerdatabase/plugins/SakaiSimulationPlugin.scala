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
	  
 	// Define an infinite feeder which calculates random numbers 
	val randomNumbers = Iterator.continually(
	  // Random number will be accessible in session under variable "randomNum"
	  Map("randomNum" -> util.Random.nextInt(Integer.MAX_VALUE))
	)
	 
	val successStatus: Int = 200

	val pauseMin: Int = 1
	val pauseMax: Int = 3

	def name: String
	def description: String
	def toolid: String
	def getSimulationChain: ChainBuilder
}

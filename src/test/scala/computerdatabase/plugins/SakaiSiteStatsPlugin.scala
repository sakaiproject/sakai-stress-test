package plugins

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SakaiSiteStatsPlugin extends SakaiSimulationPlugin {

	def name(): String = { "sitestats-get-data" }
	
	def description(): String = { "Get stats data" }
	
	def toolid(): String = { "sakai-sitestats" }
	
	val XHRHeader = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
		"Accept-Encoding" -> "gzip, deflate, sdch, br",
		"Accept-Language" -> "es-ES,es;q=0.8,en;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Connection" -> "keep-alive",
		"Upgrade-Insecure-Requests" -> "1",
		"Wicket-Ajax" -> "true",
		"Wicket-Ajax-BaseURL" -> "home",
		"X-Requested-With" -> "XMLHttpRequest")
 
 	// Define an infinite feeder which calculates random numbers 
	val randomNumbers = Iterator.continually(
	  // Random number will be accessible in session under variable "randomNum"
	  Map("randomNum" -> util.Random.nextInt(Integer.MAX_VALUE))
	)
	 
 
  	def getSimulationChain = 
  		group("SiteStats") {
	  		exec(http("Stats")
				.get("${tool._2}")
				.headers(headers)
				.check(status.is(successStatus))
				.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
				.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists))
			.pause(pauseMin,pauseMax)
			.feed(randomNumbers)				
			.exec(http("Visits")
				.get("${tool._2}/home?0-1.IBehaviorListener.0-visitsWidget-widget-ministatContainer&_=${randomNum}")
				.headers(XHRHeader)
				.check(status.is(successStatus))
				.check(xpath("//component/@id").exists))
			.pause(pauseMin,pauseMax)
			.exec(http("Activity")
				.get("${tool._2}/home?0-1.IBehaviorListener.0-activityWidget-widget-ministatContainer&_=${randomNum}")
				.headers(XHRHeader)
				.check(status.is(successStatus))
				.check(xpath("//component/@id").exists))
			.pause(pauseMin,pauseMax)
			.exec(http("Resources")
				.get("${tool._2}/home?0-1.IBehaviorListener.0-resourcesWidget-widget-ministatContainer&_=${randomNum}")
				.headers(XHRHeader)
				.check(status.is(successStatus))
				.check(xpath("//component/@id").exists))
			.pause(pauseMin,pauseMax)
			.exec(http("Lessons")
				.get("${tool._2}/home?0-1.IBehaviorListener.0-lessonsWidget-widget-ministatContainer&_=${randomNum}")
				.headers(XHRHeader)
				.check(status.is(successStatus))
				.check(xpath("//component/@id").exists))
		}		
}

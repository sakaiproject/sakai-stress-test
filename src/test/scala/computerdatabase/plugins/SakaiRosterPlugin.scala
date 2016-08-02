package plugins

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SakaiRosterPlugin extends SakaiSimulationPlugin {

	def name(): String = { "roster-get-pages" }
	
	def description(): String = { "List pages in roster" }
	
	def toolid(): String = { "sakai-site-roster2" }
 
	// Define an infinite feeder which calculates random numbers 
	val randomNumbers = Iterator.continually(
	  // Random number will be accessible in session under variable "randomNum"
	  Map("randomNum" -> util.Random.nextInt(Integer.MAX_VALUE))
	)
	 
  	def getSimulationChain = 
  		group("RosterGetPages") {
	  		exec(http("Roster")
				.get("${tool._2}")
				.headers(headers)
				.check(status.is(successStatus))
				.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
				.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
				.check(css("li.is-selected > a","href").transform(_.replace("portal/site","direct/roster-membership")).saveAs("roster_membership_url")))
			.pause(pauseMin,pauseMax)				
			.exec(http("Site")
				.get("${roster_membership_url}/get-site.json?_="+util.Random.nextInt(1000))
				.headers(headers)
				.check(status.is(successStatus))
				.check(jsonPath("$[?(@.membersTotal>0)]")))
			.asLongAs(_.get("roster_status").asOption[String].getOrElse("none") != "END","pageCount") {
				feed(randomNumbers)
				.exec(http("Membership")
					.get("${roster_membership_url}/get-membership.json?page=${pageCount}&_=${randomNum}")
					.headers(headers)
					.check(status.is(successStatus))
					.check(jsonPath("$[?(@.status=='END')].status").optional.saveAs("roster_status")))
			}
			.exec(session => { session.remove("roster_membership_url") })
		}		
}

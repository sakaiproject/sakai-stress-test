package plugins

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SakaiAnnouncementsPlugin extends SakaiSimulationPlugin {

	def name(): String = { "announcements-list-options" }
	
	def description(): String = { "List options announcements" }
	
	def toolid(): String = { "sakai-announcements" }
 
  	def getSimulationChain = 
  		group("AnnouncementsListOptions") {
	  		exec(http("Announcements")
				.get("${tool._2}")
				.headers(headers)
				.check(status.is(successStatus))
				.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
				.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
				.check(css("a[onclick*='doOptions']","onclick").transform(_.replace("location = '","").replace("';return false;","")).optional.saveAs("annc_options")))
			.pause(pauseMin,pauseMax)				
			.doIf("${annc_options.exists()}") {
				exec(http("Options")
					.get("${annc_options}")
					.headers(headers)
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
					.check(css("form[name='optionsForm']").exists))
				.exec(session => { session.remove("annc_options") })
			}
		}		
}

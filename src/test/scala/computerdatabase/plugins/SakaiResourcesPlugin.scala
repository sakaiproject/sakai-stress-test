package plugins

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class SakaiResourcesPlugin extends SakaiSimulationPlugin {

	def name(): String = { "resources-options" }
	
	def description(): String = { "List options in resources" }
	
	def toolid(): String = { "sakai-resources" }
 
  	def getSimulationChain =
  		group("ResourcesListOptions") {
	  		exec(http("Resources")
				.get("${tool._2}")
				.headers(headers)
				.check(status.is(successStatus))
				.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
				.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
				.check(css("a[href*='doOptions']","href").optional.saveAs("resources_options")))
			.doIf("${resources_options.exists()}") {
				exec(http("GetOptions")
					.get("${resources_options}")
					.headers(headers)
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
					.check(css("form[name='optionsForm']").exists))
				.exec(session => { session.remove("resources_options") })
			}
		}

}

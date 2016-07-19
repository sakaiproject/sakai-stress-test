
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import java.net._
import scala.collection.mutable.ListBuffer

class SakaiSimulation extends Simulation {

	val successStatus: Int = 200
	val pauseMin: Int = Integer.getInteger("min-pause",1)
	val pauseMax: Int = Integer.getInteger("max-pause",1)
	val randomUsers: Int = Integer.getInteger("random-users",1)
	val exhausUsers: Int = Integer.getInteger("exhaus-users",1)
	val rampUpTime: Int = Integer.getInteger("rampup-time",10)
	val siteLoop: Int = Integer.getInteger("site-loop",1)
	val toolLoop: Int = Integer.getInteger("tool-loop",1)
	val userLoop: Int = Integer.getInteger("user-loop",1)
	
	val httpProtocol = http
		.baseURL(System.getProperty("test-url"))
		/**.inferHtmlResources(BlackList(".*(\.css|\.js|\.png|\.jpg|\.gif|thumb).*"), WhiteList())*/
		.userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36")
		.extraInfoExtractor(extraInfo => List(extraInfo.request.getUrl,extraInfo.response.statusCode,extraInfo.response.bodyLength))

	val headers = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
		"Accept-Encoding" -> "gzip, deflate, sdch, br",
		"Accept-Language" -> "es-ES,es;q=0.8,en;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Connection" -> "keep-alive",
		"Upgrade-Insecure-Requests" -> "1")

	val users = csv("user_credentials.csv").random

	object Gateway {
		val gateway = group("Gateway") {
			exec(http("Portal")
				.get("/portal")
				.headers(headers)
				.check(status.is(successStatus)))
			.pause(pauseMin,pauseMax)
		}
	}

	object Login {
		val login = group("Login") {
			feed(users)
			.exec(http("XLogin")
				.post("/portal/xlogin")
				.headers(headers)
				.formParam("eid", "${username}")
				.formParam("pw", "${password}")
				.formParam("submit", "Log+In")
				.check(status.is(successStatus))
				.check(css("div.fav-title > a","href").findAll.saveAs("siteUrls"))
				.check(css("div.fav-title > a","title").findAll.saveAs("siteTitles")))
			.pause(pauseMin,pauseMax)
			.exec(session => { 
				val mySites: Vector[(String,String)] = (session("siteTitles").as[Vector[String]] zip session("siteUrls").as[Vector[String]])
				session.set("sites", util.Random.shuffle(mySites))
			})
		} 
	}

	object ExploreTool {
		val explore = /** Do not process help tool */
			doIf(session => !session("tool").as[(String,String)]._2.contains("/portal/help/main")) {
				group("${tool._1}") {
					exec(http("${tool._1}")
						.get("${tool._2}")
						.headers(headers)
						.check(status.is(successStatus))
						.check(css("title").is("Sakai : ${site._1} : ${tool._1}"))
						.check(css("iframe","src").findAll.optional.saveAs("frameUrls"))
						.check(css("iframe","title").findAll.optional.saveAs("frameNames")))
					.pause(pauseMin,pauseMax)
					/** Take care of all iframed tools */
					.doIf("${frameUrls.exists()}") {
						exec(session => { 
							val myFrames: Vector[(String,String)] = (session("frameNames").as[Vector[String]] zip session("frameUrls").as[Vector[String]].map(s => URLDecoder.decode(s,"UTF-8")))
							session.set("frames", util.Random.shuffle(myFrames)).remove("frameUrls").remove("frameNames")
						})
						.foreach("${frames}","frame") {
							exec(http("${frame._1}")
								.get("${frame._2}")
								.headers(headers)
								.check(status.is(successStatus)))
							.pause(pauseMin,pauseMax)
						}
					}
				}
			}
		
	}

	object BrowseTools {
		val browse = (random: Boolean) =>
			group("Tools") {
				doIfOrElse(random) {
					repeat(toolLoop) {
						exec(session => { 
							session.set("tool",session("tools").as[Vector[String]].lift(util.Random.nextInt(session("tools").as[Vector[String]].size)).get)
						})
						.exec(ExploreTool.explore)
					}
					 
				}
				{
					foreach("${tools}","tool") {
						ExploreTool.explore
					}
				}
			}
	}

	object ExploreSite {
		val explore = (random: Boolean) => exec(
			group("GetSite") {
				exec(http("GetSite")
					.get("${site._2}")
					.headers(headers)
					.check(status.is(successStatus))
					.check(css("title:contains('Sakai : ${site._1} :')").exists)
					.check(css("a.Mrphs-toolsNav__menuitem--link","href").findAll.saveAs("toolUrls"))
					.check(css("span.Mrphs-toolsNav__menuitem--title").findAll.saveAs("toolNames")))
				.pause(pauseMin,pauseMax)
				.exec(session => { 
					val myTools: Vector[(String,String)] = (session("toolNames").as[Vector[String]] zip session("toolUrls").as[Vector[String]].map(s => URLDecoder.decode(s,"UTF-8")))
					session.set("tools", util.Random.shuffle(myTools))
				})
			},
			BrowseTools.browse(random)
		)
	}

	object BrowseSites {
		val browse = (random: Boolean) =>
			group("Sites") {
				doIfOrElse(random) {
					repeat(siteLoop) {
						exec(session => { 
							session.set("site",session("sites").as[Vector[String]].lift(util.Random.nextInt(session("sites").as[Vector[String]].size)).get)
						})
						.exec(ExploreSite.explore(random))
					}
				}
				{
					foreach("${sites}","site") {
						ExploreSite.explore(random)
					}
				}
			}
	}

	object Logout {
		val logout = group("Logout") {
			exec(http("Logout")
				.get("/portal/logout")
				.headers(headers)
				.check(status.is(successStatus)))
		}
	}
	
	object SakaiSimulationSteps {
		val test = (random: Boolean) => repeat(userLoop) {
			exec(Gateway.gateway,Login.login,BrowseSites.browse(random),Logout.logout)
		}
	}
	

	val randomUsersScn = scenario("SakaiRandomUserSimulation").exec(SakaiSimulationSteps.test(true))
	val exhaustiveUsersScn = scenario("SakaiExhaustiveUserSimulation").exec(SakaiSimulationSteps.test(false))

	object Setup {
		val scenario = ListBuffer[io.gatling.core.structure.PopulationBuilder]()
		if (randomUsers>0) {
			scenario += randomUsersScn.inject(
			    rampUsers(randomUsers) over (rampUpTime seconds)
			    /** More options here http://gatling.io/docs/2.2.2/general/simulation_setup.html */
			)
		}
		if (exhausUsers>0) {
			scenario += exhaustiveUsersScn.inject(
			    rampUsers(exhausUsers) over (rampUpTime seconds)
			    /** More options here http://gatling.io/docs/2.2.2/general/simulation_setup.html */
			)
		}
	}

	setUp(Setup.scenario.toList).protocols(httpProtocol)
}
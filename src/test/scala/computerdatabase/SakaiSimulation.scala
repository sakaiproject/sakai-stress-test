
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import io.gatling.core.action.builder._
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
	val privatePrefix = System.getProperty("private-prefix")
	val fixedSiteId = System.getProperty("fixed-site")
	val fixedToolId = System.getProperty("fixed-tool")
	val fixedSiteTitle = System.getProperty("fixed-site-title")
	
	var pluginManager = new SakaiPluginManager();
	
	val httpProtocol = http
		.baseURL(System.getProperty("test-url"))
		/**.inferHtmlResources(BlackList(".*(\.css|\.js|\.png|\.jpg|\.gif|thumb).*"), WhiteList())*/
		.userAgentHeader("Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36")
		.disableCaching
		/** Extra info for simulation.log username, url, http status, response length */
		.extraInfoExtractor(extraInfo => List(extraInfo.session("username").asOption[String].getOrElse(extraInfo.session("adminusername").asOption[String].getOrElse("annonymous")),
											  extraInfo.request.getUrl,
											  extraInfo.response.statusCode.getOrElse(0),
											  extraInfo.response.bodyLength))

	val headers = Map(
		"Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
		"Accept-Encoding" -> "gzip, deflate, sdch, br",
		"Accept-Language" -> "es-ES,es;q=0.8,en;q=0.6",
		"Cache-Control" -> "max-age=0",
		"Connection" -> "keep-alive",
		"Upgrade-Insecure-Requests" -> "1")

	/** Let change feed strategy and avoid error if there are not enough users in the feed */
	def getFeeder(name: String, strategy: String) = strategy match {
		case "queue" => csv(name).queue
		case "shuffle" => csv(name).shuffle
		case "random" => csv(name).random
		case "circular" => csv(name).circular
		case whatever => csv(name) /** queue is default strategy */
	} 

	val prefix = if (privatePrefix == "true") "private_" else ""
	val users = getFeeder(prefix+"user_credentials.csv",System.getProperty("feed-strategy"))
	val admins = getFeeder(prefix+"admin_credentials.csv",System.getProperty("feed-strategy"))
	val jsfViewStateCheck = css("input[name=com\\.sun\\.faces\\.VIEW]", "value").saveAs("viewState")
	
	def join(first: Vector[String], second: Vector[String]) : Vector[(String,String)] = (first.map(s => s.replace("My Workspace","Home")) zip second.map(s => URLDecoder.decode(s,"UTF-8")))
	def checkAttrs(cssSelector: String, attrName: String, varName: String) = css(cssSelector,attrName).findAll.saveAs(varName)
	def checkElement(cssSelector: String, varName: String) = css(cssSelector).findAll.saveAs(varName)
	
	def checkItsMe (username: String) = 
		http("GetCurrentUser")
		.get("/direct/user/current.json")
		.headers(headers)
		.check(status.is(successStatus))
		.check(jsonPath("$[?(@.eid=='" + username + "')]"))
	
	def joinInSessionOneFiltered(session: Session, firstName: String, secondName: String, finalName: String, filteredBy: String) = 
		session
		.remove(firstName)
		.remove(secondName)
		.set(finalName, join(session(firstName).as[Vector[String]],session(secondName).as[Vector[String]]).filter(_._1 contains filteredBy)(0))

	def joinInSession(session: Session, firstName: String, secondName: String, finalName: String) = 
		session
		.remove(firstName)
		.remove(secondName)
		.set(finalName, util.Random.shuffle(join(session(firstName).as[Vector[String]],session(secondName).as[Vector[String]])))
	
	def joinInSessionFiltered(session: Session, firstName: String, secondName: String, finalName: String, oneFilteredBy: String, twoFilteredBy: String) = 
		session
		.remove(firstName)
		.remove(secondName)
		.set(finalName, join(session(firstName).as[Vector[String]],session(secondName).as[Vector[String]]).filter(_._1 matches oneFilteredBy).filter(_._2 matches twoFilteredBy))
		
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
		val login = (impersonate: Boolean) =>
		group("Login") {
			doIfOrElse(impersonate) { /** Login as admin and then impersonate user */
				feed(admins)
				.exec(http("XLogin")
					.post("/portal/xlogin")
					.headers(headers)
					.formParam("eid", "${adminusername}")
					.formParam("pw", "${adminpassword}")
					.formParam("submit", "Log+In")
					.check(status.is(successStatus)))
				.pause(pauseMin,pauseMax)
				.exec(checkItsMe("${adminusername}"))
				.group("BecomeUser") {
					exec(http("AdminWorkspace")
						.get("/portal/site/!admin")
						.headers(headers)
						.check(status.is(successStatus))
						.check(checkAttrs("a.Mrphs-toolsNav__menuitem--link","href","adminToolUrls"))
						.check(checkAttrs("span.Mrphs-toolsNav__menuitem--icon","class","adminToolIds")))
					.exec(session => { joinInSessionOneFiltered(session,"adminToolIds","adminToolUrls","sutool","icon-sakai-su") })
					.pause(pauseMin,pauseMax)
					.exec(http("BecomeUser")
						.get("${sutool._2}")
						.headers(headers)
						.check(status.is(successStatus))
						.check(jsfViewStateCheck)
						.check(css("form[id=su]","action").saveAs("supost")))
					.pause(pauseMin,pauseMax)
					.feed(users)
					.exec(http("BecomeUserPost")
						.post("${supost}")
						.headers(headers)
						.formParam("su:username", "${username}")
						.formParam("su:become", "Become user")
						.formParam("com.sun.faces.VIEW", "${viewState}")
						.formParam("su", "su")
						.check(status.is(successStatus)))
					.pause(pauseMin,pauseMax)
					.exec(checkItsMe("${username}"))
					.exec(http("UserHome")
						.get("/portal")
						.headers(headers)
						.check(status.is(successStatus))
						.check(checkAttrs("div.fav-title > a","href","siteUrls"))
						.check(checkAttrs("div.fav-title > a","title","siteTitles")))
					.exec(session => { joinInSessionFiltered(session,"siteTitles","siteUrls","sites",fixedSiteTitle,".*\\/portal\\/site\\/"+fixedSiteId) })
				}
			}
			{	/** Use real credentials */
				feed(users)
				.exec(http("XLogin")
					.post("/portal/xlogin")
					.headers(headers)
					.formParam("eid", "${username}")
					.formParam("pw", "${password}")
					.formParam("submit", "Log+In")
					.check(status.is(successStatus))
					.check(checkAttrs("div.fav-title > a","href","siteUrls"))
					.check(checkAttrs("div.fav-title > a","title","siteTitles")))
				.pause(pauseMin,pauseMax)
				.exec(session => { joinInSessionFiltered(session,"siteTitles","siteUrls","sites",fixedSiteTitle,".*\\/portal\\/site\\/"+fixedSiteId) })
				.exec(checkItsMe("${username}"))
			}
		} 
	}

	object ExploreTool {
		val explore = 
			group("${tool._1}") {
				exec(http("${tool._1}")
					.get("${tool._2}")
					.headers(headers)
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(css("a.Mrphs-hierarchy--toolName > span[class*='${tool._1}'].Mrphs-breadcrumb--icon").exists)
					.check(css("iframe[title]","src").findAll.optional.saveAs("frameUrls"))
					.check(css("iframe[title]","title").findAll.optional.saveAs("frameNames")))
				.pause(pauseMin,pauseMax)
				/** Take care of all iframed tools */
				.doIf("${frameUrls.exists()}") {
					exec(session => { joinInSession(session,"frameNames","frameUrls","frames") })
					.foreach("${frames}","frame") {
						exec(http("${frame._1}")
							.get("${frame._2}")
							.headers(headers)
							.check(status.is(successStatus)))
						.pause(pauseMin,pauseMax)
					}
				}
				.pause(pauseMin,pauseMax)
				.exec(new SwitchBuilder("${tool._1}", pluginManager.getPluginMap, None))
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
		val explore = (random: Boolean) =>
			group("GetSite") {
				exec(http("GetSite")
					.get("${site._2}")
					.headers(headers)
					.check(status.is(successStatus))
					.check(css("span.Mrphs-hierarchy--siteName","title").is("${site._1}"))
					.check(checkAttrs("a[title].Mrphs-toolsNav__menuitem--link","href","toolUrls"))
					.check(css("a[title] > span.Mrphs-toolsNav__menuitem--icon","class").findAll.transform( 
						full_list => {
							/** class also contains non useful classes, drop them */
							val new_list = new Array[String](full_list.length) 
							for (i <- 0 until full_list.length) {
								new_list(i) = full_list(i).replace("Mrphs-toolsNav__menuitem--icon","").replace("icon-active","").replace("icon-","").trim()
							}
							new_list.to[collection.immutable.Seq]
						}).saveAs("toolIds")))
				.pause(pauseMin,pauseMax)
				.exec(session => { joinInSessionFiltered(session,"toolIds","toolUrls","tools",fixedToolId,".*\\/portal\\/site\\/.*\\/(tool|page|page-reset)\\/.*") })
			}
			.doIf(_("tools").as[Vector[String]].length>0) {
				BrowseTools.browse(random)
			}
		
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
		val logout = (impersonate: Boolean) =>
		group("Logout") {
			exec(http("Logout")
				.get("/portal/logout")
				.headers(headers)
				.check(status.is(successStatus)))
			.doIf(impersonate) {
				exec(checkItsMe("${adminusername}"))
				.exec(http("AdminLogout")
					.get("/portal/logout")
					.headers(headers)
					.check(status.is(successStatus)))
			}
		}
	}
	
	object SakaiSimulationSteps {
		val test = (random: Boolean, impersonate: Boolean) => repeat(userLoop) {
			exec(
				Gateway.gateway,
				Login.login(impersonate),
				doIf(_("sites").as[Vector[String]].length>0) {
					BrowseSites.browse(random)
				},
				Logout.logout(impersonate))
		}
	}
	
	val impersonateUsers = System.getProperty("impersonate-users") == "true"
	val randomUsersScn = scenario("SakaiRandomUserSimulation").exec(SakaiSimulationSteps.test(true,impersonateUsers))
	val exhaustiveUsersScn = scenario("SakaiExhaustiveUserSimulation").exec(SakaiSimulationSteps.test(false,impersonateUsers))

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
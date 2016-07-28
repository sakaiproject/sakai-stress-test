import java.io._
import plugins._
import io.gatling.core.Predef._
import io.gatling.core.structure._

class SakaiPluginManager {

	  var pluginMap = Map[String, SakaiSimulationPlugin]()
	  
	  def getPlugins(dir: File, extensions: List[String]): List[File] = {
	    dir.listFiles.filter(_.isFile).toList.filter(!_.getName.equals("SakaiSimulationPlugin.scala")).filter { file =>
	        extensions.exists(file.getName.endsWith(_))
	    }
	  }
	  	  
	  def init() {
	  	println("Loading simulation plugins...")
	    val files = getPlugins(new File("./src/test/scala/computerdatabase/plugins"),List("scala"))
	    files.foreach(
	    	f => try {
	    		val className = f.getName.replace(".scala","")
	    		val pluginObj = Class.forName("plugins."+className).newInstance.asInstanceOf[SakaiSimulationPlugin]
	    		println("Plugin ["+className+","+pluginObj.name+"] loaded !")
	    		pluginMap += ( pluginObj.name -> pluginObj )
	    	} catch {
	    		case e: Exception => {}
	    	}
	    )
	  }
	 
	  def runPlugin(name: String) : ChainBuilder = {
	  	try {
  			pluginMap(name).runSimulation
  		} catch {
  			case n: NoSuchElementException => {
  				exec()
  			}
  		}
	  }
	  
	  init()
	  
}

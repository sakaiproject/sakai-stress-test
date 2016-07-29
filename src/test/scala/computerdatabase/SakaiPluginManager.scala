import java.io._
import plugins._
import io.gatling.core.Predef._
import io.gatling.core.structure._

class SakaiPluginManager {

	  var pluginMap = Map[Any, ChainBuilder]()
	  
	  def getPlugins(dir: File, extensions: List[String]): List[File] = {
	    dir.listFiles.filter(_.isFile).toList.filter(!_.getName.equals("SakaiSimulationPlugin.scala")).filter { file =>
	        extensions.exists(file.getName.endsWith(_))
	    }
	  }
	  	  
	  def init() {
	  	println("Loading simulation plugins...")
	  	// We need at least 2 options to work with Switch
	  	pluginMap += ( "at-least-we-need-2a" -> exec() )
	  	pluginMap += ( "at-least-we-need-2b" -> exec() )
	    val files = getPlugins(new File("./src/test/scala/computerdatabase/plugins"),List("scala"))
	    files.foreach(
	    	f => try {
	    		val className = f.getName.replace(".scala","")
	    		val pluginObj = Class.forName("plugins."+className).newInstance.asInstanceOf[SakaiSimulationPlugin]
	    		if (pluginObj.name.matches(System.getProperty("allow-plugins"))) {
	    			if (pluginMap.contains( pluginObj.toolid ) ) {
	    				pluginMap += ( pluginObj.toolid -> exec( pluginMap(pluginObj.toolid) , pluginObj.getSimulationChain ) )
	    			} else {
	    				pluginMap += ( pluginObj.toolid -> pluginObj.getSimulationChain )
	    			}
    				println("Plugin ["+className+","+pluginObj.name+"] loaded !")
	    		}
	    	} catch {
	    		case e: Exception => {}
	    	}
	    )
	  }
	 
	  def getPluginMap : List[(Any,ChainBuilder)] = {
	  	pluginMap.toList
	  }
	  
	  init()
	  
}

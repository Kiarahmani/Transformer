import java.util.Map;

import soot.PackManager;
import soot.PhaseOptions;
import soot.Transform;
import soot.options.Options;
import soot.util.cfgcmd.AltClassLoader;
import soot.util.cfgcmd.CFGGraphType;
import soot.util.cfgcmd.CFGIntermediateRep;
import soot.util.cfgcmd.CFGToDotGraph;

public class Initializer {
	static String _RT_PATH = "/Library/Java/JavaVirtualMachines/jdk1.8.0_77.jdk/Contents/Home/jre/lib/rt.jar:";
	static String _JCE_PATH = "/Library/Java/JavaVirtualMachines/jdk1.8.0_77.jdk/Contents/Home/jre/lib/jce.jar";
	private static final String altClassPathOptionName = "alt-class-path";
	static final String graphTypeOptionName = "graph-type";
	static final String defaultGraph = "BriefUnitGraph";
	static final String irOptionName = "ir";
	static final String defaultIR = "Grimp";
	static final String multipageOptionName = "multipages";
	static final String briefLabelOptionName = "brief";

	public String[] initialize() {

		Generate viewer = new Generate();
		Transform printTransform = new Transform("jtp.printcfg", viewer);
		printTransform.setDeclaredOptions("enabled " + altClassPathOptionName + ' ' + graphTypeOptionName + ' '
				+ irOptionName + ' ' + multipageOptionName + ' ' + briefLabelOptionName + ' ');
		printTransform.setDefaultOptions("enabled " + altClassPathOptionName + ": " + graphTypeOptionName + ':'
				+ defaultGraph + ' ' + irOptionName + ':' + defaultIR + ' ' + multipageOptionName + ":false " + ' '
				+ briefLabelOptionName + ":false ");
		PackManager.v().getPack("jtp").add(printTransform);

		String[] soot_args = new String[3];
		// soot_args[0] = "-cp";
		soot_args[0] = "--soot-classpath";
		soot_args[1] = "/Users/Kiarash/dev/eclipse_workspace/SampleApplication/target/classes:" + _RT_PATH + _JCE_PATH;
		//soot_args[2] = "-w";
		soot_args[2] = "MySQLAccess";
		//oot_args[3] = "A";


		return soot_args;
	}
}

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import anomaly.Anomaly;
import exceptions.UnknownUnitException;
import gimpToApp.GimpToApp;
import gimpToApp.GimpToAppOne;
import ir.*;
import ir.schema.Table;
import soot.Body;
import soot.BodyTransformer;
import soot.PhaseOptions;
import soot.Scene;
import soot.jimple.JimpleBody;
import soot.util.cfgcmd.CFGIntermediateRep;
import sql.DDLParser;
import z3.ConstantArgs;
import z3.Z3Driver;

public class Transformer extends BodyTransformer {
	static String _RT_PATH = "/Library/Java/JavaVirtualMachines/jdk1.8.0_77.jdk/Contents/Home/jre/lib/rt.jar:";
	static String _JCE_PATH = "/Library/Java/JavaVirtualMachines/jdk1.8.0_77.jdk/Contents/Home/jre/lib/jce.jar";
	private static final String irOptionName = "ir";
	private CFGIntermediateRep ir;
	static ArrayList<Body> bodies;

	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
		if (bodies == null)
			bodies = new ArrayList<Body>();
		ir = CFGIntermediateRep.getIR(PhaseOptions.getString(options, irOptionName));
		Body body = ir.getBody((JimpleBody) b);
		if (!body.getMethod().isConstructor()) {
			// System.out.println(body.getUnits().iterator());
			bodies.add(body);
		}
	}

	////// MAIN
	public static void main(String[] args) {
		ConstantArgs ca = new ConstantArgs();
		Initializer init = new Initializer();
		String[] soot_args = init.initialize();
		soot.Main.main(soot_args);
		GimpToApp gta = null;
		Application app = null;
		long start = System.currentTimeMillis();
		/*
		 * extract tables from ddl file
		 */
		DDLParser ddlp = new DDLParser();
		ArrayList<Table> tables = ddlp.parse();
		System.out.print("=============================\n===	SCHEMA \n");
		for (Table t : tables)
			t.printTable();
		System.out.println();
		long endTables = System.currentTimeMillis();
		/*
		 * generate the intermediate representation
		 */
		gta = new GimpToAppOne(Scene.v(), bodies, tables);
		try {
			app = ((GimpToAppOne) gta).transform();
		} catch (UnknownUnitException e) {
			e.printStackTrace();
		}
		app.printApp();
		long endApp = System.currentTimeMillis();

		if (ConstantArgs.EXTRACT_ONLY)
			return;
		/*
		 * generate the anomaly given the IR
		 */
		Z3Driver zdr = new Z3Driver(app, tables, false);
		Anomaly anml = null;
		int iter = 1;
		List<Anomaly> seenAnmls = new ArrayList<>();
		// partitions
		while (ConstantArgs._current_partition_size <= ConstantArgs._MAX_NUM_PARTS) {
			int currentRowInstLimit =  ConstantArgs._MIN_ROW_INSTANCES;
			// row instance limitation
			while (currentRowInstLimit <= ConstantArgs._MAX_ROW_INSTANCES) {
				currentRowInstLimit = ConstantArgs._ENFORCE_ROW_INSTANCE_LIMITS ? currentRowInstLimit : tables.size();
				for (Set<Table> includedTables : getAllTablesPerms(tables, currentRowInstLimit)) {
					ConstantArgs._Current_Cycle_Length = ConstantArgs._Minimum_Cycle_Length;
					// cycle length
					do {
						long loopBegin = System.currentTimeMillis();
						System.out.println(runHeader(iter++, includedTables));
						zdr = new Z3Driver(app, tables, false);
						anml = zdr.analyze(seenAnmls, includedTables);
						if (anml != null) {
							seenAnmls.add(anml);
							anml.announce(false, seenAnmls.size());
							anml.closeCtx();
						} else
							zdr.closeCtx();
						System.out.println(runTimeFooter(loopBegin));
						// update global variables for the next round
						if (ConstantArgs._ENFORCE_EXCLUSION) {
							if (anml == null) // keep the length unchanged untill all of this length is found
								ConstantArgs._Current_Cycle_Length++;
						} else
							ConstantArgs._Current_Cycle_Length++;

					} while (ConstantArgs._Current_Cycle_Length <= ConstantArgs._MAX_CYCLE_LENGTH);
				}
				currentRowInstLimit++;
			}
			ConstantArgs._current_partition_size++;
		}
		long endZ3 = System.currentTimeMillis();
		// print stats
		printStats(seenAnmls.size(), ((endZ3 - endApp) / (iter - 1)), (endTables - start), (endApp - endTables),
				(endZ3 - endTables));

	}

	/*
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	/*
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	private static List<Set<Table>> getAllTablesPerms(ArrayList<Table> tables, int r) {
		List<Set<Table>> result = new ArrayList<>();
		Table[] arr = tables.toArray(new Table[tables.size()]);
		int n = arr.length;
		Table data[] = new Table[r];
		combinationUtil(arr, n, r, 0, data, 0, result);
		return result;
	}

	public static void combinationUtil(Table arr[], int n, int r, int index, Table data[], int i,
			List<Set<Table>> resList) {
		if (index == r) {
			Set<Table> resSet = new HashSet<>();
			for (int j = 0; j < r; j++)
				resSet.add(data[j]);
			resList.add(resSet);
			return;
		}
		if (i >= n)
			return;
		data[index] = arr[i];
		combinationUtil(arr, n, r, index + 1, data, i + 1, resList);
		combinationUtil(arr, n, r, index, data, i + 1, resList);
	}

	private static String runTimeFooter(long beginTime) {
		return ("----------------------------\n-- Extration time:  " + (System.currentTimeMillis() - beginTime) + " ms"
				+ "\n----------------------------" + "\n\n");
	}

	private static String runHeader(int iter, Set<Table> includedTables) {
		String output = "\n~ " + String.format("%0" + 85 + "d", 0).replace("0", "=");
		output += "\n~ RUN #" + iter;
		output += "  [cycle length:" + ConstantArgs._Current_Cycle_Length + "]";
		output += "  [partitions allowed:" + ConstantArgs._current_partition_size + "]";
		output += "  [max txns allowed:"
				+ ((ConstantArgs._MAX_TXN_INSTANCES == -1) ? "∞" : String.valueOf(ConstantArgs._MAX_TXN_INSTANCES))
				+ "]  ";
		output += includedTables.stream().map(t -> t.getName()).collect(Collectors.toList());
		output += "\n~ " + String.format("%0" + 85 + "d", 0).replace("0", "=");
		return output;
	}

	private static void printStats(int anmlCount, long avgExt, long tableExtTime, long appExtTime, long modelsTime) {
		System.out.println("\n\n\n\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println("~Anomalies found: " + anmlCount);
		System.out.println("~Avg Ext. Time:   " + avgExt + " ms");
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println("~Tables Extracted in:  " + tableExtTime + " ms");
		System.out.println("~App Extracted in:     " + appExtTime + " ms");
		System.out.println("~Models Generated in:  " + modelsTime + " ms");
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}
}

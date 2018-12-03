import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import utils.Tuple;
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
		new ConstantArgs();
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
		Anomaly anml1 = null, anml2 = null;
		int iter = 1;
		// the following two keep track of the found anomalies: seenAnmls for the
		// analysis time and seenStructures for later analysises
		List<Anomaly> seenAnmls = new ArrayList<>();
		List<List<Tuple<String, Tuple<String, String>>>> seenStructures = new ArrayList<>();
		if (ConstantArgs._CONTINUED_ANALYSIS) {
			try {
				seenStructures = load();
			} catch (ClassNotFoundException | IOException e1) {
			}
		}
		// partitions
		outerMostLoop: while (ConstantArgs._current_partition_size <= ConstantArgs._MAX_NUM_PARTS) {
			int currentRowInstLimit = ConstantArgs._MIN_ROW_INSTANCES;
			// row instance limitation
			while (currentRowInstLimit <= ConstantArgs._MAX_ROW_INSTANCES) {
				currentRowInstLimit = ConstantArgs._ENFORCE_ROW_INSTANCE_LIMITS ? currentRowInstLimit : tables.size();
				for (Set<Table> includedTables : getAllTablesPerms(tables, currentRowInstLimit)) {
					ConstantArgs._Current_Cycle_Length = ConstantArgs._Minimum_Cycle_Length;
					// cycle length
					do {
						try {
							save(seenStructures);
						} catch (IOException e) {
							e.printStackTrace();
						}
						anml2 = null;
						long step1Begin = System.currentTimeMillis();
						long step1Time = -100000, step2Time = 0;
						String config = runHeader(iter++, includedTables);
						System.out.println(config);

						// do the analysis twice (second time with enforced versioning)
						// Analysis Step 1
						zdr = new Z3Driver(app, tables, false);
						ConstantArgs._current_version_enforcement = false;
						anml1 = zdr.analyze(1, seenStructures, seenAnmls, includedTables, null);

						step1Time = System.currentTimeMillis() - step1Begin;
						if (anml1 != null) {
							anml1.generateCycleStructure();
							if (!ConstantArgs._ENFORCE_VERSIONING) {
								anml1.setExtractionTime(step1Time, 0);
								seenAnmls.add(anml1);
								seenStructures.add(anml1.getCycleStructure());
								anml1.addData("\\l" + config.replaceAll("\\n", "\\l") + "\\l");
								anml1.announce(false, seenAnmls.size());
								anml1.closeCtx();
							} else {
								// Analysis Step 2
								ConstantArgs._current_version_enforcement = true;
								// zdr = new Z3Driver(app, tables, false);
								long step2Begin = System.currentTimeMillis();
								anml2 = zdr.analyze(2, null, seenAnmls, includedTables, anml1);
								step2Time = System.currentTimeMillis() - step2Begin;
								if (anml2 != null) {
									anml2.generateCycleStructure();
									seenAnmls.add(anml2);
									seenStructures.add(anml2.getCycleStructure());
									anml2.setExtractionTime(step1Time, step2Time);
									anml2.announce(false, seenAnmls.size());

									anml2.addData("\\l" + config + "\\l");
									System.out.println(runTimeFooter(step1Time, step2Time));
									// pause();

									// inner iterations pushing Z3 into finding similar anoamlies together
									// the core anomaly if this class:
									long step3Begin = System.currentTimeMillis();
									Anomaly anml3 = zdr.analyze(3, null, seenAnmls, includedTables, anml2);
									while (anml3 != null) {
										long step3Time = System.currentTimeMillis() - step3Begin;
										anml3.setExtractionTime(step3Time, 0);
										anml3.generateCycleStructure();
										seenAnmls.add(anml3);
										seenStructures.add(anml3.getCycleStructure());
										System.out.println("~ Searching for structurally simillar anomalies....\n");
										anml3.announce(false, seenAnmls.size());
										System.out.println(runTimeFooter(step3Time, 0));
										step3Begin = System.currentTimeMillis();
										anml3 = zdr.analyze(4, null, seenAnmls, includedTables, anml3);
									}

									anml1.closeCtx();
								}
							}
						} else
							zdr.closeCtx();

						// update global variables for the next round
						if (ConstantArgs._ENFORCE_EXCLUSION) {
							if (anml2 == null) // keep the length unchanged untill all of this length is found
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

	private static List<List<Tuple<String, Tuple<String, String>>>> load() throws IOException, ClassNotFoundException {
		FileInputStream streamIn = new FileInputStream(
				"anomalies/" + ConstantArgs._BENCHMARK_NAME + "/previous_data.anomaly");
		ObjectInputStream objectinputstream = new ObjectInputStream(streamIn);
		List<List<Tuple<String, Tuple<String, String>>>> result = (List<List<Tuple<String, Tuple<String, String>>>>) objectinputstream
				.readObject();
		objectinputstream.close();
		return result;
	}

	private static void save(List<List<Tuple<String, Tuple<String, String>>>> seenStructures) throws IOException {
		File oldFile = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/previous_data.anomaly");
		oldFile.delete();
		FileOutputStream fout = new FileOutputStream(
				"anomalies/" + ConstantArgs._BENCHMARK_NAME + "/previous_data.anomaly");
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(seenStructures);
		oos.close();
	}

	private static void pause() {
		try {
			Thread.sleep(300000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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

	private static String runTimeFooter(long step1Time, long step2Time) {
		return ("--------------------------------\nExtraction time -- step1 " + step1Time + " ms"
				+ "\n               -- step2 " + step2Time + " ms" + "\n--------------------------------");
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

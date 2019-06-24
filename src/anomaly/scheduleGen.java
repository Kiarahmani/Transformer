package anomaly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

import ir.schema.Column;
import ir.schema.Table;
import utils.Tuple;
import z3.ConstantArgs;
import z3.DeclaredObjects;

public class scheduleGen {
	String text = "";
	Model model;
	List<Tuple<String, Tuple<String, String>>> cqlData = new ArrayList<>();
	ArrayList<Table> tables;
	DeclaredObjects objs;
	private Map<String, FuncDecl> allNextVars;
	Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow;
	Context ctx;
	List<Expr> ts, os;

	public scheduleGen(Context ctx, Model model, DeclaredObjects objs, ArrayList<Table> tables,
			Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow, List<Expr> ts, List<Expr> os) {
		this.conflictingRow = conflictingRow;
		this.model = model;
		this.objs = objs;
		this.tables = tables;
		this.ctx = ctx;
		this.allNextVars = objs.getAllNextVars();
		this.ts = ts;
		this.os = os;
	}

	//
	/////////////////////
	public void createInstance(String fileName) {
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();
		FileWriter writer = null;
		PrintWriter printer;
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);

	}

	//
	/////////////////////
	public void createSchedule(String fileName) {
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();
		FileWriter writer = null;
		PrintWriter printer;
		String node_style = "node[shape=box, color=gray100, fontcolor=maroon, fontsize=12, fontname=\"Helvetica\"]";
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);

		printer.append("digraph {" + node_style);
		printer.append("1 [label=\"");
		printer.append(this.text.replaceAll("\"", "\\\""));
		printer.append("\"]");
		printer.append("\n}");
		printer.flush();

	}

	// w
	/////////////////////
	public void createData(String fileName) {
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();
		FileWriter writer = null;
		PrintWriter printer;
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		model.getSortUniverse(objs.getSort("T"));
		Expr[] allOs = model.getSortUniverse(objs.getSort("O"));
		/////////// ADD INSTANTIATED ROWS

		// System.out.println("\n\n\n\n\n\n\n\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

		for (Table t : tables) {
			Expr[] allRs = model.getSortUniverse(objs.getSort(t.getName()));
			String delim = "";
			String columnNames = "(";
			for (Column column : t.getColumns()) {
				columnNames += (delim + column.name);
				delim = ",";
			}
			for (Expr r : allRs) {
				//System.out.println("-------" + r.toString().replaceAll("!val!", "#"));
				for (int i = 0; i < (Math.pow(2, ConstantArgs._MAX_VERSIONS_)); i++) {
					delim = "";
					String columnVals = "(";
					String header = "V" + i + ": ";
					for (Column column : t.getColumns()) {
						// columnNames += (delim + column.name);
						FuncDecl projFunc = objs.getfuncs(t.getName() + "_PROJ_" + column.name);
						String val = model.eval(projFunc.apply(r, ctx.mkBV(i, ConstantArgs._MAX_VERSIONS_)), true)
								.toString();
						columnVals += (delim + val);
						delim = ",";
					}
					if (i == 0) {
						printer.append("\nINSERT INTO testks." + t.getName().toUpperCase() + columnNames + ")"
								+ " VALUES" + columnVals + ")" + ";");
					}
					//System.out.println(header + columnVals + ")");
				}

			}
		}

		// System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		for (Expr o : allOs)
			for (String nextVar : allNextVars.keySet()) {
				FuncDecl varFunc = allNextVars.get(nextVar);
				FuncDecl parent = objs.getfuncs("parent");
				Expr t = model.eval(parent.apply(o), true);
				Expr row = model.eval(varFunc.apply(t), true);
				Table table = tables.stream().filter(tab -> row.toString().contains(tab.getName())).findAny().get();
				FuncDecl verFunc = objs.getfuncs(table.getName() + "_VERSION");
				Expr version = model.eval(verFunc.apply(row, o), true);

				String delim = "";
				String columnNames = "";
				String columnVals = "";
				String header = t.toString().replaceAll("!val!", "") + "-" + o.toString().replaceAll("!val!", "") + ": "
						+ table.getName() + ": V" + version + " ";

				//System.out.println(header + columnNames + "" + columnVals + "");
				// System.out.println();
			}
		//System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n\n\n\n\n\n\n\n");
		/////////// ADD CONFLICTING ROWS
		for (Tuple<Expr, Integer> versionedRow : conflictingRow.values()) {
			Table table = tables.stream().filter(t -> versionedRow.x.toString().contains(t.getName())).findAny().get();
			String delim = "";
			String columnNames = "(";
			String columnVals = "(";
			for (Column column : table.getColumns()) {
				columnNames += (delim + column.name);
				FuncDecl projFunc = objs.getfuncs(table.getName() + "_PROJ_" + column.name);
				String val = model
						.eval(projFunc.apply(versionedRow.x, ctx.mkBV(versionedRow.y, ConstantArgs._MAX_VERSIONS_)),
								true)
						.toString();
				columnVals += (delim + val);
				delim = ",";
			}
			this.cqlData.add(new Tuple<String, Tuple<String, String>>(table.getName(),
					new Tuple<String, String>((columnNames + ")"), columnVals + ")")));
			break; // because we only need the very first version to be instantiated
		}
		// write to file
		for (Tuple<String, Tuple<String, String>> dataElement : cqlData) {
			String tableName = dataElement.x;
			String Cnames = dataElement.y.x;
			String Vals = dataElement.y.y;
			printer.append("\nINSERT INTO testks." + tableName.toUpperCase() + Cnames + " VALUES" + Vals + ";");
			printer.flush();
		}

	}

/////////////////////
	public void createDump(String fileName) {
		File file = new File("anomalies/" + ConstantArgs._BENCHMARK_NAME + "/" + fileName);
		file.getParentFile().mkdirs();
		FileWriter writer = null;
		PrintWriter printer;
		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		model.getSortUniverse(objs.getSort("T"));
		Expr[] allOs = model.getSortUniverse(objs.getSort("O"));


		for (Table t : tables) {
			Expr[] allRs = model.getSortUniverse(objs.getSort(t.getName()));
			String delim = "";
			String columnNames = "(";
			for (Column column : t.getColumns()) {
				columnNames += (delim + column.name);
				delim = ",";
			}
			for (Expr r : allRs) {
				printer.append("\n-------" + r.toString().replaceAll("!val!", "#"));
				for (int i = 0; i < (Math.pow(2, ConstantArgs._MAX_VERSIONS_)); i++) {
					delim = "";
					String columnVals = "(";
					String header = "V" + i + ": ";
					for (Column column : t.getColumns()) {
						// columnNames += (delim + column.name);
						FuncDecl projFunc = objs.getfuncs(t.getName() + "_PROJ_" + column.name);
						String val = model.eval(projFunc.apply(r, ctx.mkBV(i, ConstantArgs._MAX_VERSIONS_)), true)
								.toString();
						columnVals += (delim + val);
						delim = ",";
					}
					printer.append("\n"+header + columnVals + ")");
				}

			}
		}

		for (Expr o : allOs)
			for (String nextVar : allNextVars.keySet()) {
				FuncDecl varFunc = allNextVars.get(nextVar);
				FuncDecl parent = objs.getfuncs("parent");
				Expr t = model.eval(parent.apply(o), true);
				Expr row = model.eval(varFunc.apply(t), true);
				Table table = tables.stream().filter(tab -> row.toString().contains(tab.getName())).findAny().get();
				FuncDecl verFunc = objs.getfuncs(table.getName() + "_VERSION");
				Expr version = model.eval(verFunc.apply(row, o), true);

				String delim = "";
				String columnNames = "";
				String columnVals = "";
				String header = t.toString().replaceAll("!val!", "") + "-" + o.toString().replaceAll("!val!", "") + ": "
						+ table.getName() + ": V" + version + " ";

				printer.append("\n"+header + columnNames + "" + columnVals + "");
			}
		printer.flush();
		
		

	}

}

package anomaly;

/*
 * creates a .dot file to be used for visualization 
 * 
 * 
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

import ir.schema.Column;
import ir.schema.Table;
import utils.Tuple;
import z3.DeclaredObjects;

public class RecordsVisualizer {

	Model model;
	DeclaredObjects objs;
	Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow;
	List<Tuple<Expr, Integer>> orderedConflictingRows;
	ArrayList<Table> tables;
	Context ctx;

	public RecordsVisualizer(Context ctx, Model model, DeclaredObjects objs, ArrayList<Table> tables,
			Map<Tuple<Expr, Expr>, Tuple<Expr, Integer>> conflictingRow) {
		this.ctx = ctx;
		this.model = model;
		this.objs = objs;
		this.tables = tables;
		this.conflictingRow = conflictingRow;
		orderedConflictingRows = new ArrayList<>();

	}

	public void createGraph(String fileName) {
		File file = new File("anomalies/" + fileName);
		FileWriter writer = null;
		PrintWriter printer;
		String node_style = "node[shape=record, color=midnightblue, fontcolor=midnightblue, fontsize=10, fontname=\"Helvetica\"]";

		try {
			writer = new FileWriter(file, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		printer = new PrintWriter(writer);
		model.getSortUniverse(objs.getSort("T"));
		model.getSortUniverse(objs.getSort("O"));
		printer.append("digraph {\n" + node_style);
		
		
		for (Tuple<Expr, Integer> versionedRow : conflictingRow.values()) {
			Table table = tables.stream().filter(t -> versionedRow.x.toString().contains(t.getName())).findAny().get();
			String content = "";
			String lable = versionedRow.x.toString().replaceAll("!val!", "");
			String labelVersion = String.valueOf(versionedRow.y);
			String tableStringBeing = "\n" + lable + labelVersion + "" + " [label=\"{";
			String tableStringEnd = "\"];";
			content += "{";
			content += "{Z3 label|" + lable + "(v" + labelVersion + ")}";
			for (Column column : table.getColumns()) {
				FuncDecl projFunc = objs.getfuncs(table.getName() + "_PROJ_" + column.name);
				String value = (model.eval(projFunc.apply(versionedRow.x, ctx.mkInt(versionedRow.y)), true)).toString();
				content += "|{";
				content += column.name;
				content += "|";
				content += value.replaceAll("\"", "-");
				content += "}";
			}

			/*
			 * 
			 * 
			 * String outerDelim = "{Z3 label|"; for (Expr row :
			 * Arrays.asList(model.getSortUniverse(objs.getSort(tableName)))) { if
			 * (conflictingRow.values().stream().map(tuple ->
			 * tuple.x).collect(Collectors.toList()).contains(row)) { content += outerDelim;
			 * outerDelim = "|"; content += row.toString().replaceAll("!val!", ""); } }
			 * outerDelim = "}|"; for (Column column : table.getColumns()) { content +=
			 * outerDelim + "{"; outerDelim = "|"; content += column.name + "|"; String
			 * delim = ""; for (Expr row :
			 * Arrays.asList(model.getSortUniverse(objs.getSort(tableName)))) { if
			 * (conflictingRow.values().stream().map(tuple ->
			 * tuple.x).collect(Collectors.toList()) .contains(row)) { content += delim;
			 * delim = "|"; FuncDecl projFunc = objs.getfuncs(tableName + "_PROJ_" +
			 * column.name); FuncDecl versionFunc = objs.getfuncs(tableName + "_VERSION");
			 * // XXX // Expr version = model.eval(versionFunc.apply(row), true); // Expr
			 * value = model.eval(projFunc.apply(row, version), true); String value =
			 * "TEMP"; if (value.toString().equals("\"\"")) content += "\'\' \\'\\'"; else
			 * content += value.toString().replaceAll("\"", ""); } } content += "}"; }
			 */
			content += "}}";
			printer.append(tableStringBeing + content + tableStringEnd);
			printer.flush();

		}

		printer.append("\n}");
		printer.flush();

	}

	private void printTables() {
		for (Table table : tables) {
			System.out.println("\n\n----------------------------------------------------------");
			System.out.println("---- Table: " + table.getName());
			for (Expr row : Arrays.asList(model.getSortUniverse(objs.getSort(table.getName()))))
				printRow(row);
		}

	}

	private void printRow(Expr row) {
		System.out.println(row);

	}
}

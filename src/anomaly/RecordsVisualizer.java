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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

import ir.schema.Column;
import ir.schema.Table;
import z3.DeclaredObjects;

public class RecordsVisualizer {

	Model model;
	DeclaredObjects objs;
	ArrayList<Table> tables;

	public RecordsVisualizer(Model model, DeclaredObjects objs, ArrayList<Table> tables) {

		this.model = model;
		this.objs = objs;
		this.tables = tables;

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
		for (Table table : tables) {
			List<Expr> allRows = Arrays.asList(model.getSortUniverse(objs.getSort(table.getName())));
			/// XXX NOT SURE IF CORRECT -> KEEPING IT TO UNCLUTTER THE OUTPUT
			if (allRows.size() > 0) {
				String content = "";
				String tableName = table.getName();
				String tableStringBeing = "\n" + tableName + " [label=\"{";
				String tableStringEnd = "\"];";
				content += "{" + tableName + "}|{";
				String outerDelim = "{Z3 label|";
				for (Expr row : Arrays.asList(model.getSortUniverse(objs.getSort(tableName)))) {
					content += outerDelim;
					outerDelim = "|";
					content += row.toString().replaceAll("!val!", "");
				}
				outerDelim = "}|";
				for (Column column : table.getColumns()) {
					content += outerDelim + "{";
					outerDelim = "|";
					content += column.name + "|";
					String delim = "";
					for (Expr row : Arrays.asList(model.getSortUniverse(objs.getSort(tableName)))) {
						content += delim;
						delim = "|";
						FuncDecl projFunc = objs.getfuncs(tableName + "_PROJ_" + column.name);
						FuncDecl versionFunc = objs.getfuncs(tableName + "_VERSION");
						// XXX
						// Expr version = model.eval(versionFunc.apply(row), true);
						// Expr value = model.eval(projFunc.apply(row, version), true);
						String value = "TEMP";
						if (value.toString().equals("\"\""))
							content += "\'\' \\'\\'";
						else
							content += value.toString().replaceAll("\"", "");

					}
					content += "}";
				}
				content += "}}";
				printer.append(tableStringBeing + content + tableStringEnd);
			}

		}

		// salam|kiri| khan | khoobi?
		// printer.append(tableString);

		// ttype = model.eval(objs.getfuncs("ttype").apply(t), true).toString();

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

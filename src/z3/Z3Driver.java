// this function will communicate with Z3 according to the counter-example
// generation settings and will produce and return a concrete anomaly which will
// be used for user information or execution path generations
package z3;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.microsoft.z3.*;
import anomaly.Anomaly;
import ir.Application;
import ir.schema.Table;

public class Z3Driver {
	Application app;
	ArrayList<Table> tables;
	Context ctx;
	Solver slv;
	Model model;
	DeclaredObjects objs;
	StaticAssertions cons;
	// a log file containing all assertions and defs for debugging
	File file = new File("z3-encoding.smt2");
	FileWriter writer;
	PrintWriter printer;

	// constructor
	public Z3Driver(Application app, ArrayList<Table> tables) {
		this.app = app;
		this.tables = tables;
		HashMap<String, String> cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		cfg.put("unsat_core", "true");
		ctx = new Context(cfg);
		slv = ctx.mkSolver();
		model = null;
		try {
			writer = new FileWriter(file, false);
			printer = new PrintWriter(writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.objs = new DeclaredObjects(printer);
		ctxInitializeSorts();
		this.cons = new StaticAssertions(ctx, objs);

	}

	private void LogZ3(String s) {
		printer.append(s + "\n");
		printer.flush();
	}

	/*
	 * 
	 * adds the constant assertions and defs to the context
	 */
	private void ctxInitializeSorts() {
		LogZ3(";sorts");
		objs.addSort("T", ctx.mkUninterpretedSort("T"));
		objs.addSort("O", ctx.mkUninterpretedSort("O"));
		objs.addSort("Bool", ctx.mkBoolSort());
		objs.addSort("Int", ctx.mkIntSort());
		objs.addSort("String", ctx.mkStringSort());
		objs.addSort("Real", ctx.mkRealSort());
	}

	private void ctxInitialize() {
		LogZ3(";data types");
		objs.addDataType("TType",
				mkDataType("TType", new String[] { "New_reservation", "Find_flights", "Find_open_seats" }));
		objs.addDataType("OType", mkDataType("OType",
				new String[] { "New_reservation_select_1", "New_reservation_select_2", "New_reservation_update_3" }));

		LogZ3(";functions");
		objs.addFunc("ttype", ctx.mkFuncDecl("ttype", objs.getSort("T"), objs.getDataTypes("TType")));
		objs.addFunc("otype", ctx.mkFuncDecl("otype", objs.getSort("O"), objs.getDataTypes("OType")));
		objs.addFunc("is_update", ctx.mkFuncDecl("is_update", objs.getSort("O"), objs.getSort("Bool")));
		objs.addFunc("parent", ctx.mkFuncDecl("parent", objs.getSort("O"), objs.getSort("T")));
		objs.addFunc("sibling",
				ctx.mkFuncDecl("sibling", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("WR_O",
				ctx.mkFuncDecl("WR_O", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("RW_O",
				ctx.mkFuncDecl("RW_O", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("WW_O",
				ctx.mkFuncDecl("WW_O", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("vis",
				ctx.mkFuncDecl("vis", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("ar",
				ctx.mkFuncDecl("ar", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		// Dependency Graph Relations
		objs.addFunc("D",
				ctx.mkFuncDecl("D", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		objs.addFunc("X",
				ctx.mkFuncDecl("X", new Sort[] { objs.getSort("O"), objs.getSort("O") }, objs.getSort("Bool")));
		// assertions
		addAssertion("par_then_sib", cons.mk_par_then_sib());
		addAssertion("sib_then_par", cons.mk_sib_then_par());
		addAssertion("ar_on_writes", cons.mk_ar_on_writes());
		addAssertion("vis_then_ar", cons.mk_vis_then_ar());
		addAssertion("types_then_eq", cons.mk_types_then_eq());
		addAssertion("no_loops_o", cons.mk_no_loops_o());
		addAssertion("trans_ar", cons.mk_trans_ar());
		addAssertion("total_ar", cons.mk_total_ar());
		addAssertion("wr_then_vis", cons.mk_wr_then_vis());
		addAssertion("ww_then_ar", cons.mk_ww_then_ar());
		addAssertion("rw_then_not_vis", cons.mk_rw_then_not_vis());
		addAssertion("irreflx_ar", cons.mk_irreflx_ar());
		// addAssertion("irreflx_sibling", cons.mk_irreflx_sibling());
		addAssertion("gen_dep", cons.mk_gen_dep());
		addAssertion("gen_depx", cons.mk_gen_depx());
		addAssertion("cycle", cons.mk_cycle());
	}

	private void addAssertion(String name, BoolExpr ass) {
		LogZ3(";" + name);
		objs.addAssertion(name, ass);
		slv.add(ass);
	}

	/*
	 */
	private DatatypeSort mkDataType(String name, String[] consts) {
		String[] head_tail = new String[] {};
		Sort[] sorts = new Sort[] {};
		int[] sort_refs = new int[] {};
		Constructor[] constructors = new Constructor[consts.length];
		for (int i = 0; i < consts.length; i++)
			constructors[i] = ctx.mkConstructor(consts[i], "is_cons", head_tail, sorts, sort_refs);
		return ctx.mkDatatypeSort(name, constructors);
	}

	/*
	 * final call to Z3 when the context is completely done
	 */
	private Anomaly checkSAT() {
		if (slv.check() == Status.SATISFIABLE) {
			model = slv.getModel();
			return new Anomaly(model, ctx, objs);
		} else {
			System.err.println("Failed to generate a counter example");
			ctx.close();
			return null;
		}
	}

	/*
	 * public function called from main
	 */
	@SuppressWarnings("resource")
	public Anomaly analyze() {
		ctxInitialize();
		return checkSAT();
	}

}

//

//

//

//

//

//

//

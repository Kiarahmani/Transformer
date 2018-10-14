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
import ir.Transaction;
import ir.schema.Table;
import sun.net.www.content.audio.x_aiff;

public class Z3Driver {
	Application app;
	ArrayList<Table> tables;
	Context ctx;
	Solver slv;
	Model model;
	DeclaredObjects objs;
	StaticAssertions staticAssrtions;
	DynamicAssertsions dynamicAssertions;
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
		this.staticAssrtions = new StaticAssertions(ctx, objs);
		this.dynamicAssertions = new DynamicAssertsions(ctx, objs);

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
		objs.addDataType("TType", mkDataType("TType", app.getAllTxnNames()));
		objs.addDataType("OType", mkDataType("OType", app.getAllStmtTypes()));
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
		addAssertion("par_then_sib", staticAssrtions.mk_par_then_sib());
		addAssertion("sib_then_par", staticAssrtions.mk_sib_then_par());
		addAssertion("ar_on_writes", staticAssrtions.mk_ar_on_writes());
		addAssertion("vis_then_ar", staticAssrtions.mk_vis_then_ar());
		addAssertion("types_then_eq", staticAssrtions.mk_types_then_eq());
		addAssertion("no_loops_o", staticAssrtions.mk_no_loops_o());
		addAssertion("trans_ar", staticAssrtions.mk_trans_ar());
		addAssertion("total_ar", staticAssrtions.mk_total_ar());
		addAssertion("wr_then_vis", staticAssrtions.mk_wr_then_vis());
		addAssertion("ww_then_ar", staticAssrtions.mk_ww_then_ar());
		addAssertion("rw_then_not_vis", staticAssrtions.mk_rw_then_not_vis());
		addAssertion("irreflx_ar", staticAssrtions.mk_irreflx_ar());
		addAssertion("oType_to_is_update", dynamicAssertions.mk_oType_to_is_update(app.getAllUpdateStmtTypes()));
		addAssertion("is_update_to_oType", dynamicAssertions.mk_is_update_to_oType(app.getAllUpdateStmtTypes()));
		for (Transaction txn : app.getTxns()) {
			String name = txn.getName();
			for (String stmtName : txn.getStmtNames()) {
				addAssertion("op_types_" + name + "_" + stmtName,
						dynamicAssertions.op_types_to_parent_type(name, stmtName));
			}

		}

		// dependency assertions

		addAssertion("gen_dep", staticAssrtions.mk_gen_dep());
		addAssertion("gen_depx", staticAssrtions.mk_gen_depx());
		addAssertion("cycle", staticAssrtions.mk_cycle());

	}

	private void addAssertion(String name, BoolExpr ass) {
		LogZ3(";" + name);
		objs.addAssertion(name, ass);
		slv.add(ass);
	}

	/*
	 */
	private DatatypeSort mkDataType(String name, String[] consts) {
		Symbol[] head_tail = new Symbol[] {};
		Sort[] sorts = new Sort[] {};
		int[] sort_refs = new int[] {};
		Constructor[] constructors = new Constructor[consts.length];
		for (int i = 0; i < consts.length; i++)
			constructors[i] = ctx.mkConstructor(ctx.mkSymbol(consts[i]), ctx.mkSymbol("is_" + consts[i]), head_tail,
					sorts, sort_refs);
		DatatypeSort result = ctx.mkDatatypeSort(name, constructors);
		return result;
	}

	/*
	 * final call to Z3 when the context is completely done
	 */
	private Anomaly checkSAT() {
		if (slv.check() == Status.SATISFIABLE) {
			model = slv.getModel();
			return new Anomaly(model, ctx, objs);
		} else {
			System.err.println("Failed to generate a counter example +++ bound: " + ConstantArgs._DEP_CYCLE_LENGTH);
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

// this function will communicate with Z3 according to the counter-example
// generation settings and will produce and return a concrete anomaly which will
// be used for user information or execution path generations
package z3;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.z3.*;
import anomaly.Anomaly;
import ir.Application;
import ir.Transaction;
import ir.schema.Column;
import ir.schema.Table;

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
	Expr vo1, vo2, vt1, vt2;

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

		// to be used in the rules
		vo1 = ctx.mkFreshConst("o", objs.getSort("O"));
		vo2 = ctx.mkFreshConst("o", objs.getSort("O"));
		vt1 = ctx.mkFreshConst("t", objs.getSort("T"));
		vt2 = ctx.mkFreshConst("t", objs.getSort("T"));

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
		// create table sorts and constraints
		for (Table t : tables) {
			objs.addSort(t.getName(), ctx.mkUninterpretedSort(t.getName()));
		}
	}

	private void ctxInitialize() {
		LogZ3(";data types");
		objs.addDataType("TType", mkDataType("TType", app.getAllTxnNames()));
		objs.addDataType("OType", mkDataType("OType", app.getAllStmtTypes()));
		LogZ3(";functions");
		objs.addFunc("otime", ctx.mkFuncDecl("otime", objs.getSort("O"), objs.getSort("Int")));
		objs.addFunc("opart", ctx.mkFuncDecl("opart", objs.getSort("O"), objs.getSort("Int")));
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
		// TEMOPORARY XXXXX
		// change the shape of the anomaly
		if (ConstantArgs._NO_WW)
			addAssertion("no_ww", staticAssrtions.mk_no_ww());
		if (ConstantArgs._NO_WR)
			addAssertion("no_wr", staticAssrtions.mk_no_wr());
		if (ConstantArgs._NO_RW)
			addAssertion("no_rw", staticAssrtions.mk_no_rw());
		if (ConstantArgs._MAX_TXN_INSTANCES != -1)
			addAssertion("limit_txn_instances",
					dynamicAssertions.mk_limit_txn_instances(ConstantArgs._MAX_TXN_INSTANCES));
		;

		// assertions
		addAssertion("par_then_sib", staticAssrtions.mk_par_then_sib());
		addAssertion("sib_then_par", staticAssrtions.mk_sib_then_par());
		addAssertion("ar_on_writes", staticAssrtions.mk_ar_on_writes());
		addAssertion("vis_on_writes", staticAssrtions.mk_vis_on_writes());
		addAssertion("vis_then_ar", staticAssrtions.mk_vis_then_ar());
		addAssertion("types_then_eq", staticAssrtions.mk_types_then_eq());
		addAssertion("no_loops_o", staticAssrtions.mk_no_loops_o());
		addAssertion("trans_ar", staticAssrtions.mk_trans_ar());
		addAssertion("total_ar", staticAssrtions.mk_total_ar());
		addAssertion("wr_then_vis", staticAssrtions.mk_wr_then_vis());
		addAssertion("ww_then_ar", staticAssrtions.mk_ww_then_ar());
		addAssertion("rw_then_not_vis", staticAssrtions.mk_rw_then_not_vis());
		addAssertion("irreflx_ar", staticAssrtions.mk_irreflx_ar());
		addAssertion("otime_props", staticAssrtions.mk_otime_props());
		addAssertion("opart_props", staticAssrtions.mk_opart_props());
		addAssertion("oType_to_is_update", dynamicAssertions.mk_oType_to_is_update(app.getAllUpdateStmtTypes()));
		addAssertion("is_update_to_oType", dynamicAssertions.mk_is_update_to_oType(app.getAllUpdateStmtTypes()));
		// relating operation otypes to parent ttypes
		for (Transaction txn : app.getTxns()) {
			String name = txn.getName();
			for (String stmtName : txn.getStmtNames())
				addAssertion("op_types_" + name + "_" + stmtName,
						dynamicAssertions.op_types_to_parent_type(name, stmtName));
		}
		// make sure the otime assignment follows the program order
		for (Transaction txn : app.getTxns()) {
			Map<Integer, String> map = txn.getStmtNamesMap();
			for (int po : map.keySet())
				if (map.get(po + 1) != null) {
					addAssertion("otime_follows_po_" + po + "_" + map.get(po),
							dynamicAssertions.otime_follows_po(map.get(po), map.get(po + 1)));
				}
		}

		LogZ3("\n;TABLES");
		// create table sorts and constraints
		for (Table t : tables) {
			Sort tSort = objs.getSort(t.getName());
			Sort oSort = objs.getSort("O");
			for (Column c : t.getColumns())
				objs.addFunc(t.getName() + "_PROJ_" + c.getName(), ctx.mkFuncDecl(t.getName() + "_PROJ_" + c.getName(),
						new Sort[] { tSort }, objs.getSort(c.getType().toString())));
			addAssertion("pk_" + t.getName(), dynamicAssertions.mk_pk_tables(t));
			objs.addFunc("RW_O_" + t.getName(),
					ctx.mkFuncDecl("RW_O_" + t.getName(), new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("WR_O_" + t.getName(),
					ctx.mkFuncDecl("WR_O_" + t.getName(), new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("WW_O_" + t.getName(),
					ctx.mkFuncDecl("WW_O_" + t.getName(), new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("RW_Alive_" + t.getName(), ctx.mkFuncDecl("RW_Alive_" + t.getName(),
					new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("WR_Alive_" + t.getName(), ctx.mkFuncDecl("WR_Alive_" + t.getName(),
					new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));
			objs.addFunc("WW_Alive_" + t.getName(), ctx.mkFuncDecl("WW_Alive_" + t.getName(),
					new Sort[] { tSort, oSort, oSort }, objs.getSort("Bool")));

		}

		// rules
		LogZ3(";\n;\n;");
		thenWW();
		thenWR();
		thenRW();
		WWthen();
		WRthen();
		RWthen();

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
			System.out.println("-------------\n--UNSAT core:");
			for (Expr e : slv.getUnsatCore())
				System.out.println(e);
			ctx.close();
			return null;
		}
	}

	//
	//
	//
	// ---------------------------------------------------------------------------
	// functions adding assertions for every pair of operations that 'potentially'
	// create the edge
	private void RWthen() {

		Map<String, FuncDecl> Ts = objs.getAllTTypes();
		for (FuncDecl t1 : Ts.values())
			for (FuncDecl t2 : Ts.values()) {
				List<BoolExpr> conditions = dynamicAssertions.return_conditions_rw_then(vo1, vo2, vt1, vt2);
				conditions.add(ctx.mkFalse());
				BoolExpr rhs = ctx.mkOr(conditions.toArray(new BoolExpr[conditions.size()]));
				BoolExpr lhs1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo1), vt1);
				BoolExpr lhs2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo2), vt2);
				BoolExpr lhs3 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt1),
						ctx.mkApp(objs.getConstructor("TType", t1.getName().toString())));
				BoolExpr lhs4 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt2),
						ctx.mkApp(objs.getConstructor("TType", t2.getName().toString())));
				BoolExpr lhs5 = ctx.mkNot(ctx.mkEq(vo1, vo2));
				BoolExpr lhs6 = ctx.mkNot(ctx.mkEq(vt1, vt2));
				BoolExpr lhs7 = (BoolExpr) ctx.mkApp(objs.getfuncs("RW_O"), vo1, vo2);
				BoolExpr lhs = ctx.mkAnd(lhs1, lhs2, lhs3, lhs4, lhs5, lhs6, lhs7);
				BoolExpr body = ctx.mkImplies(lhs, rhs);
				Quantifier rw_then = ctx.mkForall(new Expr[] { vo1, vo2, vt1, vt2 }, body, 1, null, null, null, null);
				String rule_name = t1.getName().toString() + "-" + t2.getName().toString() + "-rw-then";
				addAssertion(rule_name, rw_then);
			}

	}

	private void WRthen() {
		Map<String, FuncDecl> Ts = objs.getAllTTypes();
		for (FuncDecl t1 : Ts.values())
			for (FuncDecl t2 : Ts.values()) {
				List<BoolExpr> conditions = dynamicAssertions.return_conditions_wr_then(vo1, vo2, vt1, vt2);
				conditions.add(ctx.mkFalse());
				BoolExpr rhs = ctx.mkOr(conditions.toArray(new BoolExpr[conditions.size()]));
				BoolExpr lhs1 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo1), vt1);
				BoolExpr lhs2 = ctx.mkEq(ctx.mkApp(objs.getfuncs("parent"), vo2), vt2);
				BoolExpr lhs3 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt1),
						ctx.mkApp(objs.getConstructor("TType", t1.getName().toString())));
				BoolExpr lhs4 = ctx.mkEq(ctx.mkApp(objs.getfuncs("ttype"), vt2),
						ctx.mkApp(objs.getConstructor("TType", t2.getName().toString())));
				BoolExpr lhs5 = ctx.mkNot(ctx.mkEq(vo1, vo2));
				BoolExpr lhs6 = ctx.mkNot(ctx.mkEq(vt1, vt2));
				BoolExpr lhs7 = (BoolExpr) ctx.mkApp(objs.getfuncs("WR_O"), vo1, vo2);
				BoolExpr lhs = ctx.mkAnd(lhs1, lhs2, lhs3, lhs4, lhs5, lhs6, lhs7);
				BoolExpr body = ctx.mkImplies(lhs, rhs);
				Quantifier rw_then = ctx.mkForall(new Expr[] { vo1, vo2, vt1, vt2 }, body, 1, null, null, null, null);
				String rule_name = t1.getName().toString() + "-" + t2.getName().toString() + "-wr-then";
				addAssertion(rule_name, rw_then);
			}

	}

	private void WWthen() {
		// TODO Auto-generated method stub

	}

	private void thenRW() {
		// TODO Auto-generated method stub

	}

	private void thenWR() {
		// TODO Auto-generated method stub

	}

	private void thenWW() {
		// TODO Auto-generated method stub

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

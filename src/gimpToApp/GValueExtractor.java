package gimpToApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import exceptions.UnknownUnitException;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.grimp.internal.GAssignStmt;
import soot.grimp.internal.GInstanceFieldRef;
import soot.grimp.internal.GInterfaceInvokeExpr;
import soot.grimp.internal.GInvokeStmt;
import soot.grimp.internal.GNewInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.NullConstant;
import soot.jimple.StringConstant;
import soot.jimple.internal.JimpleLocal;

public class GValueExtractor {
	HashMap<String, Object> dataMap;
	GValue rhs;
	GValue lhs;

	public GValueExtractor(Unit u) throws UnknownUnitException {
		// do things
		dataMap = new HashMap<String, Object>();
		switch (u.getClass().getSimpleName()) {
		case "GIdentityStmt":
			//System.err.println("Unit not handled yet");
			break;
		case "GAssignStmt":
			assignStmtHandler(u);
			break;
		case "GInvokeStmt":
			invokeStmtHandler(u);
			break;
		case "JGotoStmt":
			//System.err.println("Unit not handled yet");
			break;
		case "GThrowStmt":
			//System.err.println("Unit not handled yet");
			break;
		case "JReturnVoidStmt":
			//System.err.println("Unit not handled yet");
			break;
		case "GIfStmt":
			//System.err.println("Unit not handled yet");
			break;
		default:
			throw new UnknownUnitException("Unknown Jimple/Grimp unit class: " + u.getClass().getSimpleName());
		}

	}

	// this recursive function should not modify the global Map
	private GValue valueHandler(Value v) throws UnknownUnitException {
		GValue result;
		List<GValue> localArgs;
		SootMethod localMethod;
		Value localBase;
		switch (v.getClass().getSimpleName()) {
		case "GStaticInvokeExpr":
			// not interesting
			break;
		case "GVirtualInvokeExpr":
			// non interesting cases + string related ops which should be considered ater
			//System.err.println("value class not handled yet " + v.getClass().getSimpleName());
			break;
		case "GInstanceFieldRef":
			// r0 instances fields
			GInstanceFieldRef ifr = (GInstanceFieldRef) v;
			result = new GValue(false);
			result.setBase(valueHandler(ifr.getBase()));
			result.setField(ifr.getField());
			return result;
		case "GInterfaceInvokeExpr":
			// invokation on a local object
			GInterfaceInvokeExpr ifInvkExpr = (GInterfaceInvokeExpr) v;
			localBase = ifInvkExpr.getBase();
			localArgs = new ArrayList<GValue>();
			localMethod = ifInvkExpr.getMethod();
			for (Value vv : ifInvkExpr.getArgs())
				localArgs.add(valueHandler(vv));
			result = new GValue(false);
			result.setMethod(localMethod);
			result.setArgs(localArgs);
			result.setBase(valueHandler(localBase));
			return result;

		case "GNewInvokeExpr":
			// new object creation
			GNewInvokeExpr iExpr = (GNewInvokeExpr) v;
			localMethod = iExpr.getMethod();
			localArgs = new ArrayList<GValue>();
			for (Value vv : iExpr.getArgs())
				localArgs.add(valueHandler(vv));
			result = new GValue(false);
			result.setMethod(localMethod);
			result.setArgs(localArgs);
			return result;
		case "StaticFieldRef":
			// not interesting -- e.g. print stream
			break;
		case "GSpecialInvokeExpr":
			// intra procedure calls -- not interesting for now
			break;
		case "JimpleLocal":
			result = new GValue(true);
			result.setGrimpValue(v);
			return result;
		case "NullConstant":
			result = new GValue(true);
			result.setGrimpValue(v);
			return result;
		case "StringConstant":
			result = new GValue(true);
			result.setGrimpValue(v);
			return result;
		case "IntConstant":
			result = new GValue(true);
			result.setGrimpValue(v);
			return result;

		default:
			throw new UnknownUnitException("Unknown Jimple/Grimp unit class: " + v.getClass().getSimpleName());

		}
		return null;
	}

	private void invokeStmtHandler(Unit u) throws UnknownUnitException {
		GInvokeStmt expr = (GInvokeStmt) u;
		Value value = expr.getUseBoxes().get(expr.getUseBoxes().size() - 1).getValue();
		GValue val = valueHandler(value);
		if (val != null)
			rhs = val;


	}

	private void assignStmtHandler(Unit u) throws UnknownUnitException {
		GAssignStmt stmt = (GAssignStmt) u;
		Value rOP = stmt.getRightOp();
		Value lOP = stmt.getLeftOp();
		GValue val = valueHandler(rOP);
		if (val != null)
			rhs = val;

		val = valueHandler(lOP);
		if (val != null)
			lhs = val;


	}

	public boolean containsSqlStmt() {
		return false;
	}

}

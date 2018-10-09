package z3;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.z3.*;

public class DeclaredObjects {

	private Map<String, Sort> sorts;
	private Map<String, Symbol> symbols;
	private Map<String, FuncDecl> funcs;
	private Map<String, DatatypeSort> datatypes;
	private Map<String, BoolExpr> assertions;
	PrintWriter printer;

	public void addSort(String key, Sort value) {
		sorts.put(key, value);
		LogZ3("(declare-sort " + value.toString() + ")");
	}

	public void addSymbol(String key, Symbol value) {
		symbols.put(key, value);
	}

	public void addFunc(String key, FuncDecl value) {
		funcs.put(key, value);
		LogZ3(value.toString());
	}

	public void addDataType(String key, DatatypeSort value) {
		datatypes.put(key, value);
		LogZ3(value.toString());
		String s = "";
		for (FuncDecl x:value.getConstructors())
			s+=("	"+x.getName()+"\n");
		LogZ3(s);
	}

	public void addAssertion(String key, BoolExpr value) {
		assertions.put(key, value);
		LogZ3(value.toString());
	}

	public Sort getSort(String key) {
		return sorts.get(key);
	}

	public Symbol getSymbol(String key) {
		return symbols.get(key);
	}

	public FuncDecl getfuncs(String key) {
		return funcs.get(key);
	}

	public DatatypeSort getDataTypes(String key) {
		return datatypes.get(key);
	}

	public BoolExpr getAssertions(String key) {
		return assertions.get(key);
	}

	private void LogZ3(String s) {
		printer.append(s + "\n");
		printer.flush();
	}

	public DeclaredObjects(PrintWriter printer) {
		sorts = new HashMap<String, Sort>();
		symbols = new HashMap<String, Symbol>();
		funcs = new HashMap<String, FuncDecl>();
		datatypes = new HashMap<String, DatatypeSort>();
		assertions = new HashMap<String, BoolExpr>();
		this.printer = printer;
	}

}

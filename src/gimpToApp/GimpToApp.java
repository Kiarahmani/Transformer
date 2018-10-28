package gimpToApp;

import java.util.ArrayList;

import ir.schema.Table;
import soot.Body;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public class GimpToApp {
	private Scene v;
	ArrayList<Body> bodies;
	ArrayList<Table> tables;

	public GimpToApp(Scene v2, ArrayList<Body> bodies, ArrayList<Table> tables) {
		this.v = v;
		this.tables = tables;
		this.bodies = bodies;
	}

	/*
	 * 
	 * pretty printing the Gimp bodies for developement and debug
	 * 
	 */
	public void printGimpBody(Body b) {
		int _line_length = 120;
		System.out.println("\n\n" + String.format("%0" + _line_length + "d", 0).replace("0", "="));
		System.out.println("Txn: " + b.getMethod());
		System.out.println(String.format("%0" + _line_length + "d", 0).replace("0", "="));
		int iter = 0;
		for (Unit u : b.getUnits()) {
			System.out.print("(" + iter + ")\n");
			System.out.println(" ╰──" + u.getClass().getSimpleName());
			System.out.println(" ╰──" + u);
			System.out.println(String.format("%0" + _line_length + "d", 0).replace("0", "-"));
			iter++;
		}
	}

	

}

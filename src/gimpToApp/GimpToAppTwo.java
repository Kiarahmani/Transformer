package gimpToApp;
import java.util.ArrayList;

import ir.Application;
import ir.schema.Table;
import soot.Body;
import soot.Scene;

public class GimpToAppTwo extends GimpToApp {

	public GimpToAppTwo(Scene v2, ArrayList<Body> bodies, ArrayList<Table> tables) {
		super(v2, bodies, tables);
		// TODO Auto-generated constructor stub
	}
	
	public Application transform() {
		Application app = new Application();
		for (Body b : bodies) {
			super.printGimpBody(b);
		}
		return app;
	}

}

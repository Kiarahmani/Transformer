
import java.util.ArrayList;

import ir.Application;
import ir.Table;
import soot.Body;
import soot.Scene;
import soot.Unit;

public class GimpToApp {
	private Scene v;
	ArrayList<Body> bodies;

	public GimpToApp(Scene v2, ArrayList<Body> bodies, ArrayList<Table> tables) {
		// TODO Auto-generated constructor stub
		this.v = v;
		this.bodies = bodies;
	}

	public Application transform(int absLvl) {
		for (Body b : bodies) {
			System.out.println(
					"\n\n====================================================================================================================\n"
							+ "Txn: " + b.getMethod());
			System.out.println(
					"====================================================================================================================");
			int iter = 1;
			for (Unit u : b.getUnits()) {
				System.out.print("(" + iter + ")\n");
				System.out.println(" ╰──" + u.getClass());
				System.out.println(" ╰──" + u);
				System.out.println(
						"----------------------------------------------------------------------------------------------------------------");
				iter++;
			}
			break;// temp - to limit the console output
		}
		return new Application();
	}

}

package z3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstantArgs {

	public static int _DEP_CYCLE_LENGTH =6;
	public static int _MAX_NUM_PARTS = 2;
	public static int _MAX_TXN_INSTANCES = 2; // -1 for no constraint
	public static boolean _NO_WW = false;
	public static boolean _NO_WR = false;
	public static boolean _NO_RW = false;
	public static List<String> _EXCLUDED_TXNS = Arrays.asList("initialize");

	// ---
	public static final boolean DEBUG_MODE = false;
}

package z3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConstantArgs {

	public static int _DEP_CYCLE_LENGTH =8;
	public static int _MAX_NUM_PARTS = 1;
	public static int _MAX_TXN_INSTANCES = -1; // -1 for no constraint
	public static boolean _NO_WW = false;
	public static boolean _NO_WR = false;
	public static boolean _NO_RW = false;
	
	public static List<String> _EXCLUDED_TXNS = Arrays.asList("initialize", "updateName");

	// ---
	public static final boolean DEBUG_MODE = false;
	public static boolean _FIND_CORE = false;
}

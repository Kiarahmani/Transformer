package z3;

import java.util.Arrays;
import java.util.List;

public class ConstantArgs {

	// file under analysis
	//public static String _TESTS_OR_BENCHMARKS = "benchmarks.";
	public static String _TESTS_OR_BENCHMARKS = "tests.";
	public static String _BENCHMARK_NAME = "DualProblems";
	// maximum length of anomalous cycles looked for
	public static int _MAX_CYCLE_LENGTH = 6;
	// maxim number of partitions
	public static int _MAX_NUM_PARTS = 2;
	// size of the bit vectors representing integers
	public static int _MAX_VERSIONS_ = 4;
	// maximum number of transaction instances in the anomaly (-1 for no constraint)
	public static int _MAX_TXN_INSTANCES = -1;
	// should Z3 extract transaction parameters as well?
	public static boolean _ENFORCE_VERSIONING = true;
	// should Z3 exclue anomalies already found from the analysis?
	public static boolean _ENFORCE_EXCLUSION = true;
	// additional constraints on the anomalies
	public static boolean _NO_WW = true;
	public static boolean _NO_WR = false;
	public static boolean _NO_RW = false;

	/*
	 * Internal Global Variables (do not change unless you know what you're doing)
	 */
	public static int _Minimum_Cycle_Length = 3;
	public static int _Current_Cycle_Length = _Minimum_Cycle_Length;
	public static int _current_partition_size = 1;

	/*
	 * Developer's Area
	 */
	public static final boolean DEBUG_MODE = false;
	public static boolean _FIND_CORE = false;
	public static List<String> _EXCLUDED_TXNS = Arrays.asList("initialize", "select2");

}

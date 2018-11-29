package z3;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConstantArgs {

	/*
	 * 
	 * Application Under Analysis
	 * 
	 * 
	 */
	public static String _TESTS_OR_BENCHMARKS = "tests.";
	public static String _DDL_FILE = "ddl.sql";
	public static String _BENCHMARK_NAME = "TestApp";
	public static List<String> _EXCLUDED_TXNS = Arrays.asList("initialize");

	/*
	 * 
	 * 
	 * Parse and assign config file
	 * 
	 * 
	 */
	public ConstantArgs() {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("src/config.properties");
			prop.load(input);
			ConstantArgs._MAX_CYCLE_LENGTH = Integer.parseInt(prop.getProperty("_MAX_CYCLE_LENGTH", "6"));
			ConstantArgs._MAX_NUM_PARTS = Integer.parseInt(prop.getProperty("_MAX_NUM_PARTS", "1"));
			ConstantArgs._MAX_VERSIONS_ = Integer.parseInt(prop.getProperty("_MAX_VERSIONS_", "4"));
			ConstantArgs._MAX_LOOP_UNROLL = Integer.parseInt(prop.getProperty("_MAX_LOOP_UNROLL", "4"));
			ConstantArgs._MAX_ROWS_SIZE = Integer.parseInt(prop.getProperty("_MAX_ROWS_SIZE", "4"));
			ConstantArgs._MAX_TXN_INSTANCES = Integer.parseInt(prop.getProperty("_MAX_TXN_INSTANCES", "-1"));
			ConstantArgs._MAX_ROW_INSTANCES = Integer.parseInt(prop.getProperty("_MAX_ROW_INSTANCES", "3"));
			ConstantArgs._MIN_ROW_INSTANCES = Integer.parseInt(prop.getProperty("_MIN_ROW_INSTANCES", "1"));
			ConstantArgs._ENFORCE_VERSIONING = Boolean.parseBoolean(prop.getProperty("_ENFORCE_VERSIONING", "false"));
			ConstantArgs._current_version_enforcement = _ENFORCE_VERSIONING;
			ConstantArgs._ENFORCE_EXCLUSION = Boolean.parseBoolean(prop.getProperty("_ENFORCE_EXCLUSION", "true"));
			ConstantArgs._ENFORCE_ROW_INSTANCE_LIMITS = Boolean
					.parseBoolean(prop.getProperty("_ENFORCE_ROW_INSTANCE_LIMITS", "true"));
			ConstantArgs._NO_WW = Boolean.parseBoolean(prop.getProperty("_NO_WW", "false"));
			ConstantArgs._NO_WR = Boolean.parseBoolean(prop.getProperty("_NO_WR", "false"));
			ConstantArgs._NO_RW = Boolean.parseBoolean(prop.getProperty("_NO_RW", "false"));
			ConstantArgs.DEBUG_MODE = Boolean.parseBoolean(prop.getProperty("DEBUG_MODE", "false"));
			ConstantArgs.EXTRACT_ONLY = Boolean.parseBoolean(prop.getProperty("EXTRACT_ONLY", "false"));
			ConstantArgs._FIND_CORE = Boolean.parseBoolean(prop.getProperty("_FIND_CORE", "false"));
			/*
			 * Internal Global Variables (do not change unless you know what you're doing)
			 */
			ConstantArgs._Minimum_Cycle_Length = Integer.parseInt(prop.getProperty("_Minimum_Cycle_Length", "3"));
			ConstantArgs._Current_Cycle_Length = ConstantArgs._Minimum_Cycle_Length;
			ConstantArgs._current_partition_size = 1;

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static int _MAX_CYCLE_LENGTH;
	public static int _MAX_NUM_PARTS;
	public static int _MAX_VERSIONS_;
	public static int _MAX_LOOP_UNROLL;
	public static int _MAX_ROWS_SIZE;
	public static int _MAX_TXN_INSTANCES;
	public static int _MAX_ROW_INSTANCES;
	public static int _MIN_ROW_INSTANCES;
	public static boolean _ENFORCE_VERSIONING;
	public static boolean _current_version_enforcement;
	public static boolean _ENFORCE_EXCLUSION;
	public static boolean _ENFORCE_ROW_INSTANCE_LIMITS;
	public static boolean _NO_WW = true;
	public static boolean _NO_WR = false;
	public static boolean _NO_RW = false;
	public static int _Minimum_Cycle_Length;
	public static int _Current_Cycle_Length;
	public static int _current_partition_size = 1;
	public static boolean DEBUG_MODE;
	public static boolean EXTRACT_ONLY;
	public static boolean _FIND_CORE;

}

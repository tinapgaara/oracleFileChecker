
package oracle.apmaas.util.fileChecker;

public class Logger {

	public static final int LOG_LEVEL_Silent = 0;
	public static final int LOG_LEVEL_Exception = 1;
	public static final int LOG_LEVEL_Info = 2;
	public static final int LOG_LEVEL_Log = 3;
	
	public static int logLevel = LOG_LEVEL_Log; // how much output
	
	public static void writeLog(String log) {
		if (logLevel >= LOG_LEVEL_Log)
		    System.out.println("[LOG] " + log);
	}

	public static void writeInfo(String info) {
		if (logLevel >= LOG_LEVEL_Info)
		    System.out.println(info);
	}

	public static void writeException(String exDesp) {
		if (logLevel >= LOG_LEVEL_Exception)
		    System.out.println("[EXCEPTION] " + exDesp);
	}
}

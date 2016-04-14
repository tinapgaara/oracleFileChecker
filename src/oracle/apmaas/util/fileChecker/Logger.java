/*
 *  +===========================================================================+
 *  |      Copyright (c) 2016 Oracle Corporation, Redwood Shores, CA, USA       |
 *  |                         All rights reserved.                              |
 *  +===========================================================================+
 */

package oracle.apmaas.util.fileChecker;

import java.io.*;

public class Logger {

	public static final int LOG_LEVEL_Silent = 0;
	public static final int LOG_LEVEL_Exception = 1;
	public static final int LOG_LEVEL_Info = 2;
	public static final int LOG_LEVEL_Log = 3;

	private final static String EXCEPTION_FILE_NAME_Prefix = "crException_";

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

		// write exception to a file
		writeException2File(exDesp);
	}

	public static void writeException2File(String exDesp) {
		String exceptionFileName = "." + File.separatorChar + EXCEPTION_FILE_NAME_Prefix + ".txt";
		File exceptionFile = null;
		FileWriter fw = null;
		try {
			exceptionFile  = new File(exceptionFileName);
			if ( ! exceptionFile.exists()) {
				exceptionFile.createNewFile();
				fw = new FileWriter(exceptionFile.getCanonicalFile());
				fw.write("Exceptions in File Checking and File Edition : " + "\n");
				fw.write(exDesp + "\n");
			} else {
				fw = new FileWriter(exceptionFile.getCanonicalFile(), true);
				fw.write(exDesp + "\n");
			}

		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
		finally {
			if (fw != null) {
				try {
					fw.close();
				}
				catch (Exception ignoreException) {
					// e.printStackTrace();
				}
			}
		}
	}

}

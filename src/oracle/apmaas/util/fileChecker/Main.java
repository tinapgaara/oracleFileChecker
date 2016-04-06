
package oracle.apmaas.util.fileChecker;

public class Main {

	private final static String COMMAND_help = "-h";
	private final static String COMMAND_help_2 = "-help";
	private final static String COMMAND_log = "-log";
	private final static String COMMAND_info = "-info";
	private final static String COMMAND_exception = "-ex";
	private final static String COMMAND_silent = "-s";

	public static void main(String[] args) {
		String path = null;
		int logLevel = Logger.LOG_LEVEL_Log;
		if (args != null) {
			if (args.length == 1) { // -h -help path
				if (COMMAND_help.equalsIgnoreCase(args[0]) || COMMAND_help_2.equalsIgnoreCase(args[0])) {
					Logger.writeInfo("Usage: \r\n   -h or -help :\r\n" +
							"       Show help messages. OR\r\n" +
							"   <file-path> :\r\n" +
							"       Check this file; OR\r\n" +
							"   [directory] :\r\n" +
							"       Check all Java files in this directory and all its sub directories,\r\n" +
							"       where \"directory\" can be an absolute path or a path relative to the current directory.\r\n" +
							"       If directory is not present, check all Java files in the current directory and all its sub directories; OR\r\n" +
							"   <directory> [-s|-log|-info|-ex] :\r\n" +
							"       Check all Java files in this directory,\r\n" +
							"       -s means running silently, \r\n" +
							"       -log means displaying log entries, notifications and exceptions, \r\n" +
							"       -info means displaying notifications and exceptions, \r\n" +
							"       -ex means displaying only exceptions.\r\n");

					return;
				}
				else {
					path = args[0];
				}
			}
			else if (args.length > 1) { // path -s
				path = args[0];
				if (COMMAND_silent.equalsIgnoreCase(args[1]))
					logLevel = Logger.LOG_LEVEL_Silent;
				else if (COMMAND_info.equalsIgnoreCase(args[1]))
					logLevel = Logger.LOG_LEVEL_Info;
				else if (COMMAND_exception.equalsIgnoreCase(args[1]))
					logLevel = Logger.LOG_LEVEL_Exception;
			}
		}

		Logger.logLevel = logLevel;

		CopyrightChecker checker = new CopyrightChecker();
		boolean recursive = true;
		checker.checkPath(path, recursive);

		CopyrightEditor editor = new CopyrightEditor();
		editor.edit(checker.getIncorrectFilePaths());
	}

}

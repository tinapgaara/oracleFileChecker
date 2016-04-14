/*
 *  +===========================================================================+
 *  |      Copyright (c) 2016 Oracle Corporation, Redwood Shores, CA, USA       |
 *  |                         All rights reserved.                              |
 *  +===========================================================================+
 */
package oracle.apmaas.util.fileChecker;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yiyitan on 4/1/2016.
 */
public class CopyrightChecker {

	private final static String COPYRIGHT_FIRST_LINE_PATTERN =
			"\\s*Copyright\\s*\\(c\\)\\s*([0-9]{4}|([0-9]{4})\\s*\\-\\s*([0-9]{4}))\\s*Oracle\\s*Corporation,\\s*Redwood\\s*Shores,\\s*CA,\\s*USA";
	private final static String COPYRIGHT_SECOND_LINE_PATTERN =
			"\\s*(a|A)ll\\s*(r|R)ights\\s*(r|R)eserved\\s*";

	// Used to judge the wrong format of the first line
	// if not match, but has these at least $threshold$ of these keywords, we take this as wrong format line
	private final static int COPYRIGHT_START_LINE_KEYWORD_NUM_Threshold = 2;
	private final static String COPYRIGHT_START_LINE_KEYWORD_1 = "copyright";
	private final static String COPYRIGHT_START_LINE_KEYWORD_2 = "oracle";
	private final static String COPYRIGHT_START_LINE_KEYWORD_3 = "corporation";
	private final static String COPYRIGHT_START_LINE_KEYWORD_4 = "redwood";
	private final static String COPYRIGHT_START_LINE_KEYWORD_5 = "shores";

	private final static String APM_WLDF_INTERNAL_FILE_NAME = "apm-wldf-INTERNAL-RELEASE.properties";
	private final static String APM_WLDF_FUTURE_FILE_NAME = "apm-wldf-FUTURE.properties";

	public static int COPYRIGHT_OK = 0;
	public static int COPYRIGHT_Not_Present = 0x0001;
	public static int COPYRIGHT_Wrong_Format = 0x0002;

	public static int COPYRIGHT_FAILED = -1;

	private final static String RESULT_FILE_NAME_Prefix = "crCheckResult_";

	private List<String> wrongFormatFilePaths;
	private List<String> missingFilePaths;

	private String[] copyrightPatterns;
	private String[] startLineKeywords;

	public CopyrightChecker() {
		wrongFormatFilePaths = new ArrayList<>();
		missingFilePaths = new ArrayList<>();

		copyrightPatterns = new String[] {
				COPYRIGHT_FIRST_LINE_PATTERN,
				COPYRIGHT_SECOND_LINE_PATTERN };

		startLineKeywords = new String[] {
				COPYRIGHT_START_LINE_KEYWORD_1,
				COPYRIGHT_START_LINE_KEYWORD_2,
				COPYRIGHT_START_LINE_KEYWORD_3,
				COPYRIGHT_START_LINE_KEYWORD_4,
				COPYRIGHT_START_LINE_KEYWORD_5};
	}

	public List<String> getWrongFormatFilePaths() {
		return wrongFormatFilePaths;
	}

	public List<String> getMissingFilePaths() {
		return missingFilePaths;
	}

	public void checkDir(String path) {
		File file = new File(path);
		String canonicalPath = null;
		try {
			canonicalPath = file.getCanonicalPath();
			if (file.exists()) {
				if (file.isFile())
					checkFile(file);
				else if (file.isDirectory()) {
					Logger.writeLog("Begin to check directory: " + path + " ......");
					FileFilter filter = new FileExtNameFilter(Main.FILE_EXT_NAME_Java);
					File[] files = file.listFiles(filter);
					if (files != null) {
						for (File curFile : files) {
							checkDir(curFile.getCanonicalPath());
						}
					}
					Logger.writeLog("End checking directory: " + path);
				}
			}
			else {
				Logger.writeException("File or path " + canonicalPath + " does not exist.");
			}
		}
		catch (IOException e) {
			Logger.writeException(e.getMessage());
			e.printStackTrace();
		}
	}

	public int checkFile(File file) {
		int caseNum = COPYRIGHT_Not_Present;

		String filePath = null;
		BufferedReader bufferedReader = null;
		try {
			filePath = file.getCanonicalPath();
			Logger.writeLog("Checking file: " + filePath + " ......");

			bufferedReader = new BufferedReader(new FileReader(file));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				caseNum = checkStartLine(line);
				if (caseNum != COPYRIGHT_Not_Present) {
					if (caseNum == COPYRIGHT_OK) {
						line = bufferedReader.readLine();
						if (line == null) caseNum = COPYRIGHT_Wrong_Format;
						else caseNum = checkFollowingLine(line, 2);
					}
					break;
				}
			}
		}
		catch (Exception e) {
			Logger.writeException(e.getMessage());
			e.printStackTrace();
			caseNum = COPYRIGHT_FAILED;
		}
		finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				}
				catch (Exception ignoreException) {
					// nothing to do here
				}
				bufferedReader = null;
			}
		}

		// Dump to list
		if (caseNum != COPYRIGHT_OK) {
			if (caseNum == COPYRIGHT_Wrong_Format) wrongFormatFilePaths.add(filePath);
			else if (caseNum == COPYRIGHT_Not_Present) missingFilePaths.add(filePath);
		}

		return caseNum;
	}

	private int checkStartLine(String line) {
		int caseNum = COPYRIGHT_Not_Present;

		Pattern pattern = Pattern.compile(copyrightPatterns[0]);
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			// Found the start line of copyright block
			caseNum = COPYRIGHT_OK;

			// special case: Copyright (c) 2014-2013 Oracle Corporation, Redwood Shores, CA, USA
			if ((matcher.group(2) != null) && (matcher.group(3) != null)) {
				int fromYear = Integer.parseInt(matcher.group(2));
				int toYear = Integer.parseInt(matcher.group(3));
				if ((fromYear >= toYear) || (toYear > Main.getCurYear())) {
					caseNum = COPYRIGHT_Wrong_Format;
				}
			}
		}
		else {
			StringTokenizer st = new StringTokenizer(line.toLowerCase());
			StringBuilder sb = new StringBuilder();
			while (st.hasMoreTokens()) {
				sb.append(st.nextToken());
			}

			int matchCount = 0;
			for (int i = 0; i < startLineKeywords.length; i++) {
				if (sb.indexOf(startLineKeywords[i]) >= 0) {
					matchCount++;
				}
			}
			if (matchCount >= COPYRIGHT_START_LINE_KEYWORD_NUM_Threshold) {
				caseNum = COPYRIGHT_Wrong_Format;
			}
		}

		return caseNum;
	}

	private int checkFollowingLine(String line, int lineNum) {
		int caseNum = COPYRIGHT_Wrong_Format;

		Pattern pattern = Pattern.compile(copyrightPatterns[lineNum - 1]);
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			caseNum = COPYRIGHT_OK;
		}

		return caseNum;
	}

	private String describeCase(int caseNum) {
		String caseDesp = null;

		if ((caseNum & COPYRIGHT_Not_Present) != 0)
			caseDesp = "Copyright does not present";

		if ((caseNum & COPYRIGHT_Wrong_Format) != 0)
			caseDesp = "Copyright wrong format";

		return caseDesp;
	}

	public void writeResultToFile(String pathChecked) {
		BufferedWriter bw = null;
		File resultFile = null;
		// delete all old result files
		for (String fileName : generateAllResultFileNames()) {
			resultFile = new File(fileName);
			if (resultFile.exists()) {
				resultFile.delete();
			}
		}
		if (missingFilePaths.isEmpty() && wrongFormatFilePaths.isEmpty()) {
			Logger.writeInfo("File/Path checked: " + pathChecked);
			Logger.writeInfo("No error found.");
		}
		else {
			String completedTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
			// write wrong format file paths to result files
			if (! wrongFormatFilePaths.isEmpty())
				writeResultToFile(COPYRIGHT_Wrong_Format, resultFile, bw, pathChecked, completedTime);
			// write missing file paths to result files
			if (! missingFilePaths.isEmpty())
				writeResultToFile(COPYRIGHT_Not_Present, resultFile, bw, pathChecked, completedTime);
		}
	}

	private void writeResultToFile(int caseNum, File resultFile, BufferedWriter bw, String pathChecked, String completedTime) {
		String caseDesp = describeCase(caseNum);
		try {
			if ( (caseDesp != null) && (resultFile != null ) ) {
				resultFile = new File(generateResultFileName(caseNum));
				resultFile.createNewFile();
				FileWriter fw = new FileWriter(resultFile.getCanonicalFile());
				bw = new BufferedWriter(fw);

				bw.write("File/Path checked: " + pathChecked);
				bw.newLine();
				bw.write("Check completed at " + completedTime + ".");
				bw.newLine();
				bw.newLine();

				bw.write("*************************** Error : " + caseDesp + " ***************************");
				bw.newLine();
				List<String> incorrectFilePaths = new ArrayList<>();
				if (caseNum == COPYRIGHT_Wrong_Format)
					incorrectFilePaths = wrongFormatFilePaths;
				else if (caseNum == COPYRIGHT_Not_Present)
					incorrectFilePaths = missingFilePaths;
				for (String incorrectFilePath : incorrectFilePaths) {
					bw.write(incorrectFilePath);
					bw.newLine();
				}
			}
		}
		catch (java.io.IOException e) {
			Logger.writeException(e.getMessage());
			e.printStackTrace();
		}
		finally {
			if (bw != null) {
				try {
					bw.close();
				}
				catch (Exception ignoreException) {
					// nothing to do here
				}
			}
		}
	}

	private String generateResultFileName(int caseNum) {
		return "." + File.separatorChar + RESULT_FILE_NAME_Prefix + caseNum + ".txt";
	}

	private String[] generateAllResultFileNames() {
		String[] fileNames = new String[2];

		fileNames[0] = generateResultFileName(COPYRIGHT_Not_Present);
		fileNames[1] = generateResultFileName(COPYRIGHT_Wrong_Format);

		return fileNames;
	}

	static class FileExtNameFilter implements FileFilter {
		private String ext;

		public FileExtNameFilter(String ext) {
			this.ext = ext;
		}

		public boolean accept(File file) {
			if (file.isHidden())
				return false;
			else if (file.isDirectory())
				return true;
			else {
				String fileName = file.getName();
				if (fileName.equalsIgnoreCase(APM_WLDF_INTERNAL_FILE_NAME) ||
						fileName.equalsIgnoreCase(APM_WLDF_FUTURE_FILE_NAME)) {
					return true;
				}
				return fileName.endsWith(ext);
			}
		}
	}
}
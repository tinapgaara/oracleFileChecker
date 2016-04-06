package oracle.apmaas.util.fileChecker;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yiyitan on 4/1/2016.
 */
public class CopyrightChecker {

	private final static int COPYRIGHT_BLOCK_LINE_NUM = 2;
	private final static String COPYRIGHT_FIRST_LINE_PATTERN =
			"\\s*Copyright\\s*\\(c\\)\\s*([0-9]{4}|([0-9]{4})\\s*\\-\\s*([0-9]{4}))\\s*Oracle\\s*Corporation,\\s*Redwood\\s*Shores,\\s*CA,\\s*USA";
	private final static String COPYRIGHT_SECOND_LINE_PATTERN =
			"\\s*(a|A)ll\\s*(r|R)ights\\s*(r|R)eserved\\s*";
	/*
    private final static String COPYRIGHT_WRONG_FORMAT_PATTERN =
            "((c|C)opy\\s*(r|R)ight)(.*?)(((o|O)racle)\\s*((c|C)orporation))(.*?)((r|R)edwood\\s*(s|S)hores)";
    //*/
	private final static int COPYRIGHT_START_LINE_KEYWORD_NUM = 5;
	private final static int COPYRIGHT_START_LINE_KEYWORD_NUM_Threshold = 3;
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

	private final static String FILE_EXT_NAME_Java = ".java";
	private final static String CURRENT_DIR = ".";
	private final static String RESULT_FILE_NAME_Prefix = "crCheckResult_";

	private Map<Integer, List<String>> incorrectFilePaths;
	private String[] copyrightPatterns;
	private String[] startLineKeywords;

	public CopyrightChecker() {
		incorrectFilePaths  = new HashMap<>();

		copyrightPatterns = new String[COPYRIGHT_BLOCK_LINE_NUM];
		copyrightPatterns[0] = COPYRIGHT_FIRST_LINE_PATTERN;
		copyrightPatterns[1] = COPYRIGHT_SECOND_LINE_PATTERN;

		startLineKeywords = new String[COPYRIGHT_START_LINE_KEYWORD_NUM];
		startLineKeywords[0] = COPYRIGHT_START_LINE_KEYWORD_1;
		startLineKeywords[1] = COPYRIGHT_START_LINE_KEYWORD_2;
		startLineKeywords[2] = COPYRIGHT_START_LINE_KEYWORD_3;
		startLineKeywords[3] = COPYRIGHT_START_LINE_KEYWORD_4;
		startLineKeywords[4] = COPYRIGHT_START_LINE_KEYWORD_5;
	}

	public Map<Integer, List<String>> getIncorrectFilePaths() {
		return incorrectFilePaths;
	}

	public void checkPath(String path, boolean recursive) {
		if (path == null)
			path = CURRENT_DIR;

		File file = new File(path);
		String canonicalPath = null;
		try {
			canonicalPath = file.getCanonicalPath();
			if (file.exists()) {
				if (file.isFile())
					checkFile(file);
				else if (file.isDirectory())
					checkDir(file, recursive);

				writeResultToFile(canonicalPath);
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

	public void checkDir(File dir, boolean recursive) {
		try {
			String path = dir.getCanonicalPath();
			Logger.writeLog("Begin to check directory: " + path + " ......");

			FileFilter filter = new FileExtNameFilter(FILE_EXT_NAME_Java);
			File[] files = dir.listFiles(filter);
			if (files != null) {
				for (File curFile : files) {
					if (curFile.isFile()) {
						checkFile(curFile);
					}
				}

				if (recursive) {
					for (File curFile : files) {
						if (curFile.isDirectory()) {
							checkDir(curFile, true);
						}
					}
				}
			}

			Logger.writeLog("End checking directory: " + path);
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

			FileReader reader = new FileReader(file);
			bufferedReader = new BufferedReader(reader);

			String line;
			while ( (line = bufferedReader.readLine()) != null) {
				caseNum = checkStartLine(line);
				if (caseNum != COPYRIGHT_Not_Present) {
					if (caseNum == COPYRIGHT_OK) {
						line = bufferedReader.readLine();
						if (line == null) {
							caseNum = COPYRIGHT_Wrong_Format;
						}
						else {
							caseNum = checkFollowingLine(line, 2);
						}
					}
					break;
				}
			}
		}
		catch (FileNotFoundException e) {
			Logger.writeException(e.getMessage());
			e.printStackTrace();
		}
		catch (IOException e) {
			Logger.writeException(e.getMessage());
			e.printStackTrace();
		}
		finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				}
				catch (IOException e) {
					Logger.writeException(e.getMessage());
					e.printStackTrace();
				}
				bufferedReader = null;
			}
		}

		// Dump to list
		if (caseNum != COPYRIGHT_OK) {
			if ( ! incorrectFilePaths.containsKey(caseNum) ) {
				incorrectFilePaths.put(caseNum, new ArrayList<String>());
			}
			incorrectFilePaths.get(caseNum).add(filePath);
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
			if ((matcher.group(2) != null) && (matcher.group(3) != null) ) {
				int fromYear = Integer.parseInt(matcher.group(2));
				int toYear = Integer.parseInt(matcher.group(3));
				if ( (fromYear >= toYear) || (toYear > getCurYear()) ) {
					caseNum = COPYRIGHT_Wrong_Format;
				}
			}
		}
		else {
			StringTokenizer st = new StringTokenizer(line.toLowerCase());
			StringBuilder sb = new StringBuilder();
			while(st.hasMoreTokens()){
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

		if ( (caseNum & COPYRIGHT_Not_Present) != 0 )
			caseDesp = "Copyright does not present";

		if ( (caseNum & COPYRIGHT_Wrong_Format) != 0 ) {
			caseDesp = "Copyright wrong format";
		}

		return caseDesp;
	}

	private int getCurYear() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		return calendar.get(Calendar.YEAR);
	}

	public void writeResultToFile(String pathChecked) {
		BufferedWriter bw = null;
		try {
			File resultFile;

			// delete all old result files
			for (String fileName : generateAllResultFileNames()) {
				resultFile = new File(fileName);
				if (resultFile.exists()) {
					resultFile.delete();
				}
			}

			if (incorrectFilePaths.isEmpty()) {
				Logger.writeInfo("File/Path checked: " + pathChecked);
				Logger.writeInfo("No error found.");
			}
			else {
				String completedTime = generateTimeString(System.currentTimeMillis());

				for (Integer caseNum : incorrectFilePaths.keySet()) {
					List<String> filePaths = incorrectFilePaths.get(caseNum);
					String caseDesp = describeCase(caseNum);
					if ( (caseDesp != null) && (filePaths != null) ) {
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
						for (String path : filePaths) {
							bw.write(path);
							bw.newLine();
						}
						bw.newLine();

						bw.flush();
						bw.close();
						bw = null;
					}
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
				catch (IOException e) {
					Logger.writeException(e.getMessage());
					e.printStackTrace();
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

	private String generateTimeString(long timeInMillis) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(timeInMillis);
		return "" + (calendar.get(Calendar.MONTH) + 1) + "/" +
				calendar.get(Calendar.DAY_OF_MONTH) + "/" +
				calendar.get(Calendar.YEAR) + " " +
				calendar.get(Calendar.HOUR_OF_DAY) + ":" +
				calendar.get(Calendar.MINUTE) + ":" +
				calendar.get(Calendar.SECOND) + "." +
				calendar.get(Calendar.MILLISECOND);
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



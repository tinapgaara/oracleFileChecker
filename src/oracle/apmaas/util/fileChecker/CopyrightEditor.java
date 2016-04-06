package oracle.apmaas.util.fileChecker;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Created by yiyitan on 4/4/2016.
 */
public class CopyrightEditor {

    private static final String COPYRIGHT_STANDARD_BLOCK =
            "/*\n" +
                    " *  +===========================================================================+\n" +
                    " *  |      Copyright (c) 2016 Oracle Corporation, Redwood Shores, CA, USA       |\n" +
                    " *  |                         All rights reserved.                              |\n" +
                    " *  +===========================================================================+\n" +
                    " *  |  HISTORY                                                                  |\n" +
                    " *  +===========================================================================+\n" +
                    " */\n";

    private final static String TEMP_FILE_NAME = "$$$TMP.tmp";
    private final static int FILE_OP_OK = 0;
    private final static int FILE_OP_Failed = -1001;


    public CopyrightEditor() {
        // nothing to do here
    }

    public void edit(Map<Integer, List<String>> incorrectFilePaths) {
        if ( (incorrectFilePaths == null) || incorrectFilePaths.isEmpty() ) {
            return;
        }

        int fileOpCode = FILE_OP_OK;
        for (Integer caseNum : incorrectFilePaths.keySet()) {
            if (caseNum == CopyrightChecker.COPYRIGHT_Not_Present) {
                // missing block, add copyright to it
                List<String> filePaths = incorrectFilePaths.get(caseNum);
                if (filePaths != null) {
                    for (String path : filePaths) {
                        fileOpCode = addCopyright2MissingFiles(path);
                        if (fileOpCode != FILE_OP_OK) { // if operation on File is failed, need to break
                            break;
                        }
                    }
                }
            }

            if (fileOpCode != FILE_OP_OK) {
                break;
            }
        }
    }

    private int addCopyright2MissingFiles(String filePath) {
        int resultCode = FILE_OP_OK; // if operation on File is succedded

        Logger.writeLog("Begin to insert copyright block to file: " + filePath);

        BufferedReader bufferedReader = null;
        PrintWriter out = null;
        try {
            File inFile = new File(filePath);

            String tempFilePath = generateTempFilePath(inFile.getCanonicalPath());
            File outFile = new File(tempFilePath);
            out = new PrintWriter(new FileOutputStream(outFile));

            out.write(COPYRIGHT_STANDARD_BLOCK);

            bufferedReader = new BufferedReader(new FileReader(inFile));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                out.println(line);
            }

            out.flush();
            out.close();
            out = null;
            bufferedReader.close();
            bufferedReader = null;

            boolean success = inFile.delete();
            if (success) {
                success = outFile.renameTo(inFile);
                if ( ! success ) {
                    Logger.writeException("Failed to rename temp file: [" + tempFilePath + "] into [" + filePath + "]");
                    resultCode = FILE_OP_Failed;
                }
            }
            else {
                Logger.writeException("Failed to delete original file: [" + filePath + "]");
                resultCode = FILE_OP_Failed;
            }
        }
        catch (java.io.FileNotFoundException e) {
            Logger.writeException(e.getMessage());
            e.printStackTrace();
        }
        catch (java.io.IOException e) {
            Logger.writeException(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (out != null) {
                out.close();
            }

            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                }
                catch (IOException e) {
                    Logger.writeException(e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        Logger.writeLog("End inserting copyright block in file: " + filePath);

        return resultCode;
    }

    private String generateTempFilePath(String originalFilePath) {
        int pos = originalFilePath.lastIndexOf(File.separatorChar);
        return originalFilePath.substring(0, pos + 1) + TEMP_FILE_NAME;
    }
}

/*
 *  +===========================================================================+
 *  |      Copyright (c) 2016 Oracle Corporation, Redwood Shores, CA, USA       |
 *  |                         All rights reserved.                              |
 *  +===========================================================================+
 */
package oracle.apmaas.util.fileChecker;

import java.io.*;
import java.util.List;

/**
 * Created by yiyitan on 4/4/2016.
 */
public class CopyrightEditor {

    private static String COPYRIGHT_STANDARD_BLOCK_Java = null;
    private static String COPYRIGHT_STANDARD_BLOCK_Properties = null;

    private static final int FILE_READ_BUFFER_SIZE = 1024 * 1024;	// 1M
    private static final int FILE_WRITE_BUFFER_SIZE = 1024 * 1024;	// 1M
    private static final int MAX_FILE_SIZE = 32 * 1024 * 1024; // 32M

    private static final int FILE_IO_BLOCK_SIZE = 4 * 1024 * 1024; // 4M

    public CopyrightEditor() {
        int curYear = Main.getCurYear();

        COPYRIGHT_STANDARD_BLOCK_Java =
                "/*\n" +
                        " *  +===========================================================================+\n" +
                        " *  |      Copyright (c) "+ curYear +" Oracle Corporation, Redwood Shores, CA, USA       |\n" +
                        " *  |                         All rights reserved.                              |\n" +
                        " *  +===========================================================================+\n" +
                        " */\n";

        COPYRIGHT_STANDARD_BLOCK_Properties =
                "#  +===========================================================================+\n" +
                        "#  |      Copyright (c) " + curYear + " Oracle Corporation, Redwood Shores, CA, USA       |\n" +
                        "#  |                         All rights reserved.                              |\n" +
                        "#  +===========================================================================+\n";
    }

    public void editMissingFiles(List<String> missingFilePaths) {
        if ( (missingFilePaths == null) || (missingFilePaths.isEmpty()) ) {
            return;
        }

        try {
            for (String path : missingFilePaths) {
                if (path.endsWith(Main.FILE_EXT_NAME_Java))
                    addCopyright2MissingFiles(COPYRIGHT_STANDARD_BLOCK_Java, path);
                else if (path.endsWith(Main.FILE_EXT_NAME_PROPERTIES))
                    addCopyright2MissingFiles(COPYRIGHT_STANDARD_BLOCK_Properties, path);
            }
        }
        catch (Exception e) {
            Logger.writeException(e.getMessage());
            e.printStackTrace();
        }
    }

    private void addCopyright2MissingFiles(String copyrightBlock, String filePath)
            throws Exception {

        byte[] fileContent = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            Logger.writeLog("Begin to insert copyright block to file: " + filePath);

            File file = new File(filePath);

            long fileLength = file.length();
            if (fileLength <= MAX_FILE_SIZE) {
                if (fileLength > 0) {
                    fileContent = new byte[(int) fileLength];
                    bis = new BufferedInputStream(new FileInputStream(file), FILE_READ_BUFFER_SIZE);
                    int cBytesRead = bis.read(fileContent);
                    if (cBytesRead != fileLength) {
                        throw new IOException("Failed reading file: " + filePath + ", " +
                                fileLength + " bytes expected, but only " + cBytesRead + " bytes read.");
                    }

                    bis.close();
                    bis = null;
                }

                bos = new BufferedOutputStream(new FileOutputStream(file), FILE_WRITE_BUFFER_SIZE);
                bos.write(copyrightBlock.getBytes());
                if (fileContent != null) {
                    bos.write(fileContent);
                }
                bos.flush();
                bos.close();
                bos = null;
            }
            else {
                /*
    			insertHeadToFile_InPlace(copyrightBlock.getBytes(), file, FILE_IO_BLOCK_SIZE);
    			//*/
                throw new IOException("Too large file: " + filePath + "; file size is " +
                        fileLength + "; The maximum allowable file size is " + MAX_FILE_SIZE + ".");
            }

            Logger.writeLog("End inserting copyright block in file: " + filePath);
        }
        catch (Exception e) {
            throw new Exception("Exception in edition : [filePath=" + filePath + "]", e);
        }
        finally {
            fileContent = null; // release the memory occupied by fileContent
            if (bis != null) bis.close();
            if (bos != null) bos.close();
        }
    }

    private static void insertHeadToFile_InPlace(byte[] head, File file, int blockSize)
            throws IOException {

        RandomAccessFile raFile = null;
        byte[] buffer = null;
        try {
            int cBytesOfHead = head.length;
            long fileLength = file.length();
            raFile = new RandomAccessFile(file, "rw");

            buffer = new byte[blockSize];
            // fileReadPointer read from the last line
            long fileReadPointer = fileLength;
            int cBytesToRead, cBytesRead;
            while (true) {
                // move one block each time
                fileReadPointer -= blockSize;
                if (fileReadPointer >= 0) {
                    cBytesToRead = blockSize;
                }
                // the rest head part's size is smaller than blockSize
                else {
                    cBytesToRead = (int) (blockSize + fileReadPointer);
                    fileReadPointer = 0;
                }

                //find the start point to read file, read to buffer
                raFile.seek(fileReadPointer);
                cBytesRead = raFile.read(buffer, 0, cBytesToRead);
                if (cBytesRead < cBytesToRead) {
                    throw new IOException("Unexpected EOF while reading file: " + file.getCanonicalPath());
                }

                // pointer move one copyRightbBlock
                raFile.seek(fileReadPointer + cBytesOfHead);
                // write buffer
                raFile.write(buffer, 0, cBytesRead);

                // finally write copyRightBlock to the head of file
                if (fileReadPointer <= 0) {
                    raFile.seek(0);
                    raFile.write(head);
                    break;
                }
            }
        } finally {
            buffer = null;
            if (raFile != null) raFile.close();
        }
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.patch.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.forgerock.patch.Archive;
import org.forgerock.patch.exception.PatchException;

/**
 * Basic functionality to support patching logic, 
 * including static replacement of artifacts,
 * support for replacement or changing through logic
 * 
 * @author aegloff
 */
public class PatchUtil {

    private ZipUtil zipUtil = new ZipUtil();
    private FileUtil fileUtil = new FileUtil();
    
    private Archive archive = Archive.getInstance();

    private final static Logger logger = Logger.getLogger("PatchLog");
    
    /**
     * Access to the raw zip utility functionality
     * @return the zip utility
     */
    public ZipUtil getZipUtil() {
    	return zipUtil;
    }

    /**
     * Access to the raw file utility functionality
     * @return the file utility
     */
    public FileUtil getFileUtil() {
    	return fileUtil;
    }
    
    /**
     * Replaces installation artifacts with artifacts in the patch, 
     * under the "files" directory. Target files use the same path
     * as the name that occurs in the patch, 
     * relative to that "files" directory
     * 
     * @param patchUrl The patch file url
     * @param installDir the installation directory to patch
     * @throws IOException if accessing the patch file fails
     */
    public void replaceStaticFiles(URL patchUrl, File installDir) throws IOException {
        zipUtil.extractFolder(patchUrl, Archive.STATIC_FILES_ENTRY, installDir);
    }

    /**
     * Replaces multiple files matching corresponding name patterns with the
     * specified files.
     */
    public void replaceFilesWithPattern(URL patchUrl, File targetDir, String files[][], 
            boolean fileMustExist) throws IOException {
        if (fileMustExist) {
            for (int i = 0; i< files.length; i++) {
                replaceFileWithPattern(patchUrl, new File(targetDir, files[i][0]), files[i][1], files[i][2], true);
            }
        }
    }
    
    /**
     * Replace a file matching a given name pattern with the specified file.
     * For example replace a file with a given version in the name
     * with a file of a different version.
     */
    public void replaceFileWithPattern(URL patchUrl, File fileDir, String fileNameRegex, String patchFilePath,
            boolean fileMustExist) throws IOException {
    	
        File fileToReplace = fileUtil.find(fileDir, fileNameRegex);
        if (fileToReplace == null) {
            if (fileMustExist) {
                // TODO: better exception
                throw new RuntimeException("Precondition failed: file to replace in " 
                        + fileDir + " with pattern " + fileNameRegex + " does not exist");
            }
        } else {
            // TODO: move instead; should we also check the patch file exists before moving/removing?
            archive.insert(fileToReplace);
            fileUtil.delete(fileToReplace, false);
        }
        File fileToExtract = new File(patchFilePath);
        
        // Assume same directory as the file to replace, but with the name of the artifact in the patch
        File targetFile = new File(fileDir, fileToExtract.getName());
        zipUtil.extractFile(patchUrl, fileToExtract, targetFile);
    }
    
    /**
     * Determine if a process matching the given name pattern is running.
     * @param processNameRegex
     * @return true or false depending on if the process is running.
     * @throws PatchException if the underlying OS mechanism for obtaining the
     * running processes fails.
     */
    public boolean isProcessRunning(String processNameRegex) throws PatchException {
        String os = System.getProperty("os.name").toLowerCase();
        String cmd = "/bin/ps -e";
        boolean match = false;
        
        // Determine if we're on Windows
        if (os.indexOf("win") >= 0) {
            //TODO: Figure out how to do this on Windows
            //cmd = System.getenv("windir") +"\\system32\\"+"tasklist.exe";
            return false;
        }
        
        try {
            String line;
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader input =
                    new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = input.readLine()) != null) {
                match = line.matches(processNameRegex);
                logger.log(Level.FINEST, "Comparing process {0} : {1}", new Object[]{line, processNameRegex});
                if (match) break;
            }
            input.close();
        } catch (Exception ex) {
            throw new PatchException("Unable to determine if process matching " + processNameRegex + " is running.", ex);
        }
        return match;
    }
    
    /**
     * Compare two string representation of dot notation version numbers
     * @param v1 First version number to be compared
     * @param v2 Second version number to be compared
     * @return 0 if the versions are equal
     * @return -1 if v1 is less than v2
     * @return 1 if v1 is greater than v2
     */
    public int comapreVersionNumbers(String v1, String v2) {
        String t1[] = v1.split("\\.");
        String t2[] = v2.split("\\.");
        
        if (v1.equalsIgnoreCase(v2)) {
            return 0;
        }
        
        for (int i = 0; i < t1.length; i++) {
            if ((i >= t2.length) || (new Integer(t2[i]) < new Integer(t1[i]))) {
                return 1;
            } else if (new Integer(t2[i]) > new Integer(t1[i])) {
                return -1;
            }
        }
        return 0;   
    }
    
    /**
     * Test if a process is listening on the specified host/port
     * @param host Host name or IP address of the host
     * @param port Port to be checked
     * @return true if a process is accepting connections on the specified host/port, false otherwise.
     */
    public boolean isSocketListening(String host, int port) throws IOException {
        Socket s = null;  
        try {  
            s = new Socket(host, port);  
        } catch (ConnectException ignore) {}  
        return (s == null)?false:true;
    }
}

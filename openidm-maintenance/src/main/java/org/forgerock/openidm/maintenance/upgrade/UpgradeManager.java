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

package org.forgerock.openidm.maintenance.upgrade;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;

import org.forgerock.openidm.maintenance.impl.InvalidArgsException;
import org.forgerock.openidm.maintenance.upgrade.UpgradeException;

/**
 * Basic manager to initiate the product maintenance and upgrade mechanisms.
 *
 * The majority of patching logic is delegated to the patch package itself.
 */
public class UpgradeManager {
    
    public void execute(String urlString, File workingDir, File installDir,  Map<String, String> params)
            throws InvalidArgsException, UpgradeException {
        
        final URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            throw new InvalidArgsException("Passed in url is invalid " + ex.getMessage(), ex);
        }
        
        // Download the patch file
        final ReadableByteChannel channel;
        try {
            channel = Channels.newChannel(url.openStream());
        } catch (IOException ex) {
            throw new UpgradeException("Failed to access the specified file "  + url + " " + ex.getMessage(), ex);
        }
        
        final String targetFileName = new File(url.getPath()).getName();
        final File patchDir = new File(workingDir, "patch/bin");
        patchDir.mkdirs();
        final File targetFile = new File(patchDir, targetFileName);
        final FileOutputStream fos;
        try {
            fos = new FileOutputStream(targetFile);
        } catch (FileNotFoundException ex) {
            throw new UpgradeException("Error in getting the specified file to " 
                    + targetFile, ex);
        }

        try {
            fos.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
            System.out.println("Downloaded to " + targetFile);
        } catch (IOException ex) {
            throw new UpgradeException("Failed to get the specified file " 
                    + url + " to: " + targetFile, ex);
        }

        try {
            URL patchUrl = targetFile.toURI().toURL();
            URL[] urls = new URL[] { patchUrl };
            URLClassLoader loader = new URLClassLoader(urls);
            Class c = loader.loadClass("org.forgerock.patch.Main");
            
            Method method = c.getMethod("execute", URL.class, String.class, File.class, File.class, Map.class);
            method.invoke(null, patchUrl, urlString, workingDir, installDir, params);
        } catch (MalformedURLException ex) {
            throw new UpgradeException("Internal issue in loading jar " + ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            throw new UpgradeException("Patch file seems incorrect, failed to find Class", ex);
        } catch (NoSuchMethodException ex) {
            throw new UpgradeException("Patch file seems incorrect, failed to find Method", ex);
        } catch (IllegalAccessException ex) {
            throw new UpgradeException("Failure in loading Patch file " + ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            throw new UpgradeException("Failure during the maintenance/patch execution " + ex.getMessage(), ex);
        }
    }
    
}

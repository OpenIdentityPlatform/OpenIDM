/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openidm.shell.impl;

import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.shell.CustomCommandScope;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class LocalCommandScope implements CustomCommandScope {

    private static String DOTTED_PLACEHOLDER = "............................................";

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getFunctionMap() {
        Map<String, String> help = new HashMap<String, String>();
        help.put("validate", "Validates all json configuration file in /conf folder.");
        return help;

    }

    /**
     * {@inheritDoc}
     */
    public String getScope() {
        return "local";
    }

    /**
     * {@inheritDoc}
     */
    public void execute(String[] args) {
    }

    public void validate(InputStream console, PrintStream out, String[] args) {
        File file = IdentityServer.getFileForPath("conf");
        out.println("...................................................................");
        if (file.isDirectory()) {
            out.println("[Validating] Load JSON configuration files from:");
            out.append("[Validating] \t").println(file.getAbsolutePath());
            FileFilter filter = new FileFilter() {
                public boolean accept(File f) {
                    return (f.isDirectory()) || (f.getName().endsWith(".json"));
                }
            };
            ObjectMapper mapper = new ObjectMapper();
            File[] files = file.listFiles(filter);
            for (File subFile : files) {
                if (subFile.isDirectory()) continue;
                //TODO pretty print
                try {
                    mapper.readValue(subFile, Object.class);
                    prettyPrint(out, subFile.getName(), null);
                } catch (Exception e) {
                    prettyPrint(out, subFile.getName(), e);
                }
            }
        } else {
            out.append("[Validating] ").append("Configuration directory not found at: ").println(file.getAbsolutePath());
        }
    }

    private void prettyPrint(PrintStream out, String name, Exception reason) {
        out.append("[Validating] ").append(name).append(" ").append(DOTTED_PLACEHOLDER.substring(Math.min(name.length(), DOTTED_PLACEHOLDER.length())));
        if (null == reason) {
            out.println(" SUCCESS");
        } else {
            out.println(" FAILED");
            out.append("\t[").append(reason.getMessage()).println("]");
        }
    }
}

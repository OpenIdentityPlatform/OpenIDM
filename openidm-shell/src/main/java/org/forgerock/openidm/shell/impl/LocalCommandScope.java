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

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.shell.CustomCommandScope;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.KeyStore;
import java.util.*;

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
        help.put("keytool", "Export or import SecretKeyEntry. Keytool does not allow to export or import SecretKeyEntries.");
        return help;
    }

    /**
     * {@inheritDoc}
     */
    public String getScope() {
        return "local";
    }

    @Descriptor("")
    public void keytool(CommandSession session,
                        @Parameter(names = {"-i", "--import"}, presentValue = "true", absentValue = "false") boolean doImport,
                        @Parameter(names = {"-e", "--export"}, presentValue = "true", absentValue = "false") boolean doExport,
                        @Descriptor("Key alias") String alias) {
        if (doImport ^ doExport) {
            String type = IdentityServer.getInstance().getProperty("openidm.keystore.type", KeyStore.getDefaultType());
            String provider = IdentityServer.getInstance().getProperty("openidm.keystore.provider");
            String location = IdentityServer.getInstance().getProperty("openidm.keystore.location");

            try {
                KeyStore ks = (provider == null || provider.trim().length() == 0 ? KeyStore.getInstance(type) : KeyStore.getInstance(type, provider));
                if (location != null) {
                    File configFile = IdentityServer.getFileForPath(location);
                    if (configFile.exists()) {
                        FileInputStream in = null;
                        try {
                            in = new FileInputStream(configFile);
                            if (null != in) {
                                session.getConsole().append("Use KeyStore from: ").println(configFile.getAbsolutePath());
                                //TODO Don't use the System in OSGi.
                                char[] passwordArray = System.console().readPassword("Please enter the password: ");
                                char[] passwordCopy = Arrays.copyOf(passwordArray, passwordArray.length);
                                Arrays.fill(passwordArray, ' ');
                                ks.load(in, passwordCopy);
                                if (null != in) {
                                    in.close();
                                    in = null;
                                }
                                if (doExport) {
                                    KeyStore.Entry key = ks.getEntry(alias, new KeyStore.PasswordProtection(passwordCopy));
                                    if (key instanceof KeyStore.SecretKeyEntry) {
                                        KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) key;
                                        session.getConsole().append("[OK] ").println(secretKeyEntry);
                                        StringBuilder sb = new StringBuilder(secretKeyEntry.getSecretKey().getAlgorithm());
                                        sb.append(":").append(new BigInteger(1, secretKeyEntry.getSecretKey().getEncoded()).toString(16));
                                        session.getConsole().println(sb);
                                    } else {
                                        session.getConsole().println("SecretKeyEntry with this alias is not in KeyStore");
                                    }
                                } else if (doImport) {
                                    if (ks.containsAlias(alias)) {
                                        session.getConsole().println("KeyStore contains a key with this alias");
                                    } else {
                                        session.getConsole().println("Enter the key: ");
                                        Scanner scanner = new Scanner(session.getKeyboard());
                                        String[] tokens = scanner.nextLine().split(":");
                                        if (tokens.length == 2) {
                                            byte[] encoded = new BigInteger(tokens[1], 16).toByteArray();
                                            javax.crypto.SecretKey mySecretKey = new SecretKeySpec(encoded, tokens[0]);
                                            KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(mySecretKey);
                                            ks.setEntry(alias, skEntry, new KeyStore.PasswordProtection(passwordCopy));
                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(configFile);
                                                ks.store(fos, passwordCopy);
                                            } finally {
                                                if (fos != null) {
                                                    fos.close();
                                                }
                                            }
                                        } else {
                                            session.getConsole().println("Invalid key input");
                                        }
                                    }
                                }
                            }
                        } catch (IOException ioe) {
                            if (null != in) {
                                in.close();
                            }
                        }
                    } else {
                        session.getConsole().append("KeyStore file: ").append(configFile.getAbsolutePath()).println(" does not exists.");
                    }
                }
            } catch (Exception e) {
                session.getConsole().println(e.getMessage());
            }
        } else {
            session.getConsole().println("Import or Export have to be exclusively defined.");
        }
    }

    public void validate(CommandSession session) {
        File file = IdentityServer.getFileForPath("conf");
        session.getConsole().println("...................................................................");
        if (file.isDirectory()) {
            session.getConsole().println("[Validating] Load JSON configuration files from:");
            session.getConsole().append("[Validating] \t").println(file.getAbsolutePath());
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
                    prettyPrint(session.getConsole(), subFile.getName(), null);
                } catch (Exception e) {
                    prettyPrint(session.getConsole(), subFile.getName(), e);
                }
            }
        } else {
            session.getConsole().append("[Validating] ").append("Configuration directory not found at: ").println(file.getAbsolutePath());
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

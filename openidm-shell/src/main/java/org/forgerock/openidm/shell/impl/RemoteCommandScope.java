/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock Inc. All rights reserved.
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

package org.forgerock.openidm.shell.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class RemoteCommandScope extends AbstractRemoteCommandScope {

    private static String DOTTED_PLACEHOLDER = "............................................";

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getFunctionMap() {
        Map<String, String> help = new HashMap<String, String>();
        help.put("configimport", "imports the configuration set from local file/directory");
        help.put("configexport", "exports the entire configuration set");
        help.put("configureconnector", "Generate connector configuration");
        return help;
    }

    /**
     * {@inheritDoc}
     */
    public String getScope() {
        return "remote";
    }

    @Descriptor("imports the configuration set from local 'conf' directory")
    public void configimport(
            CommandSession session,
            @Descriptor("Replace the entire config set by deleting the additional configuration") @Parameter(
                    names = { "-r", "--replaceall" }, presentValue = "true", absentValue = "false") boolean replaceall) {
        configimport(session, replaceall, "conf");
    }

    @Descriptor("imports the configuration set from local file/directory")
    public void configimport(
            CommandSession session,
            @Descriptor("Replace the entire config set by deleting the additional configuration") @Parameter(
                    names = { "-r", "--replaceall" }, presentValue = "true", absentValue = "false") boolean replaceall,
            @Descriptor("source directory") String source) {
        File file = IdentityServer.getFileForPath(source);
        session.getConsole().println(
                "...................................................................");
        if (file.isDirectory()) {
            session.getConsole().println("[ConfigImport] Load JSON configuration files from:");
            session.getConsole().append("[ConfigImport] \t").println(file.getAbsolutePath());

            FileFilter filter = new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().endsWith(".json");
                }
            };

            File[] files = file.listFiles(filter);
            Map<String, File> localConfigSet = new HashMap<String, File>(files.length);
            for (File subFile : files) {
                if (subFile.isDirectory())
                    continue;
                String configName = subFile.getName().replaceFirst("-", "/");
                configName =
                        ConfigBootstrapHelper.unqualifyPid(configName.substring(0, configName
                                .length() - 5));
                if (configName.indexOf("-") > -1) {
                    session.getConsole()
                            .append("[WARN] ")
                            .append("Invalid file name found with multiple '-' character. The normalized config id: ")
                            .println(configName);
                }
                localConfigSet.put(configName, subFile);
            }

            JsonResourceAccessor accessor = new JsonResourceAccessor(getRouter(), null);

            Map<String, JsonValue> remoteConfigSet = new HashMap<String, JsonValue>();
            try {
                JsonValue responseValue = accessor.read("config");
                Iterator<JsonValue> iterator = responseValue.get("configurations").iterator();
                while (iterator.hasNext()) {

                    JsonValue configValue = iterator.next();
                    // TODO catch JsonValueExceptions
                    String id =
                            ConfigBootstrapHelper.unqualifyPid(configValue.get("_id").required()
                                    .asString());
                    if (!id.startsWith("org.apache")) {
                        remoteConfigSet.put(id, configValue);
                    }
                }
            } catch (JsonResourceException e) {
                session.getConsole().append("Remote operation failed: ").println(e.getMessage());
                return;
            } catch (Exception e) {
                session.getConsole().append("Operation failed: ").println(e.getMessage());
                return;
            }

            for (Map.Entry<String, File> entry : localConfigSet.entrySet()) {
                try {
                    if (remoteConfigSet.containsKey(entry.getKey())) {
                        // Update
                        accessor.update("config/" + entry.getKey(), "\"FIX\"", new JsonValue(
                                getMapper().readValue(entry.getValue(), Map.class)));
                        // Do not remove the remote old config if the update
                        // seceded otherwise remove the old config.
                        remoteConfigSet.remove(entry.getKey());
                    } else {
                        // Create
                        accessor.create("config/" + entry.getKey(), new JsonValue(getMapper()
                                .readValue(entry.getValue(), Map.class)));
                    }
                    prettyPrint(session.getConsole(), "ConfigImport", entry.getKey(), null);
                } catch (Exception e) {
                    prettyPrint(session.getConsole(), "ConfigImport", entry.getKey(), e
                            .getMessage());
                }
            }

            // Delete all additional config objects
            if (replaceall) {
                for (String configId : remoteConfigSet.keySet()) {
                    if ("authentication".equals(configId) || "router".equals(configId)
                            || "audit".equals(configId) || configId.startsWith("repo")) {
                        prettyPrint(session.getConsole(), "ConfigDelete", configId,
                                "Protected configuration can not be deleted");
                        continue;
                    }

                    try {
                        accessor.delete("config/" + configId, "\"FIX\"");
                        prettyPrint(session.getConsole(), "ConfigDelete", configId, null);
                    } catch (Exception e) {
                        prettyPrint(session.getConsole(), "ConfigDelete", configId, e.getMessage());
                    }
                }
            }

        } else if (file.exists()) {
            // TODO import archive file
            session.getConsole().println("Input path must be a directory not a file.");
        } else {
            session.getConsole().append("[ConfigImport] ").append(
                    "Configuration directory not found at: ").println(file.getAbsolutePath());
        }
    }

    @Descriptor("exports all configurations to 'conf' folder")
    public void configexport(CommandSession session) {
        configexport(session, "conf");
    }

    @Descriptor("exports all configurations")
    public void configexport(CommandSession session, @Descriptor("target directory") String target) {
        File targetDir = IdentityServer.getFileForPath(target);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        session.getConsole().println("[ConfigExport] Export JSON configurations to:");
        session.getConsole().append("[ConfigExport] \t").println(targetDir.getAbsolutePath());

        JsonResourceAccessor accessor = new JsonResourceAccessor(getRouter(), null);

        try {
            JsonValue responseValue = accessor.read("config");
            Iterator<JsonValue> iterator = responseValue.get("configurations").iterator();
            URI configSet = new URI("config/");
            String bkpPostfix =
                    "." + (new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss")).format(new Date())
                            + ".bkp";
            while (iterator.hasNext()) {
                String id = iterator.next().get("_id").required().asString();
                if (!id.startsWith("org.apache")) {
                    try {
                        responseValue = accessor.read(configSet.resolve(id).toString());
                        if (null != responseValue && !responseValue.isNull()) {
                            File configFile = new File(targetDir, id.replace("/", "-") + ".json");
                            if (configFile.exists()) {
                                configFile.renameTo(new File(configFile.getParentFile(), configFile
                                        .getName()
                                        + bkpPostfix));
                            }
                            getMapper().writerWithDefaultPrettyPrinter().writeValue(configFile,
                                    responseValue.getObject());
                            prettyPrint(session.getConsole(), "ConfigExport", id, null);
                        }
                    } catch (Exception e) {
                        prettyPrint(session.getConsole(), "ConfigExport", id, e.getMessage());
                    }
                }
            }
        } catch (JsonResourceException e) {
            session.getConsole().append("Remote operation failed: ").println(e.getMessage());
        } catch (Exception e) {
            session.getConsole().append("Operation failed: ").println(e.getMessage());
        }
    }

    public void export(InputStream console, PrintStream out, String[] args) {
        out.println("Exported");
    }

    @Descriptor("Generate connector configuration.")
    public void configureconnector(CommandSession session,
            @Descriptor("Name of the new connector configuration.") String name) {
        try {
            // Prepare temp folder and file
            File temp = IdentityServer.getFileForPath("temp");
            if (!temp.exists()) {
                temp.mkdir();
            }
            // TODO Make safe file name

            if (!name.matches("\\w++")) {
                session.getConsole().append("The given name \"").append(name).println(
                        "\" must match [a-zA-Z_0-9] pattern");
                return;
            }
            File finalConfig = new File(temp, "provisioner.openicf-" + name + ".json");

            // Common request attributes
            JsonValue params = new JsonValue(new HashMap());
            params.put(ServerConstants.ACTION_NAME, "CREATECONFIGURATION");

            JsonResourceAccessor accessor = new JsonResourceAccessor(getRouter(), null);

            JsonValue responseValue;
            Map<String, Object> configuration = null;

            // Phase#1 - Get available connectors
            if (!finalConfig.exists()) {
                responseValue = accessor.action("system", params, null);

                JsonValue connectorRef = responseValue.get("connectorRef");
                if (!connectorRef.isNull() && connectorRef.isList()) {
                    List<Object> connectorRefs = connectorRef.asList();
                    if (connectorRefs.size() > 0) {
                        for (int i = 0; i < connectorRefs.size(); i++) {
                            Map<String, String> connectorKey =
                                    (Map<String, String>) connectorRefs.get(i);
                            String displayName = connectorKey.get("displayName");
                            if (null == displayName) {
                                displayName = connectorKey.get("connectorName");
                            }
                            String version = connectorKey.get("bundleVersion");
                            String connectorHostRef = connectorKey.get("connectorHostRef");

                            session.getConsole().append(Integer.toString(i)).append(". ").append(
                                    displayName);
                            if (null != connectorHostRef) {
                                session.getConsole().append(" Remote (").append(connectorHostRef)
                                        .append(")");
                            }
                            session.getConsole().append(" version ").println(version);
                        }

                        session.getConsole().append(Integer.toString(connectorRef.size())).println(
                                ". Exit");
                        Scanner input = new Scanner(session.getKeyboard());
                        int index = -1;
                        do {
                            session.getConsole().append("Select [0..").append(
                                    Integer.toString(connectorRef.size())).append("]: ");
                            index = input.nextInt();
                        } while (index < 0 || index > connectorRefs.size());
                        if (index == connectorRefs.size()) {
                            return;
                        }
                        configuration = (Map<String, Object>) responseValue.getObject();
                        configuration.put("connectorRef", connectorRefs.get(index));
                    }
                } else {
                    session.getConsole().println("There is no available connector!");
                }
            } else {
                session.getConsole().append("Configuration was found and picked up from: ")
                        .println(finalConfig.getAbsolutePath());
                configuration = getMapper().readValue(finalConfig, Map.class);
            }

            if (null == configuration) {
                return;
            }

            // Repeatable phase #2 and #3
            responseValue = accessor.action("system", params, new JsonValue(configuration));
            responseValue.put("name", name);
            getMapper().writerWithDefaultPrettyPrinter().writeValue(finalConfig,
                    responseValue.getObject());
            session.getConsole()
                    .append("Edit the configuration file and run the command again. The configuration was saved to ")
                    .println(finalConfig.getAbsolutePath());

        } catch (JsonResourceException e) {
            session.getConsole().append("Remote operation failed: ").println(e.getMessage());
        } catch (Exception e) {
            session.getConsole().append("Operation failed: ").println(e.getMessage());
        }
    }

    private void prettyPrint(PrintStream out, String cmd, String name, String reason) {
        out.append("[").append(cmd).append("] ").append(name).append(" ").append(
                DOTTED_PLACEHOLDER.substring(Math.min(name.length(), DOTTED_PLACEHOLDER.length())));
        if (null == reason) {
            out.println(" SUCCESS");
        } else {
            out.println(" FAILED");
            out.append("\t[").append(reason).println("]");
        }
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.core.IdentityServer;

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
        help.put("configimport", getLongHeader("configimport"));
        help.put("configexport", getLongHeader("configexport"));
        help.put("configureconnector", getLongHeader("configureconnector"));
        return help;
    }

    /**
     * {@inheritDoc}
     */
    public String getScope() {
        return "remote";
    }

    @Descriptor("Imports the configuration set from local 'conf' directory")
    public void configimport(
            CommandSession session,
            @Descriptor("Replace the entire config set by deleting the additional configuration") @Parameter(
                    names = { "-r", "--replaceall", "--replaceAll" }, presentValue = "true",
                    absentValue = "false") boolean replaceall) {
        configimport(session, replaceall, "conf");
    }

    @Descriptor("Imports the configuration set from local file/directory")
    public void configimport(CommandSession session,
            @Descriptor("Replace the entire config set by deleting the additional configuration") 
            @Parameter(names = { "-r", "--replaceall", "--replaceAll" }, presentValue = "true", 
            absentValue = "false") boolean replaceall,
            @Descriptor("source directory") String source) {
        PrintStream console = session.getConsole();
        File file = IdentityServer.getFileForPath(source);
        console.println("...................................................................");
        if (file.isDirectory()) {
            console.println("[ConfigImport] Load JSON configuration files from:");
            console.append("[ConfigImport] \t").println(file.getAbsolutePath());

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
                configName = ConfigBootstrapHelper.unqualifyPid(configName.substring(0, configName.length() - 5));
                if (configName.indexOf("-") > -1) {
                    console.append("[WARN] Invalid file name found with multiple '-' character. The normalized config id: ");
                    console.println(configName);
                }
                localConfigSet.put(configName, subFile);
            }

            final Connection accessor = getRouter();

            Map<String, JsonValue> remoteConfigSet = new HashMap<String, JsonValue>();
            try {
                Resource responseValue = accessor.read(null, Requests.newReadRequest("config"));
                Iterator<JsonValue> iterator = responseValue.getContent().get("configurations").iterator();
                while (iterator.hasNext()) {
                    JsonValue configValue = iterator.next();
                    // TODO catch JsonValueExceptions
                    String id = ConfigBootstrapHelper.unqualifyPid(configValue.get("_id").required().asString());
                    // Remove apache configurations
                    if (!id.startsWith("org.apache")) {
                        remoteConfigSet.put(id, configValue);
                    }
                }
            } catch (ResourceException e) {
                console.append("Remote operation failed: ").println(e.getMessage());
                return;
            } catch (Exception e) {
                console.append("Operation failed: ").println(e.getMessage());
                return;
            }

            for (Map.Entry<String, File> entry : localConfigSet.entrySet()) {
                try {
                    if (remoteConfigSet.containsKey(entry.getKey())) {
                        // Update
                        UpdateRequest updateRequest = Requests.newUpdateRequest("config", entry.getKey(), new JsonValue(
                                getMapper().readValue(entry.getValue(), Map.class)));

                        accessor.update(null, updateRequest);
                        // Do not remove the remote old config if the update
                        // seceded otherwise remove the old config.
                        remoteConfigSet.remove(entry.getKey());
                    } else {
                        // Create
                        CreateRequest createRequest = Requests.newCreateRequest("config", entry.getKey(), new JsonValue(
                                getMapper().readValue(entry.getValue(), Map.class)));
                        Resource createdResource = accessor.create(null, createRequest);
                    }
                    prettyPrint(console, "ConfigImport", entry.getKey(), null);
                } catch (Exception e) {
                    prettyPrint(console, "ConfigImport", entry.getKey(), e.getMessage());
                }
            }

            // Delete all additional config objects
            if (replaceall) {
                for (String configId : remoteConfigSet.keySet()) {
                    if ("authentication".equals(configId) || "router".equals(configId)
                            || "audit".equals(configId) || configId.startsWith("repo")) {
                        prettyPrint(console, "ConfigDelete", configId, "Protected configuration can not be deleted");
                        continue;
                    }

                    try {
                        accessor.delete(null, Requests.newDeleteRequest("config", configId));
                        prettyPrint(console, "ConfigDelete", configId, null);
                    } catch (Exception e) {
                        prettyPrint(console, "ConfigDelete", configId, e.getMessage());
                    }
                }
            }

        } else if (file.exists()) {
            // TODO import archive file
            console.println("Input path must be a directory not a file.");
        } else {
            console.append("[ConfigImport] Configuration directory not found at: ");
            console.println(file.getAbsolutePath());
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

        final Connection accessor = getRouter();

        try {
            Resource responseValue = accessor.read(null, Requests.newReadRequest("config"));
            Iterator<JsonValue> iterator = responseValue.getContent().get("configurations").iterator();
            String bkpPostfix = "." + (new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss")).format(new Date()) + ".bkp";
            while (iterator.hasNext()) {
                String id = iterator.next().get("_id").required().asString();
                if (!id.startsWith("org.apache")) {
                    try {
                        responseValue = accessor.read(null, Requests.newReadRequest("config/" + id));
                        if (null != responseValue.getContent()
                                && !responseValue.getContent().isNull()) {
                            File configFile = new File(targetDir, id.replace("/", "-") + ".json");
                            if (configFile.exists()) {
                                configFile.renameTo(new File(configFile.getParentFile(), configFile.getName() + bkpPostfix));
                            }
                            getMapper().writerWithDefaultPrettyPrinter().writeValue(configFile,
                                    responseValue.getContent().getObject());
                            prettyPrint(session.getConsole(), "ConfigExport", id, null);
                        }
                    } catch (Exception e) {
                        prettyPrint(session.getConsole(), "ConfigExport", id, e.getMessage());
                    }
                }
            }
        } catch (ResourceException e) {
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
            ActionRequest request = Requests.newActionRequest("system", "CREATECONFIGURATION");
            request.setAdditionalParameter(ActionRequest.FIELD_ACTION, "CREATECONFIGURATION");
            request.setAdditionalParameter("_action", "CREATECONFIGURATION");

            final Connection accessor = getRouter();

            JsonValue responseValue;
            Map<String, Object> configuration = null;

            // Phase#1 - Get available connectors
            if (!finalConfig.exists()) {
                responseValue = accessor.action(null, request);

                JsonValue connectorRef = responseValue.get("connectorRef");
                if (!connectorRef.isNull() && connectorRef.isList()) {
                    List<Object> connectorRefs = connectorRef.asList();
                    if (connectorRefs.size() > 0) {
                        for (int i = 0; i < connectorRefs.size(); i++) {
                            Map<String, String> connectorKey = (Map<String, String>) connectorRefs.get(i);
                            String displayName = connectorKey.get("displayName");
                            if (null == displayName) {
                                displayName = connectorKey.get("connectorName");
                            }
                            String version = connectorKey.get("bundleVersion");
                            String connectorHostRef = connectorKey.get("connectorHostRef");

                            session.getConsole().append(Integer.toString(i)).append(". ").append(displayName);
                            if (null != connectorHostRef) {
                                session.getConsole().append(" Remote (").append(connectorHostRef).append(")");
                            }
                            session.getConsole().append(" version ").println(version);
                        }

                        session.getConsole().append(Integer.toString(connectorRef.size())).println(". Exit");
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
                session.getConsole().append("Configuration was found and picked up from: ").println(finalConfig.getAbsolutePath());
                configuration = getMapper().readValue(finalConfig, Map.class);
            }

            if (null == configuration) {
                return;
            }

            // Repeatable phase #2 and #3
            request.setContent(new JsonValue(configuration));

            responseValue = accessor.action(null, request);
            ((Map<String, Object>)responseValue.getObject()).put("name", name);
            getMapper().writerWithDefaultPrettyPrinter().writeValue(finalConfig, responseValue.getObject());
            session.getConsole().append("Edit the configuration file and run the command again. The configuration was saved to ")
                    .println(finalConfig.getAbsolutePath());

        } catch (ResourceException e) {
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

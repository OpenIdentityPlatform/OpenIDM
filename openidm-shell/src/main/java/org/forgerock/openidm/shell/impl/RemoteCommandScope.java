/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.persistence.ConfigBootstrapHelper;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.shell.CustomCommandScope;
import org.forgerock.openidm.shell.felixgogo.MetaVar;
import org.forgerock.services.context.RootContext;

/**
 * Command scope for remote operations.
 */
public class RemoteCommandScope extends CustomCommandScope {

    private static final String DOTTED_PLACEHOLDER = "............................................";

    private static final String IDM_PORT_DEFAULT = "8080";
    private static final String IDM_PORT_DESC =
            "Port of OpenIDM REST service. This will override any port in --url. Default " + IDM_PORT_DEFAULT;
    private static final String IDM_PORT_METAVAR = "PORT";

    private static final String IDM_URL_DEFAULT = "http://localhost:8080/openidm/";
    private static final String IDM_URL_DESC = "URL of OpenIDM REST service. Default " + IDM_URL_DEFAULT;
    private static final String IDM_URL_METAVAR = "URL";

    private static final String USER_PASS_DESC = "Server user and password";
    private static final String USER_PASS_METAVAR = "USER[:PASSWORD]";
    private static final String USER_PASS_DEFAULT = "";

    private static final String REPLACE_ALL_DESC =
            "Replace the entire config set by deleting the additional configuration";

    private final HttpRemoteJsonResource resource = new HttpRemoteJsonResource();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getFunctionMap() {
        Map<String, String> help = new HashMap<String, String>();
        help.put("configimport", getLongHeader("configimport"));
        help.put("configexport", getLongHeader("configexport"));
        help.put("configureconnector", getLongHeader("configureconnector"));
        help.put("update", getLongHeader("update"));
        return help;
    }

    /**
     * {@inheritDoc}
     */
    public String getScope() {
        return "remote";
    }

    private void processOptions(final String userPass, final String idmUrl, final String idmPort) {
        String username = "";
        String password = "";

        if (StringUtils.isNotBlank(userPass)) {
            int passwordIdx = userPass.indexOf(":") + 1;

            if (passwordIdx > 0) {
                username = userPass.substring(0, passwordIdx - 1);
                password = userPass.substring(passwordIdx);
            } else {
                username = userPass;
                password = new String(System.console().readPassword("Password:"));
            }

            resource.setUsername(username);
            resource.setPassword(password);
        }

        if (StringUtils.isNotBlank(idmUrl)) {
            resource.setBaseUri(idmUrl.endsWith("/") ? idmUrl : idmUrl + "/");
        }

        if (StringUtils.isNotBlank(idmPort)) {
            if (NumberUtils.isDigits(idmPort)) {
                resource.setPort(Integer.decode(idmPort));
            } else {
                throw new IllegalArgumentException("Port must be a number");
            }
        }
    }

    /**
     * Handles the Update (upgrade/patch) process.
     * @param session session that invoked the command.
     * @param userPass user:passwd to be used on rest calls.
     * @param idmUrl url to the idm instance.
     * @param idmPort port that idm is running on.
     * @param acceptLicense if true, the accept license step is skipped.
     * @param archive The simple file name of the archive that is already in bin/update.
     */
    @Descriptor("Update the system with the provided update file.")
    public void update(CommandSession session,
            @Descriptor(USER_PASS_DESC)
            @MetaVar(USER_PASS_METAVAR)
            @Parameter(names = {"-u", "--user"}, absentValue = USER_PASS_DEFAULT)
            final String userPass,

            @Descriptor(IDM_URL_DESC)
            @MetaVar(IDM_URL_METAVAR)
            @Parameter(names = {"--url"}, absentValue = IDM_URL_DEFAULT)
            final String idmUrl,

            @Descriptor(IDM_PORT_DESC)
            @MetaVar(IDM_PORT_METAVAR)
            @Parameter(names = {"-P", "--port"}, absentValue = IDM_PORT_DEFAULT)
            final String idmPort,

            @Descriptor("Automatically accepts the product license (if present). " +
                    "Defaults to 'false' to ask for acceptance.")
            @Parameter(names = {"--acceptLicense"}, presentValue = "true", absentValue = "false")
            final boolean acceptLicense,

            @Descriptor("Timeout value to wait for jobs to finish. " +
                    "Defaults to -1 to exit immediately if jobs are running.")
            @MetaVar("TIME")
            @Parameter(names = {"--maxJobsFinishWaitTimeMs"}, absentValue = "-1")
            final long maxJobsFinishWaitTimeMs,

            @Descriptor("Timeout value to wait for update process to complete. Defaults to 30000 ms.")
            @MetaVar("TIME")
            @Parameter(names = {"--maxUpdateWaitTimeMs"}, absentValue = "30000")
            final long maxUpdateWaitTimeMs,

            @Descriptor("Log file path. (optional) Defaults to logs/update.log")
            @MetaVar("LOG_FILE")
            @Parameter(names = {"-l", "--log"}, absentValue = "logs/update.log")
            final String logFilePath,

            @Descriptor("Log only to the log file.")
            @Parameter(names = {"-Q", "--Quiet"}, presentValue = "true", absentValue = "false")
            final boolean quietMode,

            @Descriptor("Filename of the Update archive within bin/update.")
            String archive) {

        processOptions(userPass, idmUrl, idmPort);
        UpdateCommandConfig config = new UpdateCommandConfig()
                .setUpdateArchive(archive)
                .setLogFilePath(logFilePath)
                .setQuietMode(quietMode)
                .setAcceptedLicense(acceptLicense)
                .setMaxJobsFinishWaitTimeMs(maxJobsFinishWaitTimeMs)
                .setMaxUpdateWaitTimeMs(maxUpdateWaitTimeMs);

        new UpdateCommand(session, resource, config)
                .execute(new RootContext());
    }

    /**
     * Import the configuration set from a local "conf" directory.
     *
     * @param session the command session
     * @param userPass the username/password
     * @param idmUrl the url of the OpenIDM instance
     * @param idmPort the OpenIDM instance's port
     * @param replaceall whether or not to replace the config
     */
    @Descriptor("Imports the configuration set from local 'conf' directory.")
    public void configimport(
            final CommandSession session,

            @Descriptor(USER_PASS_DESC)
            @MetaVar(USER_PASS_METAVAR)
            @Parameter(names = { "-u", "--user" }, absentValue = USER_PASS_DEFAULT)
            final String userPass,

            @Descriptor(IDM_URL_DESC)
            @MetaVar(IDM_URL_METAVAR)
            @Parameter(names = { "--url" }, absentValue = IDM_URL_DEFAULT)
            final String idmUrl,

            @Descriptor(IDM_PORT_DESC)
            @MetaVar(IDM_PORT_METAVAR)
            @Parameter(names = { "-P", "--port" }, absentValue = IDM_PORT_DEFAULT)
            final String idmPort,

            @Descriptor(REPLACE_ALL_DESC)
            @Parameter(names = { "-r", "--replaceall", "--replaceAll" }, presentValue = "true", absentValue = "false")
            final boolean replaceall) {
        configimport(session, userPass, idmUrl, idmPort, replaceall, "conf");
    }

    /**
     * Imports the configuration set from a local file or directory.
     *
     * @param session the command session
     * @param userPass the username/password
     * @param idmUrl the url of the OpenIDM instance
     * @param idmPort the OpenIDM instance's port
     * @param replaceall whether or not to replace the config
     * @param source the source directory
     */
    @Descriptor("Imports the configuration set from local file/directory.")
    public void configimport(
            final CommandSession session,

            @Descriptor(USER_PASS_DESC)
            @MetaVar(USER_PASS_METAVAR)
            @Parameter(names = { "-u", "--user" }, absentValue = USER_PASS_DEFAULT)
            final String userPass,

            @Descriptor(IDM_URL_DESC)
            @MetaVar(IDM_URL_METAVAR)
            @Parameter(names = { "--url" }, absentValue = IDM_URL_DEFAULT)
            final String idmUrl,

            @Descriptor(IDM_PORT_DESC)
            @MetaVar(IDM_PORT_METAVAR)
            @Parameter(names = { "-P", "--port" }, absentValue = IDM_PORT_DEFAULT)
            final String idmPort,

            @Descriptor(REPLACE_ALL_DESC)
            @Parameter(names = { "-r", "--replaceall", "--replaceAll" }, presentValue = "true",  absentValue = "false")
            final boolean replaceall,

            @Descriptor("source directory")
            final String source) {
        processOptions(userPass, idmUrl, idmPort);

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

            // Read the files from the provided source directory.
            File[] files = file.listFiles(filter);
            Map<String, File> localConfigSet = new HashMap<String, File>(files.length);
            for (File subFile : files) {
                if (subFile.isDirectory()) {
                    continue;
                }
                String configName = subFile.getName().replaceFirst("-", "/");
                configName = ConfigBootstrapHelper.unqualifyPid(configName.substring(0, configName.length() - 5));
                if (configName.indexOf("-") > -1) {
                    console.append(
                            "[WARN] Invalid file name found with multiple '-' character. The normalized config id: ");
                    console.println(configName);
                }
                localConfigSet.put(configName, subFile);
            }

            // Read the remote configs that are currently active.
            Map<String, JsonValue> remoteConfigSet = new HashMap<String, JsonValue>();
            try {
                ResourceResponse responseValue = resource.read(null, Requests.newReadRequest("config"));
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

            final ResourcePath configResource = ResourcePath.valueOf("config");
            for (Map.Entry<String, File> entry : localConfigSet.entrySet()) {
                String sourceConfigId = entry.getKey();
                try {
                    if (remoteConfigSet.containsKey(sourceConfigId)) {
                        // Update
                        UpdateRequest updateRequest = Requests.newUpdateRequest(configResource.concat(sourceConfigId),
                                new JsonValue(mapper.readValue(entry.getValue(), Map.class)));

                        resource.update(null, updateRequest);
                        // If the update succeeded, remove the entry from 'remoteConfigSet' - this prevents it
                        // from being deleted below.  If this update fails, the entry will remain in remoteConfigSet
                        // and will be deleted from the remote IDM instance.
                        remoteConfigSet.remove(sourceConfigId);
                    } else {
                        // Create
                        CreateRequest createRequest = Requests.newCreateRequest(configResource.concat(sourceConfigId),
                                new JsonValue(mapper.readValue(entry.getValue(), Map.class)));
                        resource.create(null, createRequest);
                    }
                    prettyPrint(console, "ConfigImport", sourceConfigId, null);
                } catch (Exception e) {
                    prettyPrint(console, "ConfigImport", sourceConfigId, e.getMessage());
                }
            }

            // Delete all additional config objects
            if (replaceall) {
                for (String configId : remoteConfigSet.keySet()) {
                    if (isProtectedConfigId(configId)) {
                        prettyPrint(console, "ConfigDelete", configId, "Protected configuration can not be deleted");
                        continue;
                    }

                    try {
                        // configId is concatenated to avoid file paths from getting url encoded -> '/'-> '%2f'
                        resource.delete(null, Requests.newDeleteRequest(configResource.concat(configId)));
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

    private boolean isProtectedConfigId(String configId) {
        return "authentication".equals(configId)
                || "router".equals(configId)
                || "audit".equals(configId)
                || configId.startsWith("repo");
    }

    /**
     * Exports all configuration to the "conf" folder.
     *
     * @param session the command session
     * @param userPass the username/password
     * @param idmUrl the url of the OpenIDM instance
     * @param idmPort the OpenIDM instance's port
     */
    @Descriptor("Exports all configurations to 'conf' folder.")
    public void configexport(
            CommandSession session,

            @Descriptor(USER_PASS_DESC)
            @MetaVar(USER_PASS_METAVAR)
            @Parameter(names = { "-u", "--user" }, absentValue = USER_PASS_DEFAULT)
            final String userPass,

            @Descriptor(IDM_URL_DESC)
            @MetaVar(IDM_URL_METAVAR)
            @Parameter(names = { "--url" }, absentValue = IDM_URL_DEFAULT)
            final String idmUrl,

            @Descriptor(IDM_PORT_DESC)
            @MetaVar(IDM_PORT_METAVAR)
            @Parameter(names = { "-P", "--port" }, absentValue = IDM_PORT_DEFAULT)
            final String idmPort) {
        configexport(session, userPass, idmUrl, idmPort, "conf");
    }


    /**
     * Exports all configurations.
     *
     * @param session the command session
     * @param userPass the username/password
     * @param idmUrl the url of the OpenIDM instance
     * @param idmPort the OpenIDM instance's port
     * @param target the target directory
     */
    @Descriptor("Exports all configurations.")
    public void configexport(
            CommandSession session,

            @Descriptor(USER_PASS_DESC)
            @MetaVar(USER_PASS_METAVAR)
            @Parameter(names = { "-u", "--user" }, absentValue = USER_PASS_DEFAULT)
            final String userPass,

            @Descriptor(IDM_URL_DESC)
            @MetaVar(IDM_URL_METAVAR)
            @Parameter(names = { "--url" }, absentValue = IDM_URL_DEFAULT)
            final String idmUrl,

            @Descriptor(IDM_PORT_DESC)
            @MetaVar(IDM_PORT_METAVAR)
            @Parameter(names = { "-P", "--port" }, absentValue = IDM_PORT_DEFAULT)
            final String idmPort,

            @Descriptor("target directory")
            String target) {
        processOptions(userPass, idmUrl, idmPort);

        File targetDir = IdentityServer.getFileForPath(target);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        session.getConsole().println("[ConfigExport] Export JSON configurations to:");
        session.getConsole().append("[ConfigExport] \t").println(targetDir.getAbsolutePath());

        try {
            ResourceResponse responseValue = resource.read(null, Requests.newReadRequest("config"));
            Iterator<JsonValue> iterator = responseValue.getContent().get("configurations").iterator();
            String bkpPostfix = "." + (new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss")).format(new Date()) + ".bkp";
            while (iterator.hasNext()) {
                String id = iterator.next().get("_id").required().asString();
                if (!id.startsWith("org.apache")) {
                    try {
                        responseValue = resource.read(null, Requests.newReadRequest("config/" + id));
                        if (null != responseValue.getContent()
                                && !responseValue.getContent().isNull()) {
                            File configFile = new File(targetDir, id.replace("/", "-") + ".json");
                            if (configFile.exists()) {
                                configFile.renameTo(
                                        new File(configFile.getParentFile(), configFile.getName() + bkpPostfix));
                            }
                            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile,
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

    private void export(InputStream console, PrintStream out, String[] args) {
        out.println("Exported");
    }

    /**
     * Generate connector configuration.
     *
     * @param session the command session
     * @param userPass the username/password
     * @param idmUrl the url of the OpenIDM instance
     * @param idmPort the OpenIDM instance's port
     * @param name the connector name
     */
    @Descriptor("Generate connector configuration.")
    public void configureconnector(
            CommandSession session,

            @Descriptor(USER_PASS_DESC)
            @MetaVar(USER_PASS_METAVAR)
            @Parameter(names = { "-u", "--user" }, absentValue = USER_PASS_DEFAULT)
            final String userPass,

            @Descriptor(IDM_URL_DESC)
            @MetaVar(IDM_URL_METAVAR)
            @Parameter(names = { "--url" }, absentValue = IDM_URL_DEFAULT)
            final String idmUrl,

            @Descriptor(IDM_PORT_DESC)
            @MetaVar(IDM_PORT_METAVAR)
            @Parameter(names = { "-P", "--port" }, absentValue = IDM_PORT_DEFAULT)
            final String idmPort,

            @Descriptor("Name of the new connector configuration.")
            @MetaVar("CONNECTOR")
            @Parameter(names = { "-n", "--name" }, absentValue = "test")
            String name) {
        processOptions(userPass, idmUrl, idmPort);

        try {
            // Prepare temp folder and file
            File temp = IdentityServer.getFileForPath("temp");
            if (!temp.exists()) {
                temp.mkdir();
            }
            // TODO Make safe file name

            if (StringUtils.isBlank(name) || !name.matches("\\w++")) {
                session.getConsole().append("The given name \"").append(name).println(
                        "\" must match [a-zA-Z_0-9] pattern");
                return;
            }

            File finalConfig = new File(temp, "provisioner.openicf-" + name + ".json");

            // Common request attributes
            ActionRequest request = Requests.newActionRequest("system", "CREATECONFIGURATION");

            JsonValue responseValue;
            Map<String, Object> configuration = null;

            // Phase#1 - Get available connectors
            if (!finalConfig.exists()) {
                responseValue = resource.action(null, request).getJsonContent();

                JsonValue connectorRef = responseValue.get("connectorRef");
                if (!connectorRef.isNull() && connectorRef.isList()) {
                    int i = 0;
                    for (JsonValue connector : connectorRef) {
                        String displayName = connector.get("displayName").asString();
                        if (null == displayName) {
                            displayName = connector.get("connectorName").asString();
                        }
                        String version = connector.get("bundleVersion").asString();
                        String connectorHostRef = connector.get("connectorHostRef").asString();

                        session.getConsole().append(Integer.toString(i)).append(". ").append(displayName);
                        if (null != connectorHostRef) {
                            session.getConsole().append(" Remote (").append(connectorHostRef).append(")");
                        }
                        session.getConsole().append(" version ").println(version);
                        i++;
                    }

                    session.getConsole().append(Integer.toString(connectorRef.size())).println(". Exit");
                    Scanner input = new Scanner(session.getKeyboard());
                    int index = -1;
                    do {
                        session.getConsole()
                                .append("Select [0..")
                                .append(Integer.toString(connectorRef.size()))
                                .append("]: ");
                        index = input.nextInt();
                    } while (index < 0 || index > connectorRef.size());
                    if (index == connectorRef.size()) {
                        return;
                    }
                    configuration = responseValue.asMap();

                    // If we don't getObject() JsonValue will wrap the object resulting in a JSON payload of
                    // { "connectorRef": { ..., "wrappedObject": ... }
                    configuration.put("connectorRef", connectorRef.get(index).getObject());
                } else {
                    session.getConsole().println("There are no available connector!");
                }
            } else {
                configuration = new JsonValue(mapper.readValue(finalConfig, Map.class)).asMap();
                session.getConsole().append("Configuration was found and read from: ")
                        .println(finalConfig.getAbsolutePath());
            }

            if (null == configuration) {
                return;
            }

            // Repeatable phase #2 and #3
            request.setContent(new JsonValue(configuration));
            responseValue = resource.action(null, request).getJsonContent();

            configuration = responseValue.asMap();
            configuration.put("name", name);

            mapper.writerWithDefaultPrettyPrinter().writeValue(finalConfig, configuration);

            session.getConsole()
                    .append("Edit the configuration file and run the command again. The configuration was saved to ")
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

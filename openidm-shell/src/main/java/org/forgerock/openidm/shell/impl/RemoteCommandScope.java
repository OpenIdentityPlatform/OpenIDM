/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.shell.CustomCommandScope;

import java.io.*;
import java.util.*;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class RemoteCommandScope extends AbstractRemoteCommandScope {
    /**
     * {@inheritDoc}
     */
    public Map<String, String> getFunctionMap() {
        Map<String, String> help = new HashMap<String, String>();
        //help.put("export", "Exports all the objects");
        help.put("configureconnector", "Generate connector configuration");
        return help;
    }

    /**
     * {@inheritDoc}
     */
    public String getScope() {
        return "remote";
    }


    public void export(InputStream console, PrintStream out, String[] args) {
        out.println("Exported");
    }

    @Descriptor("Generate connector configuration.")
    public void configureconnector(CommandSession session, @Descriptor("Name of the new connector configuration.") String name) {
        try {
            // Prepare temp folder and file
            File temp = IdentityServer.getFileForPath("temp");
            if (!temp.exists()) {
                temp.mkdir();
            }
            //TODO Make safe file name

            if (!name.matches("\\w++")) {
                session.getConsole().append("The given name \"").append(name).println("\" must match [a-zA-Z_0-9] pattern");
                return;
            }
            File finalConfig = new File(temp, "provisioner.openicf-" + name + ".json");

            // Common request attributes
            Map params = new HashMap();
            params.put(ServerConstants.ACTION_NAME, "CREATECONFIGURATION");
            JsonValue requestValue = new JsonValue(new HashMap());
            requestValue.put("id", "system");
            requestValue.put("method", "action");
            requestValue.put("params", params);


            JsonValue responseValue;
            Map<String, Object> configuration = null;

            //Phase#1 - Get available connectors
            if (!finalConfig.exists()) {
                responseValue = getRouter().handle(requestValue);

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
                            session.getConsole().append("Select [0..").append(Integer.toString(connectorRef.size()))
                                    .append("]: ");
                            index = input.nextInt();
                        }
                        while (index < 0 || index > connectorRefs.size());
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
                configuration = mapper.readValue(finalConfig, Map.class);
            }

            if (null == configuration) {
                return;
            }

            // Repeatable phase #2 and #3
            requestValue.put("value", configuration);
            responseValue = getRouter().handle(requestValue);
            responseValue.put("name", name);
            mapper.writerWithDefaultPrettyPrinter().writeValue(finalConfig, responseValue.getObject());
            session.getConsole().append("Edit the configuration file and run the command again. The configuration was saved to ")
                    .println(finalConfig.getAbsolutePath());

        } catch (JsonResourceException e) {
            session.getConsole().append("Remote operation failed: ").println(e.getMessage());
        } catch (Exception e) {
            session.getConsole().append("Operation failed: ").println(e.getMessage());
        }
    }
}

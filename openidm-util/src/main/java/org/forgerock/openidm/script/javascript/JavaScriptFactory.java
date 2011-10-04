/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.script.javascript;

// Java Standard Edition
import java.io.File;

// JSON-Fluent
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// OpenIDM
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.ScriptFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/**
 * Implementation of a script factory for JavaScript.
 * <p/>
 * Expects {@code "type"} configuration property of {@code "text/javascript"} and
 * {@code "source"} property, which contains the script source code.
 * <p/>
 * The optional boolean property {@code "sharedScope"} indicates if a shared scope should be
 * used. If {@code true}, a sealed shared scope containing standard JavaScript objects
 * (Object, String, Number, Date, etc.) will be used for script execution rather than
 * allocating a new unsealed scope for each execution.
 *
 * @author Paul C. Bryan
 */
public class JavaScriptFactory implements ScriptFactory {

    private ContextFactory.Listener debugListener = null;
    private volatile boolean debugStarted = false;

    private synchronized void initDebugListener() throws ScriptException {
        /*
        Allow to compile it when the required JAR is available as a Maven dependency!!

        String configString = System.getProperty("openidm.script.rhino.debug");
        if (configString != null) {
            try {
                if (!debugStarted) {
                    if (null == debugListener) {
                        debugListener = new org.eclipse.wst.jsdt.debug.rhino.debugger.RhinoDebugger(configString);
                        Context.enter().getFactory().addListener(debugListener);
                        Context.exit();
                    }
                    ((org.eclipse.wst.jsdt.debug.rhino.debugger.RhinoDebugger) debugListener).start();
                }
            } catch (Throwable ex) {
                //Catch NoClassDefFoundError exception
                if (!(ex instanceof NoClassDefFoundError)) {
                  //TODO What to do if there is an exception?
                  //throw new ScriptException("Failed to stop RhinoDebugger", ex);
                } else {
                  //TODO add logging to WARN about the missing RhinoDebugger class
                }
            } finally {
                debugStarted = true;
            }
        } else if (debugStarted && null != debugListener) {
            try {
                ((org.eclipse.wst.jsdt.debug.rhino.debugger.RhinoDebugger) debugListener).stop();
            } catch (Throwable ex) {
                //We do not care about the NoClassDefFoundError when we "Stop"
                if (!(ex instanceof NoClassDefFoundError)) {
                  //TODO What to do if there is an exception?
                  //throw new ScriptException("Failed to stop RhinoDebugger", ex);
                }
            } finally {
                debugStarted = false;
            }
        }*/
    }

    @Override
    public Script newInstance(String name, JsonNode config) throws JsonNodeException {
        String type = config.get("type").asString();
        if (type != null && type.equalsIgnoreCase("text/javascript")) {
            boolean sharedScope = config.get("sharedScope").defaultTo(true).asBoolean();
            if (config.isDefined("source")) {
                try {
                    initDebugListener();
                    return new JavaScript(name, config.get("source").asString(), sharedScope);
                } catch (ScriptException se) { // re-cast to show exact node of failure 
                    throw new JsonNodeException(config.get("source"), se);
                }
            } else if (config.isDefined("file")) { // TEMPORARY
                try {
                    initDebugListener();
                    return new JavaScript(name, IdentityServer.getFileForPath(config.get("file").asString()), sharedScope);
                } catch (ScriptException se) { // re-cast to show exact node of failure 
                    throw new JsonNodeException(config.get("file"), se);
                }
            } else {
                throw new JsonNodeException(config, "expected 'source' or 'file' property");
            }
        }
        return null;
    }
}


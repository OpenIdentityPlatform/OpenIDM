/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.provisioner.openicf.internal;

import static org.forgerock.json.JsonValueFunctions.pattern;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.openidm.core.IdentityServer;
import org.forgerock.openidm.util.FileUtil;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;


public class SystemAction {

    public static final String SCRIPT_EXECUTE_MODE = "scriptExecuteMode";
    public static final String SCRIPT_VARIABLE_PREFIX = "scriptVariablePrefix";
    public static final String SCRIPT_ID = "scriptId";

    public static final List<String> SCRIPT_PARAMS = Arrays.asList(SCRIPT_EXECUTE_MODE, SCRIPT_VARIABLE_PREFIX, SCRIPT_ID);

    private final String name;
    private final List<SystemTypeAction> actions;

    public SystemAction(JsonValue systemAction) {
        this.name = systemAction.get(SCRIPT_ID).required().asString();
        List<SystemTypeAction> actionsList = new ArrayList<SystemTypeAction>();
        for (JsonValue actionValue : systemAction.get("actions").required().expect(List.class)) {
            actionsList.add(new SystemTypeAction(actionValue));
        }
        actions = actionsList;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the ScriptContextBuilders for the system type
     * @param systemType name of system type
     * @return List of ScriptContextBuilder for system type or empty List if there are none
     */
    public List<ScriptContextBuilder> getScriptContextBuilders(String systemType) {
        List<ScriptContextBuilder> result = new ArrayList<ScriptContextBuilder>(actions.size());
        for (SystemTypeAction action : actions) {
            if (action.match(systemType)) {
                result.add(action.getScriptContextBuilder());
            }
        }
        return result;
    }

    private class SystemTypeAction {
        private Pattern systemType;
        private String actionType;
        private String actionSource;

        public SystemTypeAction(JsonValue systemTypeAction) {
            this.systemType = systemTypeAction.get("systemType").required().as(pattern());
            this.actionType = systemTypeAction.get("actionType").required().asString();
            if (systemTypeAction.isDefined("actionFile")) {
                File scriptFile =
                    IdentityServer.getFileForProjectPath(systemTypeAction.get("actionFile").required().asString());
                try {
                    actionSource = FileUtil.readFile(scriptFile);
                } catch (IOException e) {
                    throw new JsonValueException(systemTypeAction.get("actionFile"),
                            "Failed to load the action file.", e);
                }
            } else {
                actionSource = systemTypeAction.get("actionSource").required().asString();
            }
        }

        public boolean match(String systemTypeName) {
            return systemType.matcher(systemTypeName).matches();
        }

        public ScriptContextBuilder getScriptContextBuilder() {
            return new ScriptContextBuilder(actionType, actionSource);
        }
    }
}

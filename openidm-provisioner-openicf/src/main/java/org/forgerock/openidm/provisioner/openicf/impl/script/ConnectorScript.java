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
package org.forgerock.openidm.provisioner.openicf.impl.script;

import org.forgerock.json.fluent.JsonValue;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;

/**
 * Sample Class Doc
 * {
 * "_script-type" : "Boo|SHELL|Groovy",
 * "_script-expression" : "echo OPENIDM_attr1",
 * "_script-execute-mode" : "CONNECTOR|RESOURCE",
 * "attr1" : "Hello World!"
 * }
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ConnectorScript {

    public static final String SCRIPT_TYPE = "_script-type";
    public static final String SCRIPT_EXPRESSION = "_script-expression";
    public static final String SCRIPT_EXECUTE_MODE = "_script-execute-mode";
    public static final String SCRIPT_VARIABLE_PREFIX = "_script-variable-prefix";

    public enum ExecutionMode {
        CONNECTOR,
        RESOURCE;
    }

    private ScriptContextBuilder _scriptContextBuilder = null;
    private OperationOptionsBuilder _operationOptionsBuilder = null;
    private ExecutionMode _execMode = null;


    public ConnectorScript(JsonValue params) {
        init(params.required());
    }

    private void init(JsonValue params) {
        getScriptContextBuilder().setScriptLanguage(params.get(SCRIPT_TYPE).required().expect(String.class).asString());
        if (getScriptContextBuilder().getScriptLanguage().equalsIgnoreCase("SHELL")) {
            getOperationOptionsBuilder().setOption("variablePrefix", "OPENIDM_");
        }
        getScriptContextBuilder().setScriptText(params.get(SCRIPT_EXPRESSION).required().expect(String.class).asString());
        setExecMode(params.get(SCRIPT_EXECUTE_MODE).required().expect(String.class).asString());
        if (null == _execMode) {
            throw new IllegalArgumentException("Script execute mode can not be determined from: " + params.get("_script-execute-mode").asString());
        }
    }

    public ScriptContextBuilder getScriptContextBuilder() {
        if (this._scriptContextBuilder == null) {
            this._scriptContextBuilder = new ScriptContextBuilder();
        }
        return this._scriptContextBuilder;
    }


    public ExecutionMode getExecMode() {
        return this._execMode;
    }

    public Class<? extends APIOperation> getAPIOperation() {
        if (ExecutionMode.RESOURCE.equals(_execMode)) {
            return ScriptOnResourceApiOp.class;
        }
        return ScriptOnConnectorApiOp.class;
    }

    public void setExecMode(String execMode) {
        this._execMode = Enum.valueOf(ExecutionMode.class, execMode.toUpperCase());
    }

    public OperationOptionsBuilder getOperationOptionsBuilder() {
        if (this._operationOptionsBuilder == null) {
            this._operationOptionsBuilder = new OperationOptionsBuilder();
        }
        return this._operationOptionsBuilder;
    }
}

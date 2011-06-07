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

import org.forgerock.openidm.provisioner.openicf.impl.script.ConnectorScript;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.operations.ScriptOnConnectorApiOp;
import org.identityconnectors.framework.api.operations.ScriptOnResourceApiOp;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ConnectorScriptExecutor {

    final static Logger logger = LoggerFactory.getLogger(ConnectorScriptExecutor.class);

    public Object execute(ConnectorFacade connector, ConnectorScript script)
            throws Exception {

        String method = "execute";

        ScriptContextBuilder builder = script.getScriptContextBuilder();
        String scriptLanguage = builder.getScriptLanguage();
        String actionName = script.getActionName();
        String execMode = script.getExecMode();

        ScriptOnResourceApiOp scriptOnResource = (ScriptOnResourceApiOp) connector.getOperation(ScriptOnResourceApiOp.class);
        ScriptOnConnectorApiOp scriptOnConnector = (ScriptOnConnectorApiOp) connector.getOperation(ScriptOnConnectorApiOp.class);

        //TRACE.info4("execute", "Executing " + scriptLanguage + " resource action '" + actionName + "'");

        ScriptContext scriptContext = builder.build();
        OperationOptions opOptions = script.getOperationOptionsBuilder().build();

        Object scriptResult = null;

        if (execMode.equals("connector")) {
            if (scriptOnConnector == null) {
//        msg = new ErrorMessage(Severity.ERROR, "com.waveset.adapter.RAMessages:ERR_UNSUPPORTED_CONN_OP", "ScriptOnConnector");
//        throw new WavesetException(msg);
            }
            try {
                scriptResult = scriptOnConnector.runScriptOnConnector(scriptContext, opOptions);
            } catch (Exception e) {
//        msg = new ErrorMessage(Severity.ERROR, "com.waveset.adapter.RAMessages:ERR_CONN_SCRIPT_EXEC_FAILED", new Object[] { actionName, script.getResourceName() });
//        we = new WavesetException(msg, e);
//        throw we;
            }

        }

        if (execMode.equals("resource")) {
            if (scriptOnResource == null) {
//        e = new ErrorMessage(Severity.ERROR, "com.waveset.adapter.RAMessages:ERR_UNSUPPORTED_CONN_OP", "ScriptOnResource");
//        throw new WavesetException(e);
            }
            try {
                scriptResult = scriptOnResource.runScriptOnResource(scriptContext, opOptions);
            } catch (Exception e) {
//        msg = new ErrorMessage(Severity.ERROR, "com.waveset.adapter.RAMessages:ERR_CONN_SCRIPT_EXEC_FAILED", new Object[] { actionName, script.getResourceName() });
//        we = new WavesetException(msg, e);
//        throw we;
            }

        }

        return scriptResult;
    }
}

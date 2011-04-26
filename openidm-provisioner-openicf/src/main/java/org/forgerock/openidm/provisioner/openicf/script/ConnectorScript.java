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
package org.forgerock.openidm.provisioner.openicf.script;

import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ScriptContextBuilder;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ConnectorScript {

    private ScriptContextBuilder _scriptContextBuilder;
    private OperationOptionsBuilder _operationOptionsBuilder;
    private String _actionName;
    private String _execMode;
    private String _resourceName;

    public String getResourceName() {
        return this._resourceName;
    }

    public void setResourceName(String resourceName) {
        this._resourceName = resourceName;
    }

    public ScriptContextBuilder getScriptContextBuilder() {
        if (this._scriptContextBuilder == null) {
            this._scriptContextBuilder = new ScriptContextBuilder();
        }

        return this._scriptContextBuilder;
    }

    public String getActionName() {
        return this._actionName;
    }

    public void setActionName(String actionName) {
        this._actionName = actionName;
    }

    public String getExecMode() {
        return this._execMode;
    }

    public void setExecMode(String execMode) {
        this._execMode = execMode;
    }

    public OperationOptionsBuilder getOperationOptionsBuilder() {
        if (this._operationOptionsBuilder == null) {
            this._operationOptionsBuilder = new OperationOptionsBuilder();
        }

        return this._operationOptionsBuilder;
    }
}

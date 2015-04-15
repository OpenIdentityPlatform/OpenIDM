/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.tools.scriptedbundler;

/**
 * Define the available connector types with their respective template filenames and base classes.
 */
enum BaseConnectorType {
    GROOVY("UI_ScriptedBase.template",
            "ScriptedGroovyConnector.template",
            "org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration"),
    POOLABLEGROOVY("UI_ScriptedPoolable.template",
            "ScriptedPoolableConnector.template",
            "org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration"),
    SQL("UI_ScriptedSQL.template",
            "ScriptedSQLConnector.template",
            "org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration"),
    REST("UI_ScriptedREST.template",
            "ScriptedRESTConnector.template",
            "org.forgerock.openicf.connectors.scriptedrest.ScriptedRESTConfiguration"),
    CREST("UI_ScriptedCREST.template",
            "ScriptedCRESTConnector.template",
            "org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConfiguration");

    private final String uiTemplate;
    private final String connectorTemplate;
    private final String configBaseClass;

    private BaseConnectorType(String uiTemplate, String connectorTemplate, String configBaseClass) {
        this.uiTemplate = uiTemplate;
        this.connectorTemplate = connectorTemplate;
        this.configBaseClass = configBaseClass;
    }

    /**
     * Return the template file name for this connector type.
     *
     * @return
     */
    public String getUITemplate() {
        return uiTemplate;
    }

    /**
     * Return the template file name for this connector type.
     *
     * @return
     */
    public String getConnectorTemplate() {
        return connectorTemplate;
    }

    /**
     * Return the configuration base class for this connector type.
     *
     * @return
     */
    public String getConfigBaseClass() {
        return configBaseClass;
    }
}

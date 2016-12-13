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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.impl.api;

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Connector system-action, such as executing a script.
 */
@Title("System Action Item")
public class SystemActionItem {

    private String systemType;
    private String actionType;
    private String actionFile;
    private String actionSource;

    /**
     * Gets regex matching the connector-name.
     *
     * @return Regex matching the connector-name
     */
    @Description("Regex matching the connector-name (e.g., .*Connector)")
    @NotNull
    public String getSystemType() {
        return systemType;
    }

    /**
     * Sets regex matching the connector-name.
     *
     * @param systemType Regex matching the connector-name
     */
    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    /**
     * Gets action-type.
     *
     * @return Action type (e.g., groovy)
     */
    @Description("Action type (e.g., groovy)")
    @NotNull
    public String getActionType() {
        return actionType;
    }

    /**
     * Sets action-type.
     *
     * @param actionType Action type (e.g., groovy)
     */
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    /**
     * Gets path to action file (e.g., tools/myAction.groovy).
     *
     * @return Path to action file (e.g., tools/myAction.groovy)
     */
    @Description("Path to action file (e.g., tools/myAction.groovy)")
    public String getActionFile() {
        return actionFile;
    }

    /**
     * Sets path to action file (e.g., tools/myAction.groovy).
     *
     * @param actionFile Path to action file (e.g., tools/myAction.groovy)
     */
    public void setActionFile(String actionFile) {
        this.actionFile = actionFile;
    }

    /**
     * Gets literal action source-code.
     *
     * @return Literal action source-code (e.g., return "Hello";)
     */
    @Description("Literal action source-code (e.g., return \"Hello\";)")
    public String getActionSource() {
        return actionSource;
    }

    /**
     * Sets literal action source-code.
     *
     * @param actionSource Literal action source-code (e.g., return "Hello";)
     */
    public void setActionSource(String actionSource) {
        this.actionSource = actionSource;
    }

}

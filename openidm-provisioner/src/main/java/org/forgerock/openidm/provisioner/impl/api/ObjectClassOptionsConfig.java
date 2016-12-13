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

import java.util.Map;

import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Configuration settings for a given object-class under some operation.
 */
@Title("Object-Class Options Config")
public class ObjectClassOptionsConfig {

    private boolean denied;
    private OnDeny onDeny;
    private Map<String, Object> operationOptionInfo;

    /**
     * Gets operation-denied flag.
     *
     * @return Operation prevented when true
     */
    @Description("Operation prevented when true")
    public boolean isDenied() {
        return denied;
    }

    /**
     * Sets operation-denied flag.
     *
     * @param denied Operation prevented when true
     */
    public void setDenied(boolean denied) {
        this.denied = denied;
    }

    /**
     * Gets action to take when operation is denied.
     *
     * @return Action to take when operation is denied
     */
    @Description("Action to take when operation is denied (DO_NOTHING, THROW_EXCEPTION)")
    @Default("DO_NOTHING")
    public OnDeny getOnDeny() {
        return onDeny;
    }

    /**
     * Sets action to take when operation is denied.
     *
     * @param onDeny Action to take when operation is denied
     */
    public void setOnDeny(OnDeny onDeny) {
        this.onDeny = onDeny;
    }

    /**
     * Gets additional options.
     *
     * @return Additional options
     */
    @Description("Additional options")
    public Map<String, Object> getOperationOptionInfo() {
        return operationOptionInfo;
    }

    /**
     * Sets additional options.
     *
     * @param operationOptionInfo Additional options
     */
    public void setOperationOptionInfo(Map<String, Object> operationOptionInfo) {
        this.operationOptionInfo = operationOptionInfo;
    }

}

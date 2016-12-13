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
 * Configuration settings for a given operation type.
 */
@Title(OperationOptionsConfig.TITLE)
public class OperationOptionsConfig {

    static final String TITLE = "Operation Options Config";

    private boolean denied;
    private OnDeny onDeny;
    private Map<String, ObjectClassOptionsConfig> objectFeatures;

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
     * Gets fine grained operation-options per object-class.
     *
     * @return Fine grained operation-options per object-class
     */
    @Description("Fine grained operation-options per object-class")
    public Map<String, ObjectClassOptionsConfig> getObjectFeatures() {
        return objectFeatures;
    }

    /**
     * Sets fine grained operation-options per object-class.
     *
     * @param objectFeatures Fine grained operation-options per object-class
     */
    public void setObjectFeatures(Map<String, ObjectClassOptionsConfig> objectFeatures) {
        this.objectFeatures = objectFeatures;
    }

}

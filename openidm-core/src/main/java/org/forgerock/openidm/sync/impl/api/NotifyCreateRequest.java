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

package org.forgerock.openidm.sync.impl.api;

import java.util.Map;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Request class for NotifyCreate action.
 */
@Title("Notify-Create Action Request")
public class NotifyCreateRequest {
    private Map<String, Object> newValue;

    /**
     * Gets the new value of the source object.
     *
     * @return The new value of the source object
     */
    @Description("The new value of the source object")
    public Map<String, Object> getNewValue() {
        return newValue;
    }

    /**
     * Sets the new value of the source object, if any.
     *
     * @param newValue The new value of the source object, if any
     */
    public void setNewValue(Map<String, Object> newValue) {
        this.newValue = newValue;
    }
}

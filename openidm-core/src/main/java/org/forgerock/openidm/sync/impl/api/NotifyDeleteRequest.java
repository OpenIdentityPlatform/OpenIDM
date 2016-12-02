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
 * Request class for NotifyDelete action.
 */
@Title("Notify-Delete Action Request")
public class NotifyDeleteRequest {
    private Map<String, Object> oldValue;

    /**
     * Gets the former value of the source object, if any.
     *
     * @return The former value of the source object, if any
     */
    @Description("The former value of the source object, if any")
    public Map<String, Object> getOldValue() {
        return oldValue;
    }

    /**
     * Sets the former value of the source object, if any.
     *
     * @param oldValue The former value of the source object, if any
     */
    public void setOldValue(Map<String, Object> oldValue) {
        this.oldValue = oldValue;
    }
}

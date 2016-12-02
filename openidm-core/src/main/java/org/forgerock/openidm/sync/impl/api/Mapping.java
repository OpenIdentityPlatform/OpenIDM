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

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * A class for mapping.
 */
@Title("Mapping")
public class Mapping {
    private String name;
    private String type;

    /**
     * Gets mapping name (e.g., systemXmlfileAccounts_managedUser).
     *
     * @return Mapping name (e.g., systemXmlfileAccounts_managedUser)
     */
    @Description("Mapping name (e.g., systemXmlfileAccounts_managedUser)")
    public String getName() {
        return name;
    }

    /**
     * Sets mapping name (e.g., systemXmlfileAccounts_managedUser).
     *
     * @param name Mapping name (e.g., systemXmlfileAccounts_managedUser)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets mapping type (e.g., source, target).
     *
     * @return Mapping type (e.g., source, target)
     */
    @Description("Mapping type (e.g., source, target)")
    public String getType() {
        return type;
    }

    /**
     * Sets mapping type (e.g., source, target).
     *
     * @param type Mapping type (e.g., source, target)
     */
    public void setType(String type) {
        this.type = type;
    }
}

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

package org.forgerock.openidm.config.manage.api;

import javax.validation.constraints.NotNull;
import java.util.List;

import org.forgerock.api.annotations.Description;

/**
 * Lists configuration identifiers.
 */
public class ConfigListAllResponse {

    private List<ConfigIdentifiers> configurations;

    /**
     * Gets list of configuration identifiers.
     *
     * @return List of configuration identifiers
     */
    @NotNull
    @Description("List of configuration identifiers")
    public List<ConfigIdentifiers> getConfigurations() {
        return configurations;
    }

    /**
     * Sets list of configuration identifiers.
     *
     * @param configurations List of configuration identifiers
     */
    public void setConfigurations(List<ConfigIdentifiers> configurations) {
        this.configurations = configurations;
    }
    
}

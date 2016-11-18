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

package org.forgerock.openidm.auth.api;

import javax.validation.constraints.NotNull;

import org.forgerock.api.annotations.Description;

/**
 * Response to {@link org.forgerock.openidm.auth.AuthenticationService} reauthenticate-action.
 */
public class ReauthenticateActionResponse {

    private boolean reauthenticated;

    /**
     * Gets reauthentication status.
     *
     * @return {@code true} if reauthenticate action was successful and {@code false} otherwise
     */
    @NotNull
    @Description("true if reauthenticate action was successful and false otherwise")
    public boolean isReauthenticated() {
        return reauthenticated;
    }

    /**
     * Sets reauthentication status.
     *
     * @param reauthenticated {@code true} if reauthenticate action was successful and {@code false} otherwise
     */
    public void setReauthenticated(boolean reauthenticated) {
        this.reauthenticated = reauthenticated;
    }

}

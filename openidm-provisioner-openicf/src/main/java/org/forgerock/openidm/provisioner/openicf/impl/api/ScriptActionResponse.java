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

package org.forgerock.openidm.provisioner.openicf.impl.api;

import javax.validation.constraints.NotNull;
import java.util.List;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Response for {@code script}-action.
 */
@Title("Script Response")
public class ScriptActionResponse {

    private List<ScriptActionResult> actions;

    /**
     * Gets script-action results.
     *
     * @return Script-action results
     */
    @Description("Script-action results")
    @NotNull
    public List<ScriptActionResult> getActions() {
        return actions;
    }

    /**
     * Sets script-action results.
     *
     * @param actions Script-action results
     */
    public void setActions(List<ScriptActionResult> actions) {
        this.actions = actions;
    }

}

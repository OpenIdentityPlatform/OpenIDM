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
import java.util.List;

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Names a list of {@link SystemActionItem}s to execute.
 */
@Title("System Action Config")
public class SystemAction {

    private String scriptId;
    private List<SystemActionItem> actions;

    /**
     * Gets script ID.
     *
     * @return Script ID
     */
    @Description("Script ID")
    @NotNull
    public String getScriptId() {
        return scriptId;
    }

    /**
     * Sets script ID.
     *
     * @param scriptId Script ID
     */
    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    /**
     * Gets ordered array of actions.
     *
     * @return Ordered array of actions
     */
    @Description("Ordered array of actions")
    @NotNull
    public List<SystemActionItem> getActions() {
        return actions;
    }

    /**
     * Sets ordered array of actions.
     *
     * @param actions Ordered array of actions
     */
    public void setActions(List<SystemActionItem> actions) {
        this.actions = actions;
    }

}

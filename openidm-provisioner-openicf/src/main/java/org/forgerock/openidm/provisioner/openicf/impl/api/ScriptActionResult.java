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

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Result of a single executed script.
 */
@Title("Script Result")
public class ScriptActionResult {

    private Object result;
    private String error;

    /**
     * Gets successful script-result.
     *
     * @return Successful script-result
     */
    @Description("Successful script-result")
    public Object getResult() {
        return result;
    }

    /**
     * Sets successful script-result.
     *
     * @param result Successful script-result
     */
    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * Gets reason for script failure.
     *
     * @return Reason for script failure
     */
    @Description("Reason for script failure")
    public String getError() {
        return error;
    }

    /**
     * Sets reason for script failure.
     *
     * @param error Reason for script failure
     */
    public void setError(String error) {
        this.error = error;
    }

}

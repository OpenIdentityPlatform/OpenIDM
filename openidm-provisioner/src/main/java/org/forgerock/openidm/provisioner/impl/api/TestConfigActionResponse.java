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

import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Response for {@code testConfig}-action.
 */
@Title("Test-Config Response")
public class TestConfigActionResponse {

    private boolean ok;
    private String name;
    private String error;

    /**
     * Gets test result.
     *
     * @return test result
     */
    @Description("Test result")
    @NotNull
    public boolean isOk() {
        return ok;
    }

    /**
     * Sets test result.
     *
     * @param ok test result
     */
    public void setOk(boolean ok) {
        this.ok = ok;
    }

    /**
     * Gets connector name.
     *
     * @return Connector name
     */
    @Description("Unique connector-name")
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Sets connector name.
     *
     * @param name Connector name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets reason for test failure.
     *
     * @return Reason for test failure
     */
    @Description("Reason for test failure")
    public String getError() {
        return error;
    }

    /**
     * Sets reason for test failure.
     *
     * @param error Reason for test failure
     */
    public void setError(String error) {
        this.error = error;
    }

}

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

import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Configures what should happen if a {@code liveSync}-action reports a failure.
 */
@Title("Sync Failure Handler Config")
public class SyncFailureHandler {

    private int maxRetries;
    private Object postRetryAction;

    /**
     * Gets max failed modification retry attempts.
     *
     * @return Max failed modification retry attempts, 0 for no retry, or -1 for infinite retry
     */
    @Description("Max failed modification retry attempts, 0 for no retry, or -1 for infinite retry")
    @Default("-1")
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Sets max failed modification retry attempts.
     *
     * @param maxRetries Max failed modification retry attempts, 0 for no retry, or -1 for infinite retry
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Gets action after all failed modification retry attempts (logged-ignore, dead-letter-queue, or script).
     *
     * @return Action after all failed modification retry attempts (logged-ignore, dead-letter-queue, or script)
     */
    @Description("Action after all failed modification retry attempts (logged-ignore, dead-letter-queue, or script)")
    @NotNull
    public Object getPostRetryAction() {
        return postRetryAction;
    }

    /**
     * Sets action after all failed modification retry attempts (logged-ignore, dead-letter-queue, or script).
     *
     * @param postRetryAction Action after all failed modification retry attempts (logged-ignore, dead-letter-queue,
     * or script)
     */
    public void setPostRetryAction(Object postRetryAction) {
        this.postRetryAction = postRetryAction;
    }

}

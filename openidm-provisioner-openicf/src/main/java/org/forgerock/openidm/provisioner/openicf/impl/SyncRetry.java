/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.provisioner.openicf.impl;

/**
 * A container for information about a sync retry after a failure
 */
class SyncRetry {

    /**
     * The retry value, true if the sync should be retried, false otherwise.
     */
    boolean value;

    /**
     * The {@link Throwable} associated with the failure
     */
    Throwable throwable;

    public SyncRetry() {
        value = false;
        throwable = null;
    }

    /**
     * Returns the retry value.
     *
     * @return true if the sync should be retried, false otherwise.
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Sets the retry value.
     *
     * @param value true if the sync should be retried, false otherwise.
     */
    public void setValue(boolean value) {
        this.value = value;
    }

    /**
     * Returns the {@link Throwable} associated with the failure
     *
     * @return the {@link Throwable} associated with the failure
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Sets the {@link Throwable} associated with the failure.
     *
     * @param throwable the {@link Throwable} associated with the failure.
     */
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
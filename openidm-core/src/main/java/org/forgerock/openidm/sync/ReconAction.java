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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync;

/**
 * A Reconciliation action.
 *
 */
public enum ReconAction {

    /** A target object should be created and linked. */
    CREATE,

    /** A target object should be linked and updated. */
    UPDATE,

    /** The target object should be deleted and unlinked. */
    DELETE,

    /** The correlated target object should be linked. */
    LINK,

    /** The linked target object should be unlinked. */
    UNLINK,

    /** The link situation is flagged as an exception. */
    EXCEPTION,

    /** Does not perform the action. It reports only and then performs the postAction. */
    REPORT,

    /** Does not perform the action nor the report. It performs the postAction only. */
    NOREPORT,

    /** Asynchronous process has been started so it does not perform the action nor the report nor the postAction */
    ASYNC,

    /** Jumps to the end and ignores every further steps, not even assess the situation. */
    IGNORE
}

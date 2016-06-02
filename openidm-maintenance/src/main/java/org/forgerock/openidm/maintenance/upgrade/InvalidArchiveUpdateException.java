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
package org.forgerock.openidm.maintenance.upgrade;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.JsonValue.field;

import org.forgerock.json.JsonValue;

/**
 * Used for flow control while working through the validation steps to validate an Update Archive file.
 */
class InvalidArchiveUpdateException extends UpdateException {

    private static final long serialVersionUID = 1L;

    private String archiveFileName;

    public InvalidArchiveUpdateException(String archiveFileName, String message) {
        super(message);
        this.archiveFileName = archiveFileName;
    }

    public InvalidArchiveUpdateException(String archiveFileName, String message, Throwable cause) {
        super(message, cause);
        this.archiveFileName = archiveFileName;
    }

    /**
     * Returns the json that this exception represents.
     */
    public JsonValue toJsonValue() {
        JsonValue invalidJson = json(object(
                field("archive", archiveFileName),
                field("reason", getMessage())
        ));
        if (null != getCause()) {
            invalidJson.put("errorMessage", getCause().getMessage());
        }
        return invalidJson;
    }
}

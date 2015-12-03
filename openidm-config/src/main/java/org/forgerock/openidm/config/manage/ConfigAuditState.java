/*
 * Returns the contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * Returns the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.config.manage;

import static org.forgerock.json.JsonValue.json;

import org.forgerock.json.JsonValue;

/**
 * Holds the state of a config change that will be used to construct a ConfigAuditEvent.
 *
 * @see ConfigAuditEventLogger
 * @see org.forgerock.audit.events.ConfigAuditEventBuilder
 */
public class ConfigAuditState {
    private String id;
    private String revision;
    private JsonValue before;
    private JsonValue after;

    /**
     * Constructs the state of the Audit config change.
     *
     * @param id ID of the config instance changed.
     * @param revision Revision of the config change.
     * @param before Json of the config before changes were made.
     * @param after Json of the config after the changes were made.
     */
    public ConfigAuditState(String id, String revision, JsonValue before, JsonValue after) {
        this.id = id;
        this.revision = revision;
        this.before = before != null ? before : json(null);
        this.after = after != null ? after : json(null);
    }

    /**
     * Returns the content of the config change before it was modified.
     *
     * @return The content of the config change before it was modified.
     */
    public JsonValue getBefore() {
        return before;
    }

    /**
     * Returns the content of the config change after it was modified.
     *
     * @return The content of the config change after it was modified.
     */
    public JsonValue getAfter() {
        return after;
    }

    /**
     * Returns the ID of the config that was modified.
     *
     * @return The ID of the config that was modified.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the revision of the config change.
     *
     * @return The revision of the config change.
     */
    public String getRevision() {
        return revision;
    }
}

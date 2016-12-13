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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Default;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Timeouts per operation type.
 */
@Title("Operation Timeout")
public class OperationTimeout {

    private long authenticate;
    private long create;
    private long delete;
    private long get;
    private long test;
    private long schema;
    private long scriptOnConnector;
    private long scriptOnResource;
    private long search;
    private long sync;
    private long update;
    private long validate;

    /**
     * Gets AUTHENTICATE timeout or -1 to disable timeout.
     *
     * @return AUTHENTICATE timeout or -1 to disable timeout
     */
    @Description("AUTHENTICATE timeout or -1 to disable timeout")
    @JsonProperty("AUTHENTICATE")
    @Default("-1")
    public long getAuthenticate() {
        return authenticate;
    }

    /**
     * Sets AUTHENTICATE timeout or -1 to disable timeout.
     *
     * @param authenticate AUTHENTICATE timeout or -1 to disable timeout
     */
    public void setAuthenticate(long authenticate) {
        this.authenticate = authenticate;
    }

    /**
     * Gets CREATE timeout or -1 to disable timeout.
     *
     * @return CREATE timeout or -1 to disable timeout
     */
    @Description("CREATE timeout or -1 to disable timeout")
    @JsonProperty("CREATE")
    @Default("-1")
    public long getCreate() {
        return create;
    }

    /**
     * Sets CREATE timeout or -1 to disable timeout.
     *
     * @param create CREATE timeout or -1 to disable timeout
     */
    public void setCreate(long create) {
        this.create = create;
    }

    /**
     * Gets DELETE timeout or -1 to disable timeout.
     *
     * @return DELETE timeout or -1 to disable timeout
     */
    @Description("DELETE timeout or -1 to disable timeout")
    @JsonProperty("DELETE")
    @Default("-1")
    public long getDelete() {
        return delete;
    }

    /**
     * Sets DELETE timeout or -1 to disable timeout.
     *
     * @param delete DELETE timeout or -1 to disable timeout
     */
    public void setDelete(long delete) {
        this.delete = delete;
    }

    /**
     * Gets GET timeout or -1 to disable timeout.
     *
     * @return GET timeout or -1 to disable timeout
     */
    @Description("GET timeout or -1 to disable timeout")
    @JsonProperty("GET")
    @Default("-1")
    public long getGet() {
        return get;
    }

    /**
     * Sets GET timeout or -1 to disable timeout.
     *
     * @param get GET timeout or -1 to disable timeout
     */
    public void setGet(long get) {
        this.get = get;
    }

    /**
     * Gets TEST timeout or -1 to disable timeout.
     *
     * @return TEST timeout or -1 to disable timeout
     */
    @Description("TEST timeout or -1 to disable timeout")
    @JsonProperty("TEST")
    @Default("-1")
    public long getTest() {
        return test;
    }

    /**
     * Sets TEST timeout or -1 to disable timeout.
     *
     * @param test TEST timeout or -1 to disable timeout
     */
    public void setTest(long test) {
        this.test = test;
    }

    /**
     * Gets SCHEMA timeout or -1 to disable timeout.
     *
     * @return SCHEMA timeout or -1 to disable timeout
     */
    @Description("SCHEMA timeout or -1 to disable timeout")
    @JsonProperty("SCHEMA")
    @Default("-1")
    public long getSchema() {
        return schema;
    }

    /**
     * Sets SCHEMA timeout or -1 to disable timeout.
     *
     * @param schema SCHEMA timeout or -1 to disable timeout
     */
    public void setSchema(long schema) {
        this.schema = schema;
    }

    /**
     * Gets SCRIPT_ON_CONNECTOR timeout or -1 to disable timeout.
     *
     * @return SCRIPT_ON_CONNECTOR timeout or -1 to disable timeout
     */
    @Description("SCRIPT_ON_CONNECTOR timeout or -1 to disable timeout")
    @JsonProperty("SCRIPT_ON_CONNECTOR")
    @Default("-1")
    public long getScriptOnConnector() {
        return scriptOnConnector;
    }

    /**
     * Sets SCRIPT_ON_CONNECTOR timeout or -1 to disable timeout.
     *
     * @param scriptOnConnector SCRIPT_ON_CONNECTOR timeout or -1 to disable timeout
     */
    public void setScriptOnConnector(long scriptOnConnector) {
        this.scriptOnConnector = scriptOnConnector;
    }

    /**
     * Gets SCRIPT_ON_RESOURCE timeout or -1 to disable timeout.
     *
     * @return SCRIPT_ON_RESOURCE timeout or -1 to disable timeout
     */
    @Description("SCRIPT_ON_RESOURCE timeout or -1 to disable timeout")
    @JsonProperty("SCRIPT_ON_RESOURCE")
    @Default("-1")
    public long getScriptOnResource() {
        return scriptOnResource;
    }

    /**
     * Sets SCRIPT_ON_RESOURCE timeout or -1 to disable timeout.
     *
     * @param scriptOnResource SCRIPT_ON_RESOURCE timeout or -1 to disable timeout
     */
    public void setScriptOnResource(long scriptOnResource) {
        this.scriptOnResource = scriptOnResource;
    }

    /**
     * Gets SEARCH timeout or -1 to disable timeout.
     *
     * @return SEARCH timeout or -1 to disable timeout
     */
    @Description("SEARCH timeout or -1 to disable timeout")
    @JsonProperty("SEARCH")
    @Default("-1")
    public long getSearch() {
        return search;
    }

    /**
     * Sets SEARCH timeout or -1 to disable timeout.
     *
     * @param search SEARCH timeout or -1 to disable timeout
     */
    public void setSearch(long search) {
        this.search = search;
    }

    /**
     * Gets SYNC timeout or -1 to disable timeout.
     *
     * @return SYNC timeout or -1 to disable timeout
     */
    @Description("SYNC timeout or -1 to disable timeout")
    @JsonProperty("SYNC")
    @Default("-1")
    public long getSync() {
        return sync;
    }

    /**
     * Sets SYNC timeout or -1 to disable timeout.
     *
     * @param sync SYNC timeout or -1 to disable timeout
     */
    public void setSync(long sync) {
        this.sync = sync;
    }

    /**
     * Gets UPDATE timeout or -1 to disable timeout.
     *
     * @return UPDATE timeout or -1 to disable timeout
     */
    @Description("UPDATE timeout or -1 to disable timeout")
    @JsonProperty("UPDATE")
    @Default("-1")
    public long getUpdate() {
        return update;
    }

    /**
     * Sets UPDATE timeout or -1 to disable timeout.
     *
     * @param update UPDATE timeout or -1 to disable timeout
     */
    public void setUpdate(long update) {
        this.update = update;
    }

    /**
     * Gets VALIDATE timeout or -1 to disable timeout.
     *
     * @return VALIDATE timeout or -1 to disable timeout
     */
    @Description("VALIDATE timeout or -1 to disable timeout")
    @JsonProperty("VALIDATE")
    @Default("-1")
    public long getValidate() {
        return validate;
    }

    /**
     * Sets VALIDATE timeout or -1 to disable timeout.
     *
     * @param validate VALIDATE timeout or -1 to disable timeout
     */
    public void setValidate(long validate) {
        this.validate = validate;
    }

}

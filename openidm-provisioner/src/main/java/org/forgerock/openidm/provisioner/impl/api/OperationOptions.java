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

import static org.forgerock.openidm.provisioner.impl.api.OperationOptionsConfig.TITLE;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.forgerock.api.annotations.Description;
import org.forgerock.api.annotations.Title;

/**
 * Configuration settings per operation type.
 */
@Title("Operation Options")
public class OperationOptions {

    private OperationOptionsConfig authenticate;
    private OperationOptionsConfig create;
    private OperationOptionsConfig delete;
    private OperationOptionsConfig get;
    private OperationOptionsConfig test;
    private OperationOptionsConfig schema;
    private OperationOptionsConfig scriptOnConnector;
    private OperationOptionsConfig scriptOnResource;
    private OperationOptionsConfig search;
    private OperationOptionsConfig sync;
    private OperationOptionsConfig update;
    private OperationOptionsConfig validate;

    /**
     * Gets AUTHENTICATE options.
     *
     * @return AUTHENTICATE options
     */
    @Description("AUTHENTICATE options")
    @JsonProperty("AUTHENTICATE")
    @Title(TITLE)
    public OperationOptionsConfig getAuthenticate() {
        return authenticate;
    }

    /**
     * Sets AUTHENTICATE options.
     *
     * @param authenticate AUTHENTICATE options
     */
    public void setAuthenticate(OperationOptionsConfig authenticate) {
        this.authenticate = authenticate;
    }

    /**
     * Gets CREATE options.
     *
     * @return CREATE options
     */
    @Description("CREATE options")
    @JsonProperty("CREATE")
    @Title(TITLE)
    public OperationOptionsConfig getCreate() {
        return create;
    }

    /**
     * Sets CREATE options.
     *
     * @param create CREATE options
     */
    public void setCreate(OperationOptionsConfig create) {
        this.create = create;
    }

    /**
     * Gets DELETE options.
     *
     * @return DELETE options
     */
    @Description("DELETE options")
    @JsonProperty("DELETE")
    @Title(TITLE)
    public OperationOptionsConfig getDelete() {
        return delete;
    }

    /**
     * Sets DELETE options.
     *
     * @param delete DELETE options
     */
    public void setDelete(OperationOptionsConfig delete) {
        this.delete = delete;
    }

    /**
     * Gets GET options.
     *
     * @return GET options
     */
    @Description("GET options")
    @JsonProperty("GET")
    @Title(TITLE)
    public OperationOptionsConfig getGet() {
        return get;
    }

    /**
     * Sets GET options.
     *
     * @param get GET options
     */
    public void setGet(OperationOptionsConfig get) {
        this.get = get;
    }

    /**
     * Gets TEST options.
     *
     * @return TEST options
     */
    @Description("TEST options")
    @JsonProperty("TEST")
    @Title(TITLE)
    public OperationOptionsConfig getTest() {
        return test;
    }

    /**
     * Sets TEST options.
     *
     * @param test TEST options
     */
    public void setTest(OperationOptionsConfig test) {
        this.test = test;
    }

    /**
     * Gets SCHEMA options.
     *
     * @return SCHEMA options
     */
    @Description("SCHEMA options")
    @JsonProperty("SCHEMA")
    @Title(TITLE)
    public OperationOptionsConfig getSchema() {
        return schema;
    }

    /**
     * Sets SCHEMA options.
     *
     * @param schema SCHEMA options
     */
    public void setSchema(OperationOptionsConfig schema) {
        this.schema = schema;
    }

    /**
     * Gets SCRIPT_ON_CONNECTOR options.
     *
     * @return SCRIPT_ON_CONNECTOR options
     */
    @Description("SCRIPT_ON_CONNECTOR options")
    @JsonProperty("SCRIPT_ON_CONNECTOR")
    @Title(TITLE)
    public OperationOptionsConfig getScriptOnConnector() {
        return scriptOnConnector;
    }

    /**
     * Sets SCRIPT_ON_CONNECTOR options.
     *
     * @param scriptOnConnector SCRIPT_ON_CONNECTOR options
     */
    public void setScriptOnConnector(OperationOptionsConfig scriptOnConnector) {
        this.scriptOnConnector = scriptOnConnector;
    }

    /**
     * Gets SCRIPT_ON_RESOURCE options.
     *
     * @return SCRIPT_ON_RESOURCE options
     */
    @Description("SCRIPT_ON_RESOURCE options")
    @JsonProperty("SCRIPT_ON_RESOURCE")
    @Title(TITLE)
    public OperationOptionsConfig getScriptOnResource() {
        return scriptOnResource;
    }

    /**
     * Sets SCRIPT_ON_RESOURCE options.
     *
     * @param scriptOnResource SCRIPT_ON_RESOURCE options
     */
    public void setScriptOnResource(OperationOptionsConfig scriptOnResource) {
        this.scriptOnResource = scriptOnResource;
    }

    /**
     * Gets SEARCH options.
     *
     * @return SEARCH options
     */
    @Description("SEARCH options")
    @JsonProperty("SEARCH")
    @Title(TITLE)
    public OperationOptionsConfig getSearch() {
        return search;
    }

    /**
     * Sets SEARCH options.
     *
     * @param search SEARCH options
     */
    public void setSearch(OperationOptionsConfig search) {
        this.search = search;
    }

    /**
     * Gets SYNC options.
     *
     * @return SYNC options
     */
    @Description("SYNC options")
    @JsonProperty("SYNC")
    @Title(TITLE)
    public OperationOptionsConfig getSync() {
        return sync;
    }

    /**
     * Sets SYNC options.
     *
     * @param sync SYNC options
     */
    public void setSync(OperationOptionsConfig sync) {
        this.sync = sync;
    }

    /**
     * Gets UPDATE options.
     *
     * @return UPDATE options
     */
    @Description("UPDATE options")
    @JsonProperty("UPDATE")
    @Title(TITLE)
    public OperationOptionsConfig getUpdate() {
        return update;
    }

    /**
     * Sets UPDATE options.
     *
     * @param update UPDATE options
     */
    public void setUpdate(OperationOptionsConfig update) {
        this.update = update;
    }

    /**
     * Gets VALIDATE options.
     *
     * @return VALIDATE options
     */
    @Description("VALIDATE options")
    @JsonProperty("VALIDATE")
    @Title(TITLE)
    public OperationOptionsConfig getValidate() {
        return validate;
    }

    /**
     * Sets VALIDATE options.
     *
     * @param validate VALIDATE options
     */
    public void setValidate(OperationOptionsConfig validate) {
        this.validate = validate;
    }

}

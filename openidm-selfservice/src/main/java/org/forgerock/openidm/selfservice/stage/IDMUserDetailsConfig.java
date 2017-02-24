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
 * Copyright 2016-2017 ForgeRock AS.
 */

package org.forgerock.openidm.selfservice.stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.forgerock.openidm.idp.config.ProviderConfig;
import org.forgerock.selfservice.core.config.StageConfig;

/**
 * Configuration for the IDM user details stage.
 *
 */
public final class IDMUserDetailsConfig implements StageConfig {

    /**
     * Name of the stage configuration.
     */
    public static final String NAME = "idmUserDetails";

    private String identityEmailField;
    private List<ProviderConfig> providers = new ArrayList<>();
    private boolean socialRegistrationEnabled = false;
    private String successUrl;
    private Object registrationForm;
    private String identityServiceUrl = "/managed/user";
    private List<String> registrationProperties = new ArrayList<>();

    /**
     * Gets the field name for the identity email address.
     *
     * @return the identity email address field name
     */
    public String getIdentityEmailField() {
        return identityEmailField;
    }

    /**
     * Sets the field name for the identity email address.
     *
     * @param identityEmailField
     *         the identity email address field name
     *
     * @return this config instance
     */
    public IDMUserDetailsConfig setIdentityEmailField(final String identityEmailField) {
        this.identityEmailField = identityEmailField;
        return this;
    }

    /**
     * Fetch the list of provider configurations.
     *
     * @return list of provider configs
     */
    public List<ProviderConfig> getProviders() {
        return providers;
    }

    /**
     * Set the list of provider configurations.
     *
     * @param providers list of provider configs
     */
    public IDMUserDetailsConfig setProviders(final List<ProviderConfig> providers) {
        this.providers = providers;
        return this;
    }

    /**
     * Test whether social registration is enabled.
     *
     * @return true iff social registration is enabled
     */
    public boolean isSocialRegistrationEnabled() {
        return socialRegistrationEnabled;
    }

    /**
     * Set whether social registration is enabled.
     *
     * @param socialRegistrationEnabled true iff social registration should be enabled
     */
    public IDMUserDetailsConfig setSocialRegistrationEnabled(boolean socialRegistrationEnabled) {
        this.socialRegistrationEnabled = socialRegistrationEnabled;
        return this;
    }

    /**
     * Return the URL to which an end-user should be redirected following successful registration.
     *
     * @return post-successful-registration redirect URL
     */
    public String getSuccessUrl() {
        return successUrl;
    }

    /**
     * Set the URL to which an end-user should be redirected following successful registration.
     *
     * @param successUrl post-successful-registration redirect URL
     */
    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    /**
     * Return the registration form object (opaque blob managed by the client).
     *
     * @return registration form object
     */
    public Object getRegistrationForm() {
        return registrationForm;
    }

    /**
     * Set the registration form object (opaque blob managed by the client).
     *
     * @param registrationForm registration form object
     */
    public void setRegistrationForm(Object registrationForm) {
        this.registrationForm = registrationForm;
    }

    /**
     * Return the url for the identity service, e.g. /managed/user
     *
     * @return identity service url
     */
    public String getIdentityServiceUrl() {
        return identityServiceUrl;
    }

    /**
     * Set the url for the identity service
     *
     * @param identityServiceUrl the identity service url
     */
    public void setIdentityServiceUrl(String identityServiceUrl) {
        this.identityServiceUrl = identityServiceUrl;
    }

    /**
     * Retrieve the list of registration form properties the client would like to use.
     *
     * @return list of registration form properties
     */
    public List<String> getRegistrationProperties() {
        return registrationProperties;
    }

    /**
     * Set the list of registration form properties the client would like to use.
     *
     * @param registrationProperties list of registration form properties
     */
    public void setRegistrationProperties(List<String> registrationProperties) {
        this.registrationProperties = registrationProperties;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getProgressStageClassName() {
        return IDMUserDetailsStage.class.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IDMUserDetailsConfig)) {
            return false;
        }

        IDMUserDetailsConfig that = (IDMUserDetailsConfig) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getProgressStageClassName(), that.getProgressStageClassName())
                && Objects.equals(identityEmailField, that.identityEmailField)
                && Objects.equals(providers, that.providers)
                && Objects.equals(socialRegistrationEnabled, that.socialRegistrationEnabled)
                && Objects.equals(successUrl, that.successUrl)
                && Objects.equals(registrationForm, that.registrationForm)
                && Objects.equals(identityServiceUrl, that.identityServiceUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProgressStageClassName(), identityEmailField, providers,
                socialRegistrationEnabled, successUrl, registrationForm, identityServiceUrl);
    }

}

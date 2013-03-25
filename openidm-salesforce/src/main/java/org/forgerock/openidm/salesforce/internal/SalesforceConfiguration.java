/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.salesforce.internal;

import org.apache.commons.lang3.StringUtils;
import org.restlet.data.Form;


/**
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a herf="http://wiki.developerforce.com/page/Digging_Deeper_into_OAuth_2.0_on_Force.com">
 *      Digging Deeper into OAuth 2.0 on Force.com</a>
 */
public class SalesforceConfiguration{


    public final static String LOGIN_URL = "https://login.salesforce.com/services/oauth2/token";

    // Exposed configuration properties.

    /**
     * The Consumer Key
     */
    private String clientId = null;

    /**
     * The Consumer Secret
     */
    private String clientSecret = null;

    /**
     * The Callback URL
     */
    private String redirect_uri = null;

    /**
     * The Scope
     */
    private String scope = "id api refresh_token";

    /**
     * The Authorization Code
     */
    private String authorization_code = null;

    /**
     * The Refresh Token
     */
    private String refresh_token = null;

    /**
     * The Username to authenticate with..
     */
    private String username;

    /**
     * The Password to authenticate with.
     */
    private String password = null;



    /**
     * The Password to authenticate with.
     * <p/>
     * When accessing salesforce.com from outside of your company’s trusted networks, you must add a security token
     * to your password to log in to a desktop client, such as Connect for Outlook, Connect Offline, Connect for Office,
     * Connect for Lotus Notes, or the Data Loader.
     */
    private String security_token = null;

    /**
     * Constructor
     */
    public SalesforceConfiguration() {

    }

    // Client authentication

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String consumerKey) {
        this.clientId = consumerKey;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String password) {
        this.clientSecret = password;
    }

    // Password Flow

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSecurityToken() {
        return security_token;
    }

    public void setSecurityToken(String security_token) {
        this.security_token = security_token;
    }

    // Refresh Token

    public String getRedirectUri() {
        return redirect_uri;
    }

    public void setRedirectUri(String host) {
        this.redirect_uri = host;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void setRefreshToken(String refresh_token) {
        this.refresh_token = refresh_token;
    }


    /**
     * {@inheritDoc}
     */
    public void validate() {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("Parameter 'clientId' must not be blank.");
        }
        if (StringUtils.isBlank(clientSecret)) {
            throw new IllegalArgumentException("Parameter 'clientSecret' must not be blank.");
        }

        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Parameter 'username' must not be blank.");
        }
        if (StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("Parameter 'password' must not be blank.");
        }
    }

    public Form getAuthenticationForm() {

        Form form = new Form();
        form.add(SalesforceConnection.CLIENT_ID, getClientId());
        form.add(SalesforceConnection.CLIENT_SECRET, getClientSecret());

        if (StringUtils.isNotBlank(getRefreshToken())) {
            form.add(SalesforceConnection.GRANT_TYPE, SalesforceConnection.REFRESH_TOKEN);
            form.add(SalesforceConnection.REFRESH_TOKEN, refresh_token);
        } else if (StringUtils.isNotBlank(getUsername()) && StringUtils.isNotBlank(getPassword())&& StringUtils.isNotBlank(getSecurityToken())) {
            form.add(SalesforceConnection.GRANT_TYPE, SalesforceConnection.PASSWORD);

            form.add(SalesforceConnection.USERNAME, getUsername());
            form.add(SalesforceConnection.PASSWORD, getPassword()+getSecurityToken());
//            if (StringUtils.isNotBlank(scope)) {
//                form.add(SalesforceConnection.SCOPE, scope);
//            }
        }

        return form;
    }

}

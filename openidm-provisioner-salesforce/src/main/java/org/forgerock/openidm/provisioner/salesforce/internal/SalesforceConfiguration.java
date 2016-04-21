/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 ForgeRock AS. All Rights Reserved
 */
package org.forgerock.openidm.provisioner.salesforce.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.restlet.data.Form;

/**
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://wiki.developerforce.com/page/Digging_Deeper_into_OAuth_2.0_on_Force.com">Digging Deeper into OAuth 2.0 on Force.com</a>
 */
public class SalesforceConfiguration {

    private final static String LOGIN_URL = "https://login.salesforce.com/services/oauth2/token";
    public static final double API_VERSION = 29.0;

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
     * When accessing salesforce.com from outside of your company’s trusted
     * networks, you must add a security token to your password to log in to a
     * desktop client, such as Connect for Outlook, Connect Offline, Connect for
     * Office, Connect for Lotus Notes, or the Data Loader.
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

    // HTTP Connection

    private int connectTimeout = 0;
    private long idleCheckInterval = 0L;
    private long idleTimeout = 10000L;
    private int maxConnectionsPerHost = 10;
    private int maxTotalConnections = 20;
    private String proxyHost = null;
    private int proxyPort = 3128;
    private int socketTimeout = 0;
    private int stopIdleTimeout = 1000;
    private boolean tcpNoDelay = false;

    // Service

    private String loginUrl = null;

    private String instanceUrl = null;

    private double version = API_VERSION;

    public String getLoginUrl() {
        return loginUrl != null ? loginUrl : LOGIN_URL;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public double getVersion() {
        return version;
    }

    public void setVersion(double version) {
        this.version = version;
    }

    /**
     * Returns the connection timeout.
     *
     * @return The connection timeout.
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Time in milliseconds between two checks for idle and expired connections.
     * The check happens only if this property is set to a value greater than 0.
     *
     * @return A value indicating the idle connection check interval or 0 if a
     *         value has not been provided
     * @see #getIdleTimeout()
     */
    public long getIdleCheckInterval() {
        return idleCheckInterval;
    }

    public void setIdleCheckInterval(long idleCheckInterval) {
        this.idleCheckInterval = idleCheckInterval;
    }

    /**
     * Returns the time in ms beyond which idle connections are eligible for
     * reaping. The default value is 10000 ms.
     *
     * @return The time in millis beyond which idle connections are eligible for
     *         reaping.
     * @see #getIdleCheckInterval()
     */
    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * Returns the maximum number of connections that will be created for any
     * particular host.
     *
     * @return The maximum number of connections that will be created for any
     *         particular host.
     */
    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    /**
     * Returns the maximum number of active connections.
     *
     * @return The maximum number of active connections.
     */
    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    /**
     * Returns the host name of the HTTP proxy, if specified.
     *
     * @return the host name of the HTTP proxy, if specified.
     */
    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * Returns the port of the HTTP proxy, if specified, 3128 otherwise.
     *
     * @return the port of the HTTP proxy.
     */
    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * Returns the socket timeout value. A timeout of zero is interpreted as an
     * infinite timeout.
     *
     * @return The read timeout value.
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * Returns the minimum idle time, in milliseconds, for connections to be
     * closed when stopping the connector.
     *
     * @return The minimum idle time, in milliseconds, for connections to be
     *         closed when stopping the connector.
     */
    public int getStopIdleTimeout() {
        return stopIdleTimeout;
    }

    public void setStopIdleTimeout(int stopIdleTimeout) {
        this.stopIdleTimeout = stopIdleTimeout;
    }

    /**
     * Indicates if the protocol will use Nagle's algorithm
     *
     * @return True to enable TCP_NODELAY, false to disable.
     * @see java.net.Socket#setTcpNoDelay(boolean)
     */
    public boolean getTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    private final Map<String, String> predefinedQueries = new HashMap<String, String>();

    public Map<String, String> getPredefinedQueries() {
        return predefinedQueries;
    }

    public void setPredefinedQueries(Map<String, String> queries) {
        predefinedQueries.clear();
        predefinedQueries.putAll(queries);
    }

    public boolean queryIdExists(String queryId) {
        return queryId != null && predefinedQueries.containsKey(queryId);
    }

    public String getQueryExpression(String queryId) {
        return predefinedQueries.get(queryId);
    }

    /**
     * {@inheritDoc}
     */
    public SalesforceConfiguration validate() {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("Parameter 'clientId' must not be blank.");
        }
        if (StringUtils.isBlank(clientSecret)) {
            throw new IllegalArgumentException("Parameter 'clientSecret' must not be blank.");
        }

        if (StringUtils.isBlank(getRefreshToken())
                && (StringUtils.isBlank(username) || StringUtils.isBlank(password))) {
            throw new IllegalArgumentException("One of 'refreshToken' or 'username'/'password' is required.");
        }

        return this;
    }

    public Form getAuthenticationForm() {
        validate();
        Form form = new Form();
        form.add(SalesforceConnection.CLIENT_ID, getClientId());
        form.add(SalesforceConnection.CLIENT_SECRET, getClientSecret());

        if (null == getRefreshToken()) {
            form.add(SalesforceConnection.GRANT_TYPE, SalesforceConnection.PASSWORD);

            form.add(SalesforceConnection.USERNAME, getUsername());
            form.add(SalesforceConnection.PASSWORD, getPassword() + getSecurityToken());
        } else {
            form.add(SalesforceConnection.GRANT_TYPE, SalesforceConnection.REFRESH_TOKEN);

            form.add(SalesforceConnection.REFRESH_TOKEN, refresh_token);
        }

        return form;
    }

    static SalesforceConfiguration parseConfiguration(JsonValue config) {
        return SalesforceConnection.mapper.convertValue(
                config.required().expect(Map.class).asMap(),
                SalesforceConfiguration.class);
    }

}

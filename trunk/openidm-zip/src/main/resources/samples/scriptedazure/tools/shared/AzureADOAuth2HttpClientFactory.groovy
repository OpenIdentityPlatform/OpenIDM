/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.Header
import org.apache.http.HttpException
import org.apache.http.HttpHeaders
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils
import org.apache.olingo.client.api.communication.request.ODataBasicRequest
import org.apache.olingo.client.api.communication.response.ODataResponse
import org.apache.olingo.client.api.http.HttpMethod
import org.apache.olingo.client.core.communication.response.AbstractODataResponse
import org.apache.olingo.client.core.http.AbstractHttpClientFactory
import org.apache.olingo.client.core.http.OAuth2Exception
import org.identityconnectors.common.logging.Log

import java.lang.reflect.Field
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Shows how to work with OAuth 2.0 native applications protected by Azure Active Directory.
 * <a href="http://msdn.microsoft.com/en-us/library/azure/dn645542.aspx">More information</a>.
 */
public class AzureADOAuth2HttpClientFactory extends AbstractHttpClientFactory implements Closeable {


    private final String clientId;

    private final String clientSecret;

    private final String redirectURI;

    private final String resourceURI;

    private final UsernamePasswordCredentials creds;

    private final URI oauth2GrantServiceURI;

    private final URI oauth2TokenServiceURI;

    private final CloseableHttpClient httpClient;

    private final CloseableHttpClient azureClient;

    private final Lock lock = new ReentrantLock();
    
    private final IdleConnectionMonitorThread connectionMonitorThread;

    public static final ThreadLocal<ODataResponse> currentResponse = new ThreadLocal<ODataResponse>();

    private ObjectNode token;

    private Long expiresIn = null

    public static final String HEADER_PREFIX = "Bearer ";

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()


    public AzureADOAuth2HttpClientFactory(final String authority, final String refreshToken) {
        this(authority, null, null, null, null, null)
        token = new ObjectNode(null, [refresh_token: new TextNode(refreshToken)])
        this.refreshToken(httpClient)
    }

    public AzureADOAuth2HttpClientFactory(final String authority, final String clientId, final String clientSecret,
                                          final String redirectURI,
                                          final String resourceURI, final UsernamePasswordCredentials creds) {
        super();

        this.oauth2GrantServiceURI = URI.create(authority + "/oauth2/authorize");
        this.oauth2TokenServiceURI = URI.create(authority + "/oauth2/token");
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectURI = redirectURI;
        this.resourceURI = resourceURI;
        this.creds = creds;

        //Create a client to retrieve OAuth2 token
        HttpClientBuilder builder = HttpClients.custom();
        builder.setUserAgent(USER_AGENT)
        this.httpClient = builder.build();

        //Create Azure Graph API Client
        if (null != creds) {
            init();
        }
        builder.addInterceptorFirst(new HttpRequestInterceptor() {

            @Override
            public void process(
                    final HttpRequest request, final HttpContext context) throws HttpException, IOException {
                lock.lock();

                try {
                    // check if token will expire in a minute
                    if (token == null || expiresIn != null && expiresIn - System.currentTimeMillis() <= 60000) {
                        refreshToken(httpClient);
                        if (token == null) {
                            // nothing we can do without an access token
                            return;
                        }
                    }
                    request.removeHeaders(HttpHeaders.AUTHORIZATION);
                    request.addHeader(HttpHeaders.AUTHORIZATION, HEADER_PREFIX + token.get("access_token").asText());
                } finally {
                    lock.unlock();
                }
            }
        });

        builder.setRetryHandler(new HttpRequestRetryHandler() {
            boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (DefaultHttpRequestRetryHandler.INSTANCE.retryRequest(exception, executionCount, context)) {

                    final HttpClientContext clientContext = HttpClientContext.adapt(context);
                    final HttpRequest request = clientContext.getRequest();
                    final HttpResponse response = clientContext.getResponse();
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                        try {
                            lock.lock();
                            try {
                                // need to check if another thread has already refreshed the token
                                return !Objects.equals(token.get("access_token").asText(), getAccessTokenFromRequest(request)) || refreshToken(httpClient);
                            } finally {
                                lock.unlock();
                            }
                        } catch (IOException e) {
                            Log.getLog(getClass()).warn(e, "unable to refresh token")
                        }
                    }
                }
            }


            public String getAccessTokenFromRequest(HttpRequest request) {
                Header[] authorizationAsList = request.getHeaders(HttpHeaders.AUTHORIZATION);
                if (authorizationAsList != null) {
                    for (Header header : authorizationAsList) {
                        if (header.value.startsWith(HEADER_PREFIX)) {
                            return header.value.substring(HEADER_PREFIX.length());
                        }
                    }
                }
                return null;
            }
        })

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);
        // Increase max connections for httpHost to 50
        cm.setMaxPerRoute(new HttpRoute(new HttpHost("graph.windows.net", 443, "https")), 50);

        builder.setConnectionManager(cm)

        connectionMonitorThread = new IdleConnectionMonitorThread(cm)
        
        connectionMonitorThread.start()

        azureClient = builder.build();
        
    }

    private void fetchAccessToken(final HttpClient httpClient, final List<BasicNameValuePair> data) {
        token = null;

        InputStream tokenResponse = null;
        try {
            final HttpPost post = new HttpPost(oauth2TokenServiceURI);
            post.setEntity(new UrlEncodedFormEntity(data, "UTF-8"));

            final HttpResponse response = httpClient.execute(post);

            tokenResponse = response.getEntity().getContent();
            token = (ObjectNode) OBJECT_MAPPER.readTree(tokenResponse);

            if (token.has("error")) {
                String error = token.get("error").asText();
                throw new IllegalStateException(token.has("error_description") ? token.get("error_description").asText(error) : error)
            }

            //expiresIn = System.currentTimeMillis() + Integer.parseInt(token.get("expires_in").asText()) * 1000

            expiresIn = Long.parseLong(token.get("expires_on").asText()) * 1000
        } catch (Exception e) {
            throw new OAuth2Exception(e);
        } finally {
            IOUtils.closeQuietly(tokenResponse);
        }
    }

    protected void init() throws OAuth2Exception {

        // 1. access the OAuth2 grant service (with authentication)
        String code = null;
        try {
            final URIBuilder builder = new URIBuilder(oauth2GrantServiceURI).
                    addParameter("response_type", "code").
                    addParameter("client_id", clientId).
            //addParameter("redirect_uri", redirectURI).
                    addParameter("api-version", "1.0");


            HttpResponse response = httpClient.execute(new HttpGet(builder.build()));

            final String loginPage = EntityUtils.toString(response.getEntity());

            String postURL = StringUtils.substringBefore(
                    StringUtils.substringAfter(loginPage, "<form id=\"credentials\" method=\"post\" action=\""),
                    "\">");
            final String ppsx = StringUtils.substringBefore(
                    StringUtils.substringAfter(loginPage, "<input type=\"hidden\" id=\"PPSX\" name=\"PPSX\" value=\""),
                    "\"/>");
            final String ppft = StringUtils.substringBefore(
                    StringUtils.substringAfter(loginPage, "<input type=\"hidden\" name=\"PPFT\" id=\"i0327\" value=\""),
                    "\"/>");

            List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
            data.add(new BasicNameValuePair("login", creds.getUserName()));
            data.add(new BasicNameValuePair("passwd", creds.getPassword()));
            data.add(new BasicNameValuePair("PPSX", ppsx));
            data.add(new BasicNameValuePair("PPFT", ppft));

            HttpPost post = new HttpPost(postURL);
            post.setEntity(new UrlEncodedFormEntity(data, "UTF-8"));

            response = httpClient.execute(post);

            final String samlPage = EntityUtils.toString(response.getEntity());

            postURL = StringUtils.substringBefore(
                    StringUtils.substringAfter(samlPage, "<form name=\"fmHF\" id=\"fmHF\" action=\""),
                    "\" method=\"post\" target=\"_top\">");
            final String wctx = StringUtils.substringBefore(
                    StringUtils.substringAfter(samlPage, "<input type=\"hidden\" name=\"wctx\" id=\"wctx\" value=\""),
                    "\">");
            final String wresult = StringUtils.substringBefore(StringUtils.substringAfter(samlPage,
                    "<input type=\"hidden\" name=\"wresult\" id=\"wresult\" value=\""), "\">");
            final String wa = StringUtils.substringBefore(
                    StringUtils.substringAfter(samlPage, "<input type=\"hidden\" name=\"wa\" id=\"wa\" value=\""),
                    "\">");

            data = new ArrayList<BasicNameValuePair>();
            data.add(new BasicNameValuePair("wctx", wctx));
            data.add(new BasicNameValuePair("wresult", wresult.replace("&quot;", "\"")));
            data.add(new BasicNameValuePair("wa", wa));

            post = new HttpPost(postURL);
            post.setEntity(new UrlEncodedFormEntity(data, "UTF-8"));

            response = httpClient.execute(post);

            final Header locationHeader = response.getFirstHeader("Location");
            if (response.getStatusLine().getStatusCode() != 302 || locationHeader == null) {
                throw new OAuth2Exception("Unexpected response from server");
            }

            final String[] oauth2Info = StringUtils.split(
                    StringUtils.substringAfter(locationHeader.getValue(), "?"), '&');
            code = StringUtils.substringAfter(oauth2Info[0], "=");

            EntityUtils.consume(response.getEntity());
        } catch (Exception e) {
            throw new OAuth2Exception(e);
        }

        if (code == null) {
            throw new OAuth2Exception("No OAuth2 grant");
        }

        // 2. ask the OAuth2 token service
        final List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
        data.add(new BasicNameValuePair("grant_type", "authorization_code"));
        data.add(new BasicNameValuePair("code", code));
        data.add(new BasicNameValuePair("client_id", clientId));
        data.add(new BasicNameValuePair("client_secret", clientSecret));
        //data.add(new BasicNameValuePair("redirect_uri", redirectURI));
        //data.add(new BasicNameValuePair("resource", resourceURI));
        data.add(new BasicNameValuePair("resource", "https://graph.windows.net"));


        fetchAccessToken(httpClient, data);

        if (token == null) {
            throw new OAuth2Exception("No OAuth2 access token");
        }
    }

    protected boolean refreshToken(final HttpClient client) throws OAuth2Exception {
        final List<BasicNameValuePair> data = new ArrayList<BasicNameValuePair>();
        data.add(new BasicNameValuePair("grant_type", "refresh_token"));
        data.add(new BasicNameValuePair("refresh_token", token.get("refresh_token").asText()));

        fetchAccessToken(client, data);

        if (token == null) {
            throw new OAuth2Exception("No OAuth2 refresh token");
        }
        return true
    }

    HttpClient create(HttpMethod method, URI uri) {
        return azureClient
    }

    void close(HttpClient httpClient) {
        //The 4.3 API should close the response
        def response = currentResponse.get();
        if (response instanceof AbstractODataResponse) {
            //Access protected res variable
            try {
                Field f = AbstractODataResponse.getDeclaredField("res");
                f.setAccessible(true);                                                
                def httpResponse = f.get(response)
                if (httpResponse instanceof Closeable) {
                    ((Closeable) httpResponse).close()
                }
            } catch (Exception e) {
                e.printStackTrace()
            }
            currentResponse.remove()
        }
    }

    void close() throws IOException {
        azureClient.close()
        httpClient.close()
        if (null != connectionMonitorThread){
            connectionMonitorThread.shutdown()
        }
    }

    public static <T extends ODataResponse> T execute(ODataBasicRequest<T> request) {
        def response = request.execute();
        currentResponse.set(response)
        return response;
    }

    public static class IdleConnectionMonitorThread extends Thread {

        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown;

        public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(5000);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Optionally, close connections
                        // that have been idle longer than 30 sec
                        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

    }
}

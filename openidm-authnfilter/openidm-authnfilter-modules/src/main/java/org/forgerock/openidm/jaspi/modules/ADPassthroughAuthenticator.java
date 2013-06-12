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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openidm.jaspi.modules;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResource;
import org.forgerock.json.resource.JsonResourceAccessor;
import org.forgerock.json.resource.JsonResourceContext;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.http.ContextRegistrator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ADPassthroughAuthenticator {

    final static Logger logger = LoggerFactory.getLogger(ADPassthroughAuthenticator.class);

    private final String passThroughAuth;
    private final String userRolesProperty;
    private final List<String> defaultRoles;

    public ADPassthroughAuthenticator(String passThroughAuth, String userRolesProperty, List<String> defaultRoles) {
        this.passThroughAuth = passThroughAuth;
        this.userRolesProperty = userRolesProperty;
        this.defaultRoles = defaultRoles;
    }

    public AuthData authenticate(AuthData authData, String password) {

        if (!StringUtils.isEmpty(passThroughAuth) && !"anonymous".equals(authData.username)) {
            JsonResource router = getJsonResource();
            if (null != router) {
                JsonResourceAccessor accessor = new JsonResourceAccessor(router, JsonResourceContext.getContext(JsonResourceContext.newRootContext(), "resource"));

                JsonValue params = new JsonValue(new HashMap<String, Object>());
                params.put(ServerConstants.ACTION_NAME, "authenticate");
                params.put("username", authData.username);
                params.put("password", password);
                try {
                    JsonValue result  = accessor.action(passThroughAuth ,params, null);
                    boolean authenticated = result.isDefined(ServerConstants.OBJECT_PROPERTY_ID);
                    if (authenticated) {
                        //This is what I was talking about. We don't have a way to populate this. Use script to overcome it
                        //authData.roles = Arrays.asList(new String[]{"openidm-admin", "openidm-authorized"});
                        authData.resource = passThroughAuth;
                        authData.userId = result.get(ServerConstants.OBJECT_PROPERTY_ID).required().asString();
                        authData.status = authenticated;

                        result  = accessor.read(passThroughAuth + "/" + authData.userId);

                        if (userRolesProperty != null && result.isDefined(userRolesProperty)) {

                            authData.roles = (List) result.get(userRolesProperty).getObject();
                            if (authData.roles.size() == 0) {
                                authData.roles.addAll(defaultRoles);
                            }
                        }

                        return authData;
                    }
                } catch (JsonResourceException e) {
                    logger.trace("Failed pass-through authentication of {} on {}.",
                            authData.username, passThroughAuth, e);
                    //authentication failed
                }
            }
        }

        return authData;
    }

    private JsonResource getJsonResource() {
        // TODO: switch to service trackers
        BundleContext ctx = ContextRegistrator.getBundleContext();
        Collection<ServiceReference<JsonResource>> routers = null;
        try {
            routers = ctx.getServiceReferences(JsonResource.class, "(openidm.restlet.path=/)");
        } catch (InvalidSyntaxException e) {
            /* ignore, the filter is tested */
        }
        return routers.size() > 0 ? ctx.getService(routers.iterator().next()) : null;
    }
}

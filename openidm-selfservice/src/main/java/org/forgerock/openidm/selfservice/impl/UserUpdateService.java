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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openidm.selfservice.impl;

import static org.forgerock.json.resource.ResourcePath.resourcePath;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.router.IDMConnectionFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.service.component.ComponentContext;

/**
 * This service supports self-service updates of user details; namely, KBA answers.
 */
@Component(name = UserUpdateService.PID, immediate = true, policy = ConfigurationPolicy.IGNORE)
@Service
@Properties({
        @Property(name = "service.description", value = "OpenIDM SelfService User-Update"),
        @Property(name = "service.vendor", value = "ForgeRock AS"),
        @Property(name = "openidm.router.prefix", value = UserUpdateService.ROUTER_PATH)
})
public class UserUpdateService implements CollectionResourceProvider {
    static final String PID = "org.forgerock.openidm.selfservice.userupdate";

    static final String ROUTER_PATH = SelfService.ROUTER_PREFIX + "/user";

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC)
    protected IDMConnectionFactory connectionFactory;

    /** The KBA Configuration. */
    @Reference(policy = ReferencePolicy.STATIC)
    private KbaConfiguration kbaConfiguration;

    private org.forgerock.selfservice.core.UserUpdateService userUpdateService;

    @Activate
    void activate(ComponentContext context) throws Exception {
        userUpdateService = new org.forgerock.selfservice.core.UserUpdateService(
                /* Provide the Self-Service UserUpdateService with a ConnectionFactory that appears as "external".
                 * This is necessary because we want external user-kba patch requests that originate here to be fed to
                 * the decorated org.forgerock.selfservice.core.UserUpdateService and subsequently to managed/user to
                 * be handled the same as if they had been made against managed/user directly.
                 */
                new ConnectionFactory() {
                    @Override
                    public void close() {
                        connectionFactory.close();
                    }

                    @Override
                    public Connection getConnection() throws ResourceException {
                        return connectionFactory.getExternalConnection();
                    }

                    @Override
                    public Promise<Connection, ResourceException> getConnectionAsync() {
                        return connectionFactory.getExternalConnectionAsync();
                    }
                },
                resourcePath("managed/user"),
                new JsonPointer(kbaConfiguration.getConfig().get("kbaPropertyName").asString()));
    }

    @Deactivate
    void deactivate(ComponentContext compContext) {
        userUpdateService = null;
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return userUpdateService.actionCollection(context, request);
    }

    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId, ActionRequest request) {
        return userUpdateService.actionInstance(context, resourceId, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return userUpdateService.createInstance(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, String resourceId, DeleteRequest request) {
        return userUpdateService.deleteInstance(context, resourceId, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId, PatchRequest request) {
        return userUpdateService.patchInstance(context, resourceId, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, QueryRequest request, QueryResourceHandler handler) {
        return userUpdateService.queryCollection(context, request, handler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, String resourceId, ReadRequest request) {
        return userUpdateService.readInstance(context, resourceId, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId, UpdateRequest request) {
        return userUpdateService.updateInstance(context, resourceId, request);
    }
}

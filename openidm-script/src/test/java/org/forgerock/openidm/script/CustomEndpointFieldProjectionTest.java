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
 * Copyright 2026 3A Systems LLC.
 */

package org.forgerock.openidm.script;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Resources;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.Test;

/**
 * Reproduces the custom-endpoint field-projection bug discussed in
 * <a href="https://github.com/OpenIdentityPlatform/OpenIDM/discussions/183">discussion #183</a>.
 *
 * <p>A custom (scripted) endpoint returns the full object without explicitly setting the
 * response fields (see {@code ScriptedRequestHandler.evaluate()} which calls
 * {@code newResourceResponse(id, null, resultJson)} without {@code addField(...)}). As a
 * consequence the generic CREST field projection in
 * {@code org.forgerock.json.resource.Resources.filterResource(JsonValue, Collection)} is applied
 * to the raw result using {@code request.getFields()}.
 *
 * <p>That generic projection flattens every requested {@link org.forgerock.json.JsonPointer}
 * down to its {@code leaf()} name when building the filtered output. When two requested fields
 * share the same leaf name on different nesting levels (e.g. {@code userName} and
 * {@code manager/userName}), the nested one overwrites the top-level one, so the top-level
 * {@code userName} ends up containing the manager's {@code userName}.
 *
 * <p>This test asserts the <b>correct</b> behaviour. It therefore <b>fails against the buggy
 * commons {@code json-resource} (3.1.1-SNAPSHOT)</b> and is expected to pass once the generic
 * projection is fixed to preserve the pointer structure instead of collapsing to the leaf name.
 */
public class CustomEndpointFieldProjectionTest {

    private static final String USER_NAME = "bjensen";
    private static final String MANAGER_USER_NAME = "jdoe";

    /**
     * A request handler that mimics a custom scripted endpoint: it returns the full object on
     * read and does <b>not</b> set the response fields (no {@code addField(...)}), exactly like
     * {@code ScriptedRequestHandler.evaluate()}.
     */
    private static final class FullObjectEndpoint extends AbstractRequestHandler {
        @Override
        public Promise<ResourceResponse, ResourceException> handleRead(final Context context,
                final ReadRequest request) {
            final JsonValue content = json(object(
                    field("_id", "user1"),
                    field("userName", USER_NAME),
                    field("givenName", "Barbara"),
                    field("sn", "Jensen"),
                    field("description", "Example user"),
                    field("manager", object(
                            field("_id", "user2"),
                            field("userName", MANAGER_USER_NAME),
                            field("givenName", "John"),
                            field("sn", "Doe")))));
            return newResourceResponse("user1", null, content).asPromise();
        }
    }

    @Test
    public void topLevelFieldIsNotOverwrittenByNestedFieldWithSameLeafName() throws Exception {
        final Connection connection =
                Resources.newInternalConnection(new FullObjectEndpoint());

        // Request the same leaf name on two nesting levels: userName and manager/userName.
        final ReadRequest request = Requests.newReadRequest("managed/user/user1")
                .addField("description", "userName", "givenName", "sn", "manager", "manager/userName");

        final ResourceResponse response = connection.read(new RootContext(), request);
        final JsonValue content = response.getContent();

        // The top-level userName must remain the user's own userName...
        assertThat(content.get("userName").asString())
                .as("top-level userName must not be overwritten by manager/userName")
                .isEqualTo(USER_NAME);

        // ...and the manager's userName must be nested under manager.
        assertThat(content.get(new org.forgerock.json.JsonPointer("manager/userName")).asString())
                .as("manager/userName must be projected into the nested manager object")
                .isEqualTo(MANAGER_USER_NAME);
    }
}


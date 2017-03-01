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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openidm.repo.opendj.impl;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.services.context.Context;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenDJRepoServiceTest {

    private OpenDJRepoService repoService;

    @BeforeMethod
    public void setup() {
        this.repoService = new OpenDJRepoService();
    }

//    @Test
//    public void testWildcardGenericMappings() throws LdapException {
//        // given
//        // repo service with wildcard mapping
//        final ConnectionFactory ldapFactory = mock(ConnectionFactory.class);
//        final Connection connection = mock(Connection.class);
//        final OpenDJRepoService repo = (OpenDJRepoService) OpenDJRepoService.getRepoBootService(ldapFactory,
//                json(object(
//                        field("queries", object()),
//                        field("resourceMapping", object(
//                                field("defaultMapping", object(
//                                        field("resource", "default")
//                                )),
//                                field("genericMapping", object(
//                                        field("test/*", object(
//                                                field("resource", "test")
//                                        ))
//                                ))
//                        ))
//                )));
//
//        // when
//        when(ldapFactory.getConnection()).thenReturn(connection);
//        repo.handleQuery(mock(Context.class), newQueryRequest("/test/foo/bar").setQueryId("query-all-ids"), mock(QueryResourceHandler.class));
//        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
//
//        // then
//    }
}

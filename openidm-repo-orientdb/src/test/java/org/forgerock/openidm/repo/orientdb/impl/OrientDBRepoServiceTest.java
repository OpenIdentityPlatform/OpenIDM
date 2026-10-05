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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openidm.repo.orientdb.impl;

import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.testng.annotations.Test;

/**
 * Tests the request guards of {@link OrientDBRepoService} that run before any database access.
 */
public class OrientDBRepoServiceTest {

    // The message pins the revision guard: without it an empty revision reaches
    // DocumentUtil.parseVersion, which also throws ConflictException, but with another message.
    @Test(expectedExceptions = ConflictException.class,
            expectedExceptionsMessageRegExp = ".*does not have revision it expects set.*")
    public void deleteWithEmptyRevisionIsRejected() throws ResourceException {
        new OrientDBRepoService().delete(Requests.newDeleteRequest("managed/user", "1").setRevision(""));
    }

    @Test(expectedExceptions = ConflictException.class,
            expectedExceptionsMessageRegExp = ".*does not have revision it expects set.*")
    public void deleteWithoutRevisionIsRejected() throws ResourceException {
        new OrientDBRepoService().delete(Requests.newDeleteRequest("managed/user", "1"));
    }
}

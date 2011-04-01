/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.repo.orientdb.impl;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;

import org.testng.annotations.*;
import static org.testng.Assert.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class DBHelperTest {

    @Test
    public void initPoolTest() {
        String dbURL = "local:./target/testdb";
        String user = "admin";
        String password = "admin";
        int minSize = 5;
        int maxSize = 20;
        ODatabaseDocumentPool pool = DBHelper.initPool(dbURL, user, password, minSize, maxSize);
        assertNotNull(pool);
        ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
        assertNotNull(db);
        pool.release(db);
        pool.close();
    }

}
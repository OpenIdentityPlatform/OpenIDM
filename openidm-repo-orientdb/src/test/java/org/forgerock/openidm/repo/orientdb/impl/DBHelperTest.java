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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;

import org.forgerock.json.JsonValue;
import org.testng.annotations.*;

public class DBHelperTest {

    private String dbURL = "plocal:./target/testdb";
    private String user = "admin";
    private String password = "admin";
    private int minSize = 5;
    private int maxSize = 20;

    @Test
    public void initPoolTest() throws Exception {
        ODatabaseDocumentPool pool = DBHelper.getPool(dbURL, user, password, minSize, maxSize, json(object()), true);
        assertThat(pool).isNotNull();
        ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
        assertThat(db).isNotNull();
        db.drop();
        db.close();
        DBHelper.closePools();
    }
    
    @Test
    public void updateDbCredentialsTest() throws Exception {
        String newUser = "user1";
        String newPassword = "pass1";
        JsonValue completeConfig = json(object());
        int minSize = 5;
        int maxSize = 20;
        ODatabaseDocumentPool pool = DBHelper.getPool(dbURL, user, password, minSize, maxSize, completeConfig, true);
        assertThat(pool).isNotNull();
        DBHelper.updateDbCredentials(dbURL, user, password, newUser, newPassword);
        pool = DBHelper.getPool(dbURL, newUser, newPassword, minSize, maxSize, completeConfig, true);
        assertThat(pool).isNotNull();
        ODatabaseDocumentTx db = pool.acquire(dbURL, newUser, newPassword);
        assertThat(db).isNotNull();
        db.drop();
        db.close();
        DBHelper.closePools();
    }

}

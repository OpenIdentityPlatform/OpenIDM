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
package org.forgerock.openidm.repo.jdbc.internal;

import javax.naming.InitialContext;

import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * @version $Revision$ $Date$
 */
public class JDBCRepoServiceTest {

    private static IDataSet ds;
    private static DBUnitUtils utils;
    private static InitialContext ctx;

    @BeforeClass
    public static void init() {
        try {
            ctx = new InitialContext();
            utils = new DBUnitUtils(ctx);
            ds = utils.createFlatXml(JDBCRepoServiceTest.class, "JDBCRepoServiceTest/OpenIDMDataSeed.xml");
            DatabaseOperation.CLEAN_INSERT.execute(utils.getConnection(), ds);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    @AfterClass
    public static void dispose() {
        try {
            DatabaseOperation.DELETE_ALL.execute(utils.getConnection(), ds);
            utils.dispose();
            ctx.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //When delete a non existing object
    //@Test(expectedExceptions = NotFoundException.class)
    public void testDelete() throws Exception {

    }


    //@Test
    public void generateXml() throws Exception {
        IDataSet ds = utils.createQueryDatasetFromMaster("configobjects", "SELECT * FROM openidm.configobjects");
        ((QueryDataSet) ds).addTable("configobjectproperties", "SELECT * FROM openidm.configobjectproperties");
        ((QueryDataSet) ds).addTable("genericobjects", "SELECT * FROM openidm.genericobjects");
        ((QueryDataSet) ds).addTable("genericobjectproperties", "SELECT * FROM openidm.genericobjectproperties");
        ((QueryDataSet) ds).addTable("managedobjects", "SELECT * FROM openidm.managedobjects");
        ((QueryDataSet) ds).addTable("managedobjectproperties", "SELECT * FROM openidm.managedobjectproperties");
        ((QueryDataSet) ds).addTable("objecttypes", "SELECT * FROM openidm.objecttypes");
        utils.writeFlatXmlFile(ds, "OpenIDMDataSeed.xml", getClass());
    }
}

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ResourceException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepoServiceFunctionalTestBase;
import org.forgerock.openidm.repo.orientdb.impl.DocumentUtil;
import org.forgerock.testutil.osgi.ContainerUtil;

import org.testng.annotations.*;

import static org.testng.Assert.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

@Test(groups = {"jdbc-repo"})
public class JDBCRepoServiceFunctionalTest extends RepoServiceFunctionalTestBase {

    public JDBCRepoServiceFunctionalTest() {
        super("./target/openidm-jdbc-pkg/openidm", "./src/it/resources/conf/jdbc-test", "JDBC");
    }

    @BeforeClass(dependsOnMethods={"activateService"})
    public void clearDb() throws ResourceException {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put(QueryConstants.QUERY_ID, "query-all-idsandrev");
        Map result = repo.query("managed/user", params);
        List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get(QueryConstants.QUERY_RESULT);
        if (resultSet != null) {
            for (Map<String, Object> entry : resultSet) {
                String id = (String) entry.get(DocumentUtil.TAG_ID);
                String rev = (String) entry.get(DocumentUtil.TAG_REV);
                //logger.debug("Clearing db, deleting " + id + " rev: " + rev);
                repo.delete("managed/user/" + id, rev);
            }
        }
    }

/*
    //@Test(groups = {"jdbc-repo"})
    @SuppressWarnings("unchecked")
    public void insertPerformance() throws ResourceException {
        java.util.Random random = new java.util.Random();
        int testSize = 10000;

        Object[][] objToInsert = new Object[testSize][2];
        for (int count = 0; count < testSize; count++) {

            char randomChar1 = (char)(random.nextInt(26) + 'a');
            char randomChar2 = (char)(random.nextInt(26) + 'a');
            String randomPrefix = "" + randomChar1 + randomChar2;

            String uuid = randomPrefix + "perftest-c0-66e0-11e0-ae3e" + count;
            String id = "managed/user/" + uuid;
            Map<String, Object> obj = new java.util.HashMap<String, Object>();
            obj.put("firstname", randomPrefix + "hnathan" + count);
            obj.put("lastname", randomPrefix + "mbat" + count);
            obj.put("maidenname", randomPrefix + "iser" + count);
            obj.put("business", randomPrefix + "me company" + count);
            obj.put("homephone", randomPrefix + "5-123-" + count);
            obj.put("mobilephone", randomPrefix + "8-999-" + count);
            obj.put("businessphone", randomPrefix + "8-777-" + count);
            obj.put("email", randomPrefix + "me.email@abcdef" + count + ".com");
            obj.put("businessemail", randomPrefix + "other.mail@acme" + count + ".com");
            obj.put("contactpreference", randomPrefix + "il");

            Map addresses = new HashMap<String, Object>();
            Map homeAddress = new HashMap<String, Object>();
            addresses.put("home", homeAddress);
            homeAddress.put("street", randomPrefix + "urbon street" + count);
            homeAddress.put("city", randomPrefix + "w York");
            homeAddress.put("zip", randomPrefix + count);
            Map workAddress = new HashMap();
            addresses.put("business", workAddress);
            workAddress.put("street", randomPrefix + "ck road");
            workAddress.put("city", randomPrefix + "s Angeles");
            workAddress.put("zip", randomPrefix + count);
            addresses.put("business", workAddress);
            Map legalAddress = new HashMap();
            legalAddress.put("street", randomPrefix + "in street");
            legalAddress.put("city", randomPrefix + ". Peters");
            legalAddress.put("zip", randomPrefix + + count);
            obj.put("addresses", addresses);

            List<String> roles = new ArrayList<String>();
            roles.add("accounting");
            roles.add("admin");
            obj.put("roles", roles);
            objToInsert[count][0] = id;
            objToInsert[count][1] = obj;
        }
        try {
            Thread.sleep(2000);

            for (int loop = 0; loop < 100; loop++) {
                String postfix = Integer.toString(loop);
                System.out.println("Performance test starting the insert of " + testSize + " documents");
                int noOfThreads = 5;
                Thread[] threads = new Thread[noOfThreads];
                int chunkSize = testSize / noOfThreads;
                int rangeStart = 0;
                for (int threadCnt = 0; threadCnt < noOfThreads; threadCnt++) {
                    threads[threadCnt] = createThread(postfix, loop, objToInsert, rangeStart, rangeStart + chunkSize);
                    rangeStart += chunkSize;
                }
                long start = System.currentTimeMillis();
                for (int threadCnt = 0; threadCnt < noOfThreads; threadCnt++) {
                    threads[threadCnt].start();
                }
                for (int threadCnt = 0; threadCnt < noOfThreads; threadCnt++) {
                    threads[threadCnt].join();
                }
                long end = System.currentTimeMillis();
                System.out.println("Inserts of " + testSize + " documents took " + (end-start) + " ms = " + 1000f/((float)(end-start)/(float)testSize) + "msg/s");
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected exception " + ex.getMessage(), ex);
        }
    }

    Thread createThread(final String postfix, final int loop, final Object[][] objToInsert, final int rangeStart, final int rangeEnd) {
        Thread aThread =  new Thread() {
            public void run() {
                try {
                    for (int count = rangeStart; count < rangeEnd; count++) {
                        repo.create((String) objToInsert[count][0]+ "-" + postfix + "-" + loop, (Map<String, Object>) objToInsert[count][1]);
                    }
                    System.out.println("Thread complete for range " + rangeStart + "-" + rangeEnd);
                } catch (Exception ex) {
                    System.out.println("Performance test thread failure for range " + rangeStart + "-" + rangeEnd + " " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };
        return aThread;
    }
*/
    @Test
    public void createAndRead() throws ResourceException {
        super.createAndRead();
    }

    @Test(expectedExceptions = PreconditionFailedException.class)
    public void duplicateCreate() throws ResourceException {
        super.duplicateCreate();
    }

    @Test()
    public void updateCurrentObject() throws ResourceException {
        super.updateCurrentObject();
    }

    @Test()
    public void updateCurrentObjectTypeChange() throws ResourceException {
        super.updateCurrentObjectTypeChange();
    }

    @Test(expectedExceptions = PreconditionFailedException.class)
    public void updateChangedObject() throws ResourceException {
        super.updateChangedObject();
    }


    @Test()
    public void deleteCurrentObject() throws ResourceException {
        super.deleteCurrentObject();
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void deleteMissingObject() throws ResourceException {
        super.deleteMissingObject();
    }

    @Test(expectedExceptions = PreconditionFailedException.class)
    public void deleteChangedObject() throws ResourceException {
        super.deleteChangedObject();
    }

    @Test()
    public void populateQueryData() throws ResourceException {
        super.populateQueryData();
    }

    @Test(enabled = false, dependsOnMethods = {"populateQueryData"})
    public void inlineQuery() throws ResourceException {
        super.inlineQuery();
    }

    @Test(enabled = false, dependsOnMethods = {"populateQueryData"})
    public void inlineQueryWithWhereToken() throws ResourceException {
        super.inlineQueryWithWhereToken();
    }

    // Enable this test once the OrientDB parser validation fix has propagated.
    @Test(enabled = false, dependsOnMethods = {"populateQueryData"})
    public void inlineQueryWithNonSpaceWhereToken() throws ResourceException {
        super.inlineQueryWithNonSpaceWhereToken();
    }

    @Test(enabled = false, dependsOnMethods = {"populateQueryData"})
    public void inlineQueryWithWildcardWhereToken() throws ResourceException {
        super.inlineQueryWithWildcardWhereToken();
    }

    @Test(enabled = false, dependsOnMethods = {"populateQueryData"})
    public void inlineQueryWithFromToken() throws ResourceException {
        super.inlineQueryWithFromToken();
    }

    @Test(enabled = false, dependsOnMethods = {"populateQueryData"})
    public void inlineQueryWithWhereNameToken() throws ResourceException {
        super.inlineQueryWithWhereNameToken();
    }

    @Test(dependsOnMethods = {"populateQueryData"})
    public void configuredQuery() throws ResourceException {
        super.configuredQuery();
    }

    @Test(dependsOnMethods = {"populateQueryData"})
    public void configuredQueryWithWhereToken() throws ResourceException {
        super.configuredQueryWithWhereToken();
    }


    @Test(dependsOnMethods = {"populateQueryData"})
    public void configuredQueryWithFromToken() throws ResourceException {
        super.configuredQueryWithFromToken();
    }

    @Test(dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    public void invalidQueryId() throws ResourceException {
        super.invalidQueryId();
    }

    @Test(enabled = false, dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    public void invalidQueryExpression() throws ResourceException {
        String queryExpression = "select * from unknown";
        //super.invalidQueryExpression(queryExpression);
    }

    @Test(enabled = false, dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    public void invalidQueryExpression2() throws ResourceException {
        String queryExpression = "Sselect * Ffrom unknown";
        //super.invalidQueryExpression2(queryExpression);
    }

    @Test(enabled = false, dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    public void missingQueryToken() throws ResourceException {
        String queryExpression = "select * from ${_resource} where lastname = ${undefined-token";
        //super.missingQueryToken(queryExpression);
    }

    @Test(dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    public void missingQueryIdOrExpression() throws ResourceException {
        super.missingQueryIdOrExpression();

    }
}
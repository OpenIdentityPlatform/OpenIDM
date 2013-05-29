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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ResourceException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.RepositoryService;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.testutil.osgi.ContainerUtil;

import org.testng.annotations.*;

import static org.testng.Assert.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class RepoServiceFunctionalTest {

    // Utility for integration testing with embedded OSGi container
    ContainerUtil containerUtil;
    RepositoryService repo;

    @BeforeClass
    public void activateService() {
        // Starts and installs all package bundles
        String idmRootDir = "./target/openidm-orientdb-pkg/openidm";
        String bundleDirs = "./target/openidm-orientdb-pkg/openidm/bundle/init,./target/openidm-orientdb-pkg/openidm/bundle";
        String configDir = "./src/it/resources/conf/orientdb-test";
        Map<String, String> systemProps = new HashMap<String, String>();
        systemProps.put("openidm.system.server.root", idmRootDir);
        containerUtil = ContainerUtil.startContainer(bundleDirs, configDir, systemProps);
        // Waits for the service to appear
        repo = containerUtil.getService(RepositoryService.class, "(db.type=OrientDB)");
    }

    @AfterClass
    public void cleanup() {
        if (containerUtil != null) {
            containerUtil.stop();
        }
    }

    @Test(groups = {"repo"})
    @SuppressWarnings("unchecked")
    public void createAndRead() throws ResourceException {
        String uuid = "acf01ec0-66e0-11e0-ae3e-0800200c9a66";
        String id = "managed/user/" + uuid;
        Map<String, Object> obj = new java.util.HashMap<String, Object>();
        obj.put("firstname", "Johnathan");
        obj.put("lastname", "Wombat");
        obj.put("age", 100);
        obj.put("temperature", -44);
        obj.put("longnumber", Long.MAX_VALUE);
        obj.put("amount", 7654321.98765d);
        obj.put("present", true);

        Map addresses = new HashMap<String, Object>();
        Map homeAddress = new HashMap<String, Object>();
        addresses.put("home", homeAddress);
        homeAddress.put("street", "Bourbon street");
        homeAddress.put("city", "St. Louis");
        Map workAddress = new HashMap();
        addresses.put("work", workAddress);
        workAddress.put("street", "Rock road");
        workAddress.put("city", "Ladue");
        obj.put("addresses", addresses);

        repo.create(id, obj);
        String revAfterCreate = (String) obj.get(DocumentUtil.TAG_REV);
        assertThat(obj).includes(
                entry(DocumentUtil.TAG_ID, uuid));
                // Disabled check whilst OrientDB version not stable at 0
                // , entry(DocumentUtil.TAG_REV, "0"));

        Map<String, Object> result = repo.read(id);
        assertThat(result).includes(
                entry(DocumentUtil.TAG_ID, uuid),
                entry(DocumentUtil.TAG_REV, revAfterCreate),
                entry("firstname", "Johnathan"),
                entry("lastname", "Wombat"),
                entry("age", 100),
                entry("temperature", -44),
                entry("longnumber", Long.MAX_VALUE),
                entry("amount", 7654321.98765d),
                entry("present", true));

        Object checkAddr = result.get("addresses");
        assertNotNull(checkAddr, "addresses map entry null");
        assertThat(checkAddr).isInstanceOf(Map.class);
        assertThat((Map)checkAddr)
                .hasSize(2);
        Object checkHome = ((Map)checkAddr).get("home");
        assertThat(checkHome).isInstanceOf(Map.class);
        assertThat((Map)checkHome)
               .hasSize(2)
               .includes(entry("street", "Bourbon street"), entry("city", "St. Louis"));

        Object checkWork = ((Map)checkAddr).get("work");
        assertThat(checkWork).isInstanceOf(Map.class);
        assertThat((Map)checkWork)
                .hasSize(2)
                .includes(entry("street", "Rock road"), entry("city", "Ladue"));

    }

    @Test(enabled = true, groups = {"repo"}, expectedExceptions = PreconditionFailedException.class)
    @SuppressWarnings("unchecked")
    public void duplicateCreate() throws ResourceException {
        String uuid = "ccf01ec0-66e0-11e0-ae3e-0800200c9a68";
        String id = "managed/user/" + uuid;
        Map<String, Object> obj = new java.util.HashMap<String, Object>();
        obj.put("firstname", "Beta");

        repo.create(id, obj);
        assertThat(obj).includes(
                entry(DocumentUtil.TAG_ID, uuid));
                // Disabled check whilst OrientDB version not stable at 0
                //, entry(DocumentUtil.TAG_REV, "0"));

        repo.create(id, obj); // Must detect duplicate
        assertTrue(false, "Create with duplicate IDs must fail, but did not for id " + id
                + ". Second result: " + obj);

    }

    @Test(groups = {"repo"})
    @SuppressWarnings("unchecked")
    public void updateCurrentObject() throws ResourceException {
        String uuid = "bcf01ec0-66e0-11e0-ae3e-0800200c9a67";
        String id = "managed/user/" + uuid;

        Map<String, Object> obj = new java.util.HashMap<String, Object>();
        obj.put("firstname", "Adam");
        obj.put("lastname", "Zeta");
        Map addresses = new HashMap();
        Map homeAddress = new HashMap();
        addresses.put("home", homeAddress);
        homeAddress.put("street", "Main street");
        homeAddress.put("city", "Los Angeles");
        Map workAddress = new HashMap();
        addresses.put("work", workAddress);
        workAddress.put("street", "Business road");
        workAddress.put("city", "Newport Beach");
        obj.put("addresses", addresses);

        repo.create(id, obj);
        String revAfterCreate = (String) obj.get(DocumentUtil.TAG_REV);
        assertThat(obj).includes(
                entry(DocumentUtil.TAG_ID, uuid));
                // Disabled check whilst OrientDB version not stable at 0
                //, entry(DocumentUtil.TAG_REV, "0"));

        Map<String, Object> objToUpdate = repo.read(id);
        objToUpdate.remove("firstname");
        objToUpdate.put("lastname", "Adler");
        Map retrievedAddr = (Map) objToUpdate.get("addresses");
        assertNotNull(retrievedAddr, "retrieved addresses map entry null");
        assertNotNull(retrievedAddr, "addresses map entry null");
        Map retrievedHome = (Map) retrievedAddr.get("home");
        assertNotNull(retrievedHome, "home map entry null");
        retrievedHome.put("city", "Seal Beach");
        retrievedHome.remove("street");
        repo.update(id, (String) objToUpdate.get(DocumentUtil.TAG_REV), objToUpdate);

        Map<String, Object> result = repo.read(id);

        long rev = Long.valueOf(revAfterCreate).longValue();
        long expectedRev = ++rev;
        String expectedRevStr = "" + rev;

        assertThat(result).includes(
                entry(DocumentUtil.TAG_ID, uuid),
                entry(DocumentUtil.TAG_REV, expectedRevStr),
                entry("lastname", "Adler"));

        assertThat(result).excludes(entry("firstname", "Adam"));

        Object checkAddr = result.get("addresses");
        assertNotNull(checkAddr, "updated addresses map entry null");
        assertThat(checkAddr).isInstanceOf(Map.class);
        assertThat((Map)checkAddr)
                .hasSize(2);
        Object checkHome = ((Map)checkAddr).get("home");
        assertThat(checkHome).isInstanceOf(Map.class);
        assertThat((Map)checkHome)
                .hasSize(1)
                .includes(entry("city", "Seal Beach"))
                .excludes(entry("street", "Main street"));

        Object checkWork = ((Map)checkAddr).get("work");
        assertThat(checkWork).isInstanceOf(Map.class);
        assertThat((Map)checkWork)
               .hasSize(2)
               .includes(entry("street", "Business road"), entry("city", "Newport Beach"));
    }

    @Test(groups = {"repo"})
    @SuppressWarnings("unchecked")
    public void updateCurrentObjectTypeChange() throws ResourceException {
        String uuid = "xyz01ec0-66e0-11e0-ae3e-0800200c1234";
        String id = "managed/user/" + uuid;

        Map<String, Object> obj = new java.util.HashMap<String, Object>();
        obj.put("firstname", "Adam");
        obj.put("lastname", "Zeta");
        Map addresses = new HashMap();
        Map homeAddress = new HashMap();
        addresses.put("home", homeAddress);
        homeAddress.put("street", "Main street");
        homeAddress.put("city", "Los Angeles");
        Map workAddress = new HashMap();
        addresses.put("work", workAddress);
        workAddress.put("street", "Business road");
        workAddress.put("city", "Newport Beach");
        obj.put("addresses", addresses);
        obj.put("phone", "1234");

        repo.create(id, obj);
        String revAfterCreate = (String) obj.get(DocumentUtil.TAG_REV);
        assertThat(obj).includes(
                entry(DocumentUtil.TAG_ID, uuid));
                // Disabled check whilst OrientDB version not stable at 0
                //, entry(DocumentUtil.TAG_REV, "0"));

        Map<String, Object> objToUpdate = repo.read(id);
        Map phoneMap = new HashMap();
        phoneMap.put("home", "2345");
        phoneMap.put("work", "3456");
        objToUpdate.put("phone", phoneMap);
        objToUpdate.put("work", "replacement addr");

        repo.update(id, (String) objToUpdate.get(DocumentUtil.TAG_REV), objToUpdate);

        Map<String, Object> result = repo.read(id);

        long rev = Long.valueOf(revAfterCreate).longValue();
        long expectedRev = ++rev;
        String expectedRevStr = "" + rev;

        assertThat(result).includes(
                entry(DocumentUtil.TAG_ID, uuid),
                entry(DocumentUtil.TAG_REV, expectedRevStr),
                entry("work", "replacement addr"));

        assertThat(result).excludes(entry("phone", "1234"));

        Object checkPhone = result.get("phone");
        assertNotNull(checkPhone, "updated phone map entry null");
        assertThat(checkPhone).isInstanceOf(Map.class);
        assertThat((Map)checkPhone)
                .hasSize(2)
                .includes(entry("home", "2345"))
                .includes(entry("work", "3456"));
    }

    @Test(enabled = true, groups = {"repo"}, expectedExceptions = PreconditionFailedException.class)
    @SuppressWarnings("unchecked")
    public void updateChangedObject() throws ResourceException {
        // check it fails to update an object that has changed since retrieval
        String uuid = "ddddddc0-66e0-11e0-ae3e-0800200ddddd";
        String id = "managed/user/" + uuid;

        Map<String, Object> obj = new java.util.HashMap<String, Object>();
        obj.put("firstname", "Clo");
        Map addresses = new HashMap();
        Map homeAddress = new HashMap();
        addresses.put("home", homeAddress);
        homeAddress.put("street", "Clover street");
        homeAddress.put("city", "San Francisco");
        obj.put("addresses", addresses);

        repo.create(id, obj);
        assertThat(obj).includes(
                entry(DocumentUtil.TAG_ID, uuid));
            // Disabled check whilst OrientDB version not stable at 0
            //, entry(DocumentUtil.TAG_REV, "0"));

        Map<String, Object> objToUpdate1 = repo.read(id);
        String originalRev = (String) objToUpdate1.get(DocumentUtil.TAG_REV);

        // In between reading an updating, have another update change the state
        Map<String, Object> objToUpdate2 = repo.read(id);
        objToUpdate2.put("firstname", "Cloes");
        repo.update(id, (String) objToUpdate2.get(DocumentUtil.TAG_REV), objToUpdate2);

        // Updating with an outdated revision must fail
        objToUpdate1.put("firstname", "Cloe");
        repo.update(id, (String) objToUpdate1.get(DocumentUtil.TAG_REV), objToUpdate1);

        assertTrue(false, "Update of old revision must fail, but did not.");
    }


    @Test(groups = {"repo"})
    @SuppressWarnings("unchecked")
    public void deleteCurrentObject() throws ResourceException {
        String uuid = "aaa01ec0-66e0-11e0-ae3e-0800200c9aaa";
        String id = "managed/user/" + uuid;

        Map<String, Object> obj = new java.util.HashMap<String, Object>();
        obj.put("firstname", "Cesar");
        repo.create(id, obj);
        String revAfterCreate = (String) obj.get(DocumentUtil.TAG_REV);
        assertThat(obj).includes(
                entry(DocumentUtil.TAG_ID, uuid));
            // Disabled check whilst OrientDB version not stable at 0
            //, entry(DocumentUtil.TAG_REV, "0"));

        Map<String, Object> objToDelete = repo.read(id);
        assertThat(objToDelete).includes(
            entry(DocumentUtil.TAG_ID, uuid),
            entry(DocumentUtil.TAG_REV, revAfterCreate),
            entry("firstname", "Cesar"));

        repo.delete(id, (String) objToDelete.get(DocumentUtil.TAG_REV));

        boolean deleteDetected = false;
        try {
            Map<String, Object> deletedObj = repo.read(id);
        } catch (NotFoundException ex) {
            deleteDetected = true;
        }
        assertTrue(deleteDetected, "Delete of object " + id + " failed.");
    }

    @Test(groups = {"repo"}, expectedExceptions = NotFoundException.class)
    @SuppressWarnings("unchecked")
    public void deleteMissingObject() throws ResourceException {
        repo.delete("managed/user/some-fake-id", "0");
        assertTrue(false, "Delete of unknown ID must fail, but did not.");
    }

    @Test(groups = {"repo"}, expectedExceptions = PreconditionFailedException.class)
    @SuppressWarnings("unchecked")
    public void deleteChangedObject() throws ResourceException {
        // check it rejects deleting changed object
        String uuid = "bbb01ec0-66e0-11e0-ae3e-0800200c9bbb";
        String id = "managed/user/" + uuid;

        Map<String, Object> obj = new java.util.HashMap<String, Object>();
        obj.put("firstname", "Cesar");
        repo.create(id, obj);
        String revAfterCreate = (String) obj.get(DocumentUtil.TAG_REV);

        assertThat(obj).includes(
                entry(DocumentUtil.TAG_ID, uuid));
            // Disabled check whilst OrientDB version not stable at 0
            //, entry(DocumentUtil.TAG_REV, "0"));
        obj.put("lastname", "Added");
        repo.update(id, revAfterCreate, obj); // Change the object to increase the revision

        long rev = Long.valueOf(revAfterCreate).longValue();
        long expectedRev = ++rev;
        String expectedRevStr = "" + rev;

        Map<String, Object> objToDelete = repo.read(id);

        assertThat(objToDelete).includes(
            entry(DocumentUtil.TAG_ID, uuid),
            entry(DocumentUtil.TAG_REV, expectedRevStr),
            entry("firstname", "Cesar"),
            entry("lastname", "Added"));

        repo.delete(id, "0"); // Trying to delete old revision must fail
        assertTrue(false, "Delete of changed object must fail, but did not.");
    }

    // Test Query support

    // IDs for test Queries
    String queryUuid1 = "inlinequery-66e0-11e0-ae3e-0800200c9bb1";
    String queryUuid2 = "inlinequery-66e0-11e0-ae3e-0800200c9bb2";
    String queryUuid3 = "inlinequery-66e0-11e0-ae3e-0800200c9bb3";
    String queryUuid4 = "inlinequery-66e0-11e0-ae3e-0800200c9bb4";
    String queryUuid5 = "inlinequery-66e0-11e0-ae3e-0800200c9bb5";

    @Test(groups = {"repo"})
    @SuppressWarnings("unchecked")
    public void populateQueryData() throws ResourceException {
        Map userEx = new HashMap();
        userEx.put("firstname", "Cloe");
        userEx.put("lastname", "Egli");
        userEx.put("test", "inlinequery");
        repo.create("managed/user/" + queryUuid1, userEx);
        userEx.clear();

        userEx.put("firstname", "Andi");
        userEx.put("lastname", "Egloff");
        userEx.put("test", "inlinequery");
        repo.create("managed/user/" + queryUuid2, userEx);
        userEx.clear();

        userEx.put("firstname", "Dorothy");
        userEx.put("lastname", "Smorothy");
        userEx.put("test", "inlinequery");
        repo.create("managed/user/" + queryUuid3, userEx);
        userEx.clear();

        userEx.put("firstname", "Zoe");
        userEx.put("lastname", "Egloff");
        userEx.put("test", "inlinequery");
        repo.create("managed/user/" + queryUuid4, userEx);
        userEx.clear();

        userEx.put("firstname", "Cloe");
        userEx.put("lastname", "Eglolof");
        userEx.put("test", "inlinequery");
        repo.create("managed/user/" + queryUuid5, userEx);
        userEx.clear();
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"})
    @SuppressWarnings("unchecked")
    public void inlineQuery() throws ResourceException {
        Map params = new HashMap();
        //params.put("firstname", "Zebra");
        params.put(QueryConstants.QUERY_EXPRESSION, "select * from managed_user where lastname like 'Eglo%' and test = 'inlinequery'");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);

        assertThat(resultSet).hasSize(3);
        assertThat((Map)resultSet.get(0)).includes(entry(DocumentUtil.TAG_ID, queryUuid2));
        assertThat((Map)resultSet.get(1)).includes(entry(DocumentUtil.TAG_ID, queryUuid4));
        assertThat((Map)resultSet.get(2)).includes(entry(DocumentUtil.TAG_ID, queryUuid5));
    }

    @Test(enabled = true, groups = {"repo"}, dependsOnMethods = {"populateQueryData"})
    @SuppressWarnings("unchecked")
    public void inlineQueryWithWhereToken() throws ResourceException {
        Map params = new HashMap();
        params.put("lastname", "Eglo");
        params.put("querytype", "inlinequery");
        params.put(QueryConstants.QUERY_EXPRESSION, "select * from managed_user where lastname like 'Eglo%' and test = ${querytype} ");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);

        assertThat(resultSet).hasSize(3);
        assertThat((Map)resultSet.get(0)).includes(entry(DocumentUtil.TAG_ID, queryUuid2));
        assertThat((Map)resultSet.get(1)).includes(entry(DocumentUtil.TAG_ID, queryUuid4));
        assertThat((Map)resultSet.get(2)).includes(entry(DocumentUtil.TAG_ID, queryUuid5));
    }

    // Enable this test once the OrientDB parser validation fix has propagated.
    @Test(enabled = false, groups = {"repo"}, dependsOnMethods = {"populateQueryData"})
    @SuppressWarnings("unchecked")
    public void inlineQueryWithNonSpaceWhereToken() throws ResourceException {
        // OrientDB does not handle it if a token is not followed by space, e.g. :lastname%. Check we fall back onto manual token subst.

        Map params = new HashMap();
        params.put("lastname", "Eglo");
        params.put("querytype", "inlinequery");
        params.put(QueryConstants.QUERY_EXPRESSION, "select * from managed_user where lastname like ${lastname}% and test = ${querytype}");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);

        assertThat(resultSet).hasSize(3);
        assertThat((Map)resultSet.get(0)).includes(entry(DocumentUtil.TAG_ID, queryUuid2));
        assertThat((Map)resultSet.get(1)).includes(entry(DocumentUtil.TAG_ID, queryUuid4));
        assertThat((Map)resultSet.get(2)).includes(entry(DocumentUtil.TAG_ID, queryUuid5));
    }

    @Test(enabled = true, groups = {"repo"}, dependsOnMethods = {"populateQueryData"})
    @SuppressWarnings("unchecked")
    public void inlineQueryWithWildcardWhereToken() throws ResourceException {
        Map params = new HashMap();
        params.put("lastname", "Eglo%");
        params.put("querytype", "inlinequery");
        params.put(QueryConstants.QUERY_EXPRESSION, "select * from managed_user where lastname like ${lastname} and test = ${querytype} ");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);

        assertThat(resultSet).hasSize(3);
        assertThat((Map)resultSet.get(0)).includes(entry(DocumentUtil.TAG_ID, queryUuid2));
        assertThat((Map)resultSet.get(1)).includes(entry(DocumentUtil.TAG_ID, queryUuid4));
        assertThat((Map)resultSet.get(2)).includes(entry(DocumentUtil.TAG_ID, queryUuid5));
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"})
    @SuppressWarnings("unchecked")
    public void inlineQueryWithFromToken() throws ResourceException {
        Map params = new HashMap();
        params.put("lastname", "Eglo");
        params.put("querytype", "inlinequery");
        params.put(QueryConstants.QUERY_EXPRESSION, "select * from ${_resource} where lastname like '${lastname}%' and test = '${querytype}'");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);

        assertThat(resultSet).hasSize(3);
        assertThat((Map)resultSet.get(0)).includes(entry(DocumentUtil.TAG_ID, queryUuid2));
        assertThat((Map)resultSet.get(1)).includes(entry(DocumentUtil.TAG_ID, queryUuid4));
        assertThat((Map)resultSet.get(2)).includes(entry(DocumentUtil.TAG_ID, queryUuid5));
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"})
    @SuppressWarnings("unchecked")
    public void inlineQueryWithWhereNameToken() throws ResourceException {
        Map params = new HashMap();
        params.put("lastname", "Eglo");
        params.put("wherefield", "lastname");
        params.put("querytype", "inlinequery");
        params.put(QueryConstants.QUERY_EXPRESSION, "select * from ${_resource} where ${wherefield} like '${lastname}%' and test = '${querytype}'");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);

        assertThat(resultSet).hasSize(3);
        assertThat((Map)resultSet.get(0)).includes(entry(DocumentUtil.TAG_ID, queryUuid2));
        assertThat((Map)resultSet.get(1)).includes(entry(DocumentUtil.TAG_ID, queryUuid4));
        assertThat((Map)resultSet.get(2)).includes(entry(DocumentUtil.TAG_ID, queryUuid5));
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"})
    @SuppressWarnings("unchecked")
    public void configuredQuery() throws ResourceException {
        Map params = new HashMap();
        params.put(QueryConstants.QUERY_ID, "query-without-token");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);

        assertThat(resultSet).hasSize(3);
        assertThat((Map)resultSet.get(0)).includes(entry(DocumentUtil.TAG_ID, queryUuid2));
        assertThat((Map)resultSet.get(1)).includes(entry(DocumentUtil.TAG_ID, queryUuid4));
        assertThat((Map)resultSet.get(2)).includes(entry(DocumentUtil.TAG_ID, queryUuid5));
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"})
    @SuppressWarnings("unchecked")
    public void configuredQueryWithWhereToken() throws ResourceException {
        Map params = new HashMap();
        params.put("lastname", "Eglo");
        params.put("querytype", "inlinequery");
        params.put(QueryConstants.QUERY_ID, "query-with-where-token");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);

        assertThat(resultSet).hasSize(3);
        assertThat((Map)resultSet.get(0)).includes(entry(DocumentUtil.TAG_ID, queryUuid2));
        assertThat((Map)resultSet.get(1)).includes(entry(DocumentUtil.TAG_ID, queryUuid4));
        assertThat((Map)resultSet.get(2)).includes(entry(DocumentUtil.TAG_ID, queryUuid5));
    }


    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"})
    @SuppressWarnings("unchecked")
    public void configuredQueryWithFromToken() throws ResourceException {
        Map params = new HashMap();
        params.put("lastname", "Eglo");
        params.put("querytype", "inlinequery");
        params.put(QueryConstants.QUERY_ID, "query-with-from-token");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);

        assertThat(resultSet).hasSize(3);
        assertThat((Map)resultSet.get(0)).includes(entry(DocumentUtil.TAG_ID, queryUuid2));
        assertThat((Map)resultSet.get(1)).includes(entry(DocumentUtil.TAG_ID, queryUuid4));
        assertThat((Map)resultSet.get(2)).includes(entry(DocumentUtil.TAG_ID, queryUuid5));
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    @SuppressWarnings("unchecked")
    public void invalidQueryId() throws ResourceException {
        Map params = new HashMap();
        params.put(QueryConstants.QUERY_ID, "unknown-query-id");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);
        assertTrue(false, "Query with unknown ID should have failed but did not.");
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    @SuppressWarnings("unchecked")
    public void invalidQueryExpression() throws ResourceException {
        // Unknown OrientDB document class
        Map params = new HashMap();
        params.put(QueryConstants.QUERY_EXPRESSION, "select * from unknown");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);
        assertTrue(false, "Query with unknown OrientDB document class should have failed but did not.");
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    @SuppressWarnings("unchecked")
    public void invalidQueryExpression2() throws ResourceException {
        // Syntax error in query definition
        Map params = new HashMap();
        params.put(QueryConstants.QUERY_EXPRESSION, "Sselect * Ffrom unknown");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);
        assertTrue(false, "Query with malformed query should have failed but did not.");
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    @SuppressWarnings("unchecked")
    public void missingQueryToken() throws ResourceException {
        // Syntax error in query definition
        Map params = new HashMap();
        params.put(QueryConstants.QUERY_EXPRESSION, "select * from ${_resource} where lastname = ${undefined-token");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);
        assertTrue(false, "Query with undefined token should have failed but did not.");
    }

    @Test(groups = {"repo"}, dependsOnMethods = {"populateQueryData"}, expectedExceptions = BadRequestException.class )
    @SuppressWarnings("unchecked")
    public void missingQueryIdOrExpression() throws ResourceException {
        Map params = new HashMap();
        params.put("lastname", "Eglo");
        params.put("querytype", "inlinequery");
        Map result = repo.query("managed/user", params);
        List resultSet = (List) result.get(QueryConstants.QUERY_RESULT);
        assertTrue(false, "Query with undefined token should have failed but did not.");
    }

    // Test ideas:
    // - nested doc query
    // - fields token substitution when supported

}
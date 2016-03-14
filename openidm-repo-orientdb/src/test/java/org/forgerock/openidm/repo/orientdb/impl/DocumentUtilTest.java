/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2013 ForgeRock AS. All rights reserved.
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

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.OServer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.resource.ConflictException;

import org.testng.annotations.*;
import static org.testng.Assert.*;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.entry;

public class DocumentUtilTest {

    String dbURL = "plocal:./target/docutiltestdb";
    ODatabaseDocumentTx db = null;
    String orientDocClass = "Sample";
    OServer server;

    @BeforeClass 
    public void init() throws Exception {
        db = new ODatabaseDocumentTx(dbURL);
        if (!db.exists()) {
            db.create();
        } else {
            db.open("admin", "admin");
        }
    }
    
    @AfterClass 
    public void cleanup() {
        if (db != null) {
            db.close();
        }
    }

    public ODatabaseDocumentTx getDatabase() {
        ODatabaseRecordThreadLocal.INSTANCE.set(db);
        return db;
    }

    @Test
    public void docToMapNullTest() {
        assertNull(DocumentUtil.toMap(null));
    }

    @Test
    public void flatDocToMap() {

        ODocument doc = new ODocument();
        doc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        doc.field("firstname", "Sam");
        doc.field("lastname", "Iam");
        doc.field("telephone", "(555) 123-4567");
        doc.field("age", 20);
        doc.field("longnumber", Long.MAX_VALUE);
        doc.field("amount", Float.valueOf(12345678.89f));
        doc.field("present", Boolean.FALSE);
        doc.field("somedate", new Date());

        Map result = DocumentUtil.toMap(doc);
        
        assertThat(result).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"), //"-1:-1"), // ID when doc not yet stored
                entry(DocumentUtil.TAG_REV, "0"),      // Doc version starts at 0
                entry("firstname", "Sam"), 
                entry("lastname", "Iam"), 
                entry("telephone", "(555) 123-4567"),
                entry("age", 20),
                entry("longnumber", Long.MAX_VALUE),
                entry("amount", 12345678.89f),
                entry("present", false));
        
        Object somedate = result.get("somedate");
        assertNotNull(somedate, "somedate entry null");
        assertThat(somedate).isInstanceOf(Date.class);
        
        // The ID should appear in map not as the key we use for OrientDB internal
        assertFalse(result.containsKey(DocumentUtil.ORIENTDB_PRIMARY_KEY), "DB layer internal key " + DocumentUtil.ORIENTDB_PRIMARY_KEY 
                + " should not appear in map, but did.");

    }
    
    @Test
    public void embeddedDocToMap() {

        ODocument doc = new ODocument();
        doc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        doc.field("firstname", "John");
        doc.field("lastname", "Doe");
        doc.field("city", new ODocument().field("name","Paris").field("country", "France"));
        doc.field("phonenumbers", new ODocument().field("home","555-666-7777").field("mobile", "555-111-2222"), OType.EMBEDDED);
        
        Map result = DocumentUtil.toMap(doc);
        
        assertThat(result).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"), //"-1:-1"), // ID when doc not yet stored
                entry(DocumentUtil.TAG_REV, "0"), // Doc version starts at 0
                entry("firstname", "John"), 
                entry("lastname", "Doe"));

        Object city = result.get("city");
        assertNotNull(city, "city map entry null");
        assertThat(city).isInstanceOf(Map.class);
        assertThat((Map)city)
                .hasSize(2)
                .contains(entry("name", "Paris"), entry("country", "France"));
        
        Object phonenumbers = result.get("phonenumbers");
        assertNotNull(phonenumbers, "phonenumbers map entry null");
        assertThat(phonenumbers).isInstanceOf(Map.class);
        assertThat((Map)phonenumbers)
                .hasSize(2)
                .contains(entry("home", "555-666-7777"), entry("mobile", "555-111-2222"));
    }
    
    @Test
    public void embeddedListToMap() {

        ODocument doc = new ODocument();
        doc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        doc.field("firstname", "John");
        doc.field("lastname", "Doe");
        doc.field("city", new ODocument().field("name","Paris").field("country", "France"));
        List<ODocument> addresses = new ArrayList<ODocument>();
        addresses.add(new ODocument().field("type", "home").field("street", "Main st.").field("city", "San Franciso") );
        addresses.add(new ODocument().field("type", "business").field("street", "Wall st.").field("city", "New York") );
        doc.field("addresses",  addresses, OType.EMBEDDED); 
        
        Map result = DocumentUtil.toMap(doc);
        
        assertThat(result).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"), //"-1:-1"), // ID when doc not yet stored
                entry(DocumentUtil.TAG_REV, "0"), // Doc version starts at 0
                entry("firstname", "John"), 
                entry("lastname", "Doe"));

        Object addrResult = result.get("addresses");
        assertNotNull(addrResult, "addresses map entry null");
        assertThat(addrResult).isInstanceOf(List.class);
        List addr = (List)addrResult;
        assertThat(addr)
                .hasSize(2);

        assertThat(addr.get(0)).isInstanceOf(Map.class);
        Map firstEntry = (Map) addr.get(0); 
        assertThat(firstEntry).contains(
                entry("type", "home"),
                entry("street", "Main st."),
                entry("city", "San Franciso"));
        Map secondEntry = (Map) addr.get(1); 
        assertThat(secondEntry).contains(
                entry("type", "business"),
                entry("street", "Wall st."),
                entry("city", "New York")); 
    }
    
    @Test
    public void deepNestingToMap() {

        ODocument doc = new ODocument();
        doc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        List<ODocument> detail = new ArrayList<ODocument>();
        List<ODocument> addresses = new ArrayList<ODocument>();
        ODocument status1 = new ODocument("inventory", 10);
        ODocument status2 = new ODocument("inventory", 20);
        addresses.add(new ODocument().field("street", "Main st.").field("city", "San Franciso").field("status", status1, OType.EMBEDDED));
        addresses.add(new ODocument().field("street", "Wall st.").field("city", "New York").field("status", status2, OType.EMBEDDED));
        detail.add(new ODocument().field("locations", addresses, OType.EMBEDDED));
        
        doc.field("widget", new ODocument().field("name","widget-a").field("detail", detail), OType.EMBEDDED);
        
        Map result = DocumentUtil.toMap(doc);
        
        assertThat(result).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"),
                entry(DocumentUtil.TAG_REV, "0"));

        Map resultWidgets = (Map) result;
        
        Map resultWidget = (Map) resultWidgets.get("widget");
        List resultDetails = (List) resultWidget.get("detail");
        Map resultDetail1 = (Map) resultDetails.get(0);
        List resultLocations = (List) resultDetail1.get("locations");
        Map resultAddress2 = (Map) resultLocations.get(1);
        System.out.println(resultAddress2);
        Map resultStatus = (Map) resultAddress2.get("status");
        Integer resultInventory = (Integer) resultStatus.get("inventory");
        assertEquals(resultInventory.intValue(), 20);
    }    
    
    @Test
    public void deepNestingWithSetToMap() {

        ODocument doc = new ODocument();
        doc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        Set<ODocument> detail = new HashSet<ODocument>();
        Set<ODocument> addresses = new HashSet<ODocument>();
        ODocument status1 = new ODocument("inventory", 10);
        ODocument status2 = new ODocument("inventory", 20);
        addresses.add(new ODocument().field("street", "Main st.").field("city", "San Franciso").field("status", status1, OType.EMBEDDED));
        detail.add(new ODocument().field("locations", addresses, OType.EMBEDDED));
        
        doc.field("widget", new ODocument().field("name","widget-a").field("detail", detail), OType.EMBEDDED);
        
        Map result = DocumentUtil.toMap(doc);
        
        assertThat(result).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"),
                entry(DocumentUtil.TAG_REV, "0"));

        Map resultWidgets = (Map) result;
        
        Map resultWidget = (Map) resultWidgets.get("widget");
        List resultDetails = (List) resultWidget.get("detail");
        Map resultDetail1 = (Map) resultDetails.get(0);
        List resultLocations = (List) resultDetail1.get("locations");
        Map resultAddress1 = (Map) resultLocations.get(0);
        System.out.println(resultAddress1);
        Map resultStatus = (Map) resultAddress1.get("status");
        Integer resultInventory = (Integer) resultStatus.get("inventory");
        assertEquals(resultInventory.intValue(), 10);
    }   

    @Test
    public void deepNestingMixedODocAndMapToMap() {

        ODocument doc = new ODocument();
        doc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        List<Map<String, Object>> detail = new ArrayList<Map<String, Object>>();
        List<ODocument> addresses = new ArrayList<ODocument>();
        ODocument status1 = new ODocument("inventory", 10);
        ODocument status2 = new ODocument("inventory", 20);
        addresses.add(new ODocument().field("street", "Main st.").field("city", "San Franciso").field("status", status1, OType.EMBEDDED));
        addresses.add(new ODocument().field("street", "Wall st.").field("city", "New York").field("status", status2, OType.EMBEDDED));
        Map<String, Object> locationsMap = new HashMap<String, Object>();
        locationsMap.put("locations", addresses);
        detail.add(locationsMap);
        
        doc.field("widget", new ODocument().field("name","widget-a").field("detail", detail), OType.EMBEDDED);
        
        Map result = DocumentUtil.toMap(doc);
        
        assertThat(result).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"),
                entry(DocumentUtil.TAG_REV, "0"));

        Map resultWidgets = (Map) result;
        
        Map resultWidget = (Map) resultWidgets.get("widget");
        List resultDetails = (List) resultWidget.get("detail");
        Map resultDetail1 = (Map) resultDetails.get(0);
        List resultLocations = (List) resultDetail1.get("locations");
        Map resultAddress2 = (Map) resultLocations.get(1);
        System.out.println(resultAddress2);
        Map resultStatus = (Map) resultAddress2.get("status");
        Integer resultInventory = (Integer) resultStatus.get("inventory");
        assertEquals(resultInventory.intValue(), 20);
    }    
    
    @Test
    public void mapToDocNullTest() throws ConflictException {
        assertNull(DocumentUtil.toDocument(null, null, getDatabase(), orientDocClass, false, true));
    }
    
    @Test
    public void mapToNewFlatDoc() throws ConflictException{
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        map.put(DocumentUtil.TAG_REV, "1");
        map.put("firstname", "Sam");
        map.put("lastname", "Iam");
        map.put("telephone", "(555) 123-4567");
        map.put("age", 20);
        map.put("longnumber", Long.MAX_VALUE);
        map.put("amount", Float.valueOf(12345678.89f));
        map.put("present", Boolean.FALSE);
        map.put("somedate", new Date());
        
        ODocument result = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        
        assertEquals(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY), "client-assigned-id", "unexpected ID");
        assertEquals(result.field("firstname"), "Sam", "unexpected firstname");
        assertEquals(result.field("lastname"), "Iam", "unexpected lastname");
        assertEquals(result.field("telephone"), "(555) 123-4567", "unexpected telephone");
        assertEquals(result.field("age"), 20, "unexpected age");
        assertEquals(result.field("longnumber"), Long.MAX_VALUE, "unexpected longnumber");
        assertEquals(result.field("amount"), 12345678.89f, "unexpected amount");
        assertEquals(result.field("present"), false, "unexpected present boolean");
        assertEquals(result.getVersion(), 1, "Version not as expected");
        Object somedate = result.field("somedate");
        assertNotNull(somedate, "somedate entry null");
        assertTrue((somedate instanceof Date), "Date not of expected type.");
    }

    @Test
    public void mapToExistingFlatDoc() throws ConflictException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        map.put(DocumentUtil.TAG_REV, "1");
        map.put("firstname", "Sam");
        map.put("lastname", "Iam");
        map.put("telephone", "(555) 123-4567");
        map.put("age", 20);
        map.put("longnumber", Long.MAX_VALUE);
        map.put("amount", Float.valueOf(12345678.89f));
        map.put("present", Boolean.FALSE);
        map.put("somedate", new Date());
        
        // An existing document to get updated from the map
        ODocument existingDoc = getDatabase().newInstance(orientDocClass);
        existingDoc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        // firstname and lastname deliberately not in the existing doc
        existingDoc.field("fieldtoberemoved", "ABC");
        existingDoc.field("telephone", "(999) 999-9999");
        existingDoc.field("age", 20);
        existingDoc.field("longnumber", 0);
        existingDoc.field("amount", Float.valueOf(12345678.89f));
        existingDoc.field("present", Boolean.FALSE);
        existingDoc.field("somedate", new Date());
        existingDoc.field("AnotherFieldToBeRemoved", new Date());
        
        ODocument result = DocumentUtil.toDocument(map, existingDoc, getDatabase(), orientDocClass);
        
        assertEquals(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY), "client-assigned-id", "unexpected ID");
        assertEquals(result.field("firstname"), "Sam", "unexpected firstname");
        assertEquals(result.field("lastname"), "Iam", "unexpected lastname");
        assertEquals(result.field("telephone"), "(555) 123-4567", "unexpected telephone");
        assertEquals(result.field("age"), 20, "unexpected age");
        assertEquals(result.field("longnumber"), Long.MAX_VALUE, "unexpected longnumber");
        assertEquals(result.field("amount"), 12345678.89f, "unexpected amount");
        assertEquals(result.field("present"), false, "unexpected present boolean");
        assertFalse(result.containsField("fieldtoberemoved"), "Field 'fieldtoberemoved' not removed as expected");
        assertFalse(result.containsField("AnotherFieldToBeRemoved"), "Field 'AnotherFieldToBeRemoved' not removed as expected");
        assertEquals(result.getVersion(), 1, "Version not as expected");
        Object somedate = result.field("somedate");
        assertNotNull(somedate, "somedate entry null");
        assertTrue((somedate instanceof Date), "Date not of expected type.");

    }    
    
    @Test
    public void mapToNewDocExistingRevision() throws ConflictException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        map.put(DocumentUtil.TAG_REV, "100");
        map.put("firstname", "John");
        ODocument result = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        
        assertEquals(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY), "client-assigned-id", "unexpected ID");
        assertEquals(result.field("firstname"), "John", "unexpected firstname");
        assertEquals(result.getVersion(), 100, "Version not as expected");
    }
    
    @Test
    public void mapToNewEmbeddedDoc() throws ConflictException {
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        map.put("firstname", "John");
        map.put("lastname", "Doe");
        
        Map<String, Object> city = new HashMap<String, Object>();
        city.put("name","Paris");
        city.put("country", "France");
        map.put("city", city);
        
        Map<String, Object> phone = new HashMap<String, Object>();
        phone.put("home","555-666-7777");
        phone.put("mobile", "555-111-2222");
        map.put("phonenumbers", phone);
        
        ODocument result = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        
        assertEquals(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY), "client-assigned-id", "unexpected ID");
        assertEquals(result.field("firstname"), "John", "unexpected firstname");
        assertEquals(result.field("lastname"), "Doe", "unexpected lastname");
        assertEquals(result.getVersion(), 0, "Version not as expected");

        ODocument phonenumbers = (ODocument)result.field("phonenumbers");
        assertNotNull(phonenumbers, "phonenumbers map entry null");
        assertEquals(phonenumbers.field("home"), "555-666-7777", "unexpected home phone");
        assertEquals(phonenumbers.field("mobile"), "555-111-2222", "unexpected mobile phone");
        assertThat((ODocument)phonenumbers)
                .hasSize(2);
    }
    
    @Test
    public void mapToExistingEmbeddedDoc() throws ConflictException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        // deliberately remove the firstname
        map.put("lastname", "Doe");
        // deliberately remove the city 
        Map<String, Object> phone = new HashMap<String, Object>();
        //remove the home number, change the mobile, add a work number
        phone.put("mobile", "555-111-2229");
        phone.put("work","666-777-8888");
        map.put("phonenumbers", phone);
        
        ODocument existingDoc = new ODocument();
        existingDoc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        existingDoc.field("firstname", "Johnathan");
        // lastname deliberately not in existing doc
        existingDoc.field("city", new ODocument().field("name","Paris").field("country", "France"));
        existingDoc.field("phonenumbers", new ODocument().field("home","555-666-7777").field("mobile", "555-111-2222"), OType.EMBEDDED);
        
        ODocument result = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        
        assertEquals(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY), "client-assigned-id", "unexpected ID");
        assertFalse(result.containsField("firstname"), "Firstname should have been removed but is present");
        assertEquals(result.field("lastname"), "Doe", "unexpected lastname");
        assertEquals(result.getVersion(), 0, "Version not as expected");

        assertFalse(result.containsField("city"), "City map should have been removed but is present.");
        
        ODocument phonenumbers = (ODocument)result.field("phonenumbers");
        assertNotNull(phonenumbers, "phonenumbers map entry null");
        assertEquals(phonenumbers.field("work"), "666-777-8888", "unexpected work phone");
        assertEquals(phonenumbers.field("mobile"), "555-111-2229", "unexpected mobile phone");
        assertFalse(phonenumbers.containsField("home"), "Home phone should have been removed but is present.");
        assertThat((ODocument)phonenumbers)
                .hasSize(2);
    }    
    
    @Test
    public void listToEmbeddedList() throws ConflictException {
    
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        map.put("firstname", "John");
        map.put("lastname", "Doe");
        
        List<Object> cities = new ArrayList<Object>();
        cities.add("Paris");
        cities.add("St. Louis");
        map.put("cities", cities);
        
        Map<String, Object> phone = new HashMap<String, Object>();
        phone.put("home","555-666-7777");
        phone.put("mobile", "555-111-2222");
        map.put("phonenumbers", phone);
        
        ODocument result = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        
        assertEquals(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY), "client-assigned-id", "unexpected ID");
        assertEquals(result.field("firstname"), "John", "unexpected firstname");
        assertEquals(result.field("lastname"), "Doe", "unexpected lastname");
        assertEquals(result.getVersion(), 0, "Version not as expected");
        
        Object resultCities = result.field("cities");
        assertThat(resultCities).isInstanceOf(List.class);
        assertThat((List)resultCities).containsOnly("Paris", "St. Louis");

        ODocument phonenumbers = (ODocument)result.field("phonenumbers");
        assertNotNull(phonenumbers, "phonenumbers map entry null");
        assertEquals(phonenumbers.field("home"), "555-666-7777", "unexpected home phone");
        assertEquals(phonenumbers.field("mobile"), "555-111-2222", "unexpected mobile phone");
        assertThat((ODocument)phonenumbers)
                .hasSize(2);
    }
    
    @Test(expectedExceptions = ConflictException.class)
    public void mapToDocInvalidRevision() throws ConflictException {
        // Check ConflictException is thrown for invalid versions
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        map.put(DocumentUtil.TAG_REV, "invalid-version"); // OrientDB revisions are ints
        map.put("firstname", "John");
        ODocument result = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        assertTrue(false, "Invalid Revision must trigger failure");
    }
    
    @Test
    public void mapToDocToMap() throws ConflictException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        map.put(DocumentUtil.TAG_REV, "2");
        map.put("firstname", "John");
        map.put("lastname", "Doe");
        
        Map<String, Object> city = new HashMap<String, Object>();
        city.put("name","Paris");
        city.put("country", "France");
        map.put("city", city);
        
        Map<String, Object> phone = new HashMap<String, Object>();
        phone.put("home","555-666-7777");
        phone.put("mobile", "555-111-2222");
        map.put("phonenumbers", phone);
        
        ODocument intermediateResult = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        
        Map result = DocumentUtil.toMap(intermediateResult);
        
        assertThat(result).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"), 
                entry(DocumentUtil.TAG_REV, "2"), 
                entry("firstname", "John"), 
                entry("lastname", "Doe"));

        Object checkCity = result.get("city");
        assertNotNull(checkCity, "city map entry null");
        assertThat(checkCity).isInstanceOf(Map.class);
        assertThat((Map)checkCity)
                .hasSize(2)
                .contains(entry("name", "Paris"), entry("country", "France"));
        
        Object phonenumbers = result.get("phonenumbers");
        assertNotNull(phonenumbers, "phonenumbers map entry null");
        assertThat(phonenumbers).isInstanceOf(Map.class);
        assertThat((Map)phonenumbers)
                .hasSize(2)
                .contains(entry("home", "555-666-7777"), entry("mobile", "555-111-2222"));
    }
    
    @Test
    public void parseValidRevision() throws ConflictException {
        int ver = DocumentUtil.parseVersion("98765");
        assertEquals(ver, 98765);
    }
    
    @Test(expectedExceptions = ConflictException.class) 
    public void parseInvalidRevision() throws ConflictException {
        int ver = DocumentUtil.parseVersion("some-text-98765");
        assertTrue(false, "Parsing of invalid revision must fail, but did not.");
    }
}

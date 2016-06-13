/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2016 ForgeRock AS.
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
import static org.assertj.core.data.MapEntry.entry;
import static org.forgerock.json.JsonValue.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.OServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ConflictException;

import org.testng.annotations.*;


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
        assertThat(DocumentUtil.toMap(null)).isNull();
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

        Map<String, Object> result = DocumentUtil.toMap(doc);
        
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
        assertThat(somedate).isNotNull().overridingErrorMessage("somedate entry null");
        assertThat(somedate).isInstanceOf(Date.class);
        
        // The ID should appear in map not as the key we use for OrientDB internal
        assertThat(result.containsKey(DocumentUtil.ORIENTDB_PRIMARY_KEY))
                .isFalse()
                .overridingErrorMessage("DB layer internal key " + DocumentUtil.ORIENTDB_PRIMARY_KEY
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

        // wrap result in JsonValue to help with typing
        JsonValue result = json(DocumentUtil.toMap(doc));
        
        assertThat(result.asMap()).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"), //"-1:-1"), // ID when doc not yet stored
                entry(DocumentUtil.TAG_REV, "0"), // Doc version starts at 0
                entry("firstname", "John"), 
                entry("lastname", "Doe"));

        JsonValue city = result.get("city");
        assertThat(city.isNotNull()).overridingErrorMessage("city map entry null");
        assertThat(city.isMap());
        assertThat(city.asMap())
                .hasSize(2)
                .contains(entry("name", "Paris"), entry("country", "France"));

        JsonValue phonenumbers = result.get("phonenumbers");
        assertThat(phonenumbers)
                .isNotNull()
                .overridingErrorMessage("phonenumbers map entry null");
        assertThat(phonenumbers.isMap());
        assertThat(phonenumbers.asMap())
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
        addresses.add(new ODocument().field("type", "home").field("street", "Main st.").field("city", "San Francisco") );
        addresses.add(new ODocument().field("type", "business").field("street", "Wall st.").field("city", "New York") );
        doc.field("addresses",  addresses, OType.EMBEDDED);

        // wrap result in JsonValue to help with typing
        JsonValue result = json(DocumentUtil.toMap(doc));
        
        assertThat(result.asMap()).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"), //"-1:-1"), // ID when doc not yet stored
                entry(DocumentUtil.TAG_REV, "0"), // Doc version starts at 0
                entry("firstname", "John"), 
                entry("lastname", "Doe"));

        JsonValue addrResult = result.get("addresses");
        assertThat(addrResult).isNotNull().overridingErrorMessage("addresses map entry null");
        assertThat(addrResult.isList());
        assertThat(addrResult.asList())
                .hasSize(2);

        JsonValue firstEntry = addrResult.get(0);
        assertThat(firstEntry.isMap());
        assertThat(firstEntry.asMap()).contains(
                entry("type", "home"),
                entry("street", "Main st."),
                entry("city", "San Francisco"));
        JsonValue secondEntry = addrResult.get(1);
        assertThat(secondEntry.isMap());
        assertThat(secondEntry.asMap()).contains(
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
        addresses.add(new ODocument().field("street", "Main st.").field("city", "San Francisco").field("status", status1, OType.EMBEDDED));
        addresses.add(new ODocument().field("street", "Wall st.").field("city", "New York").field("status", status2, OType.EMBEDDED));
        detail.add(new ODocument().field("locations", addresses, OType.EMBEDDED));
        
        doc.field("widget", new ODocument().field("name","widget-a").field("detail", detail), OType.EMBEDDED);

        // wrap result in JsonValue to help with typing
        JsonValue resultWidgets = json(DocumentUtil.toMap(doc));

        assertThat(resultWidgets.isMap());
        assertThat(resultWidgets.asMap()).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"),
                entry(DocumentUtil.TAG_REV, "0"));

        JsonValue resultWidget = resultWidgets.get("widget");
        JsonValue resultDetails = resultWidget.get("detail");
        JsonValue resultDetail1 = resultDetails.get(0);
        JsonValue resultLocations = resultDetail1.get("locations");
        JsonValue resultAddress2 = resultLocations.get(1);
        System.out.println(resultAddress2);
        JsonValue resultStatus = resultAddress2.get("status");
        JsonValue resultInventory = resultStatus.get("inventory");
        assertThat(resultInventory.isNumber()).isTrue();
        assertThat(resultInventory.asInteger()).isEqualTo(20);
    }
    
    @Test
    public void deepNestingWithSetToMap() {

        ODocument doc = new ODocument();
        doc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        Set<ODocument> detail = new HashSet<ODocument>();
        Set<ODocument> addresses = new HashSet<ODocument>();
        ODocument status1 = new ODocument("inventory", 10);
        ODocument status2 = new ODocument("inventory", 20);
        addresses.add(new ODocument().field("street", "Main st.").field("city", "San Francisco").field("status", status1, OType.EMBEDDED));
        detail.add(new ODocument().field("locations", addresses, OType.EMBEDDED));
        
        doc.field("widget", new ODocument().field("name","widget-a").field("detail", detail), OType.EMBEDDED);

        // wrap result in JsonValue to help with typing
        JsonValue resultWidgets = json(DocumentUtil.toMap(doc));

        assertThat(resultWidgets.isMap()).isTrue();
        assertThat(resultWidgets.asMap()).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"),
                entry(DocumentUtil.TAG_REV, "0"));

        JsonValue resultWidget = resultWidgets.get("widget");
        JsonValue resultDetails = resultWidget.get("detail");
        JsonValue resultDetail1 = resultDetails.get(0);
        JsonValue resultLocations = resultDetail1.get("locations");
        JsonValue resultAddress1 = resultLocations.get(0);
        System.out.println(resultAddress1);
        JsonValue resultStatus = resultAddress1.get("status");
        JsonValue resultInventory = resultStatus.get("inventory");
        assertThat(resultInventory.isNumber()).isTrue();
        assertThat(resultInventory.asInteger()).isEqualTo(10);
    }

    @Test
    public void deepNestingMixedODocAndMapToMap() {

        ODocument doc = new ODocument();
        doc.field(DocumentUtil.ORIENTDB_PRIMARY_KEY, "client-assigned-id");
        List<Map<String, Object>> detail = new ArrayList<Map<String, Object>>();
        List<ODocument> addresses = new ArrayList<ODocument>();
        ODocument status1 = new ODocument("inventory", 10);
        ODocument status2 = new ODocument("inventory", 20);
        addresses.add(new ODocument().field("street", "Main st.").field("city", "San Francisco").field("status", status1, OType.EMBEDDED));
        addresses.add(new ODocument().field("street", "Wall st.").field("city", "New York").field("status", status2, OType.EMBEDDED));
        Map<String, Object> locationsMap = new HashMap<String, Object>();
        locationsMap.put("locations", addresses);
        detail.add(locationsMap);
        
        doc.field("widget", new ODocument().field("name","widget-a").field("detail", detail), OType.EMBEDDED);

        // wrap result in JsonValue to help with typing
        JsonValue resultWidgets = json(DocumentUtil.toMap(doc));

        assertThat(resultWidgets.isMap()).isTrue();
        assertThat(resultWidgets.asMap()).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"),
                entry(DocumentUtil.TAG_REV, "0"));

        JsonValue resultWidget = resultWidgets.get("widget");
        JsonValue resultDetails = resultWidget.get("detail");
        JsonValue resultDetail1 = resultDetails.get(0);
        JsonValue resultLocations = resultDetail1.get("locations");
        JsonValue resultAddress2 = resultLocations.get(1);
        System.out.println(resultAddress2);
        JsonValue resultStatus = resultAddress2.get("status");
        JsonValue resultInventory = resultStatus.get("inventory");
        assertThat(resultInventory.isNumber()).isTrue();
        assertThat(resultInventory.asInteger()).isEqualTo(20);
    }
    
    @Test
    public void mapToDocNullTest() throws ConflictException {
        assertThat((Object) DocumentUtil.toDocument(null, null, getDatabase(), orientDocClass, false, true)).isNull();
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
        map.put("amount", 12345678.89f);
        map.put("present", Boolean.FALSE);
        map.put("somedate", new Date());
        
        ODocument result = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        
        assertThat(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY)).isEqualTo("client-assigned-id")
                .overridingErrorMessage("unexpected ID");
        assertThat(result.field("firstname")).isEqualTo("Sam")
                .overridingErrorMessage("unexpected firstname");
        assertThat(result.field("lastname")).isEqualTo("Iam")
                .overridingErrorMessage("unexpected lastname");
        assertThat(result.field("telephone")).isEqualTo("(555) 123-4567")
                .overridingErrorMessage("unexpected telephone");
        assertThat(result.field("age")).isEqualTo(20)
                .overridingErrorMessage("unexpected age");
        assertThat(result.field("longnumber")).isEqualTo(Long.MAX_VALUE)
                .overridingErrorMessage("unexpected longnumber");
        assertThat(result.field("amount")).isEqualTo(12345678.89f)
                .overridingErrorMessage("unexpected amount");
        assertThat(result.field("present")).isEqualTo(false)
                .overridingErrorMessage("unexpected present boolean");
        assertThat(result.getVersion()).isEqualTo(1)
                .overridingErrorMessage( "Version not as expected");
        Object somedate = result.field("somedate");
        assertThat(somedate).isNotNull().overridingErrorMessage("somedate entry null");
        assertThat(somedate).isInstanceOf(Date.class).overridingErrorMessage("Date not of expected type.");
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
        map.put("amount", 12345678.89f);
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
        existingDoc.field("amount", 12345678.89f);
        existingDoc.field("present", Boolean.FALSE);
        existingDoc.field("somedate", new Date());
        existingDoc.field("AnotherFieldToBeRemoved", new Date());
        
        ODocument result = DocumentUtil.toDocument(map, existingDoc, getDatabase(), orientDocClass);
        
        assertThat(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY)).isEqualTo("client-assigned-id")
                .overridingErrorMessage("unexpected ID");
        assertThat(result.field("firstname")).isEqualTo("Sam")
                .overridingErrorMessage("unexpected firstname");
        assertThat(result.field("lastname")).isEqualTo("Iam")
                .overridingErrorMessage( "unexpected lastname");
        assertThat(result.field("telephone")).isEqualTo("(555) 123-4567")
                .overridingErrorMessage( "unexpected telephone");
        assertThat(result.field("age")).isEqualTo(20)
                .overridingErrorMessage("unexpected age");
        assertThat(result.field("longnumber")).isEqualTo(Long.MAX_VALUE)
                .overridingErrorMessage( "unexpected longnumber");
        assertThat(result.field("amount")).isEqualTo(12345678.89f)
                .overridingErrorMessage( "unexpected amount");
        assertThat(result.field("present")).isEqualTo(false)
                .overridingErrorMessage( "unexpected present boolean");
        assertThat(result.containsField("fieldtoberemoved")).isFalse()
                .overridingErrorMessage("Field 'fieldtoberemoved' not removed as expected");
        assertThat(result.containsField("AnotherFieldToBeRemoved")).isFalse()
                .overridingErrorMessage("Field 'AnotherFieldToBeRemoved' not removed as expected");
        assertThat(result.getVersion()).isEqualTo(1)
                .overridingErrorMessage("Version not as expected");
        Object somedate = result.field("somedate");
        assertThat(somedate).isNotNull().overridingErrorMessage("somedate entry null");
        assertThat(somedate).isInstanceOf(Date.class).overridingErrorMessage("date not of expected type.");
    }
    
    @Test
    public void mapToNewDocExistingRevision() throws ConflictException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        map.put(DocumentUtil.TAG_REV, "100");
        map.put("firstname", "John");
        ODocument result = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        
        assertThat(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY))
                .isEqualTo("client-assigned-id")
                .overridingErrorMessage("unexpected ID");
        assertThat(result.field("firstname"))
                .isEqualTo("John")
                .overridingErrorMessage("unexpected firstname");
        assertThat(result.getVersion())
                .isEqualTo(100)
                .overridingErrorMessage("Version not as expected");
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
        
        assertThat(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY))
                .isEqualTo("client-assigned-id")
                .overridingErrorMessage("unexpected ID");
        assertThat(result.field("firstname"))
                .isEqualTo("John")
                .overridingErrorMessage("unexpected firstname");
        assertThat(result.field("lastname"))
                .isEqualTo("Doe")
                .overridingErrorMessage("unexpected lastname");
        assertThat(result.getVersion())
                .isEqualTo(0)
                .overridingErrorMessage("Version not as expected");

        final ODocument phonenumbers = result.field("phonenumbers");
        assertThat((Object) phonenumbers)
                .isNotNull()
                .overridingErrorMessage("phonenumbers map entry null");
        assertThat(phonenumbers.field("home"))
                .isEqualTo("555-666-7777")
                .overridingErrorMessage("unexpected home phone");
        assertThat(phonenumbers.field("mobile"))
                .isEqualTo("555-111-2222")
                .overridingErrorMessage("unexpected mobile phone");
        // disambiguate assertThat(ODocument) from assertThat(Iterable) and avoid casting
        assertThat(new Iterable<Map.Entry<String, Object>>() {
            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                return phonenumbers.iterator();
            }
        }).hasSize(2);
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
        
        assertThat(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY))
                .isEqualTo("client-assigned-id")
                .overridingErrorMessage("unexpected ID");
        assertThat(result.containsField("firstname"))
                .isFalse()
                .overridingErrorMessage("Firstname should have been removed but is present");
        assertThat(result.field("lastname"))
                .isEqualTo("Doe")
                .overridingErrorMessage("unexpected lastname");
        assertThat(result.getVersion())
                .isEqualTo(0)
                .overridingErrorMessage("Version not as expected");

        assertThat(result.containsField("city"))
                .isFalse()
                .overridingErrorMessage("City map should have been removed but is present.");
        
        final ODocument phonenumbers = result.field("phonenumbers");
        assertThat((Object) phonenumbers)
                .isNotNull()
                .overridingErrorMessage("phonenumbers map entry null");
        assertThat(phonenumbers.field("work"))
                .isEqualTo("666-777-8888")
                .overridingErrorMessage("unexpected work phone");
        assertThat(phonenumbers.field("mobile"))
                .isEqualTo("555-111-2229")
                .overridingErrorMessage("unexpected mobile phone");
        assertThat(phonenumbers.containsField("home"))
                .isFalse()
                .overridingErrorMessage("Home phone should have been removed but is present.");
        // disambiguate assertThat(ODocument) from assertThat(Iterable) and avoid casting
        assertThat(new Iterable<Map.Entry<String, Object>>() {
            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                return phonenumbers.iterator();
            }
        }).hasSize(2);
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
        
        assertThat(result.field(DocumentUtil.ORIENTDB_PRIMARY_KEY))
                .isEqualTo("client-assigned-id")
                .overridingErrorMessage("unexpected ID");
        assertThat(result.field("firstname"))
                .isEqualTo("John")
                .overridingErrorMessage("unexpected firstname");
        assertThat(result.field("lastname"))
                .isEqualTo("Doe")
                .overridingErrorMessage("unexpected lastname");
        assertThat(result.getVersion())
                .isEqualTo(0)
                .overridingErrorMessage("Version not as expected");

        final ODocument phonenumbers = result.field("phonenumbers");
        assertThat((Object) phonenumbers)
                .isNotNull()
                .overridingErrorMessage("phonenumbers map entry null");
        assertThat(phonenumbers.field("home"))
                .isEqualTo("555-666-7777")
                .overridingErrorMessage("unexpected home phone");
        assertThat(phonenumbers.field("mobile"))
                .isEqualTo("555-111-2222")
                .overridingErrorMessage("unexpected mobile phone");
        // disambiguate assertThat(ODocument) from assertThat(Iterable) and avoid casting
        assertThat(new Iterable<Map.Entry<String, Object>>() {
            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                return phonenumbers.iterator();
            }
        }).hasSize(2);
    }
    
    @Test(expectedExceptions = ConflictException.class)
    public void mapToDocInvalidRevision() throws ConflictException {
        // Check ConflictException is thrown for invalid versions
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(DocumentUtil.TAG_ID, "client-assigned-id");
        map.put(DocumentUtil.TAG_REV, "invalid-version"); // OrientDB revisions are ints
        map.put("firstname", "John");
        ODocument result = DocumentUtil.toDocument(map, null, getDatabase(), orientDocClass);
        assertThat(false).isTrue().overridingErrorMessage("Invalid Revision must trigger failure");
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

        // wrap in JsonValue to help in typing
        JsonValue result = json(DocumentUtil.toMap(intermediateResult));

        assertThat(result.isMap()).isTrue();
        assertThat(result.asMap()).contains(
                entry(DocumentUtil.TAG_ID, "client-assigned-id"), 
                entry(DocumentUtil.TAG_REV, "2"), 
                entry("firstname", "John"), 
                entry("lastname", "Doe"));

        JsonValue checkCity = result.get("city");
        assertThat(checkCity)
                .isNotNull()
                .overridingErrorMessage("city map entry null");
        assertThat(checkCity.isMap()).isTrue();
        assertThat(checkCity.asMap())
                .hasSize(2)
                .contains(entry("name", "Paris"), entry("country", "France"));
        
        JsonValue phonenumbers = result.get("phonenumbers");
        assertThat(phonenumbers)
                .isNotNull()
                .overridingErrorMessage("phonenumbers map entry null");
        assertThat(phonenumbers.isMap()).isTrue();
        assertThat(phonenumbers.asMap())
                .hasSize(2)
                .contains(entry("home", "555-666-7777"), entry("mobile", "555-111-2222"));
    }
    
    @Test
    public void parseValidRevision() throws ConflictException {
        int ver = DocumentUtil.parseVersion("98765");
        assertThat(ver).isEqualTo(98765);
    }
    
    @Test(expectedExceptions = ConflictException.class) 
    public void parseInvalidRevision() throws ConflictException {
        int ver = DocumentUtil.parseVersion("some-text-98765");
        assertThat(false).isTrue().overridingErrorMessage("Parsing of invalid revision must fail, but did not.");
    }

    @Test
    public void testNestedObjectMarshaling() throws IOException {
        final JsonValue scriptJson =
                new JsonValue(new ObjectMapper().readValue(getClass().getResourceAsStream("/script.json"), Map.class));
        final ODocument scriptODoc = DocumentUtil.toDocument(scriptJson, null, getDatabase(), orientDocClass);
        final Map<String, Object> roundTripMap = DocumentUtil.toMap(scriptODoc);
        assertThat(roundTripMap).isEqualTo(scriptJson.asMap());
    }
}

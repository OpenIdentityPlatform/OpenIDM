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

import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.testng.annotations.*;
import static org.testng.Assert.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class DocumentUtilTest {
	
	@Test
	public void docToMapNullTest() {
	    assertNull(DocumentUtil.toMap(null));
	}
	
	@Test
	public void flatDocToMapTest() {
				
		ODocument doc = new ODocument();
	    doc.field("firstname", "Sam");
		doc.field("lastname", "Iam");
		doc.field("telephone", "(555) 123-4567");
		doc.field("age", 20);
		doc.field("longnumber", Long.MAX_VALUE);
		doc.field("amount", Float.valueOf(12345678.89f));
		doc.field("present", Boolean.FALSE);
		doc.field("somedate", new Date());

		Map result = DocumentUtil.toMap(doc);
		
		assertThat(result).includes(
				entry("_id", "-1:-1"), // ID when doc not yet stored
				entry("_rev", 0),      // Doc version starts at 0
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
	}
	
	@Test(enabled=false) // TODO: work in progress
	public void nestedDocToMapTest() {
		
		ODocument doc = new ODocument();

	    doc.field("firstname", "John");
		doc.field("lastname", "Doe");
		doc.field("city", new ODocument().field("name","Paris").field("country", "France"));
		doc.field("phonenumbers", new ODocument().field("home","555-666-7777").field("mobile", "555-111-2222"));
		List<ODocument> addresses = new ArrayList<ODocument>();
		addresses.add(new ODocument().field("type", "home").field("street", "Main st.").field("city", "San Franciso") );
		addresses.add(new ODocument().field("type", "business").field("street", "Wall st.").field("city", "New York") );
		doc.field("addresses",  addresses ); 
		
		Map result = DocumentUtil.toMap(doc);
		
		assertThat(result).includes(
				entry("_id", "-1:-1"), // ID when doc not yet stored
				entry("_rev", 0), // Doc version starts at 0
				entry("firstname", "John"), 
				entry("lastname", "Doe"));
		
		Object phonenumbers = result.get("phonenumbers");
		assertNotNull(phonenumbers, "phonenumbers map entry null");
		assertThat(phonenumbers).isInstanceOf(Map.class);
		assertThat((Map)phonenumbers)
		        .hasSize(2)
		        .includes(entry("home", "555-666-7777"), entry("mobile", "555-111-2222"));
		
		Object addrResult = result.get("addresses");
		assertNotNull(addrResult, "addresses map entry null");
		assertThat(addrResult).isInstanceOf(List.class);
		List addr = (List)result;
		assertThat(addr)
		        .hasSize(2);

		assertThat(addr.get(0)).isInstanceOf(Map.class);
		Map firstEntry = (Map) addr.get(0); 
		assertThat(firstEntry).includes(
				entry("type", "home"),
				entry("street", "Main st."),
				entry("city", "San Franciso"));
		Map secondEntry = (Map) addr.get(1); 
		assertThat(secondEntry).includes(
				entry("type", "business"),
				entry("street", "Wall st."),
				entry("city", "New York"));	
		
	}
}
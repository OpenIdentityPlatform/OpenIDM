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
 * Portions copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.sync.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;   
import java.util.Iterator;
import java.util.LinkedList;
import java.util.LinkedHashSet;

import org.forgerock.json.JsonValue;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

import org.forgerock.openidm.sync.impl.ResultIterable;

public class ResultIterableTest {
    
    @Test
    public void testRemoveWithFullObjects() throws Exception {
        int numItems = 6;
        ResultIterable riSource;
        ResultIterable riNew;
        Collection<String> ids;
        Collection<String> expectedValues;

        // Create a new ResultIterable object
        // true -> full objects
        // 100 as start number for ids
        riSource = createResultIterable(numItems, true, 100);

        // ids is the list of ids to be matched,
        // all other entries will be removed
        ids = Arrays.asList("Id100", "Id101", "Id102", "Id103", "Id104", "Id105");
        
        // Test removing the above ids
        riNew = riSource.removeNotMatchingEntries(ids);
        
        // riNew should now contain all ids that we wanted to match
        assertThat(riNew.getAllIds()).containsExactlyElementsOf(ids);
        
        // Using full objects the resulting values should be in sync with the ids
        expectedValues = Arrays.asList("Value100", "Value101", "Value102", "Value103", "Value104", "Value105");
        assertThat(getResultIterableValues(riNew)).containsExactlyElementsOf(expectedValues);
        
        // Repeat the test a few times with different data 
        riSource = createResultIterable(numItems, true, 200);
        ids = Arrays.asList("Id202", "Id203", "Id205");
        riNew = riSource.removeNotMatchingEntries(ids);
        assertThat(riNew.getAllIds()).containsExactlyElementsOf(ids);
        expectedValues = Arrays.asList("Value202", "Value203", "Value205");
        assertThat(getResultIterableValues(riNew)).containsExactlyElementsOf(expectedValues);
        
        riSource = createResultIterable(numItems, true, 300);
        ids = Arrays.asList("Id301", "Id302", "Id304");
        riNew = riSource.removeNotMatchingEntries(ids);
        assertThat(riNew.getAllIds()).containsExactlyElementsOf(ids);
        expectedValues = Arrays.asList("Value301", "Value302", "Value304");
        assertThat(getResultIterableValues(riNew)).containsExactlyElementsOf(expectedValues);
        
        // Nothing should match the id "bogus",
        // riNew should have empty ids and values
        riSource = createResultIterable(numItems, false, 400);
        ids = Arrays.asList("bogus");
        riNew = riSource.removeNotMatchingEntries(ids);
        assertThat(riNew.getAllIds()).isEmpty();
        assertThat(getResultIterableValues(riNew)).isEmpty();
    }

    @Test
    public void testRemoveWithIdsOnly() throws Exception {
        int numItems = 6;
        ResultIterable riSource;
        ResultIterable riNew;
        Collection<String> ids;
        Collection<String> expectedValues;

        // Create a new ResultIterable object
        // false -> no full objects
        // 500 as start number for ids
        riSource = createResultIterable(numItems, false, 500);
        
        // ids is the list of ids to be matched,
        // all other entries will be removed
        ids = Arrays.asList("Id500", "Id501", "Id502", "Id503", "Id504", "Id505");
        
        // Test removing the above ids
        riNew = riSource.removeNotMatchingEntries(ids);
                
        // riNew should now contain all ids that we wanted to match
        assertThat(riNew.getAllIds()).containsExactlyElementsOf(ids);
        
        // If ResultIterable has no full objects the values should all be null
        expectedValues = Arrays.asList(null, null, null, null, null, null);
        assertThat(getResultIterableValues(riNew)).containsExactlyElementsOf(expectedValues);
        
        // Repeat the test a few times with different data 
        riSource = createResultIterable(numItems, false, 600);
        ids = Arrays.asList("Id602", "Id603", "Id605");
        riNew = riSource.removeNotMatchingEntries(ids);
        assertThat(riNew.getAllIds()).containsExactlyElementsOf(ids);
        expectedValues = Arrays.asList(null, null, null);
        assertThat(getResultIterableValues(riNew)).containsExactlyElementsOf(expectedValues);
        
        riSource = createResultIterable(numItems, false, 700);
        ids = Arrays.asList("Id701", "Id702", "Id704");
        riNew = riSource.removeNotMatchingEntries(ids);
        assertThat(riNew.getAllIds()).containsExactlyElementsOf(ids);
        expectedValues = Arrays.asList(null, null, null);
        assertThat(getResultIterableValues(riNew)).containsExactlyElementsOf(expectedValues);
        
        // Nothing should match the id "bogus",
        // riNew should have empty ids and values
        riSource = createResultIterable(numItems, false, 800);
        ids = Arrays.asList("bogus");
        riNew = riSource.removeNotMatchingEntries(ids);
        assertThat(riNew.getAllIds()).isEmpty();
        assertThat(getResultIterableValues(riNew)).isEmpty();
    }

    ResultIterable createResultIterable(int numItems, boolean fullObject, int startId) {
        Collection<String> newIds = Collections.synchronizedSet(new LinkedHashSet<String>());
        JsonValue newObjList = null;

        if (fullObject) {
            newObjList = new JsonValue(new LinkedList());
        }
        
        for (int i = 0; i < numItems; i++) {
                newIds.add("Id" + (startId + i));

                if(fullObject) {
                    String data = "Value" + (startId + i);
                    newObjList.add(new JsonValue(data));
                }
        }

        return new ResultIterable(newIds, newObjList);
    }
    
    Collection<String> getResultIterableValues(ResultIterable ri) {
        ArrayList<String> values = new ArrayList<String>();
        Iterator<ResultEntry> it = ri.iterator();
        
        while (it.hasNext()) {
            ResultEntry re = it.next();
            JsonValue jv = re.getValue();
            
            if (jv == null) {
                values.add(null);
            }
            else if (jv.isString()) {
                values.add(jv.asString());
            }
            else {
                values.add("***unexpected***");
            }
        }

        return values;
    }
};

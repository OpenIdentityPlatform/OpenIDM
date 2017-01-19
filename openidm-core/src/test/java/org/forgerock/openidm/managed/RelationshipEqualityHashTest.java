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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openidm.managed;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openidm.util.RelationshipUtil;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RelationshipEqualityHashTest {
    @DataProvider(name = "distinctRelationshipData")
    public Object[][] createDistinctRelationshipData() {
        return new Object[][] {
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", "temporalConstraint1", true)),
                        makeRelationship("ref2", makeRefPropertiesMap("grantType2", "temporalConstraint2", true)) },
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", "temporalConstraint1", true)),
                        makeRelationship("ref2", makeRefPropertiesMap("grantType1", "temporalConstraint1", false)) },
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", "temporalConstraint1", true)),
                        makeRelationship("ref2", makeRefPropertiesMap("grantType2", "temporalConstraint2", true)) },
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", "temporalConstraint1", true)),
                        makeRelationship("ref1", makeRefPropertiesMap(null, "temporalConstraint2", true))},
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", "temporalConstraint1", true)),
                        makeRelationship("ref1", makeRefPropertiesMap("grantType1", null, true)) },
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", true)),
                        makeRelationship("ref2", makeRefPropertiesMap("grantyType1", true)) }
        };
    }

    @Test(dataProvider="distinctRelationshipData")
    public void testEqualsAndHashCode(JsonValue rel1, JsonValue rel2) {
        assertFalse(new RelationshipEqualityHash(rel1).hashCode() == new RelationshipEqualityHash(rel2).hashCode(),
                "hash code of relationships should be true. rel1: " + rel1.toString() + "\n rel2: " + rel2.toString());
        assertFalse(new RelationshipEqualityHash(rel1).equals(new RelationshipEqualityHash(rel2)),
                "relationships should be equal. rel1: " + rel1.toString() + "\n rel2: " + rel2.toString());
    }

    @DataProvider(name = "duplicateRelationshipData")
    public Object[][] createDuplicateRelationshipData() {
        return new Object[][] {
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", "temporalConstraint1", true)),
                        makeRelationship("ref1", makeRefPropertiesMap("grantType1", "temporalConstraint1", false)) },
                { makeRelationship("ref1", makeRefPropertiesMap(null, "temporalConstraint1", true)),
                        makeRelationship("ref1", makeRefPropertiesMap(null, "temporalConstraint1", false))},
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", null, true)),
                        makeRelationship("ref1", makeRefPropertiesMap("grantType1", null, true)) },
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", false)),
                        makeRelationship("ref1", makeRefPropertiesMap("grantType1", false)) },
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", "temporalConstraint1", true)),
                        makeRelationship("ref1", makeRefPropertiesMap("grantType1", "temporalConstraint1", true)) },
                { makeRelationship("ref1", makeRefPropertiesMap(null, "temporalConstraint1", false)),
                        makeRelationship("ref1", makeRefPropertiesMap(null, "temporalConstraint1", true))},
                { makeRelationship("ref1", makeRefPropertiesMap("grantType1", null, true)),
                        makeRelationship("ref1", makeRefPropertiesMap("grantType1", null, false)) },
                { makeRelationship("ref1", null),
                        makeRelationship("ref1", makeRefPropertiesMap()) },
        };
    }

    @Test(dataProvider="duplicateRelationshipData")
    public void testDuplicateEqualsAndHashCode(JsonValue rel1, JsonValue rel2) {
        assertTrue(new RelationshipEqualityHash(rel1).hashCode() == new RelationshipEqualityHash(rel2).hashCode(),
                "hash code of relationships should be equal. rel1: " + rel1.toString() + "\n rel2: " + rel2.toString());
        assertTrue(new RelationshipEqualityHash(rel1).equals(new RelationshipEqualityHash(rel2)),
                "relationships should be equal. rel1: " + rel1.toString() + "\n rel2: " + rel2.toString());
    }

    private Map<String, Object> makeRefPropertiesMap(String grantType, String temporalConstraint, boolean idPresent) {
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put(RelationshipValidator.TEMPORAL_CONSTRAINTS, Collections.singletonList(temporalConstraint));
        fieldMap.put(RelationshipValidator.GRANT_TYPE, grantType);
        return makeRefProperties(fieldMap, idPresent);
    }

    private Map<String, Object> makeRefPropertiesMap(String grantType, boolean idPresent) {
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put(RelationshipValidator.GRANT_TYPE, grantType);
        return makeRefProperties(fieldMap, idPresent);
    }

    private Map<String, Object> makeRefPropertiesMap() {
        return makeRefProperties(Collections.<String, Object>emptyMap(), true);
    }

    private JsonValue makeRelationship(String referenceId, Map<String, Object> refProperties) {
        return json(object(
                makeField(RelationshipUtil.REFERENCE_ID, referenceId),
                makeField(RelationshipUtil.REFERENCE_PROPERTIES, refProperties)
        ));
    }

    private Map<String, Object> makeRefProperties(Map<String, Object> fields, boolean idPresent) {
        // simulate both a patch and a create (id, rev present, or not)
        final JsonValue refProps = json(object());
        if (idPresent) {
            refProps.add("_id", "xxyyzz");
            refProps.add("_rev", 0);
        }
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            refProps.add(entry.getKey(), entry.getValue());
        }
        return refProps.asMap();
    }

    /*
    A 'special' field constructor which can simply return null, as opposed to JsonValue#field, which always creates an entry.
     */
    private Map.Entry<String, Object> makeField(String key, Object value) {
        if (value != null) {
            return new AbstractMap.SimpleImmutableEntry<>(key, value);
        }
        return null;
    }
}

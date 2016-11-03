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
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_REVISION;
import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_ID;
import static org.forgerock.openidm.util.RelationshipUtil.REFERENCE_PROPERTIES;

import org.forgerock.json.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * A class which over-rides hash-code (and thus, equals) to examine only those fields which constitute the uniqueness
 * of a relationship so that a HashSet can be used to determine whether duplicate relationships are in a list.
 *
 * Note that the relationship passed to this class' ctor could be from a patch request. In this case, the
 * _refProperties of the existing relationship will have an _id. The relationship specified in the patch will not.
 * Thus it is important that the hash of this class be constituted only by the _ref of the relationship, and the non _id
 * and _ref fields of the relationship _refProperties.
 */
final class RelationshipEqualityHash {
    private final JsonValue relationship;
    private final JsonValue relationshipRefProperties;

    RelationshipEqualityHash(JsonValue relationship) {
        Objects.requireNonNull(relationship, "Provided relationship must be non-null.");
        this.relationship = relationship;
        this.relationshipRefProperties = relationship.get(REFERENCE_PROPERTIES);
    }

    @Override
    public int hashCode() {
        /*
        The hashCode must be composed of the relationship _ref and all non _id and _rev fields of the _refProperties.
        Note that these values must compose the hash in the same order, so the fields are hashed in the same order.
         */
        List<Object> hashConstituents = new ArrayList();
        hashConstituents.add(relationship.get(REFERENCE_ID).getObject());
        //A TreeSet is a SortedSet, so members will be retrieved in sorted order
        Set<String> refPropertyConstituentSet = new TreeSet<>(relationshipRefProperties.keys());
        for (String property : refPropertyConstituentSet) {
            if (!FIELD_CONTENT_REVISION.equals(property) && !FIELD_CONTENT_ID.equals(property)) {
                hashConstituents.add(relationshipRefProperties.get(property).getObject());
            }
        }
        return Objects.hash(hashConstituents.toArray());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof RelationshipEqualityHash) {
            final RelationshipEqualityHash otherHash = (RelationshipEqualityHash)other;
            return relationshipsEqual(relationship, otherHash.relationship);
        }
        return false;
    }

    static boolean relationshipsEqual(JsonValue relationship1, JsonValue relationship2) {
        return Objects.equals(relationship1.get(REFERENCE_ID).getObject(), relationship2.get(REFERENCE_ID).getObject())
                && relationshipRefPropertiesEqual(relationship1.get(REFERENCE_PROPERTIES), relationship2.get(REFERENCE_PROPERTIES));
    }

    /**
     * Two relationships are considered to be equal if both _ref values are the same, and the _refProperties are the
     * same, as determined by the isEqual method of this class. However, there is an additional wrinkle: if one relationship
     * has _refProperties which only have _id and _rev fields, and the other relationship has no _refProperties, this
     * _refProperty state is considered equal. This  method will implement these additional semantics.
     * @param relRefProps1 the _refProperties of the first relationship to be compared for equality
     * @param relRefProps2 the _refProperties of the second relationship to be compared for equality
     * @return true or false, if the _refProperty values are equal, according to the semantics above
     */
    static boolean relationshipRefPropertiesEqual(JsonValue relRefProps1, JsonValue relRefProps2) {
        return Objects.equals(stripIdAndRevFields(relRefProps1).getObject(), stripIdAndRevFields(relRefProps2).getObject());
    }

    private static JsonValue stripIdAndRevFields(JsonValue resource) {
        if ((resource == null) || resource.isNull()) {
            //normalize to JsonValue encapsulating Map
            return json(object());
        } else {
            JsonValue strippedResource = resource.copy();
            strippedResource.remove(FIELD_CONTENT_ID);
            strippedResource.remove(FIELD_CONTENT_REVISION);
            return strippedResource;
        }
    }
}

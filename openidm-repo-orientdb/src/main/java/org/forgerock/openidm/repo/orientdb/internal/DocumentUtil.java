/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.repo.orientdb.internal;

import static org.forgerock.openidm.util.ResourceUtil.isSpecialAttribute;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * A utility class for handling and converting OrientDB ODocuments.
 *
 * @author aegloff
 */
public class DocumentUtil {

    /**
     * Setup logging for the {@link DocumentUtil}.
     */
    final static Logger logger = LoggerFactory.getLogger(DocumentUtil.class);

    // Identifier in the DB representation
    public final static String ORIENTDB_PRIMARY_KEY = "_openidm_id";

    /**
     * Convert to JSON object structures (akin to simple binding), composed of
     * the basic Java types: {@link Map}, {@link List}, {@link String},
     * {@link Number}, {@link Boolean}.
     *
     * @param doc
     *            the OrientDB document to convert
     * @return the doc converted into maps, lists, java types; or null if the
     *         doc was null
     * @throws JsonException
     *             when the JSON can not be parsed
     */
    public static Resource toResource(ODocument doc) {
        if (null == doc) {
            return null;
        }
        JsonValue result = JsonUtil.parseStringified(doc.toJSON());

        if (null != result) {
            String id = doc.field(ORIENTDB_PRIMARY_KEY, String.class);
            if (id == null) {
                id = doc.getIdentity().toString().substring(1);
            }
            for (String key : result.keys()) {
                if (key.startsWith("@")) {
                    result.remove(key);
                }
            }
            result.remove(ORIENTDB_PRIMARY_KEY);
            result.put(Resource.FIELD_CONTENT_ID, id);
            result.put(Resource.FIELD_CONTENT_REVISION, Integer.toString(doc.getVersion()));
            result.put("_vertex", doc.getIdentity().toString().substring(1));
            return new Resource(id, Integer.toString(doc.getVersion()), result);
        }
        return null;
    }

    /**
     * Convert from JSON object structures (akin to simple binding), composed of
     * the basic Java types: {@link Map}, {@link List}, {@link String},
     * {@link Number}, {@link Boolean}. to OrientDB document
     *
     * @param id
     * @param revision
     * @param content
     *            the JSON object structure to convert
     * @param docToPopulate
     *            an optional existing ODocument to update with new values from
     *            {@code objModel}
     * @return the converted orientdb document, or null if objModel was null
     * @throws ConflictException
     *             when the revision in the Object model is invalid
     */
    public static ODocument toDocument(final String id, final String revision,
            final JsonValue content, final ODocument docToPopulate) throws ResourceException {
        if (null == docToPopulate) {
            return null;
        }
        try {

            //TODO: Improve the performance here
            if (null != content) {
                for (String name : content.keys()) {
                    if (isSpecialAttribute(name) || name.startsWith("@")) {
                        content.remove(name);
                    }
                }
            }

            if (null != content && content.size() > 0) {
                docToPopulate.fromJSON(JsonUtil.writeValueAsString(content));
            } else {
                for (String iFieldName : docToPopulate.fieldNames()) {
                    docToPopulate.removeField(iFieldName);
                }
            }


            // OpenIDM ID mapping
            if (StringUtils.isNotBlank(id)) {
                if (!docToPopulate.containsField(ORIENTDB_PRIMARY_KEY)
                        || !docToPopulate.field(ORIENTDB_PRIMARY_KEY).equals(id)) {
                    logger.trace("Setting primary key to {}", id);
                    docToPopulate.field(ORIENTDB_PRIMARY_KEY, id);
                }
            }

            // OpenIDM revision to document version mapping
            if (StringUtils.isNotBlank(revision) && !"*".equalsIgnoreCase(revision)) {
                int rev = parseVersion(revision);
                logger.trace("Setting version to {}", rev);
                if (docToPopulate.getVersion() != rev) {
                    docToPopulate.setVersion(rev);
                }
            }

            return docToPopulate;
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to convert the content", e);
        }
    }

    /**
     * Parse an OpenIDM revision into an OrientDB MVCC version. OrientDB expects
     * these to be ints.
     *
     * @param revision
     *            the revision String with the OrientDB version in it.
     * @return the OrientDB version
     * @throws ConflictException
     *             if the revision String could not be parsed into the int
     *             expected by OrientDB
     */
    public static int parseVersion(String revision) throws ResourceException {
        int ver = -1;
        try {
            ver = Integer.parseInt(revision);
        } catch (NumberFormatException ex) {
            throw new BadRequestException("OrientDB repository expects revisions as int, "
                    + "unable to parse passed revision: " + revision);
        }
        return ver;
    }
}

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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.ConflictException;

import org.forgerock.json.resource.Responses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.json.JsonValue.json;

/**
 * A utility class for handling and converting OrientDB ODocuments
 * 
 */
public class DocumentUtil  {
    final static Logger logger = LoggerFactory.getLogger(DocumentUtil.class);
    
    // Identifiers in the object model. 
    // TDOO: replace with common definitions of these global variables 
    public final static String TAG_ID = "_id";
    public final static String TAG_REV = "_rev";
    
    // Identifier in the DB representation
    public final static String ORIENTDB_PRIMARY_KEY = "_openidm_id";

    public final static String ORIENTDB_VERSION_KEY = "version";
    
    /**
     * Convert to JSON object structures (akin to simple binding), composed of
     * the basic Java types: {@link Map}, {@link List}, {@link String},
     * {@link Number}, {@link Boolean}.
     *
     * @param doc
     *            the OrientDB document to convert
     * @return the Resource with the id, rev, and the doc converted into maps, lists, 
     *         java types; or null if the doc was null
     */
    public static ResourceResponse toResource(ODocument doc) {
        ResourceResponse result = null;
        Map<String, Object> map = toMap(doc);
        if (map != null) {
            String id = (String) map.get(ResourceResponse.FIELD_CONTENT_ID);
            String rev = (String) map.get(ResourceResponse.FIELD_CONTENT_REVISION);
            result = Responses.newResourceResponse(id, rev, new JsonValue(map));
        }
        return result;
    }
    
    /**
     * Convert to JSON object structures (akin to simple binding), 
     * composed of the basic Java types: {@link Map}, {@link List}, {@link String}, {@link Number}, {@link Boolean}.
     * @param doc the OrientDB document to convert
     * @return the doc converted into maps, lists, java types; or null if the doc was null
     */
    public static Map<String, Object> toMap(ODocument doc) {
        return toMap(doc, true);
    }

    /**
     * Convert to JSON object structures (akin to simple binding), 
     * composed of the basic Java types: {@link Map}, {@link List}, {@link String}, {@link Number}, {@link Boolean}.
     * This may change the objects in the passed doc, it is not safe to use doc contents after calling this method.
     * 
     * @param doc the OrientDB document to convert
     * @param topLevel if the passed in document represents a top level orientdb class, or false if it is an embedded document
     * @return the doc converted into maps, lists, java types; or null if the doc was null
     */
    private static Map<String, Object> toMap(ODocument doc, boolean topLevel) {        
        Map<String, Object> result = null;
        if (doc != null) {
            result = new LinkedHashMap<String, Object>(); // TODO: As JSON doesn't, do we really want to maintain order?   
            for (java.util.Map.Entry<String, Object> entry : doc) {
                Object value = entry.getValue();
                String key = entry.getKey();
                if (key.equals(ORIENTDB_PRIMARY_KEY)) {
                    logger.trace("Setting primary key to value {}", value);
                    result.put(TAG_ID, value);
                    String revision = Integer.toString(doc.getVersion());
                    if (!result.containsKey(TAG_REV)) {
                        //don't overwrite the rev value if it is already in the result
                        logger.trace("Setting revision to {}", revision);
                        result.put(TAG_REV, revision);
                    }
                } else if (key.equals(ORIENTDB_VERSION_KEY)) {
                    logger.trace("Setting revision to {}", value.toString());
                    result.put(TAG_REV, value.toString());
                } else {
                    // TODO: optimization switch: if we know that no embedded ODocuments are used 
                    // (i.e. only embedded Maps, Lists) then we would not need to traverse the whole graph
                    value = asSimpleBinding(value);
                    logger.trace("Map setting {} to value {}", key, value);
                    result.put(key, value);
                }
            }
        }
        logger.trace("Converted document {} to {}", doc, result);
        return result;
    }
    
    /**
     * Recursively ensure that the passed type is represented
     * or converted to JSON object model simple binding types,
     * i.e. ODocument to Map, Set to List 
     * 
     * Modifies the passed in objToClean where possible (List, Map), 
     * returns new types where it is not (ODocument, Set)
     * 
     * @param objToClean the object to clean/bind
     * @return the object in JSON object model representation
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object asSimpleBinding(Object objToClean) {
        if (objToClean instanceof ODocument) {
            logger.trace("Converting embedded ODocument {} to map ", objToClean);
            return DocumentUtil.toMap((ODocument) objToClean, false); 
        } else if (objToClean instanceof List) {
            logger.trace("Checking embedded list {} ", objToClean);
            return toSimpleModel((List) objToClean);
        } else if (objToClean instanceof Set) {
            logger.trace("Converting embedded Set {} ", objToClean);
            return toSimpleModel((Set) objToClean);
        } else if (objToClean instanceof Map) {
            logger.trace("Checking embedded map {} ", objToClean);
            return toSimpleModel((Map) objToClean);
        } else if (objToClean instanceof com.orientechnologies.orient.core.id.ORID) {
            // OrientDB should have resolved to an ODocument, might indicate a bug in OrientDB
            logger.warn("Unexpected value of type ORecordId in document. Returning as String.{}", objToClean);
            return objToClean.toString();
        } else {
            return objToClean;
        }
    }
    
    /**
     * Iteratively convert contents as necessary to simple model 
     * @param listToClean list to modify if necessary
     * @return the modified list
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List toSimpleModel(List listToClean) {
        ListIterator<Object> listIter = listToClean.listIterator();
        while(listIter.hasNext()) {
            Object listEntry = listIter.next();
            if (listEntry instanceof ODocument || listEntry instanceof Set) {
                // Replace the entry with new type
                listIter.set(asSimpleBinding(listEntry)); 
            } else {
                // Replace directly in the entry
                asSimpleBinding(listEntry);
            } 
        }
        return listToClean;
    }

    /**
     * Iteratively convert contents as necessary to simple model 
     * @param setToClean set to convert to List and modify if necessary
     * @return the modified list
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static List toSimpleModel(Set setToClean) {
        // In JSON there are ordered lists, not Set
        List replacementList = new ArrayList();
        for (Object setEntry : setToClean) {
            replacementList.add(asSimpleBinding(setEntry));
        }
        return replacementList;
    }

    /**
     * Iteratively convert contents as necessary to simple model 
     * @param mapToClean map to modify if necessary
     * @return the modified map
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map toSimpleModel(Map<String, Object> mapToClean) {
        for(Map.Entry<String, Object> entry : mapToClean.entrySet()) {
            entry.setValue(asSimpleBinding(entry.getValue()));
        }
        return mapToClean;
    }    
    
    /**
     * Convert from JSON object structures (akin to simple binding), 
     * composed of the basic Java types: {@link Map}, {@link List}, {@link String}, {@link Number}, {@link Boolean}.
     * to OrientDB document
     * @param objModel the JSON object structure to convert
     * @param docToPopulate an optional existing ODocument to update with new values from {@code objModel}
     * @param db the database to associate with the ODocument
     * @param orientDocClass the OrientDB class of the ODocument to create
     * @return the converted orientdb document, or null if objModel was null
     * @throws ConflictException when the revision in the Object model is invalid
     */
    public static ODocument toDocument(JsonValue objModel, ODocument docToPopulate, ODatabaseDocumentTx db, String orientDocClass)
            throws ConflictException {
        return toDocument(objModel.asMap(), docToPopulate, db, orientDocClass, false, true);
    }
    
    /**
     * Convert from JSON object structures (akin to simple binding), 
     * composed of the basic Java types: {@link Map}, {@link List}, {@link String}, {@link Number}, {@link Boolean}.
     * to OrientDB document
     * @param objModel the JSON object structure to convert
     * @param docToPopulate an optional existing ODocument to update with new values from {@code objModel}
     * @param db the database to associate with the ODocument
     * @param orientDocClass the OrientDB class of the ODocument to create
     * @return the converted orientdb document, or null if objModel was null
     * @throws ConflictException when the revision in the Object model is invalid
     */
    public static ODocument toDocument(Map<String, Object> objModel, ODocument docToPopulate, ODatabaseDocumentTx db, String orientDocClass)
            throws ConflictException {
        return toDocument(objModel, docToPopulate, db, orientDocClass, false, true);
    }
    
    /**
     * Convert from JSON object structures (akin to simple binding), 
     * composed of the basic Java types: {@link Map}, {@link List}, {@link String}, {@link Number}, {@link Boolean}.
     * to OrientDB document
     * @param objModel the JSON object structure to convert
     * @param docToPopulate an optional existing ODocument to update with new values from {@code objModel}
     * @param db the database to associate with the ODocument
     * @param orientDocClass the OrientDB class of the ODocument to create
     * @param patch whether the objModel passed in is only partial values (replacing and adding values), 
     * or if false replaces the whole document with the given {@code objModel}
     * @param topLevel
     * @return the converted orientdb document, or null if objModel was null
     * @throws ConflictException when the revision in the Object model is invalid
     */
    protected static ODocument toDocument(Map<String, Object> objModel, ODocument docToPopulate, ODatabaseDocumentTx db, String orientDocClass, boolean patch, 
            boolean topLevel) throws ConflictException {
        
        ODocument result = null;
        if (objModel != null) {
            if (docToPopulate == null) {
                result = db.newInstance(orientDocClass);
                result.setAllowChainedAccess(false);
            } else {
                result = docToPopulate;
                if (!patch) {
                    // Remove entries from existing doc that don't exist anymore. ODocument.reset resets too much.
                    List<String> removalList = new ArrayList<String>();
                    for (java.util.Map.Entry<String, Object> entry : result) {
                        String key = entry.getKey();
                        if (!key.equals(ORIENTDB_PRIMARY_KEY) && !objModel.containsKey(key)) {
                            removalList.add(key);
                        }
                    }
                    for (String entry : removalList) {
                        logger.trace("Removing entry {} ", entry);
                        result.removeField(entry);
                    }
                }
            }

            for (Map.Entry<String, Object> entry : objModel.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (entry.getKey().equals(TAG_ID)) {
                    // OpenIDM ID mapping
                    if (topLevel) {
                        if (!result.containsField(ORIENTDB_PRIMARY_KEY) 
                                || !result.field(ORIENTDB_PRIMARY_KEY).equals(value)) {
                            logger.trace("Setting primary key to {}", value);
                            result.field(ORIENTDB_PRIMARY_KEY, value);
                        }
                    } else {
                        logger.trace("Setting field {} to value {}", key, value);
                        result.field(entry.getKey(), value);
                    }
                } else if (key.equals(TAG_REV)) {
                    // OpenIDM revision to document version mapping
                    if (topLevel) {
                        String revString = (String) objModel.get(TAG_REV);
                        if (revString != null) {
                            int rev = parseVersion(revString);
                            logger.trace("Setting version to {}", rev);
                            if (result.getVersion() != rev) {
                                result.setVersion(rev);
                            }
                        }
                    } else {
                        logger.trace("Setting field {} to value {}", key, value);
                        result.field(entry.getKey(), value);
                    }
                } else if (value instanceof Map) {
                    // TODO: consider if we should replace this with nested maps rather than nested ODocuments
                    logger.trace("Handling field {} with embedded map {}", key, value);
                    ODocument existingDoc = null;
                    if (docToPopulate != null) {
                        logger.trace("Update existing embedded map entry {}", key);
                        Object o = docToPopulate.field(entry.getKey());
                        if (o instanceof ODocument) {
                            existingDoc = (ODocument) o;
                        } else {
                            docToPopulate.removeField(entry.getKey());
                        }
                    }
                    // TODO: below is temporary work-around for OrientDB update not saving embedded ODocument,
                    // unless it is a new instance
                    //if (existingDoc == null) {
                    logger.trace("Instantiate new ODocument to represent embedded map for {}.", key);
                    existingDoc = new ODocument();
                    //necessary for fields which contain '.'. See javadocs for ODocument.field for details
                    existingDoc.setAllowChainedAccess(false);

                    //} 
                    ODocument converted = toDocument(json(value).asMap(), existingDoc, db, null, patch, false);
                    result.field(entry.getKey(), converted, OType.EMBEDDED);
                } else {
                    logger.trace("Setting field {} to value {}", key, value);
                    result.field(entry.getKey(), value);
                }
            }
        }

        return result;
    }

    /**
     * Parse an OpenIDM revision into an OrientDB MVCC version. OrientDB expects these to be ints.
     * @param revision the revision String with the OrientDB version in it.
     * @return the OrientDB version
     * @throws ConflictException if the revision String could not be parsed into the int expected by OrientDB
     */
    public static int parseVersion(String revision) throws ConflictException { 
        int ver = -1;
        try {
            ver = Integer.parseInt(revision);
        } catch (NumberFormatException ex) {
            throw new ConflictException("OrientDB repository expects revisions as int, " 
                    + "unable to parse passed revision: " + revision);
        }
        return ver;
    }
}
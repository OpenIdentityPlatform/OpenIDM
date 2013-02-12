/////*
//// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
//// *
//// * Copyright © 2011 ForgeRock AS. All rights reserved.
//// *
//// * The contents of this file are subject to the terms
//// * of the Common Development and Distribution License
//// * (the License). You may not use this file except in
//// * compliance with the License.
//// *
//// * You can obtain a copy of the License at
//// * http://forgerock.org/license/CDDLv1.0.html
//// * See the License for the specific language governing
//// * permission and limitations under the License.
//// *
//// * When distributing Covered Code, include this CDDL
//// * Header Notice in each file and include the License file
//// * at http://forgerock.org/license/CDDLv1.0.html
//// * If applicable, add the following below the CDDL Header,
//// * with the fields enclosed by brackets [] replaced by
//// * your own identifying information:
//// * "Portions Copyrighted [year] [name of copyright owner]"
//// *
//// * $Id$
//// */
////
//package org.forgerock.openidm.provisioner.openicf.internal;
//
//import org.forgerock.json.fluent.JsonValue;
//import org.forgerock.json.resource.ActionRequest;
//import org.forgerock.json.resource.CreateRequest;
//import org.forgerock.json.resource.DeleteRequest;
//import org.forgerock.json.resource.Patch;
//import org.forgerock.json.resource.PatchRequest;
//import org.forgerock.json.resource.PersistenceConfig;
//import org.forgerock.json.resource.QueryFilter;
//import org.forgerock.json.resource.QueryRequest;
//import org.forgerock.json.resource.ReadRequest;
//import org.forgerock.json.resource.Requests;
//import org.forgerock.json.resource.ResourceException;
//import org.forgerock.json.resource.Resources;
//import org.forgerock.json.resource.ServerContext;
//import org.forgerock.json.resource.UpdateRequest;
//
//import java.util.HashMap;
//
////
////import org.forgerock.json.crypto.JsonCryptoException;
////import org.forgerock.json.fluent.JsonValue;
////import org.forgerock.json.resource.ForbiddenException;
////import org.forgerock.json.resource.JsonResourceException;
////import org.forgerock.json.resource.Resource;
////import org.forgerock.json.resource.ResourceException;
////import org.forgerock.openidm.core.ServerConstants;
////import org.forgerock.openidm.crypto.CryptoService;
////import org.forgerock.openidm.provisioner.Id;
////import org.forgerock.openidm.provisioner.openicf.OperationHelper;
////import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
////import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
////import org.forgerock.openidm.provisioner.openicf.query.OperatorFactory;
////import org.forgerock.openidm.provisioner.openicf.query.operators.BooleanOperator;
////import org.forgerock.openidm.provisioner.openicf.query.operators.Operator;
////import org.identityconnectors.common.Assertions;
////import org.identityconnectors.framework.api.operations.APIOperation;
////import org.identityconnectors.framework.common.objects.*;
////import org.identityconnectors.framework.common.objects.filter.Filter;
////
////import java.io.IOException;
////import java.lang.reflect.UndeclaredThrowableException;
////import java.net.URI;
////import java.util.ArrayList;
////import java.util.List;
////import java.util.Map;
////
////import static org.forgerock.openidm.provisioner.openicf.query.QueryUtil.*;
////
/////**
//// * @author $author$
//// * @version $Revision$ $Date$
//// */
//public class OperationHelper  {
//
//    public void demo() throws ResourceException {
//        ServerContext serverContext = null;
//        PersistenceConfig persistenceConfig = null;
//
//        boolean isPostNotPUT = true;
//
//        String resourceContainer = "managed/user";
//        String resourceId = "DDOE";
//        String newResourceId = isPostNotPUT ? null : resourceId;
//
//        String resourceContainerAndId = resourceContainer + "/" + resourceId;
//
//
//        JsonValue context = null;
//
//        //OLD: create(String resourceContainer,  Map content)
//        //NEW: create(String resourceContainer[, newResourceId id], Map content[, List fieldFilter][,Map context])
//        CreateRequest cr = Requests.newCreateRequest(resourceContainer, newResourceId, new JsonValue(null /*Map content*/));
//        // add fieldFilter
//        cr.addField("_id", "address/street");
//        if (null != context) {
//            ServerContext restoredContext = ServerContext.loadFromJson(context, persistenceConfig);
//             /*return*/ restoredContext.getConnection().create(restoredContext, cr);
//        }   else  {
//             /*return*/ serverContext.getConnection().create(serverContext, cr);
//        }
//
//
//        //OLD: read(String resourceContainerAndId)
//        //NEW: read(String resourceContainerAndId[, List fieldFilter][,Map context])
//        ReadRequest rr = Requests.newReadRequest(resourceContainerAndId);
//        // add fieldFilter
//        rr.addField("_id", "address/street");
//        if (null != context) {
//            ServerContext restoredContext = ServerContext.loadFromJson(context, persistenceConfig);
//             /*return*/ restoredContext.getConnection().read(restoredContext, rr);
//        }   else  {
//             /*return*/ serverContext.getConnection().read(serverContext, rr);
//        }
//
//        //OLD: update(String resourceContainerAndId, String revision, Map content)
//        //NEW: update(String resourceContainerAndId[, String revision], Map content [, List fieldFilter][,Map context])
//        UpdateRequest ur = Requests.newUpdateRequest(resourceContainerAndId,  new JsonValue(null /*Map content*/));
//        // add fieldFilter
//        ur.addField("_id", "address/street");
//        // set revision
//        ur.setRevision("revision");
//        if (null != context) {
//            ServerContext restoredContext = ServerContext.loadFromJson(context, persistenceConfig);
//             /*return*/ restoredContext.getConnection().update(restoredContext, ur);
//        }   else  {
//             /*return*/ serverContext.getConnection().update(serverContext, ur);
//        }
//
//
//        //OLD: query(String resourceContainer, Map params)
//        //NEW: I have not idea: query(String resourceContainer[, Map params][, String filter][, List fieldFilter][,Map context])
//        //NEW: I have not idea: queryExpression(String resourceContainer[, Map params][, String filter][, List fieldFilter][,Map context])
//        //NEW: I have not idea: queryFilter(String resourceContainer[, Map params][, String filter][, List fieldFilter][,Map context])
//        QueryRequest qr = Requests.newQueryRequest(resourceContainer);
//        // add fieldFilter
//        qr.addField("_id", "address/street");
//        // add sortkey
//        qr.addSortKey("name", "age");
//        //get the params
//        qr.setAdditionalQueryParameter("name","String value");
//
//        qr.setPagedResultsCookie("cookie");
//
//        qr.setPagedResultsOffset(12);
//
//        qr.setPageSize(1000);
//
//
//        //Select of of these three only
//        qr.setQueryExpression("SELECT * FROM Users");
//
//        qr.setQueryId("query-all-ids");
//
//        qr.setQueryFilter(QueryFilter.valueOf("name eq DDOE"));
//
//
//
//        //OLD: delete(String resourceContainerAndId, String rev)
//        //NEW: delete(String resourceContainerAndId[, String rev][, List fieldFilter][,Map context])
//        DeleteRequest dr = Requests.newDeleteRequest(resourceContainerAndId);
//        // add fieldFilter
//        dr.addField("_id", "address/street");
//        // set revision
//        dr.setRevision("revision");
//        if (null != context) {
//            ServerContext restoredContext = ServerContext.loadFromJson(context, persistenceConfig);
//             /*return*/ restoredContext.getConnection().delete(restoredContext, dr);
//        }   else  {
//             /*return*/ serverContext.getConnection().delete(serverContext, dr);
//        }
//
//        //OLD: action(String resourceContainerAndId, Map params[, Map content])
//        //NEW: I have not idea: action(String resourceContainerAndId, String actionId, Map params, Map content[, List fieldFilter][,Map context])
//        ActionRequest ar = Requests.newActionRequest(resourceContainerAndId, "create");
//        // add fieldFilter
//        ar.addField("_id", "address/street");
//        //get the params
//        ar.setAdditionalActionParameter("name","String value");
//
//        if (null != context) {
//            ServerContext restoredContext = ServerContext.loadFromJson(context, persistenceConfig);
//             /*return*/ restoredContext.getConnection().action(restoredContext, ar);
//        }   else  {
//             /*return*/ serverContext.getConnection().action(serverContext, ar);
//        }
//
//        //OLD: patch(String resourceContainerAndId[, String rev], Map patch[, List fieldFilter][,Map context])
//        //NEW: patch(String resourceContainerAndId, Map patch [, String rev][, List fieldFilter][,Map context])
//        PatchRequest pr = Requests.newPatchRequest(resourceContainerAndId, new Patch());
//        // add fieldFilter
//        pr.addField("_id", "address/street");
//        // set revision
//        pr.setRevision("revision");
//        if (null != context) {
//            ServerContext restoredContext = ServerContext.loadFromJson(context, persistenceConfig);
//             /*return*/ restoredContext.getConnection().patch(restoredContext, pr);
//        }   else  {
//             /*return*/ serverContext.getConnection().patch(serverContext, pr);
//        }
//
//    }
//
//}
////
////    private final ObjectClassInfoHelper objectClassInfoHelper;
////    private final Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations;
////
////
////    /**
////     * @param objectClassInfoHelper
////     * @param connectorObjectOptions
////     * @throws NullPointerException if any of the input values is null.
////     */
////    public OperationHelper(final ObjectClassInfoHelper objectClassInfoHelper,
////                           final Map<Class<? extends APIOperation>, OperationOptionInfoHelper> connectorObjectOptions) {
////        this.objectClassInfoHelper = Assertions.nullChecked(objectClassInfoHelper, "objectClassInfoHelper");
////        this.operations = Assertions.nullChecked(connectorObjectOptions, "connectorObjectOptions");
////    }
////
////
////
////
////
////    /**
////     * Gets the {@link ObjectClass} value of this newBuilder.
////     *
////     * @return
////     */
////    public ObjectClass getObjectClass() {
////        return objectClassInfoHelper.getObjectClass();
////    }
////
////
////
////
////    public ConnectorObject build(Class<? extends APIOperation> operation, JsonValue source) throws Exception {
////        return objectClassInfoHelper.build(operation, null, source, cryptoService);
////    }
////
////
////    public ConnectorObject build(Class<? extends APIOperation> operation, String id, JsonValue source) throws Exception {
////        //TODO do something with ID
////        return objectClassInfoHelper.build(operation, id, source, cryptoService);
////    }
////
////    /**
////     * Build a new Map object from the {@code source} object.
////     * <p/>
////     * This class uses the embedded schema to convert the {@code source}.
////     *
////     * @param source
////     * @return
////     * @throws Exception
////     */
////    public Resource build(ConnectorObject source) throws Exception {
////        Resource result = objectClassInfoHelper.build(source, cryptoService);
////        resetUid(source.getUid(), result);
////        if (null != source.getUid().getRevision()) {
////            //System supports Revision
////            result.put(ServerConstants.OBJECT_PROPERTY_REV, source.getUid().getRevision());
////        }
////        return result;
////    }
////
////
////    /**
////     * Resets the {@code _id} attribute in the {@code target} object to the new {@code uid} value.
////     *
////     * @param uid    new id value
////     * @param target
////     */
////    public void resetUid(Uid uid, JsonValue target) {
////        if (null != uid && null != target) {
////            target.put(ServerConstants.OBJECT_PROPERTY_ID, Id.escapeUid(uid.getUidValue()));
////        }
////    }
////
////    /**
////     * Generate the fully qualified id from unqualified object {@link org.identityconnectors.framework.common.objects.Uid}
////     * <p/>
////     * The result id will be system/{@code [endSystemName]}/{@code [objectType]}/{@code [escapedObjectId]}
////     *
////     * @param uid original un escaped unique identifier of the object
////     * @return
////     */
////    /**
////     * Generate the fully qualified id from unqualified object {@link Uid}
////     * <p/>
////     * The result id will be system/{@code [endSystemName]}/{@code [objectType]}/{@code [escapedObjectId]}
////     *
////     * @param uid original un escaped unique identifier of the object
////     * @return
////     */
////    public URI resolveQualifiedId(Uid uid) {
////        if (null != uid) {
////            try {
////                return systemObjectSetId.resolveLocalId(uid.getUidValue()).getQualifiedId();
////            } catch (JsonResourceException e) {
////                // Should never happen in a copy constructor.
////                throw new UndeclaredThrowableException(e);
////            }
////        } else {
////            return systemObjectSetId.getQualifiedId();
////        }
////    }
////
////    private String getKey(Map<String, Object> node) {
////        return node.keySet().isEmpty() ? null : node.keySet().iterator().next();
////    }
////}

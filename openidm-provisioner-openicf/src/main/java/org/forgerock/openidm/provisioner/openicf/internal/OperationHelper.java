///*
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// *
// * Copyright © 2011 ForgeRock AS. All rights reserved.
// *
// * The contents of this file are subject to the terms
// * of the Common Development and Distribution License
// * (the License). You may not use this file except in
// * compliance with the License.
// *
// * You can obtain a copy of the License at
// * http://forgerock.org/license/CDDLv1.0.html
// * See the License for the specific language governing
// * permission and limitations under the License.
// *
// * When distributing Covered Code, include this CDDL
// * Header Notice in each file and include the License file
// * at http://forgerock.org/license/CDDLv1.0.html
// * If applicable, add the following below the CDDL Header,
// * with the fields enclosed by brackets [] replaced by
// * your own identifying information:
// * "Portions Copyrighted [year] [name of copyright owner]"
// *
// * $Id$
// */
//
//package org.forgerock.openidm.provisioner.openicf.internal;
//
//import org.forgerock.json.crypto.JsonCryptoException;
//import org.forgerock.json.fluent.JsonValue;
//import org.forgerock.json.resource.ForbiddenException;
//import org.forgerock.json.resource.JsonResourceException;
//import org.forgerock.json.resource.Resource;
//import org.forgerock.json.resource.ResourceException;
//import org.forgerock.openidm.core.ServerConstants;
//import org.forgerock.openidm.crypto.CryptoService;
//import org.forgerock.openidm.provisioner.Id;
//import org.forgerock.openidm.provisioner.openicf.OperationHelper;
//import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
//import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
//import org.forgerock.openidm.provisioner.openicf.query.OperatorFactory;
//import org.forgerock.openidm.provisioner.openicf.query.operators.BooleanOperator;
//import org.forgerock.openidm.provisioner.openicf.query.operators.Operator;
//import org.identityconnectors.common.Assertions;
//import org.identityconnectors.framework.api.operations.APIOperation;
//import org.identityconnectors.framework.common.objects.*;
//import org.identityconnectors.framework.common.objects.filter.Filter;
//
//import java.io.IOException;
//import java.lang.reflect.UndeclaredThrowableException;
//import java.net.URI;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import static org.forgerock.openidm.provisioner.openicf.query.QueryUtil.*;
//
///**
// * @author $author$
// * @version $Revision$ $Date$
// */
//public class OperationHelper  {
//
//    private final ObjectClassInfoHelper objectClassInfoHelper;
//    private final Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations;
//
//
//    /**
//     * @param objectClassInfoHelper
//     * @param connectorObjectOptions
//     * @throws NullPointerException if any of the input values is null.
//     */
//    public OperationHelper(final ObjectClassInfoHelper objectClassInfoHelper,
//                           final Map<Class<? extends APIOperation>, OperationOptionInfoHelper> connectorObjectOptions) {
//        this.objectClassInfoHelper = Assertions.nullChecked(objectClassInfoHelper, "objectClassInfoHelper");
//        this.operations = Assertions.nullChecked(connectorObjectOptions, "connectorObjectOptions");
//    }
//
//
//
//
//
//    /**
//     * Gets the {@link ObjectClass} value of this newBuilder.
//     *
//     * @return
//     */
//    public ObjectClass getObjectClass() {
//        return objectClassInfoHelper.getObjectClass();
//    }
//
//
//
//
//    public ConnectorObject build(Class<? extends APIOperation> operation, JsonValue source) throws Exception {
//        return objectClassInfoHelper.build(operation, null, source, cryptoService);
//    }
//
//
//    public ConnectorObject build(Class<? extends APIOperation> operation, String id, JsonValue source) throws Exception {
//        //TODO do something with ID
//        return objectClassInfoHelper.build(operation, id, source, cryptoService);
//    }
//
//    /**
//     * Build a new Map object from the {@code source} object.
//     * <p/>
//     * This class uses the embedded schema to convert the {@code source}.
//     *
//     * @param source
//     * @return
//     * @throws Exception
//     */
//    public Resource build(ConnectorObject source) throws Exception {
//        Resource result = objectClassInfoHelper.build(source, cryptoService);
//        resetUid(source.getUid(), result);
//        if (null != source.getUid().getRevision()) {
//            //System supports Revision
//            result.put(ServerConstants.OBJECT_PROPERTY_REV, source.getUid().getRevision());
//        }
//        return result;
//    }
//
//
//    /**
//     * Resets the {@code _id} attribute in the {@code target} object to the new {@code uid} value.
//     *
//     * @param uid    new id value
//     * @param target
//     */
//    public void resetUid(Uid uid, JsonValue target) {
//        if (null != uid && null != target) {
//            target.put(ServerConstants.OBJECT_PROPERTY_ID, Id.escapeUid(uid.getUidValue()));
//        }
//    }
//
//    /**
//     * Generate the fully qualified id from unqualified object {@link org.identityconnectors.framework.common.objects.Uid}
//     * <p/>
//     * The result id will be system/{@code [endSystemName]}/{@code [objectType]}/{@code [escapedObjectId]}
//     *
//     * @param uid original un escaped unique identifier of the object
//     * @return
//     */
//    /**
//     * Generate the fully qualified id from unqualified object {@link Uid}
//     * <p/>
//     * The result id will be system/{@code [endSystemName]}/{@code [objectType]}/{@code [escapedObjectId]}
//     *
//     * @param uid original un escaped unique identifier of the object
//     * @return
//     */
//    public URI resolveQualifiedId(Uid uid) {
//        if (null != uid) {
//            try {
//                return systemObjectSetId.resolveLocalId(uid.getUidValue()).getQualifiedId();
//            } catch (JsonResourceException e) {
//                // Should never happen in a copy constructor.
//                throw new UndeclaredThrowableException(e);
//            }
//        } else {
//            return systemObjectSetId.getQualifiedId();
//        }
//    }
//
//    private String getKey(Map<String, Object> node) {
//        return node.keySet().isEmpty() ? null : node.keySet().iterator().next();
//    }
//}

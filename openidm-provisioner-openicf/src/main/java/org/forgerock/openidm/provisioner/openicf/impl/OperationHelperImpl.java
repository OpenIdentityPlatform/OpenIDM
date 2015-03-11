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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner.openicf.impl;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.common.objects.*;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @version $Revision$ $Date$
 */
public class OperationHelperImpl implements OperationHelper {

    private final ObjectClassInfoHelper objectClassInfoHelper;
    private final Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations;
    private final List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
    private final Id systemObjectSetId;
    private final CryptoService cryptoService;

    /**
     * @param systemObjectSetId
     * @param objectClassInfoHelper
     * @param connectorObjectOptions
     * @param cryptoService
     * @throws NullPointerException if any of the input values is null.
     */
    public OperationHelperImpl(Id systemObjectSetId, ObjectClassInfoHelper objectClassInfoHelper,
                               Map<Class<? extends APIOperation>, OperationOptionInfoHelper> connectorObjectOptions,
                               CryptoService cryptoService) {
        this.objectClassInfoHelper = Assertions.nullChecked(objectClassInfoHelper, "objectClassInfoHelper");
        this.operations = Assertions.nullChecked(connectorObjectOptions, "connectorObjectOptions");
        this.systemObjectSetId = Assertions.nullChecked(systemObjectSetId, "systemObjectSetId");
        this.cryptoService = cryptoService;
    }

    public boolean isOperationPermitted(Class<? extends APIOperation> operation) throws ResourceException {
        OperationOptionInfoHelper operationOptionInfoHelper = operations.get(operation);
        String reason = "not supported.";
        if (null != operationOptionInfoHelper
                && (null == operationOptionInfoHelper.getSupportedObjectTypes()
                    || operationOptionInfoHelper.getSupportedObjectTypes().contains(
                        objectClassInfoHelper.getObjectClass().getObjectClassValue()))) {

            if (!operationOptionInfoHelper.isDenied()) {
                return true;
            } else if (OperationOptionInfoHelper.OnActionPolicy.ALLOW.equals(operationOptionInfoHelper.getOnActionPolicy())) {
                return false;
            }
            reason = "denied.";
        }
        throw new ForbiddenException("Operation " + operation.getCanonicalName() + " is " + reason);
    }


    public OperationOptionsBuilder getOperationOptionsBuilder(Class<? extends APIOperation> operation, ConnectorObject connectorObject, JsonValue source) throws Exception {
        return operations.get(operation).build(source, objectClassInfoHelper);
    }


    public ObjectClass getObjectClass() {
        return objectClassInfoHelper.getObjectClass();
    }


/*
    public Filter build(Map<String, Object> query, Map<String, Object> params) throws Exception {
        Operator operator = createOperator(query, params);
        return operator.createFilter();
    }
*/
    public ConnectorObject build(Class<? extends APIOperation> operation, JsonValue source) throws Exception {
        return objectClassInfoHelper.build(operation, null, source, cryptoService);
    }

/*
    public ConnectorObject build(Class<? extends APIOperation> operation, String id, JsonValue source) throws Exception {
        //TODO do something with ID
        return objectClassInfoHelper.build(operation, id, source, cryptoService);
    }
    */


    public JsonValue build(ConnectorObject source) throws Exception {
        JsonValue result = objectClassInfoHelper.build(source, cryptoService).getContent();
        resetUid(source.getUid(), result);
        if (null != source.getUid().getRevision()) {
            //System supports Revision
            result.put(Resource.FIELD_CONTENT_REVISION, source.getUid().getRevision());
        }
        return result;
    }

    public void resetUid(Uid uid, JsonValue target) {
        if (null != uid && null != target) {
            // TODO are we going to encode ids?
            target.put(Resource.FIELD_CONTENT_ID, /*Id.escapeUid(*/uid.getUidValue()/*)*/);
        }
    }

    /**
     * Generate the fully qualified id from unqualified object {@link org.identityconnectors.framework.common.objects.Uid}
     * <p/>
     * The result id will be system/{@code [endSystemName]}/{@code [objectType]}/{@code [escapedObjectId]}
     *
     * @param uid original un escaped unique identifier of the object
     * @return
     */
    public URI resolveQualifiedId(Uid uid) {
        if (null != uid) {
            try {
                return systemObjectSetId.resolveLocalId(uid.getUidValue()).getQualifiedId();
            } catch (ResourceException e) {
                // Should never happen in a copy constructor.
                throw new UndeclaredThrowableException(e);
            }
        } else {
            return systemObjectSetId.getQualifiedId();
        }
    }

/*
    public ResultsHandler getResultsHandler() {
        resultList.clear();
        return new ConnectorObjectResultsHandler();
    }


    public List<Map<String, Object>> getQueryResult() {
        return resultList;
    }
*/

 //   private class ConnectorObjectResultsHandler implements ResultsHandler {
        /**
         * Call-back method to do whatever it is the caller wants to do with
         * each {@link org.identityconnectors.framework.common.objects.ConnectorObject} that is returned in the result of
         * {@link org.identityconnectors.framework.api.operations.SearchApiOp}.
         *
         * @param obj each object return from the search.
         * @return true if we should keep processing else false to cancel.
         * @throws RuntimeException the implementor should throw a {@link RuntimeException}
         *                          that wraps any native exception (or that describes any other problem
         *                          during execution) that is serious enough to stop the iteration.
         */
/*
        public boolean handle(ConnectorObject obj) {
            try {
                return resultList.add(objectClassInfoHelper.build(obj, cryptoService).asMap());
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            } catch (JsonCryptoException e) {
                //TODO: This is a configuaration exception. Improve it later.
                throw new IllegalArgumentException(e);
            }
        }
    }
*/
/*
    private Operator createOperator(Map<String, Object> node, final Map<String, Object> params) throws Exception {

        String nodeName = getKey(node);

        if (isBooleanOperator(nodeName)) {

            BooleanOperator booleanOperator = OperatorFactory.createBooleanOperator(nodeName);

            List<Object> parts = (List<Object>) node.get(nodeName);

            if (parts.size() < 2) {
                throw new IllegalArgumentException("To few elements in the 'BooleanOperator'-object (" + parts.size() + "). Must be 2 or more");
            }

            for (Object part : parts) {
                Operator op = createOperator((Map<String, Object>) part, params);
                booleanOperator.addOperator(op);
            }

            return booleanOperator;

        } else {
            return createFunctionalOperator(node, params);
        }
    }

    private Operator createFunctionalOperator(Map<String, Object> node, final Map<String, Object> params) throws Exception {

        String operatorName = getKey(node);

        Map<String, Object> nodeValueMap = (Map<String, Object>) node.get(operatorName);

        String field = (String) nodeValueMap.get("field");
        List<String> values = (List<String>) nodeValueMap.get("values");

        if (values == null) {
            List<String> providedValues = (List<String>) params.get(field);

            if (providedValues == null) {
                throw new IllegalArgumentException("No predefined or provided values for property: " + field);
            }

            values = (List<String>) params.get(field);
        }

        return OperatorFactory.createFunctionalOperator(operatorName, objectClassInfoHelper.build(field, values, cryptoService));
    }

    private boolean isBooleanOperator(String key) {
        return key.equals(OPERATOR_AND)
                || key.equals(OPERATOR_OR)
                || key.equals(OPERATOR_NOR)
                || key.equals(OPERATOR_NAND);

    }

    private String getKey(Map<String, Object> node) {
        return node.keySet().isEmpty() ? null : node.keySet().iterator().next();
    }
*/
}

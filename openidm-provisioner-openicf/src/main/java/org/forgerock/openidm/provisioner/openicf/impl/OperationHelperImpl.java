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

import org.forgerock.json.crypto.JsonCryptoException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.forgerock.openidm.provisioner.openicf.query.OperatorFactory;
import org.forgerock.openidm.provisioner.openicf.query.operators.BooleanOperator;
import org.forgerock.openidm.provisioner.openicf.query.operators.Operator;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.forgerock.openidm.provisioner.openicf.query.QueryUtil.*;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OperationHelperImpl implements OperationHelper {

    private APIConfiguration configuration;
    private ObjectClassInfoHelper objectClassInfoHelper;
    private Map<Class<? extends APIOperation>, OperationOptionInfoHelper> operations;
    private List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
    private Id systemObjectSetId;
    private CryptoService cryptoService;

    /**
     * @param configuration
     * @param systemObjectSetId
     * @param objectClassInfoHelper
     * @param connectorObjectOptions
     * @throws NullPointerException if any of the input values is null.
     */
    public OperationHelperImpl(APIConfiguration configuration, Id systemObjectSetId, ObjectClassInfoHelper objectClassInfoHelper,
                               Map<Class<? extends APIOperation>, OperationOptionInfoHelper> connectorObjectOptions,
                               CryptoService cryptoService) {
        this.configuration = Assertions.nullChecked(configuration, "configuration");
        this.objectClassInfoHelper = Assertions.nullChecked(objectClassInfoHelper, "objectClassInfoHelper");
        this.operations = Assertions.nullChecked(connectorObjectOptions, "connectorObjectOptions");
        this.systemObjectSetId = Assertions.nullChecked(systemObjectSetId, "systemObjectSetId");
        this.cryptoService = cryptoService;
    }


    public APIConfiguration getRuntimeAPIConfiguration() {
        return configuration;
    }


    public boolean isOperationPermitted(Class<? extends APIOperation> operation) throws ForbiddenException {
        OperationOptionInfoHelper operationOptionInfoHelper = operations.get(operation);
        String reason = "not supported.";
        if (null != operationOptionInfoHelper && (null == operationOptionInfoHelper.getSupportedObjectTypes() ||
                operationOptionInfoHelper.getSupportedObjectTypes().contains(objectClassInfoHelper.getObjectClass().getObjectClassValue()))) {

            if (!operationOptionInfoHelper.isDenied()) {
                return true;
            } else if (OperationOptionInfoHelper.OnDenyAction.DO_NOTHING.equals(operationOptionInfoHelper.getOnDeny())) {
                return false;
            }
            reason = "denied.";
        }
        throw new ForbiddenException("Operation " + operation.getCanonicalName() + " is " + reason);
    }


    public OperationOptionsBuilder getOperationOptionsBuilder(Class<? extends APIOperation> operation, ConnectorObject connectorObject, Map<String, Object> source) throws Exception {
        return operations.get(operation).build(source, objectClassInfoHelper);
    }


    public ObjectClass getObjectClass() {
        return objectClassInfoHelper.getObjectClass();
    }


    public Filter build(Map<String, Object> query, Map<String, Object> params) throws Exception {
        Operator operator = createOperator(query, params);
        return operator.createFilter();
    }


    public ConnectorObject build(Class<? extends APIOperation> operation, Map<String, Object> source) throws Exception {
        return objectClassInfoHelper.build(operation, null, source, cryptoService);
    }


    public ConnectorObject build(Class<? extends APIOperation> operation, String id, Map<String, Object> source) throws Exception {
        //TODO do something with ID
        return objectClassInfoHelper.build(operation, id, source, cryptoService);
    }


    public Map<String, Object> build(ConnectorObject source) throws Exception {
        Map<String, Object> result = objectClassInfoHelper.build(source, cryptoService);
        resetUid(source.getUid(), result);
        return result;
    }


    public void resetUid(Uid uid, Map<String, Object> target) {
        if (null != uid && null != target) {
            target.put("_id", Id.escapeUid(uid.getUidValue()));
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
            } catch (ObjectSetException e) {
                // Should never happen in a copy constructor.
                throw new UndeclaredThrowableException(e);
            }
        } else {
            return systemObjectSetId.getQualifiedId();
        }
    }


    public ResultsHandler getResultsHandler() {
        resultList.clear();
        return new ConnectorObjectResultsHandler();
    }


    public List<Map<String, Object>> getQueryResult() {
        return resultList;
    }


    private class ConnectorObjectResultsHandler implements ResultsHandler {
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
        public boolean handle(ConnectorObject obj) {
            try {
                return resultList.add(objectClassInfoHelper.build(obj, cryptoService));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            } catch (JsonCryptoException e) {
                //TODO: This is a configuaration exception. Improve it later.
                throw new IllegalArgumentException(e);
            }
        }
    }

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

        Operator operator = OperatorFactory.createFunctionalOperator(operatorName, objectClassInfoHelper.build(field, values, cryptoService));

        return operator;
    }

    private boolean isBooleanOperator(String key) {
        if (key.equals(OPERATOR_AND)
                || key.equals(OPERATOR_OR)
                || key.equals(OPERATOR_NOR)
                || key.equals(OPERATOR_NAND)) {

            return true;
        }

        return false;
    }

    private String getKey(Map<String, Object> node) {
        for (String s : node.keySet())
            return s;

        return null;
    }
}

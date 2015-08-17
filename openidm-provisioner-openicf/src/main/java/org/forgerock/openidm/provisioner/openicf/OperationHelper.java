/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All rights reserved.
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

package org.forgerock.openidm.provisioner.openicf;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.common.objects.*;

import java.net.URI;

/**
 * @version $Revision$ $Date$
 */
public interface OperationHelper {

    /**
     * Gets the {@link ObjectClass} value of this instance.
     *
     * @return
     */
    ObjectClass getObjectClass();

    /**
     * Checks the {@code operation} permission before execution.
     *
     * @param operation
     * @return if {@code denied} is true and the {@code onDeny} equals
     *         {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnDenyAction#DO_NOTHING}
     *         returns false else true
     * @throws ResourceException if {@code denied} is true and the {@code onDeny} equals
     *                            {@link org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper.OnDenyAction#THROW_EXCEPTION}
     */
    boolean isOperationPermitted(Class<? extends APIOperation> operation) throws ResourceException;

    /**
     * Gets a new instance of {@link OperationOptionsBuilder} filled with {@link OperationOptions}.
     *
     * @param operation
     * @param connectorObject
     * @param source
     * @return
     * @throws Exception
     */
    OperationOptionsBuilder getOperationOptionsBuilder(Class<? extends APIOperation> operation, ConnectorObject connectorObject, JsonValue source) throws Exception;

    /**
     * Resets the {@code _id} attribute in the {@code target} object to the new {@code uid} value.
     *
     * @param uid    new id value
     * @param target
     */
//    public void resetUid(Uid uid, JsonValue target);

    /**
     * Generate the fully qualified id from unqualified object {@link Uid}
     * <p/>
     * The result id will be system/{@code [endSystemName]}/{@code [objectType]}/{@code [escapedObjectId]}
     *
     * @param uid original un escaped unique identifier of the object
     * @return
     */
    URI resolveQualifiedId(Uid uid);


    /**
     * @return new instance of {@link ResultsHandler}
     */
//    public ResultsHandler getResultsHandler();

//    public List<Map<String, Object>> getQueryResult();

    /**
     * Build new {@code Filter} instance form the {@code query} and {@code params} values.
     *
     * @param query
     * @param params
     * @return
     * @throws Exception
     */
//    public Filter build(Map<String, Object> query, Map<String, Object> params) throws Exception;

    ConnectorObject build(Class<? extends APIOperation> operation, JsonValue source) throws Exception;

//    public ConnectorObject build(Class<? extends APIOperation> operation, String id, JsonValue source) throws Exception;

    /**
     * Build a new Map object from the {@code source} object.
     * <p/>
     * This class uses the embedded schema to convert the {@code source}.
     *
     * @param source
     * @return
     * @throws Exception
     */
    JsonValue build(ConnectorObject source) throws Exception;
}

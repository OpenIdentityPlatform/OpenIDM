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

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.schema.validator.exceptions.SchemaException;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.common.serializer.SerializerUtil;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;

import java.util.Map;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OperationHelperBuilder {

    private APIConfigurationImpl runtimeAPIConfiguration;
    private Map<String, ObjectClassInfoHelper> supportedObjectTypes;
    private Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> operationOptionHelpers;
    private String systemName;

    public OperationHelperBuilder(String system, JsonNode jsonConfiguration, APIConfiguration defaultAPIConfiguration) throws SchemaException, JsonNodeException {
        runtimeAPIConfiguration = (APIConfigurationImpl) defaultAPIConfiguration;
        ConnectorUtil.configureDefaultAPIConfiguration(jsonConfiguration, defaultAPIConfiguration);
        supportedObjectTypes = ConnectorUtil.getObjectTypes(jsonConfiguration);
        operationOptionHelpers = ConnectorUtil.getOperationOptionConfiguration(jsonConfiguration);
        this.systemName = Assertions.blankChecked(system,"systemName");
    }

    public OperationHelper build(String objectType, Map<String, Object> object) throws ObjectSetException {
        ObjectClassInfoHelper objectClassInfoHelper = supportedObjectTypes.get(objectType);
        if (null == objectClassInfoHelper) {
            throw new ObjectSetException("Unsupported object type: " + objectType 
                    + " not in supported types" + supportedObjectTypes.keySet());
        }
        APIConfiguration _configuration = getRuntimeAPIConfiguration();

//        TODO Set custom runtimeAPIConfiguration properties
//        if (null != object.get("_configuration")) {
//            ConnectorUtil.configureDefaultAPIConfiguration(null, _configuration);
//        }


        return new OperationHelperImpl(_configuration, new Id(systemName, objectType), objectClassInfoHelper, operationOptionHelpers.get(objectType));
    }


    public APIConfiguration getRuntimeAPIConfiguration() {
        //Assertions.nullCheck(runtimeAPIConfiguration,"runtimeAPIConfiguration");
        //clone in case application tries to modify
        //after the fact. this is necessary to
        //ensure thread-safety of a ConnectorFacade
        //also, runtimeAPIConfiguration is used as a key in the
        //pool, so it is important that it not be modified.
        APIConfigurationImpl _configuration = (APIConfigurationImpl) SerializerUtil.cloneObject(runtimeAPIConfiguration);
        //parent ref not included in the clone
        _configuration.setConnectorInfo(runtimeAPIConfiguration.getConnectorInfo());
        return _configuration;
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2014 ForgeRock AS. All rights reserved.
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

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.openicf.OperationHelper;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.openidm.provisioner.openicf.commons.OperationOptionInfoHelper;
import org.identityconnectors.common.Assertions;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.impl.api.APIConfigurationImpl;

import java.util.Map;

/**
 * @version $Revision$ $Date$
 */
public class OperationHelperBuilder {

    private final APIConfigurationImpl runtimeAPIConfiguration;
    private Map<String, ObjectClassInfoHelper> supportedObjectTypes;
    private Map<String, Map<Class<? extends APIOperation>, OperationOptionInfoHelper>> operationOptionHelpers;
    private String systemName;

    public OperationHelperBuilder(String system, JsonValue jsonConfiguration,
                                  APIConfiguration defaultAPIConfiguration, CryptoService cryptoService) throws JsonValueException {
        runtimeAPIConfiguration = (APIConfigurationImpl) defaultAPIConfiguration;
        ConnectorUtil.configureDefaultAPIConfiguration(jsonConfiguration, defaultAPIConfiguration, cryptoService);
        supportedObjectTypes = ConnectorUtil.getObjectTypes(jsonConfiguration);
        operationOptionHelpers = ConnectorUtil.getOperationOptionConfiguration(jsonConfiguration);
        this.systemName = Assertions.blankChecked(system, "systemName");
    }

    public OperationHelper build(String objectType, JsonValue object, CryptoService cryptoService) throws ResourceException {
        ObjectClassInfoHelper objectClassInfoHelper = supportedObjectTypes.get(objectType);
        if (null == objectClassInfoHelper) {
            throw new BadRequestException("Unsupported object type: " + objectType + " not in supported types" + supportedObjectTypes.keySet());
        }
//        APIConfiguration _configuration = getRuntimeAPIConfiguration();
//
//        TODO Set custom runtimeAPIConfiguration properties
//        if (null != object.get("_configuration")) {
//            ConnectorUtil.configureDefaultAPIConfiguration(null, _configuration);
//        }

        return new OperationHelperImpl(new Id(systemName, objectType), objectClassInfoHelper, operationOptionHelpers.get(objectType), cryptoService);
    }


    public APIConfiguration getRuntimeAPIConfiguration() {
        return runtimeAPIConfiguration;
                
        // TODO: check if it would be better to do the below clone only in case local configuration overrides are passed
/*
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
*/
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.provisioner.openicf;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.provisioner.openicf.internal.ConnectorFacadeCallback;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorInfo;

/**
 * Sample Class Doc
 *
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public interface ConnectorInfoProvider {

    /**
     * @param connectorReference
     * @return null if there is no {@link ConnectorInfo} available.
     */
    ConnectorInfo findConnectorInfo(ConnectorReference connectorReference);

    /**
     * Adds a {@code ConnectorListener}
     *
     * @param connectorReference
     * @param handler
     */
    void addConnectorFacadeCallback(ConnectorReference connectorReference, ConnectorFacadeCallback handler);

    /**
     *
     * @param handler
     */
    void deleteConnectorFacadeCallback(ConnectorFacadeCallback handler);

    /**
     * Get all available {@link ConnectorInfo} from the local and the remote
     * {@link org.identityconnectors.framework.api.ConnectorInfoManager}s
     *
     * @return list of all available {@link ConnectorInfo}s
     */
    List<ConnectorInfo> getAllConnectorInfo();

    /**
     * Tests the {@link APIConfiguration Configuration} with the connector.
     *
     * @param configuration
     * @throws RuntimeException if the configuration is not valid or the test failed.
     */
    void testConnector(APIConfiguration configuration) throws ResourceException;

    /**
     * Create a new configuration object from the {@code configuration}
     * parameter.
     * <p/>
     *
     * @param configuration
     * @param validate
     * @return
     */
    JsonValue createSystemConfiguration(APIConfiguration configuration, boolean validate) throws ResourceException;
}

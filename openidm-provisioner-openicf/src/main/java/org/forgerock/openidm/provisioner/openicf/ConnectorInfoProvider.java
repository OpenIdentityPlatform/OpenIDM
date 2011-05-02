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
 * $Id$
 */
package org.forgerock.openidm.provisioner.openicf;

import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorInfo;

import java.util.List;
import java.util.Map;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public interface ConnectorInfoProvider {

    /**
     * @param connectorReference
     * @return null if there is no {@ConnectorInfo} available.
     */
    public ConnectorInfo findConnectorInfo(ConnectorReference connectorReference);

    /**
     * Get all available {@link ConnectorInfo} from the local and the remote
     * {@link org.identityconnectors.framework.api.ConnectorInfoManager}s
     *
     * @return list of all available {@link ConnectorInfo}s
     */
    public List<ConnectorInfo> getAllConnectorInfo();

    /**
     * Tests the {@link APIConfiguration Configuration} with the connector.
     *
     * @param configuration
     * @throws RuntimeException if the configuration is not valid or the test failed.
     */
    public void testConnector(APIConfiguration configuration);

    /**
     * Create a new configuration object from the {@code configuration} parameter.
     * <p/>
     *
     * @param configuration
     * @param validate
     * @return
     */
    public Map<String, Object> createSystemConfiguration(APIConfiguration configuration, boolean validate);
}

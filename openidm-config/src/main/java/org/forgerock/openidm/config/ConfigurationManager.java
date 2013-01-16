/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.config;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.osgi.service.cm.Configuration;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public interface ConfigurationManager {

    /**
     * Default prefix for OpenIDM OSGi services
     */
    public static final String DEFAULT_SERVICE_RDN_PREFIX = "org.forgerock.openidm.";


    // Properties to set configuration file handling behavior
    // TODO Write JavaDoc
    public static final String OPENIDM_FILEINSTALL_BUNDLES_NEW_START = "openidm.fileinstall.bundles.new.start";
    public static final String OPENIDM_FILEINSTALL_FILTER = "openidm.fileinstall.filter";
    public static final String OPENIDM_FILEINSTALL_DIR = "openidm.fileinstall.dir";
    public static final String OPENIDM_FILEINSTALL_POLL = "openidm.fileinstall.poll";
    public static final String OPENIDM_FILEINSTALL_ENABLED = "openidm.fileinstall.enabled";

    public static final String FELIX_FILEINSTALL_PID = "org.apache.felix.fileinstall";

    public Resource installConfiguration(String pid, String factoryPid, JsonValue configuration)
            throws ResourceException;

    public Configuration getConfiguration(String pid, String factoryPid);

    public void deleteConfiguration(String pid, String factoryPid);

    public void listConfigurations(ResultHandler<Resource> handler);
}

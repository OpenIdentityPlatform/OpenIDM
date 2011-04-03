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
 */
package org.forgerock.openidm.repo.orientdb.impl;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;

/**
 * Component for embedded OrientDB server
 * @author aegloff
 */
@Component(name = "embedded-orientdb-server-component", immediate=true)
public class EmbeddedOServerService {
    final static Logger logger = LoggerFactory.getLogger(EmbeddedOServerService.class);

    @Activate
    private void activate(java.util.Map<String, Object> config) {
        logger.trace("Activating Service with configuration {}", config);
        EmbeddedOServer.startEmbedded();
    }
    
    @Deactivate
    private void deactivate(Map<String, Object> config) {
        EmbeddedOServer.stopEmbedded();
        logger.debug("Embedded DB server stopped.");
    }
   
}
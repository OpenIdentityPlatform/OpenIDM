/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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
*/

package org.forgerock.openidm.scheduler.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi bundle activator for scheduler facility
 */
public class Activator implements BundleActivator {
    private final static Logger logger = LoggerFactory.getLogger(Activator.class);

    public void start(BundleContext context) throws SchedulerException {
        logger.debug("Bundle started", context);
    }

    public void stop(BundleContext context) {
        logger.debug("Bundle stopped", context);
    }

 
}
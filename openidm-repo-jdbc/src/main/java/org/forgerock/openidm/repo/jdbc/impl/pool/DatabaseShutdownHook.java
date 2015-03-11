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
package org.forgerock.openidm.repo.jdbc.impl.pool;

import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import com.jolbox.bonecp.hooks.AcquireFailConfig;
import org.forgerock.openidm.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Revision$ $Date$
 */
public class DatabaseShutdownHook extends AbstractConnectionHook {
    private final static Logger logger = LoggerFactory.getLogger(DatabaseShutdownHook.class);
    // Only used for logging, so it's okay to use the default timezone
    private final static DateUtil dateUtil = DateUtil.getDateUtil();

    @Override
    public boolean onConnectionException(ConnectionHandle connection, String state, Throwable t) {
        // handle notifications here: SNMP or SMTP
        logger.warn("Database down at {}", dateUtil.now());
        return super.onConnectionException(connection, state, t);
    }

    @Override
    public boolean onAcquireFail(Throwable t, AcquireFailConfig acquireConfig) {
        // handle notifications here: SNMP or SMTP
        logger.warn("Failure to acquire connection at {}. Retry attempts remaining : {}", dateUtil.now(), acquireConfig.getAcquireRetryAttempts());
        return super.onAcquireFail(t, acquireConfig);
    }

}

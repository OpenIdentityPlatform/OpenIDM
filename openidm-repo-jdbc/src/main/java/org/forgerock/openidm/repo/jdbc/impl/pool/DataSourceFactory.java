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

import com.jolbox.bonecp.BoneCPDataSource;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.forgerock.json.fluent.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class DataSourceFactory {
    private final static Logger logger = LoggerFactory.getLogger(DatabaseShutdownHook.class);

    public static DataSource newInstance(JsonValue config) {
        //TODO Make CP implementation independent
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().set(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        BoneCPDataSource ds = mapper.convertValue(config.asMap(), BoneCPDataSource.class);
        ds.setConnectionHook(new DatabaseShutdownHook());
        ds.setTransactionRecoveryEnabled(true);// Important: This should be enabled
        ds.setAcquireRetryAttempts(10);//default is 5
        ds.setReleaseHelperThreads(5);
        logger.debug("BoneCPDataSource: {}", ds);
        return ds;
    }
}

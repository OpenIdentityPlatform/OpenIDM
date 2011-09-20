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
package org.forgerock.openidm.repo.orientdb.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.openidm.metadata.MetaDataProvider;
import org.forgerock.openidm.repo.orientdb.impl.OrientDBRepoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Meta data provider to describe configuration 
 * requirements of this bundle
 * @author aegloff
 */
public class ConfigMeta implements MetaDataProvider {
    final static Logger logger = LoggerFactory.getLogger(ConfigMeta.class);

    Map<String, List> propertiesToEncrypt;
    
    public ConfigMeta() {
        propertiesToEncrypt = new HashMap<String, List>();
        List props = new ArrayList();
        props.add("password");
        propertiesToEncrypt.put(OrientDBRepoService.PID, props);
    }
    
    /**
     * Meta-data describing which configuration properties need to be encrypted
     * 
     * @return a map from PID to a list of configuration properties (identified by JSON pointers)
     * that need to be encrypted.
     */
    public Map<String, List> getPropertiesToEncrypt() {
        return propertiesToEncrypt;
    } 
}
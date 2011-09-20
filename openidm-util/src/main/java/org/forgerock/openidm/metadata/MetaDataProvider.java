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
package org.forgerock.openidm.metadata;

import java.util.List;
import java.util.Map;

/**
 * Meta data provider interface to describe configuration 
 * requirements of a bundle. Use a meta-data.json file to declare 
 * a meta data provider for a bundle*
 * 
 * @author aegloff
 */
public interface MetaDataProvider {
    
    /**
     * Meta-data describing which configuration properties need to be encrypted
     * 
     * @return a map from PID (or factory PID for factory configuration) 
     * to a list of configuration properties (identified by JSON pointers)
     * that need to be encrypted.
     */
    Map<String, List> getPropertiesToEncrypt();
}
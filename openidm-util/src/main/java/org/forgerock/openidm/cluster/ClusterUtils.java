/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.cluster;

/**
 * Contains cluster specific labels and utilities
 * 
 */
public class ClusterUtils {

    /*
     * Stand-alone (non-clustered) instance
     */
    public static final String TYPE_STANDALONE = "standalone";
    
    /*
     * The first instance in a cluster
     */
    public static final String TYPE_CLUSTERED_FIRST = "clustered-first";
    
    /*
     * Additional instances in a cluster
     */
    public static final String TYPE_CLUSTERED_ADDITIONAL = "clustered-additional";
}

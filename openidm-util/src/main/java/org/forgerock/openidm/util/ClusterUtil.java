/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Portions copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openidm.util;

/**
 * Contains cluster specific labels and utilities
 */
public class ClusterUtil {

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

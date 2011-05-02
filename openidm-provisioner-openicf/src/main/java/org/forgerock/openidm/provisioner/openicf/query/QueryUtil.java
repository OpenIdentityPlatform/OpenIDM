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
 *
 * $Id$
 */

package org.forgerock.openidm.provisioner.openicf.query;

public abstract class QueryUtil {

    public static final String OPERATOR_AND = "AND";
    public static final String OPERATOR_OR = "OR";
    public static final String OPERATOR_NAND = "NAND";
    public static final String OPERATOR_NOR = "NOR";

    public static final String OPERATOR_EQUALS = "EQUALS";
    public static final String OPERATOR_STARTSWITH = "STARTSWITH";
    public static final String OPERATOR_LESSTHAN = "LESSTHAN";
    public static final String OPERATOR_ENDSWITH = "ENDSWITH";
    public static final String OPERATOR_CONTAINSALLVALUES = "CONTAINSALLVALUES";
    public static final String OPERATOR_CONTAINS = "CONTAINS";
    public static final String OPERATOR_GREATERTHAN = "GREATERTHAN";
    public static final String OPERATOR_GREATERTHANOREQUAL = "GREATERTHANOREQUAL";
    public static final String OPERATOR_LESSTHANOREQUAL = "LESSTHANOREQUAL";    
}

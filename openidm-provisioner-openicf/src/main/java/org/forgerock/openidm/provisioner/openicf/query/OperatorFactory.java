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

import org.forgerock.openidm.provisioner.openicf.query.operators.*;
import org.identityconnectors.framework.common.objects.Attribute;

import java.util.List;
import static org.forgerock.openidm.provisioner.openicf.query.QueryUtil.*;

public class OperatorFactory {

    public static BooleanOperator createBooleanOperator(String operatorName) {

        if (operatorName.equals(OPERATOR_AND)) {
            return new AndOperator();
        }
        else if (operatorName.equals(OPERATOR_OR)) {
            return new OrOperator();
        }
        else if (operatorName.equals(OPERATOR_NAND)) {
            return new NandOperator();
        }
        else if (operatorName.equals(OPERATOR_NOR)) {
            return new NorOperator();
        }
        else {
            throw new IllegalArgumentException("No such operator: " + operatorName);
        }
    }

    public static Operator createFunctionalOperator(String operatorName, Attribute attribute) {

        if (operatorName.equals(OPERATOR_EQUALS)) {
            return new EqualsOperator(attribute);
        }
        else if (operatorName.equals(OPERATOR_STARTSWITH)) {
            return new StartsWithOperator(attribute);
        }
        else if (operatorName.equals(OPERATOR_LESSTHAN)) {
            return new LessThanOperator(attribute);
        }
        else if (operatorName.equals(OPERATOR_ENDSWITH)) {
            return new EndsWithOperator(attribute);
        }
        else if (operatorName.equals(OPERATOR_CONTAINSALLVALUES)) {
            return new ContainsAllValuesOperator(attribute);
        }
        else if (operatorName.equals(OPERATOR_CONTAINS)) {
            return new ContainsOperator(attribute);
        }
        else if (operatorName.equals(OPERATOR_GREATERTHAN)) {
            return new GreaterThanOperator(attribute);
        }
        else if (operatorName.equals(OPERATOR_GREATERTHANOREQUAL)) {
            return new GreaterThanOrEqualOperator(attribute);
        }
        else if (operatorName.equals(OPERATOR_LESSTHANOREQUAL)) {
            return new LessThanOrEqualOperator(attribute);
        }
        else {
            throw new IllegalArgumentException("No such operator: " + operatorName);
        }
    }
}

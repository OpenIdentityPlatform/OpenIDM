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
 * $Id$
 */
package org.forgerock.commons.json.schema.validator.helpers;

import org.forgerock.commons.json.schema.validator.ErrorHandler;
import org.forgerock.commons.json.schema.validator.exceptions.SchemaException;
import org.forgerock.commons.json.schema.validator.exceptions.ValidationException;
import org.forgerock.commons.json.schema.validator.validators.SimpleValidator;

import java.lang.reflect.Method;

/**
 * Helper compares two {@link Number}s to check the minimum constraint.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @see <a href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.9">minimum</a>
 */
public class MinimumHelper implements SimpleValidator<Number> {

    /**
     * This attribute defines the minimum value of the instance property
     * when the validators of the instance value is a number.
     */
    private Number minimum;
    /**
     * This attribute indicates if the value of the instance (if the
     * instance is a number) can not equal the number defined by the
     * "minimum" attribute.  This is false by default, meaning the instance
     * value can be greater then or equal to the minimum value.
     */
    private int exclusiveMinimum = 0;

    public MinimumHelper(Number minimum, boolean exclusiveMinimum) {
        this.minimum = minimum;
        this.exclusiveMinimum = exclusiveMinimum ? -1 : 0;
    }

    public void validate(Number node, String at, ErrorHandler handler) throws SchemaException {

        if (minimum.getClass().isAssignableFrom(node.getClass())) {
            try {
                Method method = minimum.getClass().getMethod("compareTo",minimum.getClass());
                method.invoke(minimum,node);
                if ((Integer)method.invoke(minimum,node) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            } catch (Exception e) {
                handler.error(new ValidationException("Reflection exception at \"compareTo\" method invocation." ,e, at));
            }

//            if (minimum instanceof Float) {
//                if (((Float) minimum).compareTo((Float) node) > exclusiveMinimum) {
//                    handler.error(new ValidationException("minimum violation", at));
//                }
//            } else if (minimum instanceof Double) {
//                if (((Double) minimum).compareTo((Double) node) > exclusiveMinimum) {
//                    handler.error(new ValidationException("minimum violation", at));
//                }
//            } else if (minimum instanceof Integer) {
//                if (((Integer) minimum).compareTo((Integer) node) > exclusiveMinimum) {
//                    handler.error(new ValidationException("minimum violation", at));
//                }
//            }
        } else {
            if (minimum instanceof Float) {
                if (((Float) minimum).compareTo(node.floatValue()) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            } else if (minimum instanceof Double) {
                if (((Double) minimum).compareTo(node.doubleValue()) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            } else if (minimum instanceof Integer) {
                if (((Integer) minimum).compareTo(node.intValue()) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            } else if (minimum instanceof Long) {
                if (((Long) minimum).compareTo(node.longValue()) > exclusiveMinimum) {
                    handler.error(new ValidationException("minimum violation", at));
                }
            }
        }
    }
}

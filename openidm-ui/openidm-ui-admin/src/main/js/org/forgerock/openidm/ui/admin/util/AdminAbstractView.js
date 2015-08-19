/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define */

define("org/forgerock/openidm/ui/admin/util/AdminAbstractView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView"
], function($, _, AbstractView) {
    var AdminAbstractView = AbstractView.extend({

        compareObjects: function(property, obj1, obj2) {
            function compare(val1, val2) {
                _.each(val1, function(property, key) {
                    if (_.isEmpty(property) && !_.isNumber(property) && !_.isBoolean(property)) {
                        delete val1[key];
                    }
                });

                _.each(val2, function(property, key) {
                    if (_.isEmpty(property) && !_.isNumber(property) && !_.isBoolean(property)) {
                        delete val2[key];
                    }
                });

                return _.isEqual(val1, val2);
            }

            return compare(obj1[property], obj2[property]);
        }
    });

    return AdminAbstractView;
});

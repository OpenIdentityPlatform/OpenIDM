/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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
/*jslint evil: true */

define("org/forgerock/openidm/ui/admin/mapping/PropertiesView", [
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/admin/mapping/properties/LinkQualifiersView",
    "org/forgerock/openidm/ui/admin/mapping/properties/MappingAssignmentsView",
    "org/forgerock/openidm/ui/admin/mapping/properties/AttributesGridView"
], function(_,
            MappingAdminAbstractView,
            LinkQualifiersView,
            MappingAssignmentsView,
            AttributesGridView) {

    var PropertiesView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/PropertiesTemplate.html",
        element: "#mappingContent",
        noBaseTemplate: true,
        data: {},

        render: function (args, callback) {
            this.data.hasLinkQualifiers = !_.isUndefined(this.getCurrentMapping().linkQualifiers);

            this.parentRender(_.bind(function () {

                LinkQualifiersView.render();

                AttributesGridView.render({}, _.bind(function() {
                    if (callback) {
                        callback();
                    }
                }, this));

                MappingAssignmentsView.render();

            }, this));
        }
    });

    return new PropertiesView();
});

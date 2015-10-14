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

/*global define, JSON */

define("org/forgerock/openidm/ui/admin/mapping/properties/MappingAssignmentsView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate"
], function($, _,
            MappingAdminAbstractView,
            ResourceDelegate) {

    var MappingAssignmentsView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/properties/MappingAssignmentsViewTemplate.html",
        element: "#mappingAssignments",
        noBaseTemplate: true,
        events: {},
        model: {},
        data: {},

        render: function (args, callback) {
            this.model.mappingName = this.getMappingName();
            this.model.mapping = this.getCurrentMapping();

            ResourceDelegate.searchResource("/mapping eq '" +this.model.mappingName +"'", "managed/assignment").then(_.bind(function(assignments){

                this.data.assignments = assignments.result;

                this.parentRender(_.bind(function () {
                    if(this.data.assignments.length > 0) {
                        //Needs to be above the view scope to open the parent panel
                        $("a[href='#assignmentsBody']").collapse("show");
                        $("#assignmentsBody").toggleClass("in", true);
                    }

                    if (callback) {
                        callback();
                    }
                }, this));
            }, this));
        }
    });

    return new MappingAssignmentsView();
});

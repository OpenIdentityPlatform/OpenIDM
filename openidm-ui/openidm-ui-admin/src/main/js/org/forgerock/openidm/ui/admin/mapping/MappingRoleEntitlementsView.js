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

/*global define, $, _, Handlebars, form2js */

define("org/forgerock/openidm/ui/admin/mapping/MappingRoleEntitlementsView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/openidm/ui/admin/delegates/SearchDelegate"
], function(AbstractView,
            MappingBaseView,
            searchDelegate) {

    var MappingRoleEntitlementsView = AbstractView.extend({
        template: "templates/admin/mapping/MappingRoleEntitlementsTemplate.html",
        element: "#mappingRoleEntitlements",
        noBaseTemplate: true,
        events: {},
        model: {},
        data: {},

        render: function (args, callback) {
            this.model.mappingName = args;
            this.model.mapping = MappingBaseView.currentMapping();

            this.data.assignmentsToMap = _.sortBy(this.model.mapping.assignmentsToMap) || [];

            this.getRolesForAssignmentsToMap().then(_.bind(function(assignments) {
                this.data.rolesForAssignmentsToMap = (assignments.length) ? assignments : false;
                this.parentRender(_.bind(function () {
                    if(assignments.length) {
                        this.$el.parent().parent().find("#roleEntitlementsHeading a").click();
                    }
                    if (callback) {
                        callback();
                    }
                }, this));
            }, this));
        },
        getRolesForAssignmentsToMap: function() {
            var assignmentsArray = _.map(this.data.assignmentsToMap, function(assignmentName) {
                    var rolesForAssignment = searchDelegate.searchResults("managed/role",["/assignments/" + assignmentName],null,"pr");
                    
                    return rolesForAssignment.then(function(roles) {
                        return {
                            assignment: assignmentName,
                            roles: _.map(roles,function(role){
                                            return {
                                                roleId: role._id,
                                                roleName: role.properties.name,
                                                linkQualifiers: '"' + role.assignments[assignmentName].linkQualifiers.join('", "') + '"',
                                                attributes: _.map(role.assignments[assignmentName].attributes, function(attr) {
                                                    attr.value = JSON.stringify(attr.value, null, 2);
                                                    return attr;
                                                })
                                            };
                                        })
                        };
                    });
                }, this);
            
            return $.when.apply($, assignmentsArray).then(function() {
               return _.toArray(arguments);
            });
        }
    });

    return new MappingRoleEntitlementsView();
});
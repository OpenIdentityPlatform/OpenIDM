/**
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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/role/RoleEntitlementsListView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openidm/ui/admin/role/RoleEntitlementsEditView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _,
            AdminAbstractView,
            eventManager,
            constants,
            cookieHelper,
            resourceDelegate,
            messagesManager,
            roleEntitlementsEditView,
            configDelegate,
            UIUtils) {

    var RoleEntitlementsListView = AdminAbstractView.extend({
        element: "#role-entitlements",
        template: "templates/admin/role/RoleEntitlementsListViewTemplate.html",
        noBaseTemplate: true,
        events: {
            "click .add-assignment": "addAssignment",
            "click .edit-assignment": "editAssignment",
            "click .delete-assignment": "deleteAssignment"
        },
        addAssignment: function(e) {
            e.preventDefault();

            roleEntitlementsEditView.render(this.data.args, this.data.role, null, this);
        },
        editAssignment: function(e) {
            var assignmentName = $(e.target).closest(".role-assignment-item").attr("assignmentName");

            if(e) {
                e.preventDefault();
            }

            roleEntitlementsEditView.render(this.data.args, this.data.role, assignmentName, this);
        },
        deleteAssignment: function(e) {
            var assignmentName = $(e.target).closest(".role-assignment-item").attr("assignmentName");

            e.preventDefault();

            delete this.data.role.assignments[assignmentName];

            UIUtils.confirmDialog($.t("templates.admin.RoleEntitlementsTemplate.deleteEntitlementConfirm",{ assignment: assignmentName }), "danger", _.bind(function() {
                this.removeAssignmentsToMap(assignmentName).then(_.bind(function(){
                    resourceDelegate.updateResource(this.data.serviceUrl, this.data.role._id, this.data.role, _.bind(function() {
                        this.data.role._rev++;
                        messagesManager.messages.addMessage({"message": $.t("templates.admin.RoleEntitlementsTemplate.assignmentDeleteSuccess",{ assignment: assignmentName })});
                        this.render(this.data.args, this.data.role);
                    }, this));
                }, this));
            }, this));
        },
        render: function(args, role, callback) {
            this.data.args = args;
            this.data.role = role;
            this.data.serviceUrl = resourceDelegate.getServiceUrl(args);
            this.parentRender(function() {
                if(callback) {
                    callback();
                }
            });
        },
        removeAssignmentsToMap: function(assignmentName) {
            var queryFilter = '/assignments/' + assignmentName + ' pr and !(/_id eq "' + this.data.role._id + '")';
            //if no other roles have this assignmentName then remove it from each mapping that has the assignment in it's assignmentsToMap list
            return resourceDelegate.searchResource(queryFilter ,"managed/role").then(_.bind(function(rolesWithAssignment){
                if(!rolesWithAssignment.result.length) {
                    return configDelegate.readEntity("sync").then(_.bind(function(sync) {
                        sync.mappings = _.map(sync.mappings,function(mapping) {
                            if(mapping.assignmentsToMap) {
                                mapping.assignmentsToMap = _.reject(mapping.assignmentsToMap, function(assignment) { return assignment === assignmentName; });

                                if(!mapping.assignmentsToMap.length) {
                                    delete mapping.assignmentsToMap;
                                }
                            }

                            return mapping;
                        });

                        return configDelegate.updateEntity("sync", sync);

                    }, this));
                } else {
                    return $.Deferred().resolve();
                }
            },this));
        }
    });

    return new RoleEntitlementsListView();
});

/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

/*global define, $, form2js, _, Handlebars, sessionStorage */

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/admin/role/RoleEntitlementsListView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openidm/ui/admin/role/RoleEntitlementsEditView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(AbstractView, eventManager, constants, cookieHelper, uiUtils, resourceDelegate, messagesManager, roleEntitlementsEditView, configDelegate) {
    var RoleEntitlementsListView = AbstractView.extend({
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
            
            uiUtils.jqConfirm($.t("templates.admin.RoleEntitlementsTemplate.deleteEntitlementConfirm",{ assignment: assignmentName }), _.bind(function() {
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



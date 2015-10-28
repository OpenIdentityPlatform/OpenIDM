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

define("org/forgerock/openidm/ui/admin/role/EditRoleView", [
    "jquery",
    "underscore",
    "form2js",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openidm/ui/admin/role/RoleUsersView",
    "org/forgerock/openidm/ui/admin/role/RoleEntitlementsListView",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _,
            form2js,
            AdminAbstractView,
            eventManager,
            constants,
            resourceDelegate,
            messagesManager,
            roleUsersView,
            roleEntitlementsListView,
            UIUtils) {
    var EditRoleView = AdminAbstractView.extend({
        template: "templates/admin/role/EditRoleViewTemplate.html",

        events: {
            "click .saveRole": "saveRole",
            "click #deleteRole": "deleteRole"
        },
        render: function(args, callback) {
            var rolePromise,
                schemaPromise = resourceDelegate.getSchema(args),
                roleId = args[2],
                assignment = args[3];

            this.data.args = args;
            this.data.serviceUrl = resourceDelegate.getServiceUrl(args);

            if(roleId){
                rolePromise = resourceDelegate.readResource(this.data.serviceUrl, roleId);
                this.data.newRole = false;
            } else {
                rolePromise = $.Deferred().resolve({});
                this.data.newRole = true;
            }

            $.when(rolePromise, schemaPromise).then(_.bind(function(role, schema){
                if(role.length && !role[0].assignments) {
                    role[0].assignments = {};
                }
                this.data.role = role[0];
                this.parentRender(_.bind(function(){
                    if(!this.data.newRole) {
                        roleUsersView.render(this.data.args, this.data.role);
                        roleEntitlementsListView.render(this.data.args, this.data.role, _.bind(function() {
                            if(assignment) {
                                this.$el.find('[href="#role-entitlements"]').click();
                                this.$el.find("#edit-assignment-" + assignment).click();
                            }
                        }, this));
                    }

                    this.$el.find(":input.form-control:first").focus();

                    if(callback) {
                        callback();
                    }
                },this));
            },this));
        },
        saveRole: function(e, callback){
            var formVal = form2js(this.$el.find('form#addEditRoleForm')[0], '.', true),
                successCallback = _.bind(function(role){
                    var msg = (this.data.newRole) ? "templates.admin.ResourceEdit.addSuccess" : "templates.admin.ResourceEdit.editSuccess";
                    messagesManager.messages.addMessage({"message": $.t(msg,{ objectTitle: this.data.args[1] })});
                    if(this.data.newRole) {
                        this.data.args.push(role._id);
                        eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "adminEditManagedObjectView", args: this.data.args});
                    } else {
                        this.data.role._rev++;
                    }
                    this.render(this.data.args, callback);
                }, this);

            if(e) {
                e.preventDefault();
            }

            if(this.data.newRole){
                resourceDelegate.createResource(this.data.serviceUrl, null, formVal.role, successCallback);
            } else {
                resourceDelegate.patchResourceDifferences(this.data.serviceUrl, {id: this.data.role._id, rev: this.data.role._rev}, this.data.role, formVal.role, successCallback);
            }
        },
        deleteRole: function(e, callback){
            if(e) {
                e.preventDefault();
            }

            UIUtils.confirmDialog($.t("templates.admin.ResourceEdit.confirmDelete",{ objectTitle: this.data.args[1] }),  "danger", _.bind(function(){
                resourceDelegate.deleteResource(this.data.serviceUrl, this.data.role._id, _.bind(function(){
                    messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.deleteSuccess",{ objectTitle: this.data.role.properties.name })});

                    eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "adminListManagedObjectView", args: ["managed","role"]});

                    if(callback) {
                        callback();
                    }
                }, this));
            }, this));
        }
    });

    return new EditRoleView();
});
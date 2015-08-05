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

define("org/forgerock/openidm/ui/admin/util/WorkflowUtils", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "bootstrap-dialog"
], function($, _, UIUtils, ResourceDelegate, messagesManager, BootstrapDialog) {
    var obj = {};

    /**
     * opens a bootstrap dialog with a selectized autocomplete field pre-populated
     * with all the taskinstance's candidate users
     *
     * @param parentView {can be any Backbone view with it's "this.model" set to the taskinstance being modified}
     * @returns {nothing}
     * @constructor
     */
    obj.showCandidateUserSelection = function (parentView) {
        var _this = parentView,
            candidateUsersQueryFilter =  _.map(_this.model.get("candidates").candidateUsers, function (user) {
                return 'userName eq "' + user + '"';
            }).join(" or ");

        if (!candidateUsersQueryFilter.length) {
            candidateUsersQueryFilter = "false";
        }

        ResourceDelegate.searchResource(candidateUsersQueryFilter, "managed/user").then(function (queryResult) {
            var candidateUsers = [{ _id: "noUserAssigned", givenName: "None", sn:"", userName:"" }].concat(queryResult.result),
                select = '<select class="form-control selectize" id="candidateUsersSelect" placeholder="' + $.t("templates.taskInstance.selectUser") + '..."></select>';

            BootstrapDialog.show({
                title: $.t("templates.taskInstance.assignTask"),
                size: 430,
                type: BootstrapDialog.TYPE_DEFAULT,
                message: select,
                onshown: function () {
                    $("#candidateUsersSelect").selectize({
                        valueField: "_id",
                        labelField: "userName",
                        searchField: ["userName","givenName","sn"],
                        create: false,
                        options: candidateUsers,
                        render: {
                            item: function (item, escape) {
                                return '<div>' + item.givenName + ' ' + item.sn +
                                       '<br/> <span class="text-muted">' + item.userName + '</span></div>';
                            },
                            option: function (item, escape) {
                                return '<div>' + item.givenName + ' ' + item.sn +
                                       '<br/> <span class="text-muted">' + item.userName + '</span></div>';
                            }
                        },
                        load: _.bind(function(query, callback) {
                            var queryFilter;

                            if (!query.length) {
                                return callback();
                            } else {
                                queryFilter = "userName sw \"" + query + "\" or givenName sw \"" + query + "\" or  sn sw \"" + query + "\"";
                            }

                            ResourceDelegate.searchResource(queryFilter, "managed/user").then(function (search) {
                                    callback(search.result);
                                },
                                function() {
                                    callback();
                                }
                            );
                        }, this)

                    });
                },
                buttons: [
                    {
                        label: $.t("common.form.cancel"),
                        action: function(dialogRef) {
                            dialogRef.close();
                        }
                    },
                    {
                        label: $.t("common.form.submit"),
                        cssClass: "btn-primary",
                        action: function(dialogRef) {
                            var id = $("#candidateUsersSelect").val(),
                                label = $("#candidateUsersSelect option:selected").text(),
                                callback = function () {
                                    _this.render([_this.model.id], _.bind(function () {
                                        messagesManager.messages.addMessage({"message": $.t("templates.taskInstance.assignedSuccess")});
                                    }, this));
                                };

                            obj.assignTask(_this.model, id, label, callback);
                            dialogRef.close();
                        }
                    }
                ]
            });
        });

        /**
         * sets the assignee attribute on a taskinstance
         *
         * @param model {a taskinstance model}
         * @id {the new assignee id to be set}
         * @label {the username text to be displayed in the nonCandidateWarning}
         * @successCallback
         * @returns {nothing}
         * @constructor
         */
        obj.assignTask = function(model, id, label, successCallback) {
            var assignNow = function () {
                    model.set("assignee",id);

                    if (id === "noUserAssigned") {
                        model.set("assignee",null);
                    }

                    model.save().then(successCallback);
                };

            /*
             * before changing assignee alert the "assigner" that the user
             * being assigned does not exist in the list of candidate users
             */
            if (id !== "noUserAssigned" && !_.contains(model.get("candidates").candidateUsers, id)) {
                UIUtils.jqConfirm($.t("templates.taskInstance.nonCanditateWarning",{ userName: label }), _.bind(function() {
                    assignNow();
                }, this));
            } else {
                assignNow();
            }

        };

    };

    return obj;
});

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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars",
    "backgrid",
    "backbone",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate"
], function($, _, Handlebars,
            Backgrid,
            Backbone,
            AdminAbstractView,
            BackgridUtils,
            MaintenanceDelegate) {
    var RepoUpdatesView = AdminAbstractView.extend({
        template: "templates/admin/settings/update/RepoUpdateTemplate.html",
        element: "#repoUpdatesView",
        noBaseTemplate: true,
        events: {
            "click #complete": "completerepoUpdates",
            "click #cancelUpdate": "cancelUpdate",
            "click #continueUpdate": "continueUpdate",
            "click #completeRepoUpdates": "completeRepoUpdates"
        },
        data: {},
        model: {},
        repoUpdatesList: [],
        restartRequired: false,
        archiveName: "",
        partials: [
            "partials/settings/_updateReposGrid.html"
        ],

        /**
         * @param configs {object}
         * @param configs.archiveModel {object}
         * @param configs.repoUpdatesList {array}
         * @param configs.success {function}
         * @param configs.error {function}
         * @param [callback]
         */

        render: function(configs, callback) {

            this.repoUpdatesList = configs.data.repoUpdatesList;
            this.archiveName = configs.archiveModel.get("archive");
            this.data = configs.data;
            this.model = configs;

            var repoUpdatesGrid = this.makeGrid(this.getRepoUpdate(this.repoUpdatesList));

            _.delay(_.bind(function() {
                this.parentRender(_.bind(function () {
                    this.$el.find("#repoUpdatesGrid").append(repoUpdatesGrid.render().el);
                }, this));
            }, this), 500);

            if (callback) {
                callback();
            }
            return this;
        },

        getRepoUpdate: function(repoUpdatesList) {

            var repoUpdates = new Backbone.Collection();

            _.each(repoUpdatesList, _.bind(function (repoUpdate) {
                MaintenanceDelegate.getUpdateFile(this.archiveName, repoUpdate.path).then(

                    _.bind(function(response) {
                        repoUpdate.contents = encodeURIComponent(response.contents);
                        repoUpdates.add(repoUpdate);
                    }, this)

                );
            },  this));

            return repoUpdates;
        },

        makeGrid: function(repoUpdates) {
            return new Backgrid.Grid({
                className: "table backgrid",
                columns: BackgridUtils.addSmallScreenCell([{
                    label: $.t("templates.update.repoUpdates.tableHeader"),
                    name: "repoUpdates",
                    cell: Backgrid.Cell.extend({
                        className: "col-md-10",
                        render: function () {
                            this.$el.html(Handlebars.compile("{{> settings/_updateReposGrid}}")({
                                "file": this.model.get("file")
                            }));
                            return this;
                        }
                    }),
                    sortable: false,
                    editable: false
                }, {
                    label: "",
                    name: "",

                    cell: Backgrid.Cell.extend({
                        className: "col-md-2",
                        render: function () {
                            var url = this.model.get("url"),
                                file = this.model.get("file"),
                                buttonText = $.t("templates.update.repoUpdates.download"),
                                button;

                            button = '<a href="data:text/plain;charset=utf-8,' + url + '" download="' + file + '"><button type="button" class="test pull-right btn btn-primary btn-sm">' + buttonText + '</button>';
                            this.$el.html(button);
                            return this;
                        }
                    }),
                    sortable: false,
                    editable: false
                }], true),
                collection: repoUpdates
            });
        },

        cancelUpdate: function(e) {
            if (e) {
                e.preventDefault();
            }
            this.model.cancel();
        },

        continueUpdate: function(e) {
            if (e) {
                e.preventDefault();
            }
            this.model.archiveModel.viewed = true;
            this.model.install(this.model.archiveModel, this.model.data.repoUpdatesList);
        },

        completeRepoUpdates: function(e) {
            if (e) {
                e.preventDefault();
            }
            MaintenanceDelegate.markComplete(this.model.runningID).then(
                _.bind(function(res) {

                    $("#menu, #footer, #settingsBody").hide();
                    this.data.successful = true;

                    this.model.completeInstall({
                        "archiveModel": this.model.archiveModel,
                        "runningID": this.model.runningID,
                        "data": this.data,
                        "response": this.model.response
                    });
                }, this)
            );
        }
    });

    return new RepoUpdatesView();
});

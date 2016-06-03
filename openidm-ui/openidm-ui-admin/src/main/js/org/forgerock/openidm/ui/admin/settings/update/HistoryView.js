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

/*global define*/

define("org/forgerock/openidm/ui/admin/settings/update/HistoryView", [
    "jquery",
    "underscore",
    "handlebars",
    "backgrid",
    "bootstrap-dialog",
    "backbone",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/InfoDelegate",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate",
    "org/forgerock/openidm/ui/admin/delegates/ClusterDelegate"

], function($, _, Handlebars, Backgrid, BootstrapDialog, Backbone,
            AdminAbstractView,
            InfoDelegate,
            BackgridUtils,
            DateUtil,
            Constants,
            MaintenanceDelegate,
            ClusterDelegate) {

    var HistoryView = AdminAbstractView.extend({
        template: "templates/admin/settings/update/HistoryTemplate.html",
        element: "#historyView",
        noBaseTemplate: true,
        data: {
            "errorMessage": false,
            "version": "",
            "revision": "",
            "noVersions": false,
            "updateCLI": false,
            "docHelpUrl": Constants.DOC_URL,
            "logs": {}
        },
        model: {},
        partials: [
            "partials/settings/_updateHistoryGridMain.html"
        ],

        /**
         * If not in a clustered environment and there are version they will be rendered
         *
         * @param {object} configs
         * @param {object} configs.errorMsg - the error message to display, if any
         * @param {object} configs.archiveSelected - the function called when an archive is selected
         * @param [callback]
         */
        render: function(configs, callback) {

            var PreviousUpdates = new Backbone.Collection();
            this.data.previousUpdates = configs.previousUpdates;
            this.model.viewDetails = configs.viewDetails;

            this.data.previousUpdates.forEach(function(update) {
                PreviousUpdates.add(update);
            });
            this.init(PreviousUpdates, callback);
        },

        init: function(Versions, callback) {
            var versionGrid, self = this;

            this.parentRender(_.bind(function () {
                    versionGrid = new Backgrid.Grid({
                        className: "table backgrid",
                        emptyText: $.t("templates.update.versions.noVersionsBG"),
                        columns: BackgridUtils.addSmallScreenCell([{
                            label: 'update',
                            name: "archive",
                            cell: Backgrid.Cell.extend({
                                className: "col-md-10",
                                render: function () {
                                    var date;

                                    if (this.model.get("endDate")) {
                                        date = DateUtil.formatDate(this.model.get("endDate"), "MMM dd, yyyy");
                                    } else {
                                        date = "";
                                    }

                                    this.$el.html(Handlebars.compile("{{> settings/_updateHistoryGridMain}}")({
                                        "archive": this.model.get("archive"),
                                        "user": this.model.get("userName"),
                                        "date": date
                                    }));

                                    return this;
                                }
                            }),
                            sortable: false,
                            editable: false
                        }, {
                            name: "",
                            cell: Backgrid.Cell.extend({
                                events: {
                                    "click .test": "goToDetails"
                                },
                                className: "col-md-2",

                                render: function () {
                                    var disable,
                                        button;

                                    button = '<button type="button" class="test pull-right btn btn-primary btn-sm">';
                                    button += $.t("templates.update.history.viewDetails");
                                    button += '</button>';

                                    this.$el.html(button);
                                    this.delegateEvents();
                                    return this;
                                },

                                goToDetails: function () {
                                    var id = this.model.get('_id'),
                                        response = this.model.attributes,
                                        version = this.model.get('archive'),
                                        isHistoricalInstall = true;

                                    self.model.viewDetails(id, response, version, isHistoricalInstall);
                                }
                            }),
                            sortable: false,
                            editable: false
                        }], true),
                        collection: Versions
                    });

                    this.$el.find("#historyGrid").append(versionGrid.render().el);

                if (callback) {
                    callback();
                }
            }, this));
        }
    });

    return new HistoryView();
});

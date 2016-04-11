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
 * Copyright 2015 ForgeRock AS.
 */

/*global define, window*/

define("org/forgerock/openidm/ui/admin/settings/update/InstallationReportView", [
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/util/TreeGridUtils",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/SpinnerManager",
    "org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate"

], function($, _, Handlebars,
            AdminAbstractView,
            TreeGridUtils,
            UIUtils,
            DateUtil,
            Constants,
            SpinnerManager,
            MaintenanceDelegate
        ) {

    var InstallationReportView = AdminAbstractView.extend({
        template: "templates/admin/settings/update/InstallationReportTemplate.html",
        element: "#installationReportView",
        noBaseTemplate: true,
        events: {
            "click .treegrid-expander": "showHideNode",
            "click .collapse-treegrid": "collapseTreegrid",
            "click .expand-treegrid": "expandTreegrid",
            "click .back": "back"
        },
        partials: [
            "partials/settings/_updateTreeGrid.html"
        ],
        data: {
            "treeGrid": {},
            "responseB64": "",
            "version": ""
        },
        model: {},

        /**
         * @param configs
         * @param configs.response {object}
         * @param configs.version {string}
         * @param configs.error {function}
         * @param configs.back {function}
         * @param [callback]
         */
        render: function(configs, callback) {

            // Manipulating the treegrid could take a few seconds given enough data, so we invoke the spinner manually.
            SpinnerManager.showSpinner();

            this.model = configs;
            this.data = _.extend(this.data, _.pick(this.model, ["treeGrid", "responseB64", "version"]));

            if (configs.isHistoricalInstall) {
                this.data.isHistoricalInstall = true;
                this.data.date = DateUtil.formatDate(this.model.response.endDate, "MMM dd, yyyy");
                this.data.user = this.model.response.userName;
            }

            MaintenanceDelegate.getLogDetails(this.model.runningID)
            .then(function(logData) {
                this.model.response.files = logData.files;

                    UIUtils.preloadPartial("partials/settings/_updateStatePopover.html").then(_.bind(function() {
                        this.data.treeGrid = TreeGridUtils.filepathToTreegrid("filePath", this.formatFiles(), ["filePath", "actionTaken"]);

                        if (this.model.response) {
                            this.data.responseB64 = window.btoa(JSON.stringify(this.model.response));
                        }

                        this.parentRender(_.bind(function() {
                            SpinnerManager.hideSpinner();

                            this.$el.find('[data-toggle="popover"]').popover({
                                placement: 'top',
                                container: 'body',
                                title: ''
                            });

                            if (callback) {
                                callback();
                            }
                        }, this));
                    }, this));

            }.bind(this));

        },

        back: function(e) {
            if (e) {
                e.preventDefault();
            }
            this.model.back();
        },

        collapseTreegrid: function(e) {
            if (e) {
                e.preventDefault();
            }

            // Manipulating the treegrid could take a few seconds given enough data, so we invoke the spinner manually.
            SpinnerManager.showSpinner();

            // The delay is to ensure that the spinner is rendered before any resource heavy rendering
            // beings, otherwise the spinner may not show at all.
            _.delay(_.bind(function() {
                this.$el.find(".node-container").hide();
                this.$el.find(".treegrid-expander").toggleClass("fa-caret-right", true);
                this.$el.find(".treegrid-expander").toggleClass("fa-caret-down", false);
                SpinnerManager.hideSpinner();
            }, this), 1);
        },

        expandTreegrid: function(e) {
            if (e) {
                e.preventDefault();
            }

            // Manipulating the treegrid could take a few seconds given enough data, so we invoke the spinner manually.
            SpinnerManager.showSpinner();

            // The delay is to ensure that the spinner is rendered before any resource heavy rendering
            // beings, otherwise the spinner may not show at all.
            _.delay(_.bind(function() {
                this.$el.find(".node-container").show();
                this.$el.find(".treegrid-expander").toggleClass("fa-caret-right", false);
                this.$el.find(".treegrid-expander").toggleClass("fa-caret-down", true);
                SpinnerManager.hideSpinner();
            }, this), 1);
        },

        showHideNode: function(e) {
            if (e) {
                e.preventDefault();
            }

            $(e.currentTarget).siblings("div").toggle();
            $(e.currentTarget).toggleClass("fa-caret-right");
            $(e.currentTarget).toggleClass("fa-caret-down");
        },

        formatFiles: function() {
            var formattedFileList,
                files = _.clone(this.model.response.files, true);

                formattedFileList = _.map(files, function (file) {
                    var temp = file.filePath.split("/");
                    file.actionTaken = Handlebars.compile("{{> settings/_updateStatePopover}}")({
                        "desc": $.t("templates.update.review.actionTaken." + file.actionTaken + ".desc"),
                        "name": $.t("templates.update.review.actionTaken." + file.actionTaken + ".reportName")
                    });
                    file.fileName = _.last(temp);
                    file.partialFilePath = _.take(temp, temp.length - 1).join("");
                    return file;
                });

                return _.sortByAll(formattedFileList, [
                    function(i) {
                        if (i.partialFilePath.length > 0) {
                            return i.partialFilePath.toLowerCase();
                        } else {
                            return false;
                        }
                    },
                    function(i) { return i.fileName.toLowerCase();}
                ]);

        }
    });

    return new InstallationReportView();
});

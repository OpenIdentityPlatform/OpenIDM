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

define("org/forgerock/openidm/ui/admin/settings/update/InstallationPreviewView", [
    "jquery",
    "underscore",
    "handlebars",
    "bootstrap",
    "bootstrap-dialog",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/util/TreeGridUtils",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/SpinnerManager",
    "org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate"

], function($, _, Handlebars, Bootstrap, BootstrapDialog,
            AdminAbstractView,
            TreeGridUtils,
            UIUtils,
            Constants,
            SpinnerManager,
            MaintenanceDelegate) {

    var VersionsView = AdminAbstractView.extend({
        template: "templates/admin/settings/update/InstallationPreviewTemplate.html",
        element: "#installationPreviewView",
        noBaseTemplate: true,
        events: {
            "click .show-modified-files": "showModifiedTreegrid",
            "click .show-all-files": "showAllTreegrid",
            "click #cancelUpdate": "cancelUpdate",
            "click #installUpdate": "installUpdate",
            "click .treegrid-expander": "showHideNode",
            "click .collapse-treegrid": "collapseTreegrid",
            "click .expand-treegrid": "expandTreegrid"
        },
        partials: [
            "partials/settings/_updateTreeGrid.html"
        ],
        data: {
            "version": "",
            "link": false,
            "modifiedFilesExist": true,
            "all": false,
            "treeGrid": {}
        },
        model: {},

        /**
         * @param configs {object}
         * @param configs.files {array}
         * @param configs.archiveModel {object}
         * @param configs.install {function}
         * @param configs.cancel {function}
         * @param configs.error {function}
         * @param [callback]
         */
        render: function(configs, callback) {
            // Manipulating the treegrid could take a few seconds given enough data, so we invoke the spinner manually.
            SpinnerManager.showSpinner();

            // The delay is to ensure that the spinner is rendered before any resource heavy rendering
            // beings, otherwise the spinner may not show at all.
            _.delay(_.bind(function() {
                // This partial is used before the parent render where it would normally be loaded.
                UIUtils.preloadPartial("partials/settings/_updateStatePopover.html").then(_.bind(function () {
                    this.model = configs;

                    this.data = _.extend(this.data, _.pick(this.model, ["modifiedFilesExist", "all", "treeGrid"]));
                    this.data.link = this.model.archiveModel.get("resource");
                    this.data.version = this.model.archiveModel.get("version");

                    if (this.data.all && _.has(this.model, "allTreeGrid")) {
                        this.data.treeGrid = this.model.allTreeGrid;

                    } else if (!this.data.all && _.has(this.model, "modifiedTreeGrid")) {
                        this.data.treeGrid = this.model.modifiedTreeGrid;

                    } else {
                        this.data.treeGrid = TreeGridUtils.filepathToTreegrid("filePath", this.formatFiles(this.data.all), ["filePath", "fileState"]);

                        if (this.data.all) {
                            this.model.allTreeGrid = this.data.treeGrid;
                        } else {
                            this.model.modifiedTreeGrid = this.data.treeGrid;
                        }
                    }

                    this.parentRender(_.bind(function () {
                        SpinnerManager.hideSpinner();

                        this.$el.find('[data-toggle="popover"]').popover({
                            trigger: 'hover click',
                            placement: 'top',
                            container: 'body',
                            title: ''
                        });

                        if (callback) {
                            callback();
                        }
                    }, this));

                }, this));
            },this), 1);
        },

        showModifiedTreegrid: function(e) {
            if (e) {
                e.preventDefault();
            }

            this.$el.find(".filter-file-list").toggleClass("active", false);
            $(e.currentTarget).toggleClass("active");
            this.data.all = false;
            this.render(_.extend(this.data, this.model));
        },

        showAllTreegrid: function(e) {
            if (e) {
                e.preventDefault();
            }

            this.$el.find(".filter-file-list").toggleClass("active", false);
            $(e.currentTarget).toggleClass("active");
            this.data.all = true;
            this.render(_.extend(this.data, this.model));
        },

        installUpdate: function(e) {
            if (e) {
                e.preventDefault();
            }

            MaintenanceDelegate.getLicense(this.model.archiveModel.get("archive")).then(
                _.bind(function(response) {
                    if (response.license) {

                        var self = this;
                        BootstrapDialog.show({
                            title: $.t("templates.update.preview.licenseAgreement"),
                            type: "type-default",
                            message: response.license,
                            cssClass: "scrollingLicenseAgreement",
                            buttons: [
                                {
                                    label: $.t('common.form.cancel'),
                                    action: _.bind(function (dialog) {
                                        dialog.close();
                                        this.model.cancel();
                                    }, this)
                                }, {
                                    label: $.t("templates.update.preview.acceptLicense"),
                                    cssClass: "btn-primary",
                                    action: _.bind(function (dialog) {
                                        dialog.close();
                                        this.model.install(self.model.archiveModel);
                                    }, this)
                                }
                            ]
                        });
                    } else {
                        this.model.install(this.model.archiveModel);
                    }
                }, this),
                _.bind(function() {
                    this.model.error($.t("templates.update.preview.errorInitiatingUpdate"));
                }, this)
            );

        },

        cancelUpdate: function(e) {
            if (e) {
                e.preventDefault();
            }

            this.model.cancel();
        },

        collapseTreegrid: function(e) {
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
            $(e.currentTarget).siblings("div").toggle();
            $(e.currentTarget).toggleClass("fa-caret-right");
            $(e.currentTarget).toggleClass("fa-caret-down");
        },

        formatFiles: function(all) {
            var formattedFileList = [],
                files = _.clone(this.model.files, true);

            if (all) {
               formattedFileList = _.map(files, function (file) {
                    var temp = file.filePath.split("/");
                    file.fileState = Handlebars.compile("{{> settings/_updateStatePopover}}")({
                        "desc": $.t("templates.update.preview.fileStates." + file.fileState + ".desc"),
                        "name": $.t("templates.update.preview.fileStates." + file.fileState + ".previewName")
                    });
                    file.fileName = _.last(temp);
                    file.partialFilePath = _.take(temp, temp.length-1).join("");
                    return file;
                });

            } else {
                files = _.filter(files, function(file) {
                    return _.has(file, "fileState") && file.fileState !== "UNCHANGED";
                });

                if (files.length === 0) {
                    this.data.modifiedFilesExist = false;
                    this.data.all = true;
                    this.render(_.extend(this.data, this.model));

                } else {
                    formattedFileList = _.map(files, function (file) {
                        var temp = file.filePath.split("/");
                        file.fileState = Handlebars.compile("{{> settings/_updateStatePopover}}")({
                            "desc": $.t("templates.update.preview.fileStates." + file.fileState + ".desc"),
                            "name": $.t("templates.update.preview.fileStates." + file.fileState + ".previewName")
                        });
                        file.fileName = _.last(temp);
                        file.partialFilePath = _.take(temp, temp.length-1).join("");
                        return file;
                    });
                }
            }

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

    return new VersionsView();
});

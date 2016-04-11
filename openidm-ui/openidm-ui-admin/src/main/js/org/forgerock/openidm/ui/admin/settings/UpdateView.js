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
 * Copyright 2015-2016 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/openidm/ui/admin/settings/UpdateView", [
    "jquery",
    "underscore",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/main/AbstractCollection",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/settings/update/VersionsView",
    "org/forgerock/openidm/ui/admin/settings/update/HistoryView",
    "org/forgerock/openidm/ui/admin/settings/update/InstallationPreviewView",
    "org/forgerock/openidm/ui/admin/settings/update/MaintenanceModeView",
    "org/forgerock/openidm/ui/admin/settings/update/InstallView",
    "org/forgerock/openidm/ui/admin/settings/update/InstallationReportView",
    "org/forgerock/openidm/ui/admin/settings/update/RepoUpdateView",
    "org/forgerock/openidm/ui/admin/delegates/MaintenanceDelegate"

], function($, _, Backgrid,
            AdminAbstractView,
            AbstractModel,
            AbstractCollection,
            Constants,
            VersionsView,
            HistoryView,
            InstallationPreviewView,
            MaintenanceModeView,
            InstallView,
            InstallationReportView,
            RepoUpdateView,
            MaintenanceDelegate) {

    var UpdateView = AdminAbstractView.extend({
        template: "templates/admin/settings/UpdateTemplate.html",
        element: "#updateContainer",
        noBaseTemplate: true,
        events: {},

        render: function(args, callback) {

            this.data.docHelpUrl = Constants.DOC_URL;

            this.parentRender(_.bind(function() {

                switch (args.step) {

                    /*
                     *  Version is the first step, here all versions are listed for update.
                     *  If for some reason you are in maintenance mode at this point, and there is no running
                     *  ID then maintenance mode will be exited before the Version view is rendered.
                     *  If you do have a running ID this step will render the install step.
                     */
                    case "version":
                        MaintenanceDelegate.getStatus()

                        .then(_.bind(function(maintenanceData) {
                            MaintenanceDelegate.getUpdateLogs({excludeFields: ['files']})
                            .then(_.bind(function(updateLogData) {
                                var runningUpdate = _.findWhere(updateLogData.result, {"status": "IN_PROGRESS"});

                                // There isn't a running install and OpenIDM is in maintenance mode
                                if (maintenanceData.maintenanceEnabled === true && updateLogData.result.length === 0) {
                                    this.render({"step": "exitMaintenanceMode"});

                                // OpenIDM is performing an update
                                } else if (!_.isUndefined(runningUpdate)) {
                                    this.render({"step": "install", "runningID": runningUpdate._id});

                                // The user wishes to begin a new update, show them which version they have
                                } else {
                                    VersionsView.render({
                                        "errorMsg": args.errorMessage,
                                        "archiveSelected": _.bind(function (model) {
                                            this.render({"step": "enterMaintenanceMode", "model": model});
                                        }, this)
                                    }, function() {
                                        this.$el.find('#versionHistoryGroup').toggleClass('hidden');
                                    }.bind(this));
                                    if (updateLogData.resultCount > 0 ) {
                                        HistoryView.render({
                                             "errorMsg": args.errorMessage,
                                             "previousUpdates": updateLogData.result,
                                             "viewDetails": _.bind(function (runningID, response, version, isHistoricalInstall) {
                                                 this.render({
                                                     "step": "installationReport",
                                                     "runningID": runningID,
                                                     "response": response,
                                                     "version": version,
                                                     "isHistoricalInstall": true
                                                 });
                                             }, this)
                                         }, _.noop);
                                    }
                                }
                            }, this));
                        }, this));
                        break;

                    /*
                     *  Enter Maintenance Mode takes the selected archive model and passes it, along with the files, to the preview after shutting down schedules,
                     *  entering maintenance mode and getting preview data.  Preview data may take a few moments to get,
                     *  so its combined into this loading zone.
                     */
                    case "enterMaintenanceMode":
                        MaintenanceModeView.render({
                            "enterMaintenanceMode": true,
                            "archive": args.model.get("archive"),
                            "success": _.bind(function(files) {
                                this.render({"step": "previewInstallation", "files": files, archiveModel: args.model});
                            }, this),
                            "error": _.bind(function() {
                                this.render({"step": "version", "errorMessage": $.t("templates.update.maintenanceMode.errorFailedToEnter")});
                            }, this)

                        }, _.noop);
                        break;

                    /*
                     *  Preview Installation takes the files and archive model, and generates a preview for the installation.  If the user cancels,
                     *  we exit maintenance mode and start up the scheduler, if they wish to proceed with the install and agree to the license (if any),
                     *  then we to pass the archive model and last update id to the installer page.
                     */
                    case "previewInstallation":
                        InstallationPreviewView.render({
                            "files": args.files,
                            "archiveModel": args.archiveModel,
                            "install": _.bind(function (archiveModel, repoUpdates) {
                                this.render({"step": "install", "archiveModel": archiveModel, "repoUpdates": repoUpdates});
                            }, this),
                            "repoUpdates": _.bind(function (args) {
                                this.render({"step": "repoUpdates", "archiveModel": args.archiveModel, "data": args.data, "files": args.files});
                            }, this),
                            "cancel": _.bind(function () {
                                this.render({"step": "exitMaintenanceMode"});
                            }, this),
                            "error": _.bind(function (errorMsg) {
                                this.render({"step": "version", "errorMessage": errorMsg});
                            }, this)
                        }, _.noop);
                        break;

                    /*
                     *  Displays repo updates that must be manually completed before update can complete
                     */
                    case "repoUpdates":
                        RepoUpdateView.render({
                            "archiveModel": args.archiveModel,
                            "data": args.data,
                            "runningID": args.runningID || null,
                            "files": args.files || null,
                            "response": args.response || null,
                            "install": _.bind(function (archiveModel, repoUpdatesList) {
                                this.render({"step": "install", "archiveModel": archiveModel, "repoUpdatesList": repoUpdatesList});
                            }, this),
                            "completeInstall": _.bind(function(args) {
                                this.render({"step": "install", "archiveModel": args.archiveModel, "runningID": args.runningID, "data": args.data, "response": args.response});
                            }, this),
                            "cancel": _.bind(function () {
                                this.render({"step": "exitMaintenanceMode"});
                            }, this),
                            "error": _.bind(function(error) {
                                this.render({"step": "version", "errorMessage": error});
                            }, this)
                        }, _.noop);
                        break;

                    /*
                     *  Given the archive data model or a running id and the last update id OpenIDM will update and show the status.
                     */
                    case "install":
                        InstallView.render({
                            "archiveModel": args.archiveModel || null,
                            "repoUpdatesList": args.repoUpdatesList || null,
                            "runningID": args.runningID || null,
                            "lastUpdateId": args.lastUpdateId || null,
                            "data": args.data || null,
                            "response": args.response || null,
                            "success": _.bind(function(data) {
                                this.render({"step": "exitMaintenanceMode", data: {"response": data.response, "runningID": data.runningID, "version": data.version}});
                            }, this),
                            "repoUpdates": _.bind(function (args) {
                                this.render({"step": "repoUpdates", "archiveModel": args.archiveModel, "data": args.data, "runningID": args.model.runningID, "response": args.model.response});
                            }, this),
                            "error": _.bind(function(error) {
                                this.render({"step": "version", "errorMessage": error});
                            }, this)
                        }, _.noop);

                        break;

                    /*
                     *  Exits maintenance mode and restarts the schedules.  If arguments are included the installationReport will be rendered,
                     *  otherwise the version view will be rendered.
                     */
                    case "exitMaintenanceMode":
                        MaintenanceModeView.render({
                            "enterMaintenanceMode": false,
                            "data": args.data || false,
                            "success": _.bind(function(data) {
                                if (data && _.has(data, "response")) {
                                    this.render({"step": "installationReport", "response": data.response, "runningID": data.runningID, "version": data.version});
                                } else {
                                    this.render({"step": "version"});
                                }
                            }, this),
                            "error": _.bind(function() {
                                this.render({"step": "version", "errorMessage": $.t("templates.update.maintenanceMode.errorFailedToExit")});
                            }, this)

                        }, _.noop);
                        break;

                    /*
                     *  Given the running ID and the updated file list this view will display the install results.
                     */
                    case "installationReport":
                        InstallationReportView.render({
                            "runningID": args.runningID,
                            "response": args.response,
                            "version": args.version,
                            "isHistoricalInstall": args.isHistoricalInstall,
                            "error": _.bind(function(msg) {
                                this.render({"step": "version", "errorMessage": msg});
                            }, this),
                            "back": _.bind(function(kik) {
                                this.render({"step": "version"});
                            }, this)
                        }, _.noop);

                        break;



                    default:
                        this.render({"step": "version", "errorMessage": "Update state unfound."});
                        break;
                }

                if (callback) {
                    callback();
                }

            }, this));

        }
    });

    return new UpdateView();
});

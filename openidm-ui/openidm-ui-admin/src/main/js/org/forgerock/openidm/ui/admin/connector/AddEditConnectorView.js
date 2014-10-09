/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global define, $, _, Handlebars, form2js, window */

define("org/forgerock/openidm/ui/admin/connector/AddEditConnectorView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/admin/connector/ConnectorTypeView",
    "org/forgerock/openidm/ui/admin/connector/ConnectorRegistry",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/objectTypes/ObjectTypesDialog",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/util/Scheduler",
    "org/forgerock/openidm/ui/admin/util/ScriptEditor"

], function(AdminAbstractView,
            eventManager,
            validatorsManager,
            constants,
            ConnectorDelegate,
            ConnectorType,
            ConnectorRegistry,
            connectorUtils,
            router,
            ConfigDelegate,
            objectTypesDialog,
            SchedulerDelegate,
            Scheduler,
            ScriptEditor) {

    var AddEditConnectorView = AdminAbstractView.extend({
        template: "templates/admin/connector/AddEditConnectorTemplate.html",
        events: {
            "change #connectorType" : "loadConnectorTemplate",
            "onValidate": "onValidate",
            "click #connectorForm fieldset legend" : "sectionHideShow",
            "click .alert-message .close-button": "closeError",
            "click #addEditObjectType": "addEditObjectTypes",
            "click #validateConnector": "validate",
            "change input" : "disableButtons",
            "click .addLiveSync" : "addLiveSync",
            "change .retryOptions": "retryOptionChanged",
            "change .postRetryAction": "postRetryActionChange",
            "blur #connectorName" : "updateLiveSyncObjects"
        },
        data: {

        },
        connectorTypeRef: null,
        connectorList: null,
        oAuthConnector: false,
        oAuthReturned: false,

        render: function(args, callback) {
            this.data = {};
            this.data.versionDisplay = {};
            this.data.currentMainVersion = null;
            this.oAuthConnector = false;
            this.oAuthReturned = false;
            this.connectorTypeRef = null;
            this.connectorList = null;
            this.postActionBlockScript = null;
            this.name = null;
            this.objectTypes = null;
            this.addedLiveSyncSchedules = [];
            this.connectorTypeRef = null;
            this.userDefinedObjectTypes = null;

            ConnectorDelegate.availableConnectors().then(_.bind(function(connectors){
                this.data.connectors = connectors.connectorRef;

                //Clean up display names to use translated names
                _.each(this.data.connectors , function(connector){
                    connector.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(connector.connectorName));
                }, this);

                //Build Connector type selection
                this.data.versionDisplay = _.chain(this.data.connectors)
                    .groupBy( function(connectorRef) {
                        return connectorRef.displayName;
                    })
                    .pairs()
                    .sortBy(function(connectorRef) {
                        return connectorRef[0];
                    })
                    .map(function(connectorRef){
                        connectorRef[1].displayName = connectorRef[0];

                        return {
                            "groupName" : connectorRef[0],
                            "versions" : connectorRef[1]
                        };
                    })
                    .value();

                if(args.length === 0) {
                    this.data.editState = false;
                    this.data.connectorName = "";
                    this.data.addEditTitle = $.t("templates.connector.addTitle");
                    this.data.addEditSubmitTitle = $.t("templates.connector.addButtonTitle");
                    this.data.addEditObjectTypeTitle = $.t("templates.connector.addObjectTypeTitle");
                    this.data.addEditSubmitTitle = $.t("common.form.add");

                    this.parentRender(_.bind(function () {
                        validatorsManager.bindValidators(this.$el);

                        this.loadConnectorTemplate();

                        this.setupLiveSync();

                        if(callback){
                            callback();
                        }
                    }, this));
                } else {
                    var splitDetails = args[0].split("_");
                    this.data.editState = true;

                    this.data.systemType = splitDetails[0];

                    // FIXME support multiple provisioners based on systemType
                    ConfigDelegate.readEntity(this.data.systemType +"/" +splitDetails[1]).then(_.bind(function(data){
                        var tempVersion;

                        data.connectorRef.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(data.connectorRef.connectorName));
                        this.data.connectorName = this.name = data.name;
                        this.data.connectorType = data.connectorRef.connectorName;
                        this.data.enabled = data.enabled;
                        this.data.addEditTitle = $.t("templates.connector.editTitle");
                        this.data.addEditSubmitTitle = $.t("templates.connector.updateButtonTitle");
                        this.data.addEditObjectTypeTitle = $.t("templates.connector.editObjectTypeTitle");
                        this.data.addEditSubmitTitle = $.t("common.form.update");

                        this.objectTypes = data.objectTypes;

                        //Filter down to the current edited connector Type
                        this.data.versionDisplay = _.filter(this.data.versionDisplay, function(connector){
                            return $.t("templates.connector." +connectorUtils.cleanConnectorName(this.data.connectorType)) === connector.groupName;
                        }, this);

                        data.connectorRef.bundleVersion = this.versionRangeCheck(data.connectorRef.bundleVersion);
                        this.data.currentMainVersion = this.findMainVersion(data.connectorRef.bundleVersion);

                        //Filter the connector types down to the current major version
                        this.data.versionDisplay[0].versions = _.filter(this.data.versionDisplay[0].versions, function(version){
                            tempVersion = this.findMainVersion(version.bundleVersion);

                            return parseFloat(this.data.currentMainVersion) === parseFloat(tempVersion);
                        }, this);

                        this.parentRender(_.bind(function() {
                            var urlArgs = window.location.hash;

                            switch (data.syncFailureHandler.maxRetries) {
                                case 0:
                                    this.$el.find(".retryOptions").val("0").change();
                                    break;
                                case -1:
                                    this.$el.find(".retryOptions").val("-1").change();
                                    break;
                                default:
                                    this.$el.find(".retryOptions").val("*").change();
                                    this.$el.find(".maxRetries").val(data.syncFailureHandler.maxRetries);
                                    break;
                            }

                            if (_.has(data.syncFailureHandler.postRetryAction, "script")) {
                                this.$el.find(".postRetryAction").val("script");
                                this.postActionBlockScript = ScriptEditor.generateScriptEditor({
                                    "element": this.$el.find(".postActionBlock .script"),
                                    "eventName": "",
                                    "deleteElement": false,
                                    "scriptData": data.syncFailureHandler.postRetryAction.script
                                });
                                this.$el.find(".postActionBlock .script").show();
                            } else {
                                this.$el.find(".postRetryAction").val(data.syncFailureHandler.postRetryAction);
                            }

                            validatorsManager.bindValidators(this.$el);

                            $("#connectorType").val(this.data.connectorType +"_" +data.connectorRef.bundleVersion +"_" +this.data.systemType);

                            if(this.data.rangeFound) {
                                this.$el.find("#connectorErrorMessage .alert-message .message").html($.t("config.messages.ConnectorMessages.connectorVersionChange", {"range" : this.data.oldVersion, "version" : data.connectorRef.bundleVersion}));
                                this.$el.find("#connectorErrorMessage").show();
                            }

                            this.connectorTypeRef = ConnectorRegistry.getConnectorModule(data.connectorRef.connectorName +"_" +this.data.currentMainVersion);

                            if (urlArgs.match(/code=/)) {
                                this.oAuthCode = urlArgs.match(/code=([^&]+)/)[1];
                                this.oAuthReturned = true;
                            } else {
                                this.oAuthReturned = false;
                            }

                            this.connectorTypeRef.render({"connectorType": data.connectorRef.connectorName +"_" +this.data.currentMainVersion,
                                    "animate": true,
                                    "connectorDefaults": data,
                                    "oAuthReturned" : this.oAuthReturned,
                                    "editState" : this.data.editState,
                                    "systemType" : this.data.systemType },
                                _.bind(function(){
                                    validatorsManager.validateAllFields(this.$el);

                                    if(this.connectorTypeRef.oAuthConnector) {
                                        this.oAuthConnector = true;
                                    } else {
                                        this.oAuthConnector = false;
                                    }

                                    this.setSubmitFlow();

                                    //Set the current newest version incase there is a range
                                    this.connectorTypeRef.data.connectorDefaults.connectorRef.bundleVersion = data.connectorRef.bundleVersion;
                                }, this));

                            this.setupLiveSync();

                            if(callback){
                                callback();
                            }

                        }, this));

                    }, this));
                }

            }, this));
        },

        setSubmitFlow: function() {
            this.$el.find("#addEditConnector").unbind("click");

            if(this.oAuthConnector) {
                this.$el.find("#addEditObjectType").hide();

                if(this.oAuthReturned || this.connectorTypeRef.data.connectorDefaults.configurationProperties.refreshToken !== null) {
                    this.$el.find("#addEditConnector").bind("click", _.bind(this.formSubmit, this));
                } else {
                    this.$el.find("#addEditConnector").bind("click", _.bind(this.oAuthFormSubmit, this));
                }
            } else {
                this.$el.find("#addEditObjectType").show();
                this.$el.find("#addEditConnector").bind("click", _.bind(this.formSubmit, this));
            }
        },

        addLiveSync: function(schedule) {
            var source = this.$el.find(".sources option:selected");

            if (source.length > 0) {
                this.$el.find("#schedules").append("<div class='liveSyncScheduleContainer'></div>");

                Scheduler.generateScheduler({
                    "element": this.$el.find("#schedules .liveSyncScheduleContainer").last(),
                    "defaults": {},
                    "onDelete": _.bind(this.removeSchedule, this),
                    "invokeService": "provisioner",
                    "source": source.val(),
                    "newSchedule": true
                });

                this.addedLiveSyncSchedules.push(source.val());
                source.remove();

                if (this.$el.find(".sources option:selected").length === 0) {
                    this.$el.find(".addLiveSync").prop('disabled', true);
                    this.$el.find(".sources").prop('disabled', true);
                }
            }
        },

        setupLiveSync: function() {
            var tempName = "",
                sourcePieces = [];

            this.updateLiveSyncObjects();

            // Get all schedule IDS
            SchedulerDelegate.availableSchedules().then(_.bind(function (schedules) {
                var schedulerPromises = [];

                _.each(schedules.result, function (index) {
                    // Get the schedule of each ID
                    schedulerPromises.push(SchedulerDelegate.specificSchedule(index._id));
                }, this);

                $.when.apply($, schedulerPromises).then(_.bind(function () {
                    _.each(schedulerPromises, function (schedule) {
                        schedule = schedule.responseJSON;
                        //////////////////////////////////////////////////////////////////////////////////////////////////
                        //                                                                                              //
                        // TODO: Use queryFilters to avoid having to pull back all schedules and sifting through them.  //
                        //                                                                                              //
                        //////////////////////////////////////////////////////////////////////////////////////////////////
                        if (schedule.invokeContext.action === "liveSync") {

                            sourcePieces = schedule.invokeContext.source.split("/");

                            if(sourcePieces[1] === this.name) {
                                this.$el.find(".sources option[value='" + schedule.invokeContext.source + "']").remove();

                                this.$el.find("#schedules").append("<div class='liveSyncScheduleContainer'></div>");
                                Scheduler.generateScheduler({
                                    "element": this.$el.find("#schedules .liveSyncScheduleContainer").last(),
                                    "defaults": {
                                        enabled: schedule.enabled,
                                        schedule: schedule.schedule,
                                        persisted: schedule.persisted,
                                        misfirePolicy: schedule.misfirePolicy,
                                        liveSyncSeconds: schedule.schedule
                                    },
                                    "onDelete": _.bind(this.removeSchedule, this),
                                    "invokeService": schedule.invokeService,
                                    "source": schedule.invokeContext.source,
                                    "scheduleId": schedule._id
                                });
                                this.addedLiveSyncSchedules.push(schedule.invokeContext.source);
                            }
                        }

                    }, this);

                    if (this.$el.find(".sources option").length === 0) {
                        this.$el.find(".addLiveSync").prop('disabled', true);
                        this.$el.find(".sources").prop('disabled', true);
                    } else {
                        this.$el.find(".addLiveSync").prop('disabled', false);
                        this.$el.find(".sources").prop('disabled', false);
                    }

                }, this));
            }, this));

            if (!this.postActionBlockScript) {
                this.postActionBlockScript = ScriptEditor.generateScriptEditor({
                    "element": this.$el.find(".postActionBlock .script"),
                    "eventName": "",
                    "deleteElement": false
                });
            }
        },

        updateLiveSyncObjects: function() {
            var objectTypes = [],
                tempName = "",
                curName = "",
                sourcePieces = [];

            if (!this.data.editState) {
                curName = this.$el.find("#connectorName").val();

                if (curName.length  > 0) {
                    this.name = curName;
                } else {
                    this.name = "";
                }
            }

            if (this.name) {
                this.$el.find(".nameFieldMessage").hide();

                if (this.userDefinedObjectTypes && _.size(this.userDefinedObjectTypes) > 0) {
                    objectTypes = _.map(this.userDefinedObjectTypes, function (object, key) {
                        return "system/" + this.name + "/" + key;
                    }, this);
                } else {
                    objectTypes = _.map(this.objectTypes, function (object, key) {
                        return "system/" + this.name + "/" + key;
                    }, this);
                }

                this.$el.find(".sources").empty();

                if (objectTypes && _.size(objectTypes) > 0) {

                    // For each schedule on the page
                    _.each(this.addedLiveSyncSchedules, function (source) {
                        // The schedule is not included in the livesync source list
                        if (_.indexOf(objectTypes, source) === -1) {
                            $("#" + source.split("/").join("")).find(".deleteSchedule").click();
                            this.addedLiveSyncSchedules.splice(_.indexOf(this.addLiveSyncScheduler, source), 1);
                        }
                    }, this);


                    // For each possible liveSync
                    _.each(objectTypes, function (objectName) {
                        // The source is not scheduled add it to dropdown
                        if (_.indexOf(this.addedLiveSyncSchedules, objectName) === -1) {
                            this.$el.find(".sources").append("<option value='" + objectName + "'>" + objectName + "</option>");
                            this.$el.find(".addLiveSync").prop('disabled', false);
                            this.$el.find(".sources").prop('disabled', false);
                        }
                    }, this);
                } else {
                    this.$el.find(".addLiveSync").prop('disabled', true);
                    this.$el.find(".sources").prop('disabled', true);
                }
            } else {
                this.$el.find(".addLiveSync").prop('disabled', true);
                this.$el.find(".sources").prop('disabled', true);
                this.$el.find(".nameFieldMessage").show();
            }
        },

        removeSchedule: function (id, name) {
            this.addedLiveSyncSchedules.splice(_.indexOf(this.addLiveSyncScheduler, name), 1);

            this.$el.find(".liveSyncScheduleContainer:not(:has(.schedulerBody))").remove();
            this.$el.find(".sources").append("<option value='"+ name +"'>"+ name +"</option>");
            this.$el.find(".addLiveSync").prop('disabled', false);
            this.$el.find(".sources").prop('disabled', false);

            this.updateLiveSyncObjects();
        },

        retryOptionChanged: function() {
            switch (this.$el.find(".retryOptions").val()) {
                case "0":
                    this.$el.find(".retryBlock").hide();
                    this.$el.find(".maxRetries").val("0");
                    this.$el.find(".postActionBlock").hide();
                    break;
                case "-1":
                    this.$el.find(".retryBlock").hide();
                    this.$el.find(".maxRetries").val("-1");
                    this.$el.find(".postActionBlock").show();
                    break;
                case "*":
                    this.$el.find(".retryBlock").show();
                    this.$el.find(".maxRetries").val("5");
                    this.$el.find(".postActionBlock").show();
                    break;
            }
        },

        postRetryActionChange: function() {
            if (this.$el.find(".postRetryAction").val() === "script") {
                this.$el.find(".postActionBlock .script").show();
            } else {
                this.$el.find(".postActionBlock .script").hide();
            }
        },

        //This function is to find the newest version of a connector and select it if a user provides a range
        versionRangeCheck: function(version) {
            var cleanVersion = null,
                tempVersion,
                tempMinorVersion,
                mainVersion,
                minorVersion;

            //Checks to see if there is a range
            if(version.indexOf("(") !== -1 || version.indexOf(")") !== -1 || version.indexOf("[") !== -1 || version.indexOf("]") !== -1) {
                if(this.data.versionDisplay[0].versions.length === 1) {
                    cleanVersion = this.data.versionDisplay[0].versions[0].bundleVersion;
                } else {
                    _.each(this.data.versionDisplay[0].versions, function (versions) {
                        if (cleanVersion === null) {
                            cleanVersion = versions.bundleVersion;
                        } else {
                            tempVersion = this.findMainVersion(versions.bundleVersion);
                            tempMinorVersion = this.findMinorVersion(versions.bundleVersion);

                            mainVersion = this.findMainVersion(cleanVersion);
                            minorVersion = this.findMinorVersion(cleanVersion);

                            //Parse float is used to convert the returned string version to a number to allow basic comparison of greater / lesser value
                            if (parseFloat(mainVersion) < parseFloat(tempVersion)) {
                                cleanVersion = versions.bundleVersion;
                            } else if (parseFloat(mainVersion) === parseFloat(tempVersion)){
                                if (parseFloat(minorVersion) < parseFloat(tempMinorVersion)) {
                                    cleanVersion = versions.bundleVersion;
                                }
                            }
                        }
                    }, this);
                }

                this.data.rangeFound = true;
                this.data.oldVersion = version;
            } else {
                this.data.rangeFound = false;
                cleanVersion = version;
            }

            return cleanVersion;
        },

        //Find the major version. If a range is used it will select the newest version of a connector template available
        //A bad main version will kill the connector edit process
        findMainVersion: function(version){
            if(version.length > 0) {
                version = version.split(".");
                version = version[0] + "." + version[1];

                return version;
            } else {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorBadMainVersion");
                return "0.0";
            }

        },

        //Finds the minor version.
        //A bad minor version will NOT kill the connector editing process since we primarily rely on major version for everything except for JAR selection
        findMinorVersion: function(version) {
            if(version.length > 0) {
                version = version.split(".");
                version = version[2] + "." + version[3];

                return version;
            } else {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorBadMinorVersion");
                return "0.0";
            }
        },

        loadConnectorTemplate: function() {
            var connectorData,
                connectorTemplate,
                selectedValue = this.$el.find("#connectorType").val().split("_"),
                mainVersion,
                connectorRef;

            connectorData = _.findWhere(this.data.connectors, {"connectorName": selectedValue[0], "bundleVersion": selectedValue[1]});

            //If for some reason no connector data
            if(_.isUndefined(connectorData)) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorsNotAvailable");
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorView});
            } else {
                mainVersion = this.findMainVersion(connectorData.bundleVersion);

                //Checking to ensure we don't reload the page if a minor version is changed
                if(this.data.currentMainVersion === null || (parseFloat(this.data.currentMainVersion) !== parseFloat(mainVersion)) || this.data.connectorType !==  selectedValue[0]) {
                    this.data.connectorType = selectedValue[0];
                    this.data.systemType = selectedValue[2];
                    this.data.currentMainVersion = this.findMainVersion(connectorData.bundleVersion);

                    connectorTemplate = connectorData.connectorName +"_" +mainVersion;

                    connectorRef = {
                        connectorRef: connectorData
                    };

                    ConnectorDelegate.detailsConnector(connectorRef).then(_.bind(function(connectorDefaults){
                        this.connectorTypeRef = ConnectorRegistry.getConnectorModule(connectorTemplate);

                        this.connectorTypeRef.render({"connectorType": connectorTemplate,
                                "animate": true,
                                "connectorDefaults": connectorDefaults,
                                "oAuthReturned" : false,
                                "editState" : this.data.editState,
                                "systemType" : this.data.systemType },
                            _.bind(function(){
                                if(this.connectorTypeRef.oAuthConnector) {
                                    this.oAuthConnector = true;
                                } else {
                                    this.oAuthConnector = false;
                                }

                                this.setSubmitFlow();

                                validatorsManager.validateAllFields(this.$el);
                            }, this));
                    }, this));
                } else {
                    //Set the bundle version on a minor version change so it saves
                    this.connectorTypeRef.data.connectorDefaults.connectorRef.bundleVersion = selectedValue[1];
                }
            }
        },

        getProvisioner: function() {
            var connectorData,
                connDetails = this.connectorTypeRef.data.connectorDefaults,
                mergedResult = {};

            connectorData = form2js('connectorForm', '.', true);

            if (connectorData.enabled === "true") {
                connectorData.enabled = true;
            } else {
                connectorData.enabled = false;
            }

            delete connectorData.connectorType;

            connectorData.syncFailureHandler.maxRetries = parseInt(connectorData.syncFailureHandler.maxRetries, 10);

            if (connectorData.syncFailureHandler.postRetryAction === "script") {
                connectorData.syncFailureHandler.postRetryAction = {"script": this.postActionBlockScript.getScriptHook().script};
            }

            connectorData.configurationProperties.readSchema = false;
            connectorData.objectTypes = {};

            $.extend(true, mergedResult, connDetails, connectorData);

            mergedResult.objectTypes = this.userDefinedObjectTypes || this.objectTypes;

            return mergedResult;
        },

        formSubmit: function(event) {
            event.preventDefault();
            var mergedResult = this.getProvisioner();

            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorSaved");

            if(this.data.editState) {
                ConfigDelegate.updateEntity(this.data.systemType + "/" + mergedResult.name, mergedResult).then(_.bind(function () {
                    _.delay(function () {
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.resourcesView});
                    }, 1500);
                }, this));
            } else {
                ConfigDelegate.createEntity(this.data.systemType + "/" + mergedResult.name, mergedResult).then(_.bind(function () {
                    _.delay(function() {
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.resourcesView});
                    }, 1500);
                }, this));
            }
        },

        oAuthFormSubmit: function(event) {
            event.preventDefault();
            var mergedResult = this.getProvisioner();

            this.connectorTypeRef.submitOAuth(mergedResult);
        },

        validate: function(event) {
            event.preventDefault();

            var mergedResult = this.getProvisioner();

            ConnectorDelegate.testConnector(mergedResult).then(_.bind(function (testResult) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestPass");

                    if(this.oAuthReturned) {
                        this.connectorTypeRef.getToken(mergedResult, this.oAuthCode).then(
                            _.bind(function(tokenResult){
                                this.$el.find("#clientRefreshToken").val(tokenResult.refresh_token);

                                if(!this.data.editState) {
                                    this.objectTypes = testResult.objectTypes;
                                    this.updateLiveSyncObjects();
                                }

                                this.userDefinedObjectType = null;
                                this.$el.find("#addEditObjectType").prop('disabled', false);
                                this.$el.find("#addEditConnector").prop('disabled', false);

                                this.oAuthReturned = false;
                            }, this));
                    } else {
                        if(!this.data.editState) {
                            this.objectTypes = testResult.objectTypes;
                            this.updateLiveSyncObjects();
                        }

                        this.userDefinedObjectType = null;
                        this.$el.find("#addEditObjectType").prop('disabled', false);
                        this.$el.find("#addEditConnector").prop('disabled', false);
                    }

                }, this), _.bind(function(result) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestFailed");

                    if(!this.data.editState) {
                        this.objectTypes = {};
                    }

                    this.$el.find("#addEditObjectType").prop('disabled', true);
                    this.$el.find("#addEditConnector").prop('disabled', true);
                    this.showError(result);
                }, this)
            );
        },

        disableButtons: function() {
            this.$el.find("#addEditObjectType").prop('disabled', true);
            this.$el.find("#addEditConnector").prop('disabled', true);
        },

        sectionHideShow: function(event) {
            var clickedEle = event.target;

            if($(clickedEle).not("legend")){
                clickedEle = $(clickedEle).closest("legend");
            }

            $(clickedEle).find("i").toggleClass("fa-plus-square-o");
            $(clickedEle).find("i").toggleClass("fa-minus-square-o");

            $(clickedEle).parent().find(".group-body").slideToggle("slow");
        },

        showError: function(msg) {
            var error = JSON.parse(msg.responseText);

            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestFailed");

            this.$el.find("#connectorErrorMessage .alert-message .message").html(this.parseErrorMessage(error.message));
            this.$el.find("#connectorErrorMessage").show();
        },

        closeError : function() {
            this.$el.find("#connectorErrorMessage").hide();
        },

        parseErrorMessage: function(err) {
            var transformErrors = [
                {
                    searchString: 'UnknownHostException',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.unknownHost'
                },
                {
                    searchString: 'port out of range',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.portOutOfRange'
                },
                {
                    searchString: 'Connection refused',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.connectionRefused'
                },
                {
                    searchString: 'Operation timed out',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.operationTimedOut'
                },
                {
                    searchString: 'SSLHandshakeException',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.sslHandshakeException'
                },
                {
                    searchString: 'data 52e',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.invalidCredentials'
                },
                {
                    searchString: 'NamingException',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.invalidCredentials'
                },
                {
                    searchString: 'Bad Base Context(s)',
                    replaceAll: false,
                    replaceString: 'templates.connector.errorMessages.badBaseContext'
                },
                {
                    searchString: 'java.lang.String to int',
                    replaceAll: true,
                    replaceString: 'templates.connector.errorMessages.portOutOfRange'
                }
            ];

            _.each(transformErrors, function(e){
                if(err.indexOf(e.searchString) > -1){
                    if(e.replaceAll) {
                        err = $.t(e.replaceString);
                    } else {
                        err = err.replace(e.searchString,$.t(e.replaceString));
                    }
                }
            });

            return err;
        },

        addEditObjectTypes: function() {
            objectTypesDialog.render(this.userDefinedObjectTypes || this.objectTypes, this.getProvisioner(), _.bind(this.saveObjectTypes, this));
        },

        saveObjectTypes: function(newObjectTypes) {
            this.userDefinedObjectTypes = newObjectTypes;
            this.updateLiveSyncObjects();
        }
    });

    return new AddEditConnectorView();
});
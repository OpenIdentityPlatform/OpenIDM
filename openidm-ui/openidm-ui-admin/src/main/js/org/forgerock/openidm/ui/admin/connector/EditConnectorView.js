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

define("org/forgerock/openidm/ui/admin/connector/EditConnectorView", [
    "jquery",
    "underscore",
    "form2js",
    "org/forgerock/openidm/ui/admin/connector/AbstractConnectorView",
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
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/commons/ui/common/util/UIUtils"

], function($, _, form2js,
            AbstractConnectorView,
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
            InlineScriptEditor,
            uiUtils) {

    var AddEditConnectorView = AbstractConnectorView.extend({
        template: "templates/admin/connector/EditConnectorTemplate.html",
        events: {
            "change #connectorType" : "loadConnectorTemplate",
            "onValidate": "onValidate",
            "click #updateObjectTypes" : "objectTypeFormSubmit",
            "click #updateSync" : "syncFormSubmit",
            "click .addLiveSync" : "addLiveSync",
            "customValidate": "customValidate",
            "click .edit-objectType" : "editObjectType",
            "click .delete-objectType" : "deleteObjectType",
            "click #addObjectType" : "addObjectType",
            "click #deleteResource" : "deleteResource",
            "change #selectObjectConfig" : "changeObjectTypeConfig",
            "change .retryOptions": "retryOptionChanged",
            "change .maxRetries" : "pendingSyncChangesCheck",
            "change .postRetryAction": "postRetryActionChange",
            "change #connectorForm :input" : "connectorChangesCheck",
            "keypress #connectorForm :input" : "connectorFlowCheck",
            "paste #connectorForm :input" : "connectorFlowCheck"
        },
        data: {

        },
        objectTypeConfigs: {
            "org.forgerock.openicf.connectors.ldap-connector" : [
                {
                    "displayName" : "Saved Configuration",
                    "fileName" : "savedConfig",
                    "type": "ldap"
                },
                {
                    "displayName" : "AD LDAP Configuration",
                    "fileName" : "provisioner.openicf-adldap",
                    "type": "ldap"
                },
                {
                    "displayName" : "ADLDS LDAP Configuration",
                    "fileName" : "provisioner.openicf-adldsldap",
                    "type": "ldap"
                },
                {
                    "displayName" : "DJ LDAP Configuration",
                    "fileName" : "provisioner.openicf-ldap",
                    "type": "ldap"
                },
                {
                    "displayName" : "Full LDAP Configuration",
                    "fileName" : "fullConfig",
                    "type": "ldap"
                },
                {
                    "displayName" : "IBM LDAP Configuration",
                    "fileName" : "provisioner.openicf-racfldap",
                    "type": "ldap"
                }
            ]
        },
        connectorTypeRef: null,
        connectorList: null,
        oAuthConnector: false,

        render: function(args, callback) {
            this.data = {};
            this.data.docHelpUrl = constants.DOC_URL;
            this.data.versionDisplay = {};
            this.data.currentMainVersion = null;
            this.data.objectTypes = null;
            this.oAuthConnector = false;
            this.connectorTypeRef = null;
            this.connectorList = null;
            this.postActionBlockScript = null;
            this.name = null;
            this.addedLiveSyncSchedules = [];
            this.connectorTypeRef = null;
            this.userDefinedObjectTypes = null;

            //Get available list of connectors
            $.when(ConnectorDelegate.availableConnectors(), connectorUtils.getIconList()).then(_.bind(function(connectors, iconList){
                connectors = connectors[0];
                this.data.connectors = connectors.connectorRef;

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

                var splitDetails = args[0].match(/(.*?)_(.*)/).splice(1),
                    urlArgs = router.convertCurrentUrlToJSON();

                this.data.editState = true;
                this.data.systemType = splitDetails[0];
                this.data.connectorId = this.name = splitDetails[1];

                //Get current connector details
                ConfigDelegate.readEntity(this.data.systemType +"/" + this.data.connectorId).then(_.bind(function(data){
                    var tempVersion;

                    this.data.connectorIcon = connectorUtils.getIcon(data.connectorRef.connectorName, iconList);
                    this.currentObjectTypeLoaded = "savedConfig";
                    this.data.objectTypeDefaultConfigs = this.objectTypeConfigs[data.connectorRef.bundleName];

                    this.data.connectorName = data.name;
                    this.data.connectorTypeName = data.connectorRef.connectorName;
                    this.data.enabled = data.enabled;

                    //Store in memory version of connector details. This is to ensure we can move around tabs and keep the correct data state.
                    this.connectorDetails = data;

                    //Build a version object
                    _.each(this.data.versionDisplay, function (group) {
                        group.versions = _.map(group.versions, function (v) {
                            v.selected = v.connectorName === this.data.connectorTypeName &&
                                v.bundleVersion === data.connectorRef.bundleVersion &&
                                v.systemType === this.data.systemType;
                            return v;
                        }, this);
                    }, this);

                    this.previousObjectType = data.objectTypes;
                    this.data.objectTypes = data.objectTypes;

                    //Filter down to the current edited connector Type
                    this.data.versionDisplay = _.filter(this.data.versionDisplay, function(connector){
                        return  data.connectorRef.connectorName  === connector.versions[0].connectorName;
                    }, this);

                    this.data.fullversion = this.versionRangeCheck(data.connectorRef.bundleVersion);
                    data.connectorRef.bundleVersion = this.data.fullversion;
                    this.data.currentMainVersion = this.findMainVersion(data.connectorRef.bundleVersion);

                    //Filter the connector types down to the current major version
                    this.data.versionDisplay[0].versions = _.filter(this.data.versionDisplay[0].versions, function(version){
                        tempVersion = this.findMainVersion(version.bundleVersion);

                        return parseFloat(this.data.currentMainVersion) === parseFloat(tempVersion);
                    }, this);

                    //Get the connector type for top header display
                    this.data.displayConnectorType = this.data.versionDisplay[0].versions[0].displayName;

                    //OAuth branch
                    if (urlArgs.params && urlArgs.params.code) {
                        this.oAuthCode = urlArgs.params.code;

                        ConnectorRegistry.getConnectorModule(this.data.connectorTypeName + "_" +this.data.currentMainVersion).then(_.bind(function (connectorTypeRef) {
                            this.connectorTypeRef = connectorTypeRef;
                            this.connectorTypeRef.getToken(data, this.oAuthCode).then(_.bind(function(tokenDetails) {
                                this.connectorTypeRef.setToken(tokenDetails, data, this.data.systemType + "/" +this.data.connectorId, urlArgs);
                            }, this));
                        }, this));
                    } else {
                        this.parentRender(_.bind(function () {

                            //Sync settings
                            if (data.syncFailureHandler && _.has(data.syncFailureHandler, "maxRetries")) {
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

                                this.$el.find("#connectorWarningMessage .message .sync-pending").remove();
                                this.warningMessageCheck();
                            }

                            if (data.syncFailureHandler && _.has(data.syncFailureHandler.postRetryAction, "script")) {
                                this.$el.find(".postRetryAction").val("script");
                                this.postActionBlockScript = InlineScriptEditor.generateScriptEditor({
                                    "element": this.$el.find(".postActionBlock .script"),
                                    "eventName": "",
                                    "deleteElement": false,
                                    "scriptData": data.syncFailureHandler.postRetryAction.script
                                });
                                this.$el.find(".postActionBlock .script").show();
                            } else if (data.syncFailureHandler) {
                                this.$el.find(".postRetryAction").val(data.syncFailureHandler.postRetryAction);
                            }

                            //set validation for the connector form
                            validatorsManager.bindValidators(this.$el.find("#connectorForm"));

                            //alert if a connector is currently set to a bundle version that doesn't exist
                            if (this.data.rangeFound) {
                                this.$el.find("#connectorWarningMessage .message").append('<div class="pending-changes connector-version">' +$.t("config.messages.ConnectorMessages.connectorVersionChange", {"range": this.data.oldVersion, "version": data.connectorRef.bundleVersion}) +'</div>');
                                this.$el.find("#connectorWarningMessage").show();
                            }

                            //Get connector template
                            ConnectorRegistry.getConnectorModule(this.data.connectorTypeName + "_" + this.data.currentMainVersion).then(_.bind(function (connectorTypeRef) {
                                this.connectorTypeRef = connectorTypeRef;

                                //Determine if the template is OAuth
                                if (this.connectorTypeRef.oAuthConnector) {
                                    this.oAuthConnector = true;
                                } else {
                                    this.oAuthConnector = false;
                                }

                                //Render the connector template / details
                                this.connectorTypeRef.render({"connectorType": this.data.connectorTypeName + "_" + this.data.currentMainVersion,
                                        "animate": true,
                                        "connectorDefaults": data,
                                        "editState": this.data.editState,
                                        "systemType": this.data.systemType },
                                    _.bind(function () {
                                        validatorsManager.validateAllFields(this.$el);

                                        //Set the current newest version incase there is a range
                                        this.connectorTypeRef.data.connectorDefaults.connectorRef.bundleVersion = data.connectorRef.bundleVersion;

                                        this.setSubmitFlow();

                                        if (callback) {
                                            callback();
                                        }
                                    }, this));

                                this.setupLiveSync();
                            }, this));

                        }, this));
                    }
                }, this));
            }, this));
        },

        connectorFlowCheck: function() {
            if(this.oAuthConnector) {
                this.setSubmitFlow();
            }
        },

        deleteResource: function(event) {
            event.preventDefault();

            uiUtils.jqConfirm($.t("templates.connector.connectorDelete"), _.bind(function(){
                ConfigDelegate.deleteEntity(this.data.systemType +"/" +this.data.connectorId).then(function(){
                        ConnectorDelegate.deleteCurrentConnectorsCache();

                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorListView});

                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorSuccess");
                    },
                    function(){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteConnectorFail");
                    });
            } , this));
        },

        //Saves the connector tab
        connectorFormSubmit: function(event) {
            event.preventDefault();

            var mergedResult = this.getProvisioner();

            //Checks for connector specific save function to do any additional changes to data
            if(this.connectorTypeRef.connectorSaved) {
                mergedResult = this.connectorTypeRef.connectorSaved(mergedResult);
            }

            mergedResult.configurationProperties.readSchema = false;

            ConnectorDelegate.testConnector(mergedResult).then(_.bind(function () {
                ConnectorDelegate.deleteCurrentConnectorsCache();

                ConfigDelegate.updateEntity(this.data.systemType + "/" + this.data.connectorId, mergedResult).then(_.bind(function () {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorSaved");

                    this.connectorDetails = mergedResult;

                    this.$el.find("#connectorWarningMessage .message .connector-version").remove();
                    this.$el.find("#connectorWarningMessage .message .connector-pending").remove();
                    this.warningMessageCheck();
                    this.$el.find("#connectorErrorMessage").hide();
                }, this));
            }, this), _.bind(function(result) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestFailed");

                this.showError(result);
            }, this));
        },

        //Saves the sync tab
        syncFormSubmit: function() {
            var syncData = form2js('syncForm', '.', true);

            this.connectorDetails.syncFailureHandler.maxRetries = parseInt(syncData.syncFailureHandler.maxRetries, 10);
            this.connectorDetails.syncFailureHandler.postRetryAction = syncData.syncFailureHandler.postRetryAction;


            if (this.connectorDetails.syncFailureHandler.postRetryAction === "script") {
                this.connectorDetails.syncFailureHandler.postRetryAction = {"script": this.postActionBlockScript.generateScript()};
            }

            ConfigDelegate.updateEntity(this.data.systemType + "/" + this.data.connectorId, this.connectorDetails).then(_.bind(function () {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "liveSyncSaved");

                this.$el.find("#connectorWarningMessage .message .sync-pending").remove();
                this.warningMessageCheck();
            }, this));
        },

        //Saves the object type tab
        objectTypeFormSubmit: function() {
            if(!this.userDefinedObjectTypes) {
                this.userDefinedObjectTypes = this.data.objectTypes;
            }

            this.connectorDetails.objectTypes = this.userDefinedObjectTypes;

            ConfigDelegate.updateEntity(this.data.systemType + "/" + this.data.connectorId, this.connectorDetails).then(_.bind(function () {
                this.previousObjectType = this.userDefinedObjectTypes;
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeSaved");

                this.$el.find("#connectorWarningMessage .message .objecttype-pending").remove();

                this.warningMessageCheck();

                this.updateActionDropdown(this.previousObjectType);
            }, this));
        },

        //will hide warning message if no messages left
        warningMessageCheck: function() {
            if(this.$el.find("#connectorWarningMessage .message .pending-changes").length === 0) {
                this.$el.find("#connectorWarningMessage").hide();
            }
        },

        addLiveSync: function(schedule) {
            var source = this.$el.find(".sources option:selected");

            if (source.length > 0) {
                this.$el.find("#schedules").append("<div class='liveSyncScheduleContainer'></div>");

                Scheduler.generateScheduler({
                    "element": this.$el.find("#schedules .liveSyncScheduleContainer").last(),
                    "defaults": {
                        "enabled" : true,
                        "persisted" : true
                    },
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
                this.postActionBlockScript = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find(".postActionBlock .script"),
                    "eventName": "",
                    "deleteElement": false
                });
            }
        },

        updateLiveSyncObjects: function() {
            var objectTypes = [];

            if (this.name) {
                this.$el.find(".nameFieldMessage").hide();

                if (this.userDefinedObjectTypes && _.size(this.userDefinedObjectTypes) > 0) {
                    objectTypes = _.map(this.userDefinedObjectTypes, function (object, key) {
                        return "system/" + this.name + "/" + key;
                    }, this);
                } else {
                    objectTypes = _.map(this.data.objectTypes, function (object, key) {
                        return "system/" + this.name + "/" + key;
                    }, this);
                }

                this.$el.find(".sources").empty();

                if (objectTypes && _.size(objectTypes) > 0) {
                    this.$el.find(".objectTypeFieldMessage").hide();

                    // For each schedule on the page
                    _.each(this.addedLiveSyncSchedules, function (source) {
                        // The schedule is not included in the livesync source list
                        if (_.indexOf(objectTypes, source) === -1) {
                            this.$el.find("#" + source.split("/").join("")).find(".deleteSchedule").click();
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
                    this.$el.find(".objectTypeFieldMessage").show();
                    this.$el.find(".addLiveSync").prop('disabled', true);
                    this.$el.find(".sources").prop('disabled', true);
                }
            } else {
                this.$el.find(".objectTypeFieldMessage").hide();
                this.$el.find(".addLiveSync").prop('disabled', true);
                this.$el.find(".sources").prop('disabled', true);
                this.$el.find(".nameFieldMessage").show();
            }
        },

        removeSchedule: function (id, name, element) {
            this.addedLiveSyncSchedules.splice(_.indexOf(this.addLiveSyncScheduler, name), 1);

            element.remove();
            this.$el.find(".sources").append("<option value='"+ name +"'>"+ name +"</option>");
            this.$el.find(".addLiveSync").prop('disabled', false);
            this.$el.find(".sources").prop('disabled', false);

            this.updateLiveSyncObjects();
        },

        retryOptionChanged: function() {
            switch (this.$el.find(".retryOptions").val()) {
                case "0":
                    this.$el.find(".maxRetries").val("0");
                    this.$el.find(".postActionBlock").hide();
                    break;
                case "-1":
                    this.$el.find(".maxRetries").val("-1");
                    this.$el.find(".postActionBlock").show();
                    break;
                case "*":
                    this.$el.find(".maxRetries").val("5");
                    this.$el.find(".postActionBlock").show();
                    break;
            }

            this.pendingSyncChangesCheck();
        },

        postRetryActionChange: function() {
            if (this.$el.find(".postRetryAction").val() === "script") {
                this.$el.find(".postActionBlock .script").show();
            } else {
                this.$el.find(".postActionBlock .script").hide();
            }

            this.pendingSyncChangesCheck();
        },

        pendingSyncChangesCheck: function() {
            if(this.$el.find("#connectorWarningMessage .message .sync-pending").length === 0) {
                this.$el.find("#connectorWarningMessage .message").append('<div class="pending-changes sync-pending">' +$.t("templates.connector.pendingSync") +'</div>');
                this.$el.find("#connectorWarningMessage").show();
            }
        },

        connectorChangesCheck: function() {
            if(this.$el.find("#connectorWarningMessage .message .connector-pending").length === 0) {
                this.$el.find("#connectorWarningMessage .message").append('<div class="pending-changes connector-pending">' +$.t("templates.connector.pendingConnectorChanges") +'</div>');
                this.$el.find("#connectorWarningMessage").show();
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

        //Returns the current provisioner based on a merged copy of the connector defaults and what was set in the template by the user
        getProvisioner: function() {
            var connectorData,
                connDetails = this.connectorTypeRef.data.connectorDefaults,
                mergedResult = {},
                tempArrayObject,
                tempKeys,
                arrayComponents = $(".connector-array-component");

            connectorData = form2js('connectorForm', '.', true);

            if(this.connectorTypeRef.getGenericState()) {
                delete connectorData.root;
                connectorData.configurationProperties = this.connectorTypeRef.getGenericConnector();
            }

            if (connectorData.enabled === "true") {
                connectorData.enabled = true;
            } else {
                connectorData.enabled = false;
            }

            delete connectorData.connectorType;

            connectorData.configurationProperties.readSchema = false;
            connectorData.objectTypes = {};

            $.extend(true, mergedResult, connDetails, connectorData);

            //Added logic to ensure array parts correctly add and delete what is set
            _.each(arrayComponents, function(component){
                tempArrayObject = form2js($(component).prop("id"), ".", true);
                tempKeys = _.keys(tempArrayObject.configurationProperties);

                if(tempKeys.length) {
                    mergedResult.configurationProperties[tempKeys[0]] = tempArrayObject.configurationProperties[tempKeys[0]];
                }

            }, this);

            mergedResult.objectTypes = this.userDefinedObjectTypes || this.data.objectTypes;

            return mergedResult;
        },

        //Renders the object type table
        renderObjectTypes: function(newObjectTypes) {
            this.userDefinedObjectTypes = newObjectTypes;

            this.$el.find("#objectTypesTab table tbody").empty();

            _.each(this.userDefinedObjectTypes, _.bind(function(object, key){
                this.$el.find("#objectTypesTab table tbody").append(
                        "<tr data-objecttype='" +key +"'><td>"+key +"</td>"
                        + "<td><button class='btn btn-link edit-objectType'><i class='fa fa-pencil'></i></button>"
                        + "<button class='btn btn-link delete-objectType'><i class='fa fa-times'></i></button></td></tr>");
            }, this));


            this.updateLiveSyncObjects();

            if(this.$el.find("#connectorWarningMessage .message .objecttype-pending").length === 0) {
                this.$el.find("#connectorWarningMessage .message").append('<div class="pending-changes objecttype-pending">' +$.t("templates.connector.pendingObjectTypes") +'</div>');
                this.$el.find("#connectorWarningMessage").show();
            }
        },

        //Since we are using tab panels we need custom validate to correctly enable / disable the connector submit
        customValidate: function() {
            if(validatorsManager.formValidated(this.$el.find("#connectorForm"))) {
                this.$el.find("#submitConnector").attr("disabled", false);
            } else {
                this.$el.find("#submitConnector").attr("disabled", true);
            }
        },

        //When clicking the pencil for an object type
        editObjectType: function(event) {
            var objectTypeName = $(event.target).parents("tr").attr("data-objectType");

            objectTypesDialog.render(this.userDefinedObjectTypes || this.data.objectTypes, objectTypeName, this.getProvisioner(), _.bind(this.renderObjectTypes, this));
        },

        //When clicking the delete icon for an object type
        deleteObjectType: function(event){
            var objectTypeName = $(event.target).parents("tr").attr("data-objectType");

            if(!this.userDefinedObjectTypes) {
                this.userDefinedObjectTypes = this.data.objectTypes;
            }

            delete this.userDefinedObjectTypes[objectTypeName];

            this.renderObjectTypes(this.userDefinedObjectTypes);
        },

        //After saving or deleting an object type re-renders the action list so it is in sync with the available data pieces
        updateActionDropdown: function(objectTypes) {
            this.$el.find(".dropdown-menu .data-link").remove();

            _.each(objectTypes, function(object, key){
                this.$el.find(".dropdown-menu .divider").before(
                        '<li class="data-link">'
                        +'<a href="#resource/system/' +this.data.connectorName +'/' +key+'/list/"><i class="fa fa-database"> Data ('+key  +')</i></a>'
                        +'</li>');
            }, this);
        },

        //When adding a new object type
        addObjectType: function() {
            objectTypesDialog.render(this.userDefinedObjectTypes || this.data.objectTypes, null, this.getProvisioner(), _.bind(this.renderObjectTypes, this));
        },

        //Used when an object type template selector is available.
        changeObjectTypeConfig: function(event) {
            var value = $(event.target).val(),
                type = $(event.target).attr("data-type");

            if(!this.userDefinedObjectTypes) {
                this.userDefinedObjectTypes = this.data.objectTypes;
            }

            $(event.target).val(this.currentObjectTypeLoaded);

            uiUtils.jqConfirm($.t('templates.connector.objectTypes.changeConfiguration'), _.bind(function(){
                if(value === "fullConfig") {
                    this.connectorDetails.configurationProperties.readSchema = true;

                    ConnectorDelegate.testConnector(this.connectorDetails).then(_.bind(function (result) {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeLoaded");

                            this.renderObjectTypes(result.objectTypes);
                        }, this), _.bind(function () {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeFailedToLoad");
                        }, this)
                    );
                } else if(value === "savedConfig") {
                    if(this.previousObjectType) {
                        this.renderObjectTypes(this.previousObjectType);
                    }
                } else {
                    ConnectorDelegate.connectorDefault(value, type).then(_.bind(function (result) {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeLoaded");

                            this.renderObjectTypes(result.objectTypes);
                        }, this), _.bind(function () {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeFailedToLoad");
                        }, this)
                    );
                }

                this.currentObjectTypeLoaded = value;
                $(event.target).val(this.currentObjectTypeLoaded);

            }, this), "330px");
        }
    });

    return new AddEditConnectorView();
});

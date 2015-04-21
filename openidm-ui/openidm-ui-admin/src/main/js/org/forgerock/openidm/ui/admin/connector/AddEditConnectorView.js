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
    "org/forgerock/openidm/ui/admin/util/ScriptEditor",
    "org/forgerock/commons/ui/common/util/UIUtils"

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
            ScriptEditor,
            uiUtils) {

    var AddEditConnectorView = AdminAbstractView.extend({
        template: "templates/admin/connector/AddEditConnectorTemplate.html",
        events: {
            "change #connectorType" : "loadConnectorTemplate",
            "onValidate": "onValidate",
            "click #connectorForm fieldset legend" : "sectionHideShow",
            "click #addEditObjectType": "addEditObjectTypes",
            "click #validateConnector": "validate",
            "change :input" : "setConnectorState",
            "keyup :input" : "setConnectorState",
            "paste :input" : "setConnectorState",
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

        render: function(args, callback) {
            this.data = {};
            this.data.docHelpUrl = constants.DOC_URL;
            this.data.versionDisplay = {};
            this.data.currentMainVersion = null;
            this.oAuthConnector = false;
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

                if(args.length === 0 || args[0] === null) {

                    this.data.versionDisplay = _.filter(this.data.versionDisplay, function(version){
                        return version.versions[0].bundleName !== "org.forgerock.openicf.connectors.groovy-connector";
                    }, this);

                    this.data.editState = false;
                    this.data.connectorName = "";
                    this.data.addEditTitle = $.t("templates.connector.addConnectorTitle");
                    this.data.addEditSubmitTitle = $.t("templates.connector.addButtonTitle");
                    this.data.addEditObjectTypeTitle = $.t("templates.connector.addObjectTypeTitle");
                    this.data.addEditSubmitTitle = $.t("common.form.add");

                    this.parentRender(_.bind(function () {
                        validatorsManager.bindValidators(this.$el);

                        this.loadConnectorTemplate(callback);

                        this.setupLiveSync();
                    }, this));
                } else {
                    var splitDetails = args[0].match(/(.*?)_(.*)/).splice(1),
                        urlArgs = uiUtils.convertCurrentUrlToJSON();

                    this.data.editState = true;
                    this.data.systemType = splitDetails[0];
                    this.data.connectorId = this.name = splitDetails[1];

                    // FIXME support multiple provisioners based on systemType
                    ConfigDelegate.readEntity(this.data.systemType +"/" + this.data.connectorId).then(_.bind(function(data){
                        var tempVersion,
                            urlName;

                        this.data.connectorName = data.name;
                        this.data.connectorTypeName = data.connectorRef.connectorName;
                        this.data.enabled = data.enabled;
                        this.data.addEditTitle = $.t("templates.connector.editTitle");
                        this.data.addEditSubmitTitle = $.t("templates.connector.updateButtonTitle");
                        this.data.addEditObjectTypeTitle = $.t("templates.connector.editObjectTypeTitle");
                        this.data.addEditSubmitTitle = $.t("common.form.update");

                        data.connectorRef.displayName = $.t("templates.connector." +connectorUtils.cleanConnectorName(this.data.connectorTypeName));

                        _.each(this.data.versionDisplay, function (group) {
                            group.versions = _.map(group.versions, function (v) {
                                v.selected = v.connectorName === this.data.connectorTypeName &&
                                    v.bundleVersion === data.connectorRef.bundleVersion &&
                                    v.systemType === this.data.systemType;
                                return v;
                            }, this);
                        }, this);


                        this.objectTypes = data.objectTypes;

                        //Filter down to the current edited connector Type
                        this.data.versionDisplay = _.filter(this.data.versionDisplay, function(connector){
                            return  data.connectorRef.displayName  === connector.groupName;
                        }, this);

                        data.connectorRef.bundleVersion = this.versionRangeCheck(data.connectorRef.bundleVersion);
                        this.data.currentMainVersion = this.findMainVersion(data.connectorRef.bundleVersion);

                        //Filter the connector types down to the current major version
                        this.data.versionDisplay[0].versions = _.filter(this.data.versionDisplay[0].versions, function(version){
                            tempVersion = this.findMainVersion(version.bundleVersion);

                            return parseFloat(this.data.currentMainVersion) === parseFloat(tempVersion);
                        }, this);

                        if (urlArgs.params && urlArgs.params.code) {
                            this.oAuthCode = urlArgs.params.code;

                            this.connectorTypeRef = ConnectorRegistry.getConnectorModule(this.data.connectorTypeName + "_" +this.data.currentMainVersion);
                            this.connectorTypeRef.getToken(data, this.oAuthCode).then(_.bind(function(tokenDetails) {
                                this.connectorTypeRef.setToken(tokenDetails, data, this.data.systemType + "/" +this.data.connectorId, urlArgs);
                            }, this));
                        } else {
                            this.parentRender(_.bind(function () {
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
                                }

                                if (data.syncFailureHandler && _.has(data.syncFailureHandler.postRetryAction, "script")) {
                                    this.$el.find(".postRetryAction").val("script");
                                    this.postActionBlockScript = ScriptEditor.generateScriptEditor({
                                        "element": this.$el.find(".postActionBlock .script"),
                                        "eventName": "",
                                        "deleteElement": false,
                                        "scriptData": data.syncFailureHandler.postRetryAction.script
                                    });
                                    this.$el.find(".postActionBlock .script").show();
                                } else if (data.syncFailureHandler) {
                                    this.$el.find(".postRetryAction").val(data.syncFailureHandler.postRetryAction);
                                }

                                validatorsManager.bindValidators(this.$el);

                                if (this.data.rangeFound) {
                                    this.$el.find("#connectorWarningMessage .message").html($.t("config.messages.ConnectorMessages.connectorVersionChange", {"range": this.data.oldVersion, "version": data.connectorRef.bundleVersion}));
                                    this.$el.find("#connectorWarningMessage").show();
                                }

                                this.connectorTypeRef = ConnectorRegistry.getConnectorModule(this.data.connectorTypeName + "_" + this.data.currentMainVersion);

                                if (this.connectorTypeRef.oAuthConnector) {
                                    this.oAuthConnector = true;
                                } else {
                                    this.oAuthConnector = false;
                                    this.setConnectorState();
                                }

                                this.connectorTypeRef.render({"connectorType": this.data.connectorTypeName + "_" + this.data.currentMainVersion,
                                        "animate": true,
                                        "connectorDefaults": data,
                                        "editState": this.data.editState,
                                        "systemType": this.data.systemType },
                                    _.bind(function () {
                                        validatorsManager.validateAllFields(this.$el);
                                        this.setConnectorPage();
                                        this.setSubmitFlow();

                                        //Set the current newest version incase there is a range
                                        this.connectorTypeRef.data.connectorDefaults.connectorRef.bundleVersion = data.connectorRef.bundleVersion;


                                        if (callback) {
                                            callback();
                                        }
                                    }, this));

                                this.setupLiveSync();
                            }, this));
                        }
                    }, this));
                }
            }, this));
        },

        setConnectorPage: function() {
            if(this.oAuthConnector) {
                this.$el.find("#validateConnector").prop("type", "button");
                this.$el.find("#addEditConnector").prop("type", "submit");
                this.$el.find("#validateConnector").hide();
                this.$el.find("#addEditObjectType").hide();

                if(!this.data.editState) {
                    this.$el.find("#connectorEnabled").val("false");
                    this.$el.find("#connectorEnabled").parents(".group-field-block").hide();
                }

            } else {
                this.$el.find("#validateConnector").prop("type", "submit");
                this.$el.find("#addEditConnector").prop("type", "button");
                this.$el.find("#validateConnector").show();
                this.$el.find("#addEditObjectType").show();
                this.$el.find("#connectorEnabled").parents(".group-field-block").show();

                if (!this.data.editState) {
                    this.$el.find("#connectorEnabled").val("true");
                }
            }
        },

        setSubmitFlow: function() {
            var connectorSpecificCheck = false;

            if(this.connectorTypeRef.connectorSpecificValidation) {
                connectorSpecificCheck = this.connectorTypeRef.connectorSpecificValidation();
            }

            this.$el.find("#addEditConnector").unbind("click");

            if(this.oAuthConnector) {
                if(this.connectorTypeRef.data.connectorDefaults.configurationProperties.clientId !== this.$el.find("#clientId").val() ||
                    this.$el.find("#secret").length === 0 ||
                    this.$el.find("#secret").val().length > 0 ||
                    this.connectorTypeRef.data.connectorDefaults.configurationProperties.refreshToken === null ||
                    connectorSpecificCheck) {

                    this.$el.find("#addEditConnector").bind("click", _.bind(this.oAuthFormSubmit, this));
                } else {
                    this.$el.find("#addEditConnector").bind("click", _.bind(this.formSubmit, this));
                }
            } else {
                this.$el.find("#addEditConnector").bind("click", _.bind(this.formSubmit, this));
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
                this.postActionBlockScript = ScriptEditor.generateScriptEditor({
                    "element": this.$el.find(".postActionBlock .script"),
                    "eventName": "",
                    "deleteElement": false
                });
            }
        },

        updateLiveSyncObjects: function() {
            var objectTypes = [],
                curName = "";

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

        loadConnectorTemplate: function(callback) {
            var connectorData,
                connectorTemplate,
                selectedValue = this.$el.find("#connectorType option:selected"),
                mainVersion,
                connectorRef;

            connectorData = _.findWhere(this.data.connectors, {"connectorName": selectedValue.attr('connectorTypeName'), "bundleVersion": selectedValue.attr('bundleVersion')});


            // For each schedule on the page
            _.each(this.addedLiveSyncSchedules, function (source) {
                this.$el.find("#" + source.split("/").join("")).find(".deleteSchedule").click();
                this.addedLiveSyncSchedules.splice(_.indexOf(this.addLiveSyncScheduler, source), 1);
            }, this);
            this.objectTypes = [];


            //If for some reason no connector data
            if(_.isUndefined(connectorData)) {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorsNotAvailable");
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorView});
            } else {
                mainVersion = this.findMainVersion(connectorData.bundleVersion);

                //Checking to ensure we don't reload the page if a minor version is changed
                if(this.data.currentMainVersion === null || (parseFloat(this.data.currentMainVersion) !== parseFloat(mainVersion)) || this.data.connectorTypeName !== selectedValue.attr('connectorTypeName')) {
                    this.data.connectorTypeName = selectedValue.attr('connectorTypeName');
                    this.data.systemType = selectedValue.attr('systemType');
                    this.data.currentMainVersion = this.findMainVersion(connectorData.bundleVersion);

                    connectorTemplate = connectorData.connectorName +"_" +mainVersion;

                    connectorRef = {
                        connectorRef: connectorData
                    };

                    ConnectorDelegate.detailsConnector(connectorRef).then(_.bind(function(connectorDefaults){
                        this.connectorTypeRef = ConnectorRegistry.getConnectorModule(connectorTemplate);

                        if(this.connectorTypeRef.oAuthConnector) {
                            this.oAuthConnector = true;
                        } else {
                            this.oAuthConnector = false;
                            this.setConnectorState();
                        }

                        this.connectorTypeRef.render({"connectorType": connectorTemplate,
                                "animate": true,
                                "connectorDefaults": connectorDefaults,
                                "editState" : this.data.editState,
                                "systemType" : this.data.systemType },
                            _.bind(function(){
                                this.setConnectorPage();
                                this.setSubmitFlow();

                                validatorsManager.validateAllFields(this.$el);

                                if(_.isFunction(callback)){
                                    callback();
                                }

                            }, this));
                    }, this));
                } else {
                    //Set the bundle version on a minor version change so it saves
                    this.connectorTypeRef.data.connectorDefaults.connectorRef.bundleVersion = selectedValue.attr('bundleVersion');
                }
            }
        },

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

            connectorData.syncFailureHandler.maxRetries = parseInt(connectorData.syncFailureHandler.maxRetries, 10);

            if (connectorData.syncFailureHandler.postRetryAction === "script") {
                connectorData.syncFailureHandler.postRetryAction = {"script": this.postActionBlockScript.getScriptHook().script};
            }

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

            mergedResult.objectTypes = this.userDefinedObjectTypes || this.objectTypes;

            return mergedResult;
        },

        formSubmit: function(event) {
            event.preventDefault();
            var mergedResult = this.getProvisioner(),
                urlName;

            if(this.data.connectorId) {
                urlName = this.data.connectorId;
            } else {
                urlName = mergedResult.name;
            }

            //Checks for connector specific save function to do any additional changes to data
            if(this.connectorTypeRef.connectorSaved) {
                mergedResult = this.connectorTypeRef.connectorSaved(mergedResult);
            }

            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorSaved");

            ConnectorDelegate.deleteCurrentConnectorsCache();

            ConfigDelegate[this.data.editState ? "updateEntity" : "createEntity" ](this.data.systemType + "/" + urlName, mergedResult).then(_.bind(function () {
                _.delay(function () {
                    eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.resourcesView});
                }, 1500);
            }, this));
        },

        oAuthFormSubmit: function(event) {
            event.preventDefault();
            var mergedResult = this.getProvisioner();

            this.connectorTypeRef.submitOAuth(mergedResult, this.data.editState);
        },

        validate: function(event) {
            event.preventDefault();

            var mergedResult = this.getProvisioner();

            //checks for any additional validation functions to allow a connector specific validation adjustment
            if(this.connectorTypeRef.connectorValidate) {
                this.connectorTypeRef.connectorValidate(_.bind(function(result) {
                    this.connectorValidate(result);
                }, this), mergedResult);
            } else {
                this.connectorValidate(mergedResult);
            }
        },

        connectorValidate: function(mergedResult) {
            ConnectorDelegate.testConnector(mergedResult).then(_.bind(function (testResult) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestPass");

                    if(!this.data.editState) {
                        this.objectTypes = testResult.objectTypes;
                        this.updateLiveSyncObjects();
                    }

                    this.userDefinedObjectType = null;
                    this.$el.find("#addEditObjectType").prop('disabled', false);
                    this.$el.find("#addEditConnector").prop('disabled', false);

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

            this.$el.find("#connectorErrorMessage .message").html(this.parseErrorMessage(error.message));
            this.$el.find("#connectorErrorMessage").show();
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

        setConnectorState: function() {
            if(!this.oAuthConnector) {
                this.$el.find("#addEditConnector").prop('disabled', true);
                this.$el.find("#addEditObjectType").prop('disabled', true);
            } else {
                this.setSubmitFlow();
            }
        },

        addEditObjectTypes: function() {
            objectTypesDialog.render(this.userDefinedObjectTypes || this.objectTypes, this.getProvisioner(), _.bind(this.saveObjectTypes, this));
        },

        saveObjectTypes: function(newObjectTypes) {
            this.userDefinedObjectTypes = newObjectTypes;
            this.updateLiveSyncObjects();


            this.$el.find("#connectorWarningMessage .message").html($.t("templates.connector.pendingObjectTypes"));
            this.$el.find("#connectorWarningMessage").show();
        }
    });

    return new AddEditConnectorView();
});
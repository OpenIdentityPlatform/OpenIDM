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

define([
    "jquery",
    "underscore",
    "backbone",
    "backgrid",
    "form2js",
    "handlebars",
    "org/forgerock/openidm/ui/admin/connector/AbstractConnectorView",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/ConnectorDelegate",
    "org/forgerock/openidm/ui/admin/connector/ConnectorRegistry",
    "org/forgerock/openidm/ui/admin/connector/ConnectorTypeView",
    "org/forgerock/openidm/ui/admin/util/ConnectorUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor",
    "org/forgerock/openidm/ui/admin/connector/liveSyncDialog",
    "org/forgerock/openidm/ui/admin/objectTypes/ObjectTypesDialog",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/admin/util/Scheduler",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function($, _,
            Backbone,
            Backgrid,
            form2js,
            Handlebars,
            AbstractConnectorView,
            BackgridUtils,
            ConfigDelegate,
            ConnectorDelegate,
            ConnectorRegistry,
            ConnectorType,
            connectorUtils,
            constants,
            eventManager,
            InlineScriptEditor,
            liveSyncDialog,
            objectTypesDialog,
            router,
            Scheduler,
            SchedulerDelegate,
            UIUtils,
            validatorsManager) {

    var AddEditConnectorView = AbstractConnectorView.extend({
        template: "templates/admin/connector/EditConnectorTemplate.html",
        events: {
            "change #connectorType" : "loadConnectorTemplate",
            "click #updateObjectTypes" : "objectTypeFormSubmit",
            "click #updateSync" : "syncFormSubmit",
            "click #updateAdvanced" : "advancedFormSubmit",
            "click .addSchedule" : "addLiveSync",
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
            "paste #connectorForm :input" : "connectorFlowCheck",
            "change #advancedForm :input" : "advancedChangesCheck",
            "click .editSchedule" : "editSchedule"
        },
        partials: [
            "partials/connector/_liveSyncGrid.html"
        ],
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
                }
            ]
        },
        connectorList: null,
        connectorTypeRef: null,
        liveSyncCollection: null,
        oAuthConnector: false,

        render: function(args, callback) {
            this.liveSyncCollection = new Backbone.Collection;
            this.data = {};
            this.data.docHelpUrl = constants.DOC_URL;
            this.data.versionDisplay = {};
            this.data.currentMainVersion = null;
            this.data.objectTypes = null;
            this.data.versionCheck = null;
            this.oAuthConnector = false;
            this.connectorTypeRef = null;
            this.connectorList = null;
            this.postActionBlockScript = null;
            this.connectorTypeRef = null;
            this.userDefinedObjectTypes = null;

            //Get available list of connectors
            $.when(ConnectorDelegate.availableConnectors()).then((connectors) => {
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

                let splitDetails = args[0].match(/(.*?)_(.*)/).splice(1),
                    urlArgs = router.convertCurrentUrlToJSON(),
                    version;

                this.data.editState = true;
                this.data.systemType = splitDetails[0];
                this.data.connectorId = splitDetails[1];

                //Get current connector details
                ConfigDelegate.readEntity(this.data.systemType + "/" + this.data.connectorId).then((data) => {
                    let tempVersion;

                    this.data.connectorIcon = connectorUtils.getIcon(data.connectorRef.connectorName);
                    this.currentObjectTypeLoaded = "savedConfig";
                    this.data.objectTypeDefaultConfigs = this.objectTypeConfigs[data.connectorRef.bundleName];

                    this.data.connectorName = data.name;
                    this.data.connectorTypeName = data.connectorRef.connectorName;

                    this.data.enabled = data.enabled;

                    //Need a check here in the instances where connectors do not have enable
                    if(_.isUndefined(this.data.enabled)) {
                        this.data.enabled = true;
                    }

                    this.data.poolConfigOption = data.poolConfigOption;
                    this.data.resultsHandlerConfig = data.resultsHandlerConfig;
                    this.data.operationTimeout = data.operationTimeout;

                    if(this.data.resultsHandlerConfig) {
                        this.data.resultsHandlerConfig.enableAttributesToGetSearchResultsHandler = (this.data.resultsHandlerConfig.enableAttributesToGetSearchResultsHandler === "true" || this.data.resultsHandlerConfig.enableAttributesToGetSearchResultsHandler === true);
                        this.data.resultsHandlerConfig.enableCaseInsensitiveFilter = (this.data.resultsHandlerConfig.enableCaseInsensitiveFilter === "true" || this.data.resultsHandlerConfig.enableCaseInsensitiveFilter === true);
                        this.data.resultsHandlerConfig.enableFilteredResultsHandler = (this.data.resultsHandlerConfig.enableFilteredResultsHandler === "true" || this.data.resultsHandlerConfig.enableFilteredResultsHandler === true);
                        this.data.resultsHandlerConfig.enableNormalizingResultsHandler = (this.data.resultsHandlerConfig.enableNormalizingResultsHandler === "true" || this.data.resultsHandlerConfig.enableNormalizingResultsHandler === true);
                    }

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
                    this.data.versionDisplay = _.filter(this.data.versionDisplay, (connector) => {
                        return  data.connectorRef.connectorName  === connector.versions[0].connectorName;
                    });

                    this.data.fullversion = this.versionRangeCheck(data.connectorRef.bundleVersion);
                    data.connectorRef.bundleVersion = this.data.fullversion;
                    this.data.currentMainVersion = this.findMainVersion(data.connectorRef.bundleVersion);

                    //Filter the connector types down to the current major version
                    this.data.versionDisplay[0].versions = _.filter(this.data.versionDisplay[0].versions, (version) => {
                        tempVersion = this.findMainVersion(version.bundleVersion);

                        return parseFloat(this.data.currentMainVersion) === parseFloat(tempVersion);
                    });

                    version = this.data.fullversion;

                    if(version.indexOf("(") !== -1 || version.indexOf(")") !== -1 || version.indexOf("[") !== -1 || version.indexOf("]") !== -1) {
                        version = version.replace(/\[|\)|\(|\]/g,'');
                        version = version.split(",");
                        version = version[0].split(".");
                        version = version[0] +"." +version[1];
                    } else {
                        version = version.split(".");
                        version = version[0] +"." +version[1];
                    }

                    if(version >= 1.4) {
                        this.data.versionCheck = true;
                    } else {
                        this.data.versionCheck = false;
                    }

                    //Get the connector type for top header display
                    this.data.displayConnectorType = this.data.versionDisplay[0].versions[0].displayName;

                    //OAuth branch
                    if (urlArgs.params && urlArgs.params.code) {
                        this.oAuthCode = urlArgs.params.code;

                        ConnectorRegistry.getConnectorModule(this.data.connectorTypeName + "_" +this.data.currentMainVersion).then((connectorTypeRef) => {
                            this.connectorTypeRef = connectorTypeRef;
                            this.connectorTypeRef.getToken(data, this.oAuthCode).then((tokenDetails) => {
                                this.connectorTypeRef.setToken(tokenDetails, data, this.data.systemType + "/" +this.data.connectorId, urlArgs);
                            });
                        });
                    } else {
                        this.parentRender(() => {

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
                            ConnectorRegistry.getConnectorModule(this.data.connectorTypeName + "_" + this.data.currentMainVersion).then((connectorTypeRef) => {
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
                                    () => {
                                        validatorsManager.validateAllFields(this.$el);

                                        //Set the current newest version incase there is a range
                                        this.connectorTypeRef.data.connectorDefaults.connectorRef.bundleVersion = data.connectorRef.bundleVersion;
                                        this.setSubmitFlow();

                                        if (callback) {
                                            callback();
                                        }
                                    });

                                this.setupLiveSync();
                            });

                        });
                    }
                });
            });
        },

        connectorFlowCheck: function() {
            if(this.oAuthConnector) {
                this.setSubmitFlow();
            }
        },

        deleteResource: function(event) {
            let connectorPath = "config/" + this.data.systemType +"/" +this.data.connectorId;

            event.preventDefault();

            connectorUtils.deleteConnector(this.data.connectorName, connectorPath, () => {
                eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.connectorListView});
            });
        },

        advancedFormSubmit: function(event) {
            event.preventDefault();

            let advancedData = form2js('advancedFields', '.', true),

                mergedResults = this.advancedDetailsGenerate(this.connectorDetails, advancedData);

            ConfigDelegate.updateEntity(this.data.systemType + "/" + this.data.connectorId,  mergedResults).then(() => {
                this.connectorDetails = mergedResults;

                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "advancedSaved");

                this.$el.find("#connectorWarningMessage .message .advanced-pending").remove();

                this.warningMessageCheck();
            });
        },

        advancedDetailsGenerate: function(oldAdvanced, newAdvanced) {
            let mergedResults = {},
                tempNumber,
                defaultOperationTimeout = -1,
                defaultPoolConfigOption = 10;

            $.extend(true, mergedResults, oldAdvanced, newAdvanced);

            //Need to convert all strings to numbers also some safety check to prevent bad values
            _.each(mergedResults.operationTimeout, (value, key) => {
                tempNumber = parseInt(value, 10);

                if(!_.isNaN(tempNumber)) {
                    mergedResults.operationTimeout[key] = parseInt(value, 10);
                } else {
                    mergedResults.operationTimeout[key] = defaultOperationTimeout;
                }
            });

            _.each(mergedResults.poolConfigOption, (value, key) => {
                tempNumber = parseInt(value, 10);

                if(!_.isNaN(tempNumber)) {
                    mergedResults.poolConfigOption[key] = parseInt(value, 10);
                } else {
                    mergedResults.poolConfigOption[key] = defaultPoolConfigOption;
                }
            });

            return mergedResults;
        },

        //Saves the connector tab
        connectorFormSubmit: function(event) {
            event.preventDefault();

            let mergedResult = this.getProvisioner();

            //Checks for connector specific save function to do any additional changes to data
            if(this.connectorTypeRef.connectorSaved) {
                mergedResult = this.connectorTypeRef.connectorSaved(mergedResult);
            }

            mergedResult.configurationProperties.readSchema = false;

            ConnectorDelegate.testConnector(mergedResult).then(() => {
                ConnectorDelegate.deleteCurrentConnectorsCache();

                ConfigDelegate.updateEntity(this.data.systemType + "/" + this.data.connectorId, mergedResult).then(() => {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorSaved");

                    this.connectorDetails = mergedResult;

                    this.$el.find("#connectorWarningMessage .message .connector-version").remove();
                    this.$el.find("#connectorWarningMessage .message .connector-pending").remove();
                    this.warningMessageCheck();
                    this.$el.find("#connectorErrorMessage").hide();
                });
            },
            (result) => {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "connectorTestFailed");

                this.showError(result);
            });
        },

        //Saves the sync tab
        syncFormSubmit: function() {
            let syncData = form2js('syncForm', '.', true);

            this.connectorDetails.syncFailureHandler.maxRetries = parseInt(syncData.syncFailureHandler.maxRetries, 10);
            this.connectorDetails.syncFailureHandler.postRetryAction = syncData.syncFailureHandler.postRetryAction;


            if (this.connectorDetails.syncFailureHandler.postRetryAction === "script") {
                this.connectorDetails.syncFailureHandler.postRetryAction = {"script": this.postActionBlockScript.generateScript()};
            }

            ConfigDelegate.updateEntity(this.data.systemType + "/" + this.data.connectorId, this.connectorDetails).then(() => {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "liveSyncSaved");

                this.$el.find("#connectorWarningMessage .message .sync-pending").remove();
                this.warningMessageCheck();
            });
        },

        //Saves the object type tab
        objectTypeFormSubmit: function() {
            if(!this.userDefinedObjectTypes) {
                this.userDefinedObjectTypes = this.data.objectTypes;
            }

            this.connectorDetails.objectTypes = this.userDefinedObjectTypes;

            ConfigDelegate.updateEntity(this.data.systemType + "/" + this.data.connectorId, this.connectorDetails).then(() => {
                this.previousObjectType = this.userDefinedObjectTypes;
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeSaved");

                this.$el.find("#connectorWarningMessage .message .objecttype-pending").remove();

                this.warningMessageCheck();

                this.updateActionDropdown(this.previousObjectType);
            });
        },

        //will hide warning message if no messages left
        warningMessageCheck: function() {
            if(this.$el.find("#connectorWarningMessage .message .pending-changes").length === 0) {
                this.$el.find("#connectorWarningMessage").hide();
            }
        },

        addLiveSync: function() {
            let source = this.$el.find(".sources option:selected");

            if (source.length > 0) {

                liveSyncDialog.render({
                    "source": source.val(),
                    "changeType": "add"
                }, _.bind(this.amendLiveSyncGrid, this));
            }
        },

        amendLiveSyncGrid: function(schedule) {
            let row = {
                "source": schedule.source,
                "id": schedule.scheduleId,
                "enabled": schedule.enabled,
                "persisted": schedule.persisted
            };

            if (schedule.changeType === "add") {
                this.liveSyncCollection.add(row);
                this.$el.find('.sources [value="' + schedule.source + '"]').remove();
            } else if (schedule.changeType === "edit") {
                this.liveSyncCollection.set(row, {remove: false});
            }
            this.renderGrid();
            this.changeDropdown();
        },

        editSchedule: function(e) {
            e.preventDefault();

            let scheduleName = $(e.currentTarget.closest("tr")).data("source");
            let scheduleId = $(e.currentTarget.closest("tr")).data("id");

            liveSyncDialog.render({
                "id": scheduleId,
                "source": scheduleName,
                "changeType": "edit"
            }, _.bind(this.amendLiveSyncGrid, this));

        },

        renderGrid: function() {

            let state = "",
                liveSyncGrid,
                RenderRow = null,
                _this = this;

            this.$el.find("#liveSyncSchedule table").remove();

            RenderRow = Backgrid.Row.extend({
                render: function () {
                    RenderRow.__super__.render.apply(this, arguments);

                    this.$el.attr('data-source', this.model.attributes.source);
                    this.$el.attr('data-enabled', this.model.attributes.enabled);
                    this.$el.attr('data-persisted', this.model.attributes.persisted);
                    this.$el.attr('data-id', this.model.id);

                    return this;
                }
            });

            liveSyncGrid = new Backgrid.Grid({
                className: "group-field-block table backgrid",
                row: RenderRow,
                columns: BackgridUtils.addSmallScreenCell([
                    {
                        name: "source",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            render: function () {
                                var display = '<div data-sync="' + this.model.attributes.source + '">' + _.startCase(_.last(this.model.attributes.source.split("/"))) + '</div>';

                                this.$el.html(display);

                                return this;
                            }
                        })
                    },
                    {
                        name: "enabled",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({className: "text-muted"})
                    },
                    {
                        name: "persisted",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({className: "text-muted"})
                    },
                    {
                        name: "",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            events: {
                                "click .deleteSchedule" : "deleteSchedule"
                            },

                            deleteSchedule: function(e) {
                                e.preventDefault();

                                let scheduleName = this.model.attributes.source;
                                let scheduleId = this.model.id;

                                _this.$el.find(".sources").append("<option value='" + scheduleName + "'>" +  _.startCase(_.last(scheduleName.split("/"))) + "</option>");

                                SchedulerDelegate.deleteSchedule(scheduleId).then( () => {
                                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "scheduleDeleted");
                                    _this.$el.find("tr[data-id=" + scheduleId + "]").remove();
                                    _this.liveSyncCollection.remove({id: scheduleId});
                                    _this.changeDropdown();

                                    if (_this.$el.find('tbody tr[data-source]').length === 0) {
                                        _this.$el.find('#liveSyncSchedule table').remove();
                                    }
                                });
                            },

                            render: function () {
                                this.$el.html(Handlebars.compile("{{> connector/_liveSyncGrid}}")());

                                this.delegateEvents();
                                return this;
                            }
                        })
                    }
                ]),
                collection: this.liveSyncCollection
            });

            this.$el.find("#liveSyncTable").append(liveSyncGrid.render().el);

        },

        changeDropdown: function() {
            if (this.$el.find(".sources option").length === 0) {
                this.$el.find(".addSchedule").prop('disabled', true);
                this.$el.find(".sources").prop('disabled', true);
            } else {
                this.$el.find(".addSchedule").prop('disabled', false);
                this.$el.find(".sources").prop('disabled', false);
            }
        },

        setupLiveSync: function() {

            this.$el.find("#schedules table tbody").empty();

            SchedulerDelegate.getLiveSyncSchedulesByConnectorName(this.connectorDetails.name).then((schedules) => {

                if (schedules.length > 0) {
                    _.each(schedules, (schedule) => {
                        this.liveSyncCollection.add({
                            source: schedule.invokeContext.source,
                            id: schedule._id,
                            enabled: schedule.enabled,
                            persisted: schedule.persisted}
                        );

                    });
                    this.renderGrid();
                }


                this.updateLiveSyncObjects();
                this.changeDropdown();

            });

            if (!this.postActionBlockScript) {
                this.postActionBlockScript = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find(".postActionBlock .script"),
                    "eventName": "",
                    "deleteElement": false
                });
            }
        },

        updateLiveSyncObjects: function() {
            let objectTypes = [];

            if (this.connectorDetails.name) {
                this.$el.find(".nameFieldMessage").hide();

                if (this.userDefinedObjectTypes && _.size(this.userDefinedObjectTypes) > 0) {
                    objectTypes = _.map(this.userDefinedObjectTypes, (object, key) => {
                        return "system/" + this.connectorDetails.name + "/" + key;
                    });
                } else {
                    objectTypes = _.map(this.data.objectTypes, (object, key) => {
                        return "system/" + this.connectorDetails.name + "/" + key;
                    });
                }

                this.$el.find(".sources").empty();

                if (objectTypes && _.size(objectTypes) > 0) {
                    this.$el.find(".objectTypeFieldMessage").hide();

                    // For each possible liveSync
                    _.each(objectTypes, (objectName) => {
                        // The source is not scheduled add it to dropdown
                        if (this.liveSyncCollection.where({source: objectName}).length === 0) {
                            this.$el.find(".sources").append("<option value='" + objectName + "'>" + _.startCase(_.last(objectName.split("/"))) + "</option>");
                            this.$el.find(".addLiveSync").prop('disabled', false);
                            this.$el.find(".sources").prop('disabled', false);
                        }
                    });
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

        advancedChangesCheck: function() {
            if(this.$el.find("#connectorWarningMessage .message .advanced-pending").length === 0) {
                this.$el.find("#connectorWarningMessage .message").append('<div class="pending-changes advanced-pending">' +$.t("templates.connector.advanced.pendingAdvancedChanges") +'</div>');
                this.$el.find("#connectorWarningMessage").show();
            }
        },

        //This function is to find the newest version of a connector and select it if a user provides a range
        versionRangeCheck: function(version) {
            let cleanVersion = null,
                tempVersion,
                tempMinorVersion,
                mainVersion,
                minorVersion;

            //Checks to see if there is a range
            if(version.indexOf("(") !== -1 || version.indexOf(")") !== -1 || version.indexOf("[") !== -1 || version.indexOf("]") !== -1) {
                if(this.data.versionDisplay[0].versions.length === 1) {
                    cleanVersion = this.data.versionDisplay[0].versions[0].bundleVersion;
                } else {
                    _.each(this.data.versionDisplay[0].versions, (versions) => {
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
                    });
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
            let connectorData = {},
                connDetails = this.connectorDetails,
                mergedResult = {},
                tempArrayObject,
                arrayComponents = $(".connector-array-component");

            if(this.connectorTypeRef.getGenericState()) {
                delete connectorData.root;

                $.extend(true, mergedResult, connDetails);

                mergedResult.configurationProperties = this.connectorTypeRef.getGenericConnector();
                mergedResult.enabled = this.$el.find("#connectorEnabled").val();
            } else {
                connectorData = form2js('connectorForm', '.', true);

                delete connectorData.connectorType;

                connectorData.configurationProperties.readSchema = false;
                connectorData.objectTypes = {};

                $.extend(true, mergedResult, connDetails, connectorData);

                //Added logic to ensure array parts correctly add and delete what is set
                _.each(arrayComponents, (component) => {
                    tempArrayObject = form2js($(component).prop("id"), ".", true);

                    _.each(tempArrayObject.configurationProperties, (item, key) => {
                        mergedResult.configurationProperties[key] = item;

                        //Need this check for when an array is saved with an empty string after containing data to properly remove it
                        if(_.isArray(mergedResult.configurationProperties[key]) && mergedResult.configurationProperties[key].length === 1 && mergedResult.configurationProperties[key][0] === "") {
                            delete mergedResult.configurationProperties[key];
                        }
                    });
                });
            }

            mergedResult.objectTypes = this.userDefinedObjectTypes || this.data.objectTypes;

            return mergedResult;
        },

        //Renders the object type table
        renderObjectTypes: function(newObjectTypes) {
            this.userDefinedObjectTypes = newObjectTypes;

            this.$el.find("#objectTypesTab table tbody").empty();

            _.each(this.userDefinedObjectTypes, (object, key) => {
                this.$el.find("#objectTypesTab table tbody").append(
                        "<tr data-objecttype='" + key + "'><td>" + key + "</td>"
                        + "<td><button class='btn btn-link edit-objectType'><i class='fa fa-pencil'></i></button>"
                        + "<button class='btn btn-link delete-objectType'><i class='fa fa-times'></i></button></td></tr>");
            });


            this.updateLiveSyncObjects();

            if(this.$el.find("#connectorWarningMessage .message .objecttype-pending").length === 0) {
                this.$el.find("#connectorWarningMessage .message").append('<div class="pending-changes objecttype-pending">' +$.t("templates.connector.pendingObjectTypes") +'</div>');
                this.$el.find("#connectorWarningMessage").show();
            }
        },

        //Since we are using tab panels we need custom validate to correctly enable / disable the connector submit
        validationSuccessful: function (event) {
            AbstractConnectorView.prototype.validationSuccessful(event);

            this.$el.find("#submitConnector").attr("disabled", false);
        },

        validationFailed: function (event, details) {
            AbstractConnectorView.prototype.validationFailed(event, details);

            this.$el.find("#submitConnector").attr("disabled", true);
        },

        //When clicking the pencil for an object type
        editObjectType: function(event) {
            let objectTypeName = $(event.target).parents("tr").attr("data-objectType");

            objectTypesDialog.render(this.userDefinedObjectTypes || this.data.objectTypes, objectTypeName, this.getProvisioner(), _.bind(this.renderObjectTypes, this));
        },

        //When clicking the delete icon for an object type
        deleteObjectType: function(event){
            let objectTypeName = $(event.target).parents("tr").attr("data-objectType");

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
                        +'<a href="#resource/system/' + this.data.connectorId +'/' +key+'/list/"><i class="fa fa-database"> Data ('+key  +')</i></a>'
                        +'</li>');
            }, this);
        },

        //When adding a new object type
        addObjectType: function() {
            objectTypesDialog.render(this.userDefinedObjectTypes || this.data.objectTypes, null, this.getProvisioner(), _.bind(this.renderObjectTypes, this));
        },

        //Used when an object type template selector is available.
        changeObjectTypeConfig: function(event) {
            let value = $(event.target).val(),
                type = $(event.target).attr("data-type");

            if(!this.userDefinedObjectTypes) {
                this.userDefinedObjectTypes = this.data.objectTypes;
            }

            $(event.target).val(this.currentObjectTypeLoaded);

            UIUtils.jqConfirm($.t('templates.connector.objectTypes.changeConfiguration'), () => {
                if(value === "fullConfig") {
                    this.connectorDetails.configurationProperties.readSchema = true;

                    ConnectorDelegate.testConnector(this.connectorDetails).then(
                        (result) => {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeLoaded");

                            this.renderObjectTypes(result.objectTypes);
                        },
                        () => {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeFailedToLoad");
                        }
                    );
                } else if(value === "savedConfig") {
                    if(this.previousObjectType) {
                        this.renderObjectTypes(this.previousObjectType);
                    }
                } else {
                    ConnectorDelegate.connectorDefault(value, type).then(
                        (result) => {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeLoaded");

                            this.renderObjectTypes(result.objectTypes);
                        },
                        () => {
                            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "objectTypeFailedToLoad");
                        }
                    );
                }

                this.currentObjectTypeLoaded = value;
                $(event.target).val(this.currentObjectTypeLoaded);

            });
        }
    });

    return new AddEditConnectorView();
});

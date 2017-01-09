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
 * Copyright 2014-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "bootstrap-dialog"

], function($, _, handlebars,
            AbstractView,
            eventManager,
            constants,
            router,
            ConfigDelegate,
            ValidatorsManager,
            BootstrapDialog) {
    var MapResourceView = AbstractView.extend({
        template: "templates/admin/MapResourceView.html",
        noBaseTemplate: true,
        element: "#resourceMappingBody",
        events: {
            "click .select-resource" : "selectAnotherResource",
            "click #createMapping" : "submitNewMapping",
            "click .mapping-swap" : "swapSourceTarget"
        },
        partials: [
            "partials/mapping/_mapResourceDialog.html"
        ],
        sourceAdded: false,
        targetAdded: false,
        sourceDetails: null,
        targetDetails: null,
        resourceAddRef: null,
        targetAddRef: null,
        mappingList: null,
        syncExist: true,

        render: function(args, callback) {

            //Reset variables on render to ensure data is clean when navigating around
            this.sourceAdded = false;
            this.targetAdded = false;
            this.sourceDetails = null;
            this.targetDetails = null;
            this.resourceAddRef = null;
            this.targetAddRef = null;
            this.mappingList = null;
            this.removeCallback = null;
            this.addCallback = null;

            this.removeCallback = args.removeCallback;
            this.addCallback = args.addCallback;

            ConfigDelegate.readEntity("sync").then(
                _.bind(function(sync) {
                    this.mappingList = sync.mappings;
                    this.syncExist = true;
                    this.parentRender(_.bind(function(){

                        if (callback) {
                            callback();
                        }

                    }, this));
                }, this),
                _.bind(function(){
                    this.mappingList = [];
                    this.syncExist = false;
                    this.parentRender(_.bind(function(){

                        if (callback) {
                            callback();
                        }

                    }, this));
                }, this));
        },
        addMapping: function(mappingObj) {
            if(!this.sourceAdded) {
                this.sourceDetails = mappingObj;
                this.displayDetails("mappingSource", this.sourceDetails);
                this.sourceAdded = true;
            } else {
                this.targetDetails = mappingObj;
                this.displayDetails("mappingTarget", this.targetDetails);
                this.targetAdded = true;
            }

            if(this.sourceAdded && this.targetAdded) {
                this.$el.find("#createMapping").prop("disabled", false);
            } else {
                this.$el.find("#createMapping").prop("disabled", true);
            }

            if(this.addCallback) {
                this.addCallback(this.sourceAdded, this.targetAdded);
            }
        },
        submitNewMapping: function(event) {
            event.preventDefault();

            var _this = this,
                mappingName,
                nameCheck,
                counter = 0,
                generateMappingInfo = this.createMappingName(this.targetDetails, this.sourceDetails, this.$el.find("#mappingTarget .resource-object-type-select").val(), this.$el.find("#mappingSource .resource-object-type-select").val()),
                tempName = generateMappingInfo.generatedName,
                cleanName = tempName,
                currentData = {
                    "mappingName" : "",
                    "availableLinks" : ""
                };

            this.targetDetails.saveName = generateMappingInfo.target;
            this.sourceDetails.saveName =  generateMappingInfo.source;

            while(!mappingName) {
                nameCheck = this.checkMappingName(tempName);

                if(nameCheck) {
                    mappingName = tempName;
                } else {
                    tempName = cleanName + counter;
                    counter++;
                }
            }

            currentData.mappingName = mappingName;
            currentData.availableLinks = this.findLinkedMapping();

            BootstrapDialog.show({
                title: $.t("templates.mapping.createMappingDialog"),
                type: BootstrapDialog.TYPE_DEFAULT,
                message:  $(handlebars.compile("{{> mapping/_mapResourceDialog}}")(currentData)),
                onshown : function (dialogRef) {
                    _this.setElement(dialogRef.$modalBody);
                    ValidatorsManager.bindValidators(dialogRef.$modalBody.find("form"));
                },
                onhide: function() {
                    _this.setElement($("#resourceMappingBody"));
                },
                buttons: [{
                    label: $.t("common.form.cancel"),
                    id:"mappingSaveCancel",
                    action: function(dialogRef) {
                        dialogRef.close();
                    }
                }, {
                    label: $.t('common.form.ok'),
                    id:"mappingSaveOkay",
                    cssClass: "btn-primary",
                    action: _.bind(function(dialogRef) {
                        this.saveMapping(dialogRef, function(){ dialogRef.close();});
                    }, this)
                }]
            });
        },
        saveMapping: function(dialogRef, callback) {
            var completeMapping = {
                    "mappings": this.mappingList
                },
                tempMapping;

            tempMapping = {
                "target" : this.targetDetails.saveName,
                "source" : this.sourceDetails.saveName,
                "name" : dialogRef.$modalBody.find("#saveMappingDialog .mappingName").val(),
                "properties": [],
                "policies" : [
                    {
                        "action" : "ASYNC",
                        "situation" : "ABSENT"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "ALL_GONE"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "AMBIGUOUS"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "CONFIRMED"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "FOUND"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "FOUND_ALREADY_LINKED"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "LINK_ONLY"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "MISSING"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "SOURCE_IGNORED"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "SOURCE_MISSING"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "TARGET_IGNORED"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "UNASSIGNED"
                    },
                    {
                        "action" : "ASYNC",
                        "situation" : "UNQUALIFIED"
                    }
                ]
            };

            if (dialogRef.$modalBody.find("#saveMappingDialog .mappingLinked").val() !== "none") {
                tempMapping.links = dialogRef.$modalBody.find("#saveMappingDialog .mappingLinked").val();
            }

            completeMapping.mappings.push(tempMapping);

            ConfigDelegate[this.syncExist ? "updateEntity" : "createEntity" ]("sync", completeMapping).then(_.bind(function() {

                if(callback) {
                    callback();
                }

                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "mappingSaveSuccess");
                eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: "propertiesView", args: [tempMapping.name]});
            }, this));
        },
        findLinkedMapping: function() {
            var linksFound = _.filter(this.mappingList, function(mapping) {
                    return mapping.links;
                }),
                availableLinks = _.filter(this.mappingList, function(mapping) {
                    return !mapping.links;
                }),
                returnedLinks = [];

            _.find(availableLinks, function(available) {
                var safe = true;

                _.each(linksFound, function(link) {
                    if(link.links === available.name) {
                        safe = false;
                    }
                }, this);

                if(safe) {
                    returnedLinks.push(available);
                } else {
                    safe = true;
                }
            }, this);

            returnedLinks = _.filter(returnedLinks, function(link) {
                return (link.source === this.targetDetails.saveName && link.target === this.sourceDetails.saveName);
            }, this);

            return returnedLinks;
        },
        /**
         *
         * @param targetDetails - Object of the mapping target information
         * @param sourceDetails - Object of the mapping source information
         * @param targetObjectType - Object type name for a target connector
         * @param sourceObjectType - Object type name for a source connector
         *
         * Generates a mapping name based off of the mapping source and target items created
         */
        createMappingName: function(targetDetails, sourceDetails, targetObjectType, sourceObjectType) {
            var targetName = "",
                sourceName = "",
                mappingSource,
                mappingTarget,
                tempName,
                tempObjectType;

            tempName = this.properCase(targetDetails.name);

            if(targetDetails.resourceType === "connector") {
                tempObjectType = targetObjectType.charAt(0).toUpperCase() +targetObjectType.substring(1);
                targetName = "system" + tempName +tempObjectType;
                mappingTarget = "system/" + targetDetails.name +"/" +targetObjectType;
            } else{
                targetName = "managed" + tempName;
                mappingTarget = "managed/" + targetDetails.name;
            }

            tempName = this.properCase(sourceDetails.name);

            if(sourceDetails.resourceType === "connector") {
                tempObjectType = sourceObjectType.charAt(0).toUpperCase() + sourceObjectType.substring(1);
                sourceName = "system" +tempName +tempObjectType;
                mappingSource = "system/" + sourceDetails.name +"/" +sourceObjectType;
            } else{
                sourceName = "managed" + tempName;
                mappingSource = "managed/" + sourceDetails.name;
            }

            tempName = sourceName + "_" +targetName;

            if(tempName.length > 50) {
                tempName = tempName.substring(0, 49);
            }

            return {
                source: mappingSource,
                target: mappingTarget,
                generatedName : tempName
            };
        },
        //Used to create a properly formatted name of the user selected resources. example managedSystem or sourceLdapAccount
        properCase: function(name) {
            var tempName;

            if(name.length > 1) {
                tempName = name.charAt(0).toUpperCase() + name.substring(1).toLowerCase();
            } else {
                tempName = name.charAt(0).toUpperCase();
            }

            return tempName;
        },
        swapSourceTarget: function(event) {
            event.preventDefault();

            var currentTarget = this.targetDetails,
                currentSource = this.sourceDetails,
                currentSourceAdded = this.sourceAdded,
                currentTargetAdded = this.targetAdded;

            this.setEmpty("mappingSource");
            this.setEmpty("mappingTarget");

            this.targetDetails = currentSource;
            this.sourceDetails = currentTarget;
            this.sourceAdded = currentTargetAdded;
            this.targetAdded = currentSourceAdded;

            if(currentTarget !== null) {
                this.displayDetails("mappingSource", currentTarget);
            }

            if(currentSource !== null) {
                this.displayDetails("mappingTarget", currentSource);
            }

            if(this.sourceAdded && this.targetAdded) {
                $("#createMapping").prop("disabled", false);
            }

            if(this.addCallback) {
                this.addCallback(this.sourceAdded, this.targetAdded);
            }
        },
        displayDetails: function(id, details) {
            if(details.resourceType === "connector") {
                this.$el.find("#"+id +" .resource-small-icon").attr('class', "resource-small-icon " +details.iconClass);

                this.$el.find("#"+id +" .resource-type-name").html(details.displayName);
                this.$el.find("#"+id +" .resource-given-name").html(details.name);
                this.$el.find("#"+id +" .edit-objecttype").show();

                this.$el.find("#"+id +" .object-type-name").hide();
                this.$el.find("#"+id +" .resource-object-type-select").show();
                this.$el.find("#"+id +" .resource-object-type-select option").remove();

                _.each(details.objectTypes, function(value){
                    this.$el.find("#"+id +" .resource-object-type-select").append("<option value='"+value +"'>" +value  +"</option>");
                }, this);

            } else {
                if(!details.schema || !details.schema.icon) {
                    this.$el.find("#" + id + " .resource-small-icon").attr('class', "resource-small-icon " +details.iconClass);
                } else {
                    this.$el.find("#" + id + " .resource-small-icon").attr('class','resource-small-icon fa ' +details.schema.icon);
                }

                this.$el.find("#"+id +" .resource-type-name").html($.t("templates.managed.managedObjectType"));
                this.$el.find("#"+id +" .resource-given-name").html(details.name);
                this.$el.find("#"+id +" .edit-objecttype").hide();
                this.$el.find("#"+id +" .object-type-name").show();
                this.$el.find("#"+id +" .resource-object-type-select").hide();
            }

            this.$el.find("#"+id +" .mapping-resource").show();
            this.$el.find("#"+id +" .mapping-resource-empty").hide();
            this.$el.find("#"+id +" .select-resource").prop("disabled", false);
        },
        selectAnotherResource: function(event) {
            var targetId = $(event.currentTarget).parents(".mapping-resource-body").prop("id");

            this.setEmpty(targetId);
        },
        setEmpty: function(id) {
            this.$el.find("#" +id +" .mapping-resource-empty").show();
            this.$el.find("#" +id +" .mapping-resource").hide();
            this.$el.find("#" +id +" .select-resource").prop("disabled", true);
            this.$el.find("#createMapping").prop("disabled", true);

            if(id === "mappingSource") {
                this.sourceAdded = false;
                this.sourceDetails = null;
            } else {
                this.targetAdded = false;
                this.targetDetails = null;
            }

            this.removeCallback();
        },
        checkMappingName: function(value) {
            return !_.find(this.mappingList, function(mapping) {
                return value === mapping.name;
            }, this);
        },
        validationSuccessful: function (event) {
            AbstractView.prototype.validationSuccessful(event);
            this.customValidate();
        },

        validationFailed: function (event, details) {
            AbstractView.prototype.validationFailed(event, details);
            this.customValidate();
        },

        customValidate: function() {
            var formValid = ValidatorsManager.formValidated(this.$el.find("form"));

            if (formValid) {
                //Save button not in this.$el scope
                $("#mappingSaveOkay").attr("disabled", false);
            } else {
                $("#mappingSaveOkay").attr("disabled", true);
            }

        }
    });

    return new MapResourceView();
});

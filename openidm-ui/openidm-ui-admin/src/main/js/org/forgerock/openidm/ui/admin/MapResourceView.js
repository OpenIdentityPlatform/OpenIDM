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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "bootstrap-dialog"

], function($, _, AbstractView, eventManager, constants, router, ConfigDelegate, BootstrapDialog) {
    var MapResourceView = AbstractView.extend({
        template: "templates/admin/MapResourceView.html",
        noBaseTemplate: true,
        element: "#resourceMappingBody",
        events: {
            "click .select-resource" : "selectAnotherResource",
            "click #createMapping" : "submitNewMapping",
            "click .mapping-swap" : "swapSourceTarget"
        },
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
            BootstrapDialog.show({
                title: $.t("templates.mapping.createMappingDialog"),
                type: BootstrapDialog.TYPE_DEFAULT,
                message: this.$el.find("#saveMappingDialog").clone().attr("id","saveMappingDialogClone"),
                onshown : _.bind(function (dialogRef) {
                    var mappingName = null,
                        nameCheck,
                        tempName = this.createMappingName(dialogRef.$modalBody.find("#saveMappingDialogClone .mappingName")),
                        cleanName = tempName,
                        counter = 0,
                        availableLinks;

                    while(!mappingName) {
                        nameCheck = this.checkMappingName(tempName);

                        if(nameCheck) {
                            mappingName = tempName;
                        } else {
                            tempName = cleanName + counter;
                            counter++;
                        }
                    }

                    dialogRef.$modalBody.find("#saveMappingDialogClone .mappingName").val(mappingName);

                    availableLinks = this.findLinkedMapping();

                    dialogRef.$modalBody.find("#saveMappingDialogClone .mappingLinked .mapping-linked-option").remove();

                    if(availableLinks.length > 0) {
                        dialogRef.$modalBody.find("#saveMappingDialogClone .mappingLinked").prop("disabled", false);

                        _.each(availableLinks, function(link){
                            dialogRef.$modalBody.find("#saveMappingDialogClone .mappingLinked").append("<option class='mapping-linked-option' value='" +link.name +"'>" +link.name  +"</option>");
                        }, this);
                    } else {
                        dialogRef.$modalBody.find("#saveMappingDialogClone .mappingLinked").prop("disabled", true);
                    }
                }, this),
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
                "name" : dialogRef.$modalBody.find("#saveMappingDialogClone .mappingName").val(),
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

            if (dialogRef.$modalBody.find("#saveMappingDialogClone .mappingLinked").val() !== "none") {
                tempMapping.links = dialogRef.$modalBody.find("#saveMappingDialogClone .mappingLinked").val();
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
        createMappingName: function() {
            var targetName = "",
                sourceName = "",
                tempName,
                tempObjectType;

            tempName = this.properCase(this.targetDetails.name);

            if(this.targetDetails.resourceType === "connector") {
                tempObjectType = this.$el.find("#mappingTarget .resource-object-type-select").val().charAt(0).toUpperCase() + this.$el.find("#mappingTarget .resource-object-type-select").val().substring(1);
                targetName = "system" + tempName +tempObjectType;
                this.targetDetails.saveName = "system/" + this.targetDetails.name +"/" +this.$el.find("#mappingTarget .resource-object-type-select").val();
            } else{
                targetName = "managed" + tempName;
                this.targetDetails.saveName = "managed/" + this.targetDetails.name;
            }

            tempName = this.properCase(this.sourceDetails.name);

            if(this.sourceDetails.resourceType === "connector") {
                tempObjectType = this.$el.find("#mappingSource .resource-object-type-select").val().charAt(0).toUpperCase() + this.$el.find("#mappingSource .resource-object-type-select").val().substring(1);
                sourceName = "system" +tempName +tempObjectType;
                this.sourceDetails.saveName = "system/" + this.sourceDetails.name +"/" +this.$el.find("#mappingSource .resource-object-type-select").val();
            } else{
                sourceName = "managed" + tempName;
                this.sourceDetails.saveName = "managed/" + this.sourceDetails.name;
            }

            return sourceName + "_" +targetName;
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

                this.$el.find("#"+id +" .resource-type-name").html($.t("templates.connector.managedObjectType"));
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
        }
    });

    return new MapResourceView();
});

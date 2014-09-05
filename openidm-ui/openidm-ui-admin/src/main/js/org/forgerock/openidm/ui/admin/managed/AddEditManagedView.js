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

/*global define, $, _, Handlebars, form2js */

define("org/forgerock/openidm/ui/admin/managed/AddEditManagedView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate"
], function(AdminAbstractView, eventManager, validatorsManager, constants, router, ConfigDelegate) {

    var AddEditManagedView = AdminAbstractView.extend({
        template: "templates/admin/managed/AddEditManagedTemplate.html",
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "click #managedObjectForm fieldset legend" : "sectionHideShow",
            "click #addManagedProperties": "addProperty",
            "click .remove-btn" : "removeProperty"
        },
        data: {
            currentManagedObject: {

            }
        },
        propertiesCounter: 0,

        render: function(args, callback) {
            var managedPromise,
                repoCheckPromise;

            //Remove when commons updates
            Handlebars.registerHelper('select', function(value, options){
                var selected = $('<select />').html(options.fn(this));
                selected.find('[value=' + value + ']').attr({'selected':'selected'});

                return selected.html();
            });

            //Remove when commons updated
            Handlebars.registerHelper('notequal', function(val, val2, options) {
                if(val !== val2){
                    return options.fn(this);
                }
            });

            managedPromise = ConfigDelegate.readEntity("managed");
            repoCheckPromise = ConfigDelegate.getConfigList();

            $.when(managedPromise, repoCheckPromise).then(_.bind(function(managedObjects, configFiles){
                this.data.managedObjects = managedObjects;

                if(args.length === 0) {
                    this.data.addEditSubmitTitle = $.t("common.form.add");
                    this.data.addEditTitle = $.t("templates.managed.addTitle");
                    this.data.currentManagedObject.name = "";
                    this.data.currentManagedObject.properties = {};
                    this.data.addState = true;

                } else {
                    this.data.addEditTitle = $.t("templates.managed.editTitle");
                    this.data.addEditSubmitTitle = $.t("common.form.update");
                    this.data.addState = false;

                    _.each(managedObjects.objects, _.bind(function(managedObject, iterator) {
                        if(managedObject.name === args[0]) {
                            this.data.currentManagedObject = managedObject;
                            this.data.currentManagedObjectIndex = iterator;
                        }
                    }, this));
                }

                this.data.currentRepo = _.find(configFiles[0].configurations, function(file){
                    return file.pid.search("repo.") !== -1;
                }, this);

                this.data.currentRepo = this.data.currentRepo.pid;

                if(this.data.currentRepo === "repo.orientdb") {
                    ConfigDelegate.readEntity(this.data.currentRepo).then(_.bind(function (repo) {
                        this.data.repoObject = repo;
                        this.managedRender(callback);
                    }, this));
                } else {
                    this.managedRender(callback);
                }
            }, this));
        },

        managedRender: function(callback) {
            this.parentRender(_.bind(function () {
                validatorsManager.bindValidators(this.$el);
                validatorsManager.validateAllFields(this.$el);

                this.propertiesCounter = this.$el.find(".add-remove-block").length;

                if (callback) {
                    callback();
                }

            }, this));
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

        formSubmit: function(event) {
            event.preventDefault();

            var managedObject = this.setPropertyObject(form2js('managedObjectForm', '.', true)),
                promises = [],
                savedObject = {};

            if(!this.data.addState) {
                $.extend(savedObject, this.data.managedObjects.objects[this.data.currentManagedObjectIndex], managedObject);
            } else {
                savedObject = managedObject;
            }

            if(savedObject.properties.length === 0) {
                delete savedObject.properties;
            }

            if(!this.data.addState) {
                this.data.managedObjects.objects[this.data.currentManagedObjectIndex] = savedObject;
            } else {
                this.data.managedObjects.objects.push(savedObject);
            }

            eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "managedObjectSaveSuccess");
            promises.push(ConfigDelegate.updateEntity("managed", {"objects" : this.data.managedObjects.objects}));

            if(this.data.currentRepo === "repo.orientdb") {
                this.orientRepoChange(managedObject);
                promises.push(ConfigDelegate.updateEntity(this.data.currentRepo, this.data.repoObject));
            }

            $.when.apply($, promises).then(function() {

                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "managedObjectSaveSuccess");

                _.delay(function () {
                    eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.resourcesView});
                }, 1500);
            });
        },

        setPropertyObject: function(managedObject) {
            if(managedObject.properties !== undefined) {
                _.each(managedObject.properties, function (prop) {
                    if (prop.encryption) {
                        prop.encryption = {
                            key: "openidm-sym-default"
                        };
                    } else {
                        delete prop.encryption;
                    }

                    if (prop.scope) {
                        prop.scope = "private";
                    } else {
                        delete prop.scope;
                    }

                    if (prop.type) {
                        prop.type = "virtual";
                    } else {
                        delete prop.type;
                    }
                });
            } else {
                managedObject.properties = [];
            }

            return managedObject;
        },

        addProperty: function(event) {
            event.preventDefault();

            var checkboxes,
                field,
                input;

            field = this.$el.find("#managed-object-hidden-property").clone();
            field.removeAttr("id");
            input = field.find('input[type=text]');
            input.val("");
            input.attr("data-validator-event","keyup blur");
            input.attr("data-validator","required");

            checkboxes = field.find(".checkbox");

            this.propertiesCounter = this.propertiesCounter + 1;

            input.prop( "name", "properties[" +this.propertiesCounter  +"].name");
            $(checkboxes[0]).prop( "name", "properties[" +this.propertiesCounter  +"].encryption");
            $(checkboxes[1]).prop( "name", "properties[" +this.propertiesCounter  +"].scope");
            $(checkboxes[2]).prop( "name", "properties[" +this.propertiesCounter  +"].type");

            field.show();

            this.$el.find('#managedPropertyWrapper').append(field);

            validatorsManager.bindValidators(this.$el.find('#managedPropertyWrapper'));
            validatorsManager.validateAllFields(this.$el.find('#managedPropertyWrapper'));
        },

        removeProperty: function(event) {
            event.preventDefault();

            var clickedEle = event.target;

            if($(clickedEle).not("button")){
                clickedEle = $(clickedEle).closest("button");
            }

            $(clickedEle).parents(".group-field-block").remove();

            validatorsManager.bindValidators(this.$el.find('#managedPropertyWrapper'));
            validatorsManager.validateAllFields(this.$el);
        },

        orientRepoChange: function(managedObject) {
            var orientClasses = this.data.repoObject.dbStructure.orientdbClass;

            if(orientClasses["managed_" +managedObject.name] === undefined) {
                orientClasses["managed_" +managedObject.name] = {
                    "index" : [
                        {
                            "propertyName" : "_openidm_id",
                            "propertyType" : "string",
                            "indexType" : "unique"
                        }
                    ]
                };
            }
        }
    });

    return new AddEditManagedView();
});

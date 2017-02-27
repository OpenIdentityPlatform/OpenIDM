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
 * Copyright 2014-2017 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "form2js",
    "org/forgerock/openidm/ui/admin/managed/AbstractManagedView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/RepoDelegate",
    "org/forgerock/openidm/ui/admin/util/ScriptList",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "faiconpicker",
    "bootstrap-tabdrop",
    "org/forgerock/openidm/ui/admin/managed/schema/SchemaEditorView",
    "org/forgerock/commons/ui/common/components/ChangesPending",
    "org/forgerock/openidm/ui/admin/managed/schema/util/SchemaUtils"
], function($, _,
            form2js,
            AbstractManagedView,
            EventManager,
            validatorsManager,
            Constants,
            Router,
            ConfigDelegate,
            RepoDelegate,
            ScriptList,
            UIUtils,
            faiconpicker,
            tabdrop,
            SchemaEditorView,
            ChangesPending,
            SchemaUtils
    ) {

    var EditManagedView = AbstractManagedView.extend({
        template: "templates/admin/managed/EditManagedTemplate.html",
        events: {
            "click #saveManagedDetails" : "saveManagedDetails",
            "submit #managedObjectScriptsForm" : "saveManagedScripts",
            "onValidate": "onValidate",
            "click #deleteManaged": "deleteManaged",
            "click .tab-menu li": "changeTabs",
            "click #generalDetails" : "makeChanges",
            "keyup input" : "makeChanges"
        },
        eventHooks: [],
        propertyHooks: [],
        partials: [],
        model: {
            eventList : ["onCreate", "postCreate", "onRead", "onUpdate", "postUpdate", "onDelete", "postDelete", "onValidate", "onRetrieve", "onStore", "onSync"]
        },

        render: function(args, callback) {
            var eventKeys;

            this.args = args;
            this.data = {
                selectEvents: [],
                addedEvents: [],
                docHelpUrl : Constants.DOC_URL,
                noSchema: true
            };

            $.when(
                ConfigDelegate.readEntity("managed"),
                RepoDelegate.findRepoConfig()
            ).then(_.bind(function(managedObjects, repoConfig){
                this.data.managedObjects = managedObjects;
                this.data.repoConfig = repoConfig;

                _.each(managedObjects.objects, _.bind(function(managedObject, iterator) {
                    if(managedObject.name === args[0]) {
                        this.data.currentManagedObject = managedObject;
                        this.data.currentManagedObjectIndex = iterator;

                        if(this.data.currentManagedObject.schema) {
                            this.data.noSchema = _.isEmpty(this.data.currentManagedObject.schema.properties);
                        } else {
                            this.data.noSchema = true;
                        }
                    }
                }, this));

                eventKeys =
                    _.chain(this.data.currentManagedObject)
                        .keys()
                        .without("name", "properties")
                        .value();

                //Added events are used for the events that are currently set by the managed object
                this.data.addedEvents = _.intersection(eventKeys, this.model.eventList);

                //Select events are the events currently available for the select
                this.data.selectEvents = _.difference(this.model.eventList, eventKeys);

                this.managedRender(callback);

            }, this));
        },

        managedRender: function(callback) {
            this.parentRender(_.bind(function () {
                validatorsManager.bindValidators(this.$el);
                validatorsManager.validateAllFields(this.$el);

                this.$el.find(".nav-tabs").tabdrop();

                SchemaEditorView.render([this], () => {
                    this.$el.find('#managedObjectIcon').iconpicker({
                        hideOnSelect: true
                    });

                    this.model.managedScripts = ScriptList.generateScriptList({
                        element: this.$el.find("#managedScripts"),
                        label: $.t("templates.managed.addManagedScript"),
                        selectEvents: this.data.selectEvents,
                        addedEvents:this.data.addedEvents,
                        currentObject: this.data.currentManagedObject,
                        hasWorkflow: true,
                        workflowContext: _.pluck(SchemaEditorView.model.managedObjectSchema.getValue().properties, "propertyName"),
                        saveCallback: () => {
                            this.saveManagedScripts(false, () => {
                                this.$el.find('a[href="#managedScriptsContainer"]').tab('show');
                            });
                        }
                    });

                    if (callback) {
                        callback();
                    }
                });

                this.model.changesModule = ChangesPending.watchChanges({
                    element: this.$el.find(".changes-pending-container"),
                    undo: true,
                    watchedObj: _.clone(this.data.currentManagedObject, true),
                    undoCallback: () => {
                        this.render(this.args, () => {
                            this.$el.find('a[href="#managedDetailsContainer"]').tab('show');
                        });
                    }
                });

                this.makeChanges();

                this.setTabChangeEvent();

                if (this.data.currentTab) {
                    this.$el.find('a[href="' + this.data.currentTab + '"]').tab('show');
                    delete this.data.currentTab;
                }

            }, this));
        },

        saveManagedDetails: function(event) {
            event.preventDefault();

            var data = form2js('managedForm2JS', '.', true),
                nameCheck,
                currentTab = this.data.currentTab;

            nameCheck = this.checkManagedName(data.name, this.data.managedObjects.objects);

            if(!nameCheck || data.name === this.data.currentManagedObject.name) {

                if(_.isUndefined(this.data.currentManagedObject.schema)) {
                    this.data.currentManagedObject.schema = {};
                }

                this.data.currentManagedObject.name = data.name;

                this.$el.find("#managedErrorMessage").hide();

                this.data.currentManagedObject.schema = SchemaEditorView.getManagedSchema();

                this.data.currentManagedObject.schema.icon = this.$el.find("#managedObjectIcon").val();
                this.data.currentManagedObject.schema.title = this.$el.find("#managedObjectTitle").val();
                this.data.currentManagedObject.schema.description = this.$el.find("#managedObjectDescription").val();

                this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects, false, () => {
                    this.$el.find('a[href="' + currentTab + '"]').tab('show');
                });
            } else {
                this.$el.find("#managedErrorMessage .message").html($.t("templates.managed.duplicateNameError"));
                this.$el.find("#managedErrorMessage").show();
                this.$el.find("#saveManagedDetails").prop("disabled", true);
            }
        },
        saveManagedScripts: function(event, callback) {
            if (event) {
                event.preventDefault();
            }

            var scriptList = this.model.managedScripts.getScripts();

            _.each(this.model.eventList, function(val) {
                if(scriptList[val]) {
                    this.data.currentManagedObject[val] = scriptList[val];
                } else {
                    delete this.data.currentManagedObject[val];
                }
            }, this);

            this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects, false, callback);
        },

        deleteManaged: function(event) {
            event.preventDefault();

            var promises = [];

            UIUtils.confirmDialog($.t("templates.managed.managedDelete"), "danger", _.bind(function(){
                this.data.managedObjects.objects = _.reject(this.data.managedObjects.objects, function(managedObject){
                    return managedObject.name === this.data.currentManagedObject.name;
                }, this);

                this.data.managedObjects.objects = SchemaUtils.removeRelationshipOrphans(this.data.managedObjects.objects, this.data.currentManagedObject.name);

                promises.push(ConfigDelegate.updateEntity("managed", {"objects" : this.data.managedObjects.objects}));
                promises.push(RepoDelegate.deleteManagedObject(this.data.repoConfig, this.data.currentManagedObject.name));

                $.when.apply($, promises).then(
                    function(){
                        EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedSuccess");
                        EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: Router.configuration.routes.managedListView});
                    },
                    function(){
                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "deleteManagedFail");
                    });

            },this));
        },

        changeTabs: function(e) {
            if ($(e.currentTarget).hasClass("disabled")) {
                return false;
            }
        },
        /**
        This function is called any time the form is updated. It updates the current config,
        checks with the changes pending module for any differences with the original form,
        toggles the save button on when there are changes, and off when the form is current.
        **/
        makeChanges: function () {
            var data = form2js('managedForm2JS', '.', true);

            this.data.currentManagedObject.name = data.name;
            this.data.currentManagedObject.schema.icon = this.$el.find("#managedObjectIcon").val();
            this.data.currentManagedObject.schema.title = data.title;
            this.data.currentManagedObject.schema.description = data.description;

            this.model.changesModule.makeChanges(_.clone(this.data.currentManagedObject));

            if (this.model.changesModule.isChanged()) {
                this.$el.find("#saveManagedDetails").prop("disabled",false);
            } else {
                this.$el.find("#saveManagedDetails").prop("disabled",true);
            }
        },
        /**
        * This function sets an event for each bootstrap tab on "show" which looks for any
        * pending form changes in the currently visible tab. If there are changes the the tab
        * change is halted and a dialog is displayed asking the user if he/she would like to discard
        * or save the changes before actually changing tabs.
        *
        * @param {string} tabId - (optional) specific tab on which to set the change event...otherwise the event will be set on all tabs
        **/
        setTabChangeEvent: function (tabId) {
            var scope = this.$el;

            if (tabId) {
                scope = scope.find("#" + tabId);
            }

            //look for all bootstrap tabs within "scope"
            scope.on('show.bs.tab', 'a[data-toggle="tab"]', (e) => {
                this.data.currentTab = e.target.hash;

                //check to see if there are changes pending
                if (this.$el.find(".changes-pending-container:visible").length) {
                    //stop processing this tab change
                    e.preventDefault();
                    //throw up a confirmation dialog
                    SchemaUtils.confirmSaveChanges(this, e.target.hash, () => {
                        //once confirmed save the form then continue showing the new tab
                        this.saveManagedObject(this.data.currentManagedObject, this.data.managedObjects, false, () => {
                            this.$el.find('a[href="' + e.target.hash + '"]').tab('show');
                        });
                    });
                }
            });

        }
    });

    return new EditManagedView();
});

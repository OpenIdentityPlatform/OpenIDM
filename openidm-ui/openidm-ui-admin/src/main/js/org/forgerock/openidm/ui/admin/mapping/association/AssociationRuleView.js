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

/*global define */

define("org/forgerock/openidm/ui/admin/mapping/association/AssociationRuleView", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/mapping/association/correlationQuery/CorrelationQueryDialog",
    "org/forgerock/openidm/ui/admin/util/SaveChangesView",
    "bootstrap-dialog",
    "org/forgerock/openidm/ui/admin/util/LinkQualifierUtils",
    "org/forgerock/openidm/ui/admin/util/InlineScriptEditor"
], function($, _,
            MappingAdminAbstractView,
            eventManager,
            constants,
            CorrelationQueryDialog,
            SaveChangesView,
            BootstrapDialog,
            LinkQualifierUtils,
            InlineScriptEditor) {

    var AssociationRuleView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/association/AssociationRuleTemplate.html",
        element: "#correlationQueryView",
        noBaseTemplate: true,
        events: {
            "click #addNewCorrelationQuery": "addEditNewCorrelationQuery",
            "click .edit": "editLinkQualifier",
            "click .trash": "deleteLinkQualifier",
            "change .correlationQueryType": "changeCorrelationQueryType",
            "click .saveCorrelationQuery": "saveQuery",
            "click .resetCorrelationQuery": "resetQuery",
            "click .undo": "undoChange"
        },
        data: {
            noLinkQualifiers: false
        },
        model: {
            changes: [],
            addedLinkQualifiers: [],
            linkQualifiers:[]
        },

        render: function(args) {
            this.model.sync = this.getSyncConfig();
            this.model.mapping = this.getCurrentMapping();
            this.model.mappingName = this.getMappingName();

            this.model.startSync = this.getSyncNow();

            this.model.changes = args.changes || [];
            this.model.linkQualifiers = LinkQualifierUtils.getLinkQualifier(this.model.mappingName) || ["default"];
            this.model.addedLinkQualifiers = _.union(_.pluck(this.model.mapping.correlationQuery, "linkQualifier"), _.pluck(this.model.changes, "linkQualifier"));

            // Legacy Support
            if(_.has(this.model.mapping, "correlationQuery") && !_.isArray(this.model.mapping.correlationQuery)) {
                this.model.mapping.correlationQuery.linkQualifier = "default";
                this.model.mapping.correlationQuery = [this.model.mapping.correlationQuery];
            }

            this.data.correlationQueries = _.clone(this.model.mapping.correlationQuery, true);

            if (_.difference(this.model.linkQualifiers, this.model.addedLinkQualifiers).length === 0) {
                this.data.noLinkQualifiers = true;
            } else {
                this.data.noLinkQualifiers = false;
            }

            // Add the pending changes to the array of correlation queries used for rendering purposes, saved data is not generated from the data object.
            _.each(this.model.changes, _.bind(function(query) {
                var linkQualifier = query.linkQualifier,
                    correlationQuery = _.find(this.data.correlationQueries, {"linkQualifier": linkQualifier}),
                    correlationQueryIndex = _.indexOf(this.data.correlationQueries, correlationQuery);

                if (!_.isArray(this.data.correlationQueries)) {
                    this.data.correlationQueries = [];
                }

                switch (query.changes) {
                    case "add":
                        this.data.correlationQueries.push(_.extend(query, {"added": true}));
                        break;
                    case "toAdd":
                        this.data.correlationQueries.push(_.extend(query, {"toAdd": true}));
                        break;
                    case "edit":
                        this.data.correlationQueries[correlationQueryIndex] = _.extend(correlationQuery, {"edited": true});
                        break;
                    case "delete":
                        this.data.correlationQueries[correlationQueryIndex] = _.omit(_.extend(correlationQuery, {"deleted": true}), "edited");
                        break;
                }
            }, this));

            _.each(this.data.correlationQueries, function(query, key) {
                if (this.model.linkQualifiers.indexOf(query.linkQualifier) === -1) {
                    this.data.correlationQueries[key].error = true;
                }
            }, this);

            this.parentRender(function () {
                var scriptData = "";

                if ((_.has(this.model.mapping, "correlationQuery") && this.model.mapping.correlationQuery.length > 0) || this.model.changes.length > 0) {
                    this.$el.find(".correlationQueryType").val("queries");

                    if(this.model.mapping.correlationQuery === undefined) {
                        this.model.mapping.correlationQuery = [];
                    }

                } else if (_.has(this.model.mapping, "correlationScript")) {
                    scriptData = this.model.mapping.correlationScript;
                    this.$el.find(".correlationQueryType").val("script");
                } else {
                    this.model.mapping.correlationQuery = [];
                }

                this.correlationScript = InlineScriptEditor.generateScriptEditor({
                    "element": this.$el.find("#correlationScriptContent"),
                    "eventName": "correlationScript",
                    "disableValidation": false,
                    "scriptData": scriptData,
                    "disablePassedVariable": false,
                    "placeHolder" : "['test', 'default']",
                    "validationCallback": _.bind(function(valid) {
                        if (this.$el.find(".correlationQueryType").val() === "script") {
                            this.$el.find(".saveCorrelationQuery").prop('disabled', !valid);
                            this.$el.find(".resetCorrelationQuery").prop('disabled', !valid);
                        }
                    }, this)
                }, _.bind(function(){
                    this.changeCorrelationQueryType();
                    this.checkButtons();
                }, this));

            });
        },

        checkButtons: function() {
            var showWarning = true;

            if (this.$el.find(".correlationQueryType").val() === "queries") {
                if (this.model.changes.length > 0) {
                    this.$el.find(".correlationQueryChangesMsg").show();
                    showWarning = false;
                } else {
                    this.$el.find(".correlationQueryChangesMsg").hide();
                }

            } else if (this.$el.find(".correlationQueryType").val() === "none") {
                showWarning = false;
                this.$el.find(".correlationQueryChangesMsg").hide();

            }  else if (this.$el.find(".correlationQueryType").val() === "script") {
                this.$el.find(".correlationQueryChangesMsg").hide();

                if(this.correlationScript.generateScript()) {
                    showWarning = false;
                }
            }

            this.$el.find(".saveCorrelationQuery").prop('disabled', showWarning);
            this.$el.find(".resetCorrelationQuery").prop('disabled', showWarning);
        },

        undoChange: function(e) {
            var linkQualifier = $(e.target).closest("tr").find(".linkQualifierLabel").text(),
                changesQuery = _.find(this.model.changes, {"linkQualifier": linkQualifier}),
                changesIndex = _.indexOf(this.model.changes, changesQuery);

            this.model.changes.splice(changesIndex, 1);

            this.reload();
        },

        reload: function() {
            this.render({
                changes: this.model.changes
            });
        },

        editLinkQualifier: function(e) {
            if (!$(e.target).parent().parent().hasClass("disabled")) {
                var linkQualifier = $(e.target).closest("tr").find(".linkQualifierLabel").text();
                this.addEditNewCorrelationQuery(null, linkQualifier);
            }
        },

        changeCorrelationQueryType: function() {
            this.checkButtons();

            if (this.$el.find(".correlationQueryType").val() === "queries") {
                this.$el.find(".correlationQueries").show();
                this.$el.find("#correlationScriptBody").hide();
            } else if (this.$el.find(".correlationQueryType").val() === "script") {
                this.$el.find(".correlationQueries").hide();
                this.$el.find("#correlationScriptBody").show();
            } else {
                this.$el.find(".correlationQueries").hide();
                this.$el.find("#correlationScriptBody").hide();
            }
        },

        deleteLinkQualifier: function(e) {
            if (!$(e.target).parent().parent().hasClass("disabled")) {
                var linkQualifier = $(e.target).closest("tr").find(".linkQualifierLabel").text(),
                    correlationQuery = _.find(this.model.mapping.correlationQuery, {"linkQualifier": linkQualifier}),
                    correlationQueryIndex = _.indexOf(this.model.mapping.correlationQuery, correlationQuery),
                    changesQuery = _.find(this.model.changes, {"linkQualifier": linkQualifier}),
                    changesIndex = _.indexOf(this.model.changes, changesQuery);

                if (correlationQueryIndex >= 0) {
                    this.model.changes.push(_.extend(_.clone(this.model.mapping.correlationQuery[correlationQueryIndex], true), {"changes": "delete"}));
                } else if (changesIndex >= 0) {
                    this.model.changes.splice(changesIndex, 1);
                }

                this.reload();
            }
        },

        addEditNewCorrelationQuery: function(e, linkQualifier) {
            // There are no more linkQualifiers so we can't add a correlation Query
            if (this.data.noLinkQualifiers && !linkQualifier) {
                return false;
            }

            var correlationQuery = _.find(this.model.mapping.correlationQuery, {"linkQualifier": linkQualifier}),
                added = _.find(this.model.changes, {"linkQualifier": linkQualifier}),
                query = added || correlationQuery;

            CorrelationQueryDialog.render({
                query: _.clone(query, true),
                mapping: _.clone(this.model.mapping, true),
                mappingName: this.model.mappingName,
                linkQualifiers: this.model.linkQualifiers,
                addedLinkQualifiers: this.model.addedLinkQualifiers,
                linkQualifier: linkQualifier || null,
                edit: _.isString(linkQualifier)

            }, _.bind(function(data) {
                var added = _.find(this.model.changes, {"linkQualifier": data.linkQualifier}),
                    addedIndex = _.indexOf(this.model.changes, added);


                if (!added && _.isString(linkQualifier)) {
                    this.model.changes.push(_.extend(data, {"changes": "edit"}));
                } else if (!added) {
                    this.model.changes.push(_.extend(data, {"changes": "add"}));
                } else if (added && _.find(this.model.mapping.correlationQuery, {"linkQualifier": data.linkQualifier})) {
                    this.model.changes[addedIndex] = _.extend(data, {"changes": "edit"});
                } else if (added) {
                    this.model.changes[addedIndex] = _.extend(data, {"changes": "add"});
                }

                this.reload();
            }, this));
        },

        save: function(callback) {
            var edited,
                editedIndex,
                scriptDetails;

            if (this.$el.find(".correlationQueryType").val() === "queries") {
                _.each(this.model.changes, function (change) {
                    switch (change.changes) {
                        case "add":
                            this.model.mapping.correlationQuery.push(_.omit(change, "deleted", "added", "edited", "changes"));
                            break;

                        case "edit":
                            edited = _.find(this.model.mapping.correlationQuery, {"linkQualifier": change.linkQualifier});
                            editedIndex = _.indexOf(this.model.mapping.correlationQuery, edited);

                            this.model.mapping.correlationQuery[editedIndex] = _.omit(change, "deleted", "added", "edited", "changes");
                            break;

                        case "delete":
                            edited = _.find(this.model.mapping.correlationQuery, {"linkQualifier": change.linkQualifier});
                            editedIndex = _.indexOf(this.model.mapping.correlationQuery, edited);

                            this.model.mapping.correlationQuery.splice(editedIndex, 1);
                            break;
                    }
                }, this);

            }

            if (this.$el.find(".correlationQueryType").val() === "none") {
                if (_.has(this.model.mapping, "correlationQuery")) {
                    delete this.model.mapping.correlationQuery;
                }

                if (_.has(this.model.mapping, "correlationScript")) {
                    delete this.model.mapping.correlationScript;
                }

            } else if($(".correlationQueryType").val() === "script") {
                scriptDetails = this.correlationScript.generateScript();

                if (_.has(this.model.mapping, "correlationQuery")) {
                    delete this.model.mapping.correlationQuery;
                }

                if (scriptDetails !== null) {
                    this.model.mapping.correlationScript = this.correlationScript.generateScript();
                } else if (this.model.mapping.correlationScript !== undefined) {
                    delete this.model.mapping.correlationScript;
                }
            }

            this.AbstractMappingSave(this.model.mapping, _.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "correlationQuerySaveSuccess");

                this.model.changes = [];

                this.reload();

                if (callback) {
                    callback();
                }
            }, this));
        },

        resetQuery: function() {
            var _this = this;

            BootstrapDialog.show({
                title: $.t("templates.correlation.resetTitle"),
                type: BootstrapDialog.TYPE_DEFAULT,
                message: $("<div id='dialogDetails'>" + $.t("templates.correlation.resetMsg") + "</div>"),
                onshown : function (dialogRef) {

                },
                buttons: [
                    {
                        label: $.t("common.form.cancel"),
                        action: function(dialogRef) {
                            dialogRef.close();
                        }
                    },
                    {
                        label: $.t("common.form.reset"),
                        cssClass: "btn-primary",
                        action: function(dialogRef){
                            _this.model.changes = [];
                            _this.reload();
                            dialogRef.close();
                        }
                    }]
            });
        },

        saveQuery: function() {
            var changes = [
                    {"category": $.t("templates.correlation.added"), values:[]},
                    {"category": $.t("templates.correlation.edited"), values:[]},
                    {"category": $.t("templates.correlation.deleted"), values:[]}
                ],
                _this = this;

            _.each(this.model.changes, function(change){
                switch(change.changes) {
                    case "add":
                        changes[0].values.push(change.linkQualifier);
                        break;
                    case "edit":
                        changes[1].values.push(change.linkQualifier);
                        break;
                    case "delete":
                        changes[2].values.push(change.linkQualifier);
                        break;
                }
            });

            _.each(changes, function(change) {
                change.values = change.values.join(", ");
            });


            BootstrapDialog.show({
                title: $.t("templates.correlation.save"),
                type: BootstrapDialog.TYPE_DEFAULT,
                message: $("<div id='dialogDetails'>" + $.t("templates.correlation.resetMsg") + "</div>"),
                onshown : function (dialogRef) {
                    if ($(".correlationQueryType").val() === "none" || $(".correlationQueryType").val() === "script") {
                        changes = null;
                    }

                    SaveChangesView.render({"id": "dialogDetails", "msg": $.t("templates.correlation.note"), "changes": changes, "empty": $.t("templates.correlation.empty")});
                },
                buttons: [
                    {
                        label: $.t("common.form.cancel"),
                        action: function(dialogRef){
                            dialogRef.close();
                        }
                    },
                    {
                        label: $.t("templates.correlation.runReconcile"),
                        cssClass: "btn-primary",
                        action: function(dialogRef) {
                            _this.save(_.bind(function() {
                                _this.model.startSync();
                                dialogRef.close();
                            }, this));
                        }
                    },
                    {
                        label: $.t("templates.correlation.dontRunReconcile"),
                        cssClass: "btn-primary",
                        action: function(dialogRef){
                            _this.save();
                            dialogRef.close();
                        }
                    }]
            });
        }
    });

    return new AssociationRuleView();
});

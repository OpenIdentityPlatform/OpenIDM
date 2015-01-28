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

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/sync/CorrelationQueryView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/sync/CorrelationQueryDialog",
    "org/forgerock/openidm/ui/admin/delegates/BrowserStorageDelegate"
], function(AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            CorrelationQueryDialog,
            BrowserStorageDelegate) {

    var CorrelationQueryView = AdminAbstractView.extend({
        template: "templates/admin/sync/CorrelationQueryTemplate.html",
        element: "#correlationQueryView",
        noBaseTemplate: true,
        events: {
            "click #addNewCorrelationQuery": "addEditNewCorrelationQuery",
            "click .edit": "editLinkQualifier",
            "click .trash": "deleteLinkQualifier",
            "change .correlationQueryType": "changeCorrelationQueryType",
            "click .saveCorrelationQuery": "saveQuery"
        },
        data: {
            linkQualifiers:[]
        },
        model: {},

        render: function(args) {
            this.model.sync = args.sync;
            this.model.mapping = args.mapping;
            this.model.mappingName = args.mappingName;
            this.model.startSync = args.startSync;
            this.data.linkQualifiers = _.pluck(args.mapping.correlationQuery, "linkQualifier");

            // Legacy Support
            if(_.has(this.model.mapping, "correlationQuery") && !_.isArray(this.model.mapping.correlationQuery)) {
                args.mapping.correlationQuery.linkQualifier = $.t("templates.correlation.legacy");
                this.model.mapping.correlationQuery = [args.mapping.correlationQuery];
                this.data.linkQualifiers = _.pluck(this.model.mapping.correlationQuery, "linkQualifier");
            }

            this.parentRender(function () {
                if(_.has(this.model.mapping, "correlationQuery")) {
                    this.$el.find(".correlationQueryType").val("queries");
                } else {
                    this.model.mapping.correlationQuery = [];
                    this.changeCorrelationQueryType();
                }
            });
        },

        editLinkQualifier: function(e) {
            var linkQualifier = $(e.target).parent().find(".linkQualifierName").text();
            this.addEditNewCorrelationQuery(null, linkQualifier);
        },

        changeCorrelationQueryType: function() {
            if (this.$el.find(".correlationQueryType").val() === "queries") {
                this.$el.find(".correlationQueries").show();
            } else {
                this.$el.find(".correlationQueries").hide();
            }
        },

        deleteLinkQualifier: function(e) {
            var linkQualifier = $(e.target).parent().find(".linkQualifierName").text(),
                correlationQuery = _.find(this.model.mapping.correlationQuery, {"linkQualifier": linkQualifier}),
                correlationQueryIndex = _.indexOf(this.model.mapping.correlationQuery, correlationQuery);

            this.model.mapping.correlationQuery.splice(correlationQueryIndex, 1);

            this.render({
                sync: this.model.sync,
                mapping: this.model.mapping,
                mappingName: this.model.mappingName,
                startSync: this.model.startSync
            });
        },

        addEditNewCorrelationQuery: function(e, linkQualifier) {
            CorrelationQueryDialog.render({
                mapping: this.model.mapping,
                mappingName: this.model.mappingName,
                linkQualifiers: this.data.linkQualifiers,
                linkQualifier: linkQualifier || null,
                edit: _.isString(linkQualifier)
            }, _.bind(function(data) {
                var correlationQuery = _.find(this.model.mapping.correlationQuery, {"linkQualifier": data.linkQualifier}),
                    correlationQueryIndex = _.indexOf(this.model.mapping.correlationQuery, correlationQuery);

                if (correlationQueryIndex > -1) {
                    this.model.mapping.correlationQuery[correlationQueryIndex] = data;
                } else {
                    this.model.mapping.correlationQuery.push(data);
                }

                this.render({
                    sync: this.model.sync,
                    mapping: this.model.mapping,
                    mappingName: this.model.mappingName
                });
            }, this));
        },

        save: function(callback) {
            _.each(this.model.sync.mappings, function(map, key) {
                if ($(".correlationQueryType").val() === "queries" && map.name === this.model.mappingName) {
                    this.model.sync.mappings[key].correlationQuery = this.model.mapping.correlationQuery;
                } else if (_.has(this.model.sync.mappings[key], "correlationQuery")) {
                    delete this.model.sync.mappings[key].correlationQuery;
                }
            }, this);

            ConfigDelegate.updateEntity("sync", this.model.sync).then(_.bind(function() {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "correlationQuerySaveSuccess");
                BrowserStorageDelegate.set("currentMapping", _.extend(this.model.mapping, this.model.recon));

                if (callback) {
                    callback();
                }
            }, this));
        },

        saveQuery: function() {
            var btns = {};

            btns[$.t("templates.correlation.runReconcile")] = _.bind(function() {
                this.save(_.bind(function() {
                    this.model.startSync();
                }, this));
                $("#jqConfirm").dialog("close");
            }, this);

            btns[$.t("templates.correlation.dontRunReconcile")] = _.bind(function() {
                this.save();
                $("#jqConfirm").dialog("close");
            }, this);

            btns[$.t("common.form.cancel")] = function() {
                $(this).dialog("close");
            };

            $("<div id='jqConfirm'>" + $.t("templates.correlation.note") + "</div>")
                .dialog({
                    title: "Save and Reconcile",
                    modal: true,
                    resizable: false,
                    width: "550px",
                    buttons: btns,
                    close: function() {
                        $('#jqConfirm').dialog('destroy').remove();
                    }
                });
        }
    });

    return new CorrelationQueryView();
});

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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "backbone",
    "backgrid",
    "handlebars",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/admin/authentication/AuthenticationModuleDialogView",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils"
], function($, _,
            Backbone,
            Backgrid,
            Handlebars,
            Constants,
            AuthenticationAbstractView,
            AuthenticationModuleDialogView,
            BackgridUtils) {

    var AuthenticationModuleView = AuthenticationAbstractView.extend({
        template: "templates/admin/authentication/AuthenticationModuleTemplate.html",
        element: "#modulesContainer",
        noBaseTemplate: true,
        events: {
            "click .add-auth-module": "addAuthModule",
            "change #moduleType": "checkAddButton"
        },
        partials: [
            "partials/authentication/_authenticationModuleRow.html",
            "partials/_alert.html"
        ],
        data: {
            module_types: [
                {"key": "STATIC_USER", "value": ""},
                {"key": "CLIENT_CERT", "value": ""},
                {"key": "IWA", "value": ""},
                {"key": "MANAGED_USER", "value": ""},
                {"key": "OPENAM_SESSION", "value": ""},
                {"key": "INTERNAL_USER", "value": ""},
                {"key": "OAUTH", "value": ""},
                {"key": "OPENID_CONNECT", "value": ""},
                {"key": "SOCIAL_PROVIDERS", "value": ""},
                {"key": "PASSTHROUGH", "value": ""},
                {"key": "TRUSTED_ATTRIBUTE", "value": ""}
            ],
            "docHelpUrl": Constants.DOC_URL
        },

        /**
         * @param configs {object}
         * @param configs.addedOpenAM {function}
         * @param callback
         */
        render: function (showWarning, callback) {
            var self = this;

            this.data.hideWarning = !(showWarning || false);
            this.model = _.extend(
                {},
                this.getAuthenticationData()
            );

            this.model.authModulesClean = _.clone(this.model.authModules, true);

            // Translate the name of the modules
            _.each(this.data.module_types, _.bind(function(module, index) {
                this.data.module_types[index].value = $.t("templates.auth.modules." + this.data.module_types[index].key + ".name");
            }, this));
            this.data.module_types = _.sortBy(this.data.module_types, "key");

            this.parentRender(_.bind(function() {
                this.model.authModulesCollection = new Backbone.Collection();

                _.each(this.model.authModules, _.bind(function (module) {
                    this.model.authModulesCollection.add(module);
                }, this));

                var authModuleGrid = new Backgrid.Grid({
                    className: "table backgrid",
                    row: BackgridUtils.ClickableRow.extend({
                        callback: _.bind(function(e) {
                            e.preventDefault();
                            self.editAuthModule(e);
                        }, this)
                    }),
                    columns: BackgridUtils.addSmallScreenCell([
                        {
                            label: $.t("templates.auth.modules.gridLabel"),
                            cell: Backgrid.Cell.extend({
                                className: "col-sm-9",
                                render: function () {
                                    this.$el.html(Handlebars.compile("{{> authentication/_authenticationModuleRow}}")({
                                        "disabled": this.model.get("enabled") === false,
                                        "name": $.t("templates.auth.modules."+ this.model.get("name") + ".name"),
                                        "resource": this.model.get("properties").queryOnResource,
                                        "msg": $.t("templates.auth.modules."+ this.model.get("name") + ".msg")
                                    }));
                                    return this;
                                }
                            }),
                            sortable: true,
                            editable: false
                        }, {
                            label: "",
                            sortable: false,
                            editable: false,
                            cell: BackgridUtils.ButtonCell([
                                {
                                    className: "fa fa-times grid-icon col-sm-1 pull-right",
                                    callback: function(e){
                                        self.model.authModules.splice(self.getClickedRowIndex(e), 1);
                                        self.saveChanges();
                                        self.render();
                                    }
                                }, {
                                    // No callback necessary, the row click will trigger the edit
                                    className: "fa fa-pencil grid-icon col-sm-1 pull-right"
                                }, {
                                    className: "dragToSort fa fa-arrows grid-icon col-sm-1 pull-right"
                                }
                            ])
                        }]),
                    collection: this.model.authModulesCollection
                });

                this.$el.find("#authModuleGrid").append(authModuleGrid.render().el);

                this.makeSortable();

                this.$el.find(".auth-message").popover({
                    content: function () {
                        return $(this).attr("data-title");
                    },
                    placement: 'top',
                    container: 'body',
                    html: 'true',
                    title: ''
                });

                this.$el.find("#moduleType").selectize({
                    render: {
                        option: function(item, selectizeEscape) {
                            var element = $('<div class="fr-search-option"></div>');

                            $(element).append('<div class="fr-search-primary">' +selectizeEscape(item.text) +'</div>');
                            $(element).append('<div class="fr-search-secondary text-muted">' + $.t("templates.auth.modules." + item.value + ".msg") + '</div>');

                            return element.prop('outerHTML');
                        }
                    }
                });

                if (callback) {
                    callback();
                }

            }, this));
        },

        makeSortable: function() {
            BackgridUtils.sortable({
                "containers": [this.$el.find("#authModuleGrid tbody")[0]],
                "rows": _.clone(this.model.authModules, true)
            }, _.bind(function(newOrder) {
                this.model.authModules = newOrder;
                this.saveChanges();
            }, this));
        },

        saveChanges: function() {
            this.setProperties(["authModules"], {"authModules": this.model.authModules});
            this.saveAuthentication().then(() => {
                this.render(true);
            });
        },

        editAuthModule: function(e) {
            AuthenticationModuleDialogView.render({
                "config": this.model.authModules[this.getClickedRowIndex(e)],
                "saveCallback": _.bind(function(config) {
                    this.model.authModules[this.getClickedRowIndex(e)] = config;
                    this.saveChanges();
                }, this)
            }, _.noop);
        },

        addAuthModule: function(e) {
            if (e) {
                e.preventDefault();
            }

            var newModule = this.$el.find("#moduleType").val();

            if (!_.isNull(newModule)) {
                AuthenticationModuleDialogView.render({
                    "config": {
                        "name": newModule,
                        "enabled": true,
                        "properties": {}
                    },
                    "newModule": true,
                    "saveCallback": _.bind(function (config) {
                        this.model.authModules.push(config);
                        this.saveChanges();
                    }, this)
                }, _.noop);
            }
        },

        checkAddButton: function(e) {
            if (e) {
                e.preventDefault();
            }
            var newModule = this.$el.find("#moduleType").val();
            this.$el.find(".add-auth-module").prop("disabled", _.isNull(newModule));
        },

        getClickedRowIndex: function(e) {
            var index = -1;

            _.each($(e.currentTarget).closest("table tbody").find("tr"), function(tr, i) {
                if (tr === $(e.currentTarget).closest("tr")[0]) {
                    index = i;
                }
            });

            return index;
        }
    });

    return new AuthenticationModuleView();
});

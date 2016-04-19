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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationModuleView", [
    "jquery",
    "underscore",
    "backbone",
    "backgrid",
    "handlebars",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationAbstractView",
    "org/forgerock/openidm/ui/admin/settings/authentication/AuthenticationModuleDialogView",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/components/ChangesPending"

], function($, _,
            Backbone,
            Backgrid,
            Handlebars,
            Configuration,
            AuthenticationAbstractView,
            AuthenticationModuleDialogView,
            BackgridUtils,
            ChangesPending) {

    var AuthenticationModuleView = AuthenticationAbstractView.extend({
        template: "templates/admin/settings/authentication/AuthenticationModuleTemplate.html",
        element: "#authenticationModuleView",
        noBaseTemplate: true,
        events: {
            "click .add-auth-module": "addAuthModule",
            "change #moduleType": "checkAddButton"
        },
        partials: [
            "partials/settings/_authenticationModuleRow.html"
        ],
        data: {
            module_types: {
                "STATIC_USER": null,
                "CLIENT_CERT": null,
                "IWA": null,
                "MANAGED_USER": null,
                "OPENAM_SESSION": null,
                "INTERNAL_USER": null,
                "OPENID_CONNECT": null,
                "PASSTHROUGH": null,
                "TRUSTED_ATTRIBUTE": null
            },
            amUIProperties: [
                "openamLoginUrl",
                "openamLoginLinkText",
                "openamUseExclusively"
            ],
            amTruststoreType : "&{openidm.truststore.type}",
            amTruststorePath : "&{openidm.truststore.location}",
            amTruststorePassword : "&{openidm.truststore.password}"
        },

        /**
         * @param configs {object}
         * @param configs.addedOpenAM {function}
         * @param callback
         */
        render: function (configs, callback) {
            var self = this;

            this.model = _.extend(
                {},
                configs,
                this.getAuthenticationData()
            );

            this.addOpenAMUISettings();

            // this.model.authModules should not be altered until a save is done.  Use this.model.changes for the local copy.
            if (!_.has(this.model, "changes")) {
                this.model.changes = _.clone(this.model.authModules);
            }

            // Translate the name of the modules
            _.each(this.data.module_types, _.bind(function(module, key) {
                this.data.module_types[key] = $.t("templates.auth.modules." + key + ".name");
            }, this));

            this.parentRender(_.bind(function() {
                this.model.authModulesCollection = new Backbone.Collection();
                this.model.defaultAuth = _.clone(this.model.changes, true);

                _.each(this.model.changes, _.bind(function (module) {
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
                            label: "",
                            cell: Backgrid.Cell.extend({
                                className: "col-sm-9",
                                render: function () {
                                    this.$el.html(Handlebars.compile("{{> settings/_authenticationModuleRow}}")({
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
                                        self.model.changes.splice(self.getClickedRowIndex(e), 1);
                                        self.render(self.model);
                                        self.checkChanges();
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

                if (!_.has(this.model, "changesModule")) {
                    this.model.changesModule = ChangesPending.watchChanges({
                        element: this.$el.find(".authentication-module-changes"),
                        undo: true,
                        watchedObj: this.model,
                        watchedProperties: ["authModules"],
                        undoCallback: _.bind(function (original) {
                            this.model.changes = original.authModules;
                            this.render(this.model);
                            this.checkChanges();
                        }, this)
                    });
                } else {
                    this.model.changesModule.reRender(this.$el.find(".authentication-module-changes"));
                }

                if (callback) {
                    callback();
                }

            }, this));
        },

        makeSortable: function() {
            BackgridUtils.sortable({
                "containers": [this.$el.find("#authModuleGrid tbody")[0]],
                "rows": _.clone(this.model.changes, true)
            }, _.bind(function(newOrder) {
                this.model.changes = newOrder;
                this.checkChanges();
            }, this));
        },

        checkChanges: function() {
            this.checkHasAM();
            this.setProperties(["authModules"], {"authModules": this.model.changes});
            this.model.changesModule.makeChanges({"authModules": this.model.changes}, true);
        },

        /**
         * If an AM session module is present the session configurations may need to be altered
         */
        checkHasAM: function() {
            var amExists = _.findWhere(this.model.changes, {"name": "OPENAM_SESSION", "enabled": true});

            if (amExists) {
                this.model.addedOpenAM();
            }
        },

        editAuthModule: function(e) {
            AuthenticationModuleDialogView.render({
                "config": this.model.changes[this.getClickedRowIndex(e)],
                "saveCallback": _.bind(function(config) {
                    this.model.changes[this.getClickedRowIndex(e)] = config;
                    this.render(this.model);
                    this.checkChanges();
                    this.handleOpenAMUISettings(config);
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
                    "config": {"name": newModule, "properties": {}},
                    "newModule": true,
                    "saveCallback": _.bind(function (config) {
                        this.model.changes.push(config);
                        this.render(this.model);
                        this.checkChanges();
                        this.handleOpenAMUISettings(config);
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
        },

        handleOpenAMUISettings: function (config) {
            var prom = $.Deferred(),
                amAuthIndex = _.findIndex(this.model.changes, { name: "OPENAM_SESSION" }),
                amSettings = _.pick(config.properties, this.data.amUIProperties, "openamDeploymentUrl");

            amSettings.openamAuthEnabled = config.enabled;
            delete amSettings.enabled;

            this.model.amSettings = amSettings;

            //before saving these properties need to be changed back to the untranslated versions
            this.model.changes[amAuthIndex].properties.truststoreType = this.data.amTruststoreType;
            this.model.changes[amAuthIndex].properties.truststorePath = this.data.amTruststorePath;
            this.model.changes[amAuthIndex].properties.truststorePassword = this.data.amTruststorePassword;
        },

        addOpenAMUISettings: function () {
            var amAuthIndex = _.findIndex(this.model.authModules, { name: "OPENAM_SESSION" });

            if (!this.model.changes && amAuthIndex >= 0) {
                //add amUIProperties
                this.model.authModules[amAuthIndex].properties = _.extend(
                    this.model.authModules[amAuthIndex].properties,
                    _.pick(Configuration.globalData, this.data.amUIProperties)
                );
            }
        }

    });

    return new AuthenticationModuleView();
});

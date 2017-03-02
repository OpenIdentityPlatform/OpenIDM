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
 * Copyright 2016-2017 ForgeRock AS.
 */

define([
    "jquery",
    "backbone",
    "lodash",
    "handlebars",
    "form2js",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SiteConfigurationDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/util/AdminUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/selfservice/SelfServiceStageDialogView",
    "org/forgerock/openidm/ui/common/delegates/SocialDelegate",
    "org/forgerock/openidm/ui/admin/mapping/properties/AttributesGridView",
    "org/forgerock/openidm/ui/common/util/oAuthUtils",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "selectize",
    "bootstrap-tabdrop"
], function($, Backbone, _,
            Handlebars,
            form2js,
            Backgrid,
            AdminAbstractView,
            ConfigDelegate,
            SiteConfigurationDelegate,
            UiUtils,
            AdminUtils,
            EventManager,
            Constants,
            SelfServiceStageDialogView,
            SocialDelegate,
            AttributesGridView,
            OAuthUtils,
            BackgridUtils) {

    var UserRegistrationConfigView = AdminAbstractView.extend({
        template: "templates/admin/selfservice/UserRegistrationConfigTemplate.html",
        events: {
            "click .all-check" : "controlAllSwitch",
            "change .section-check" : "controlSectionSwitch",
            "click .save-config" : "saveConfig",
            "click .wide-card.active" : "showDetailDialog",
            "click #configureCaptcha": "configureCaptcha",
            "click #enableRegistrationModal" : "openRegistrationDetails",
            "click .add-registration-field" : "addRegistrationField"
        },
        partials: [
            "partials/_toggleIconBlock.html",
            "partials/selfservice/_registrationFormCell.html"
        ],
        model: {
            emailServiceAvailable: false,
            dragAndDropGridList: [],
            surpressSave: false,
            uiConfigurationParameter: "selfRegistration",
            configUrl: "selfservice/registration",
            accountClaimUrl: "selfservice/socialUserClaim",
            msgType: "selfServiceUserRegistration",
            codeMirrorConfig: {
                lineNumbers: true,
                autofocus: false,
                viewportMargin: Infinity,
                theme: "forgerock",
                mode: "xml",
                htmlMode: true,
                lineWrapping: true
            },
            configList: [],
            "configAccountClaimDefault" : {
                "stageConfigs" : [
                    {
                        "name" : "socialUserClaim",
                        "identityServiceUrl" : "managed/user",
                        "claimQueryFilter" : "/mail eq \"{{mail}}\""
                    }
                ],
                "snapshotToken" : {
                    "type" : "jwt",
                    "jweAlgorithm" : "RSAES_PKCS1_V1_5",
                    "encryptionMethod" : "A128CBC_HS256",
                    "jwsAlgorithm" : "HS256",
                    "tokenExpiry" : "1800"
                },
                "storage" : "stateless"
            },
            "configDefault": {
                "allInOneRegistration" : true,
                "stageConfigs" : [
                    {
                        "name" : "idmUserDetails",
                        "identityEmailField" : "mail",
                        "socialRegistrationEnabled" : false,
                        "registrationProperties" : [
                            "userName",
                            "givenName",
                            "sn",
                            "mail"
                        ]
                    },
                    {
                        "name" : "termsAndConditions",
                        "termsTranslations" : {
                            "en" : "Some fake terms",
                            "fr" : "More fake terms"
                        }
                    },
                    {
                        "name" : "captcha",
                        "recaptchaSiteKey": "",
                        "recaptchaSecretKey": "",
                        "recaptchaUri" : "https://www.google.com/recaptcha/api/siteverify"
                    },
                    {
                        "name" : "emailValidation",
                        "identityEmailField" : "mail",
                        "emailServiceUrl": "external/email",
                        "emailServiceParameters" : {
                            "waitForCompletion" : false
                        },
                        "from" : "info@admin.org",
                        "subject" : "Register new account",
                        "mimeType" : "text/html",
                        "subjectTranslations" : {
                            "en" : "Register new account",
                            "fr" : "Créer un nouveau compte"
                        },
                        "messageTranslations" : {
                            "en" : "<h3>This is your registration email.</h3><h4><a href=\"%link%\">Email verification link</a></h4>",
                            "fr" : "<h3>Ceci est votre mail d'inscription.</h3><h4><a href=\"%link%\">Lien de vérification email</a></h4>"
                        },
                        "verificationLinkToken" : "%link%",
                        "verificationLink" : "https://localhost:8443/#register/"
                    },
                    {
                        "name" : "kbaSecurityAnswerDefinitionStage",
                        "numberOfAnswersUserMustSet": 1,
                        "kbaConfig" : null
                    },
                    {
                        "name" : "selfRegistration",
                        "identityServiceUrl" : "managed/user"
                    }
                ],
                "snapshotToken" : {
                    "type" : "jwt",
                    "jweAlgorithm" : "RSAES_PKCS1_V1_5",
                    "encryptionMethod" : "A128CBC_HS256",
                    "jwsAlgorithm" : "HS256",
                    "tokenExpiry" : 1800
                },
                "storage" : "stateless"
            },
            "saveConfig": {},
            data: {
                config: {},
                emailRequired: false
            }
        },
        initDefaultConfig: function () {
            this.model.saveConfig = {};
            $.extend(true, this.model.saveConfig, this.model.configDefault);
        },
        render: function(args, callback) {
            //List broken for subsection consumption by the UI
            this.data.registrationOptions = [{
                type: "captcha",
                name: $.t("templates.selfservice.user.captchaTitle"),
                details: $.t("templates.selfservice.captcha.description"),
                editable: true,
                off: false,
                togglable: true,
                displayIcon: "shield"
            }, {
                type: "emailValidation",
                name: $.t("templates.selfservice.emailValidation"),
                details: $.t("templates.selfservice.emailValidationDescription"),
                editable: true,
                off: false,
                togglable: true,
                displayIcon: "envelope"
            }, {
                type: "kbaSecurityAnswerDefinitionStage",
                name: $.t("templates.selfservice.kbaSecurityAnswerDefinitionStageTitle"),
                details: $.t("templates.selfservice.kbaSecurityAnswerDefinitionStageHelp"),
                editable: true,
                off: false,
                togglable: true,
                displayIcon: "list-ol"
            }, {
                type: "termsAndConditions",
                name: $.t("templates.selfservice.termsAndConditions.title"),
                details: $.t("templates.selfservice.termsAndConditions.details"),
                editable: true,
                off: false,
                togglable: true,
                displayIcon: "file-text-o"
            }];

            //Master config list for controlling various states such as what is editable and what is enabled by default when turned on
            this.model.configList = [{
                type : "termsAndConditions",
                enabledByDefault: false,
                toggledOn: false
            }, {
                type: "captcha",
                enabledByDefault: false,
                toggledOn: false
            }, {
                type: "emailValidation",
                enabledByDefault: true,
                toggledOn: false
            }, {
                type: "kbaSecurityAnswerDefinitionStage",
                enabledByDefault: true,
                toggledOn: false
            }, {
                type : "selfRegistration",
                toggledOn: true
            }, {
                type: "socialUserDetails",
                enabledByDefault: false,
                toggledOn: true
            }];

            this.data.advancedConfig = {};

            $.when(
                this.getResources(),
                ConfigDelegate.readEntity("selfservice.propertymap"),
                ConfigDelegate.readEntity("ui/configuration"),
                ConfigDelegate.readEntityAlways(this.model.configUrl),
                ConfigDelegate.readEntityAlways("external.email"),
                ConfigDelegate.readEntity("managed"),
                SocialDelegate.providerList()
            ).then(_.bind(function(resources, propertyMap, uiConfig, selfServiceConfig, emailConfig, managedConfig, availableProviders) {
                this.model.propertyMap = propertyMap;
                this.model.emailServiceAvailable = !_.isUndefined(emailConfig) && _.has(emailConfig, "host");
                this.model.resources = resources;
                this.model.uiConfig = uiConfig;
                this.model.managed = managedConfig.objects;

                availableProviders.providers = OAuthUtils.setDisplayIcons(availableProviders.providers);

                this.data.socialProviders = {
                    providerList : availableProviders.providers,
                    type: "socialUserDetails",
                    editable: false,
                    togglable: true
                };

                if(!this.model.emailServiceAvailable) {
                    _.each(this.data.registrationOptions, (option) => {
                        if(option.type === "emailValidation") {
                            option.off = true;
                            option.offMessage = $.t("templates.selfservice.noEmailConfig");
                            option.offLink = "#emailsettings/";
                        }
                    });
                }

                if (selfServiceConfig) {
                    let tempIdmUserDetails;

                    this.data.identityServiceUrl = _.get(_.filter(selfServiceConfig.stageConfigs, {
                        "name": "selfRegistration"
                    })[0], "identityServiceUrl");

                    this.model.saveConfig = {};

                    $.extend(true, this.model.saveConfig, selfServiceConfig);

                    tempIdmUserDetails = _.find(this.model.saveConfig.stageConfigs, {
                        "name": "idmUserDetails"
                    });

                    this.model.registrationProperties = _.clone(tempIdmUserDetails.registrationProperties);

                    this.data.enableSelfService = true;
                    this.data.advancedConfig.snapshotToken = selfServiceConfig.snapshotToken;

                    this.model.currentManagedObject = this.findManagedSchema(this.model.managed, this.data.identityServiceUrl);

                    this.parentRender(_.bind(function () {
                        this.renderAttributeGrid();
                        this.renderRegistrationProperties(this.model.currentManagedObject, this.model.registrationProperties);

                        this.$el.find(".all-check").prop("checked", true);

                        this.model.surpressSave = true;

                        //Set UI to stages to current active and available stages
                        _.each(selfServiceConfig.stageConfigs, function (stage) {
                            this.$el.find(".wide-card[data-type='" + stage.name + "']").toggleClass("disabled", false);

                            if(stage.name === "idmUserDetails" && stage.socialRegistrationEnabled) {
                                this.$el.find(".wide-card[data-type='socialUserDetails'] .section-check").prop("checked", true).trigger("change");
                            } else {
                                this.$el.find(".wide-card[data-type='" + stage.name + "'] .section-check").prop("checked", true).trigger("change");
                            }
                        }, this);

                        this.model.surpressSave = false;

                        this.$el.find(".nav-tabs").tabdrop();

                        if (callback) {
                            callback();
                        }
                    }, this));

                } else {
                    let registrationProperties = _.get(_.filter(this.model.configDefault.stageConfigs, {
                        "name": "idmUserDetails"
                    })[0], "registrationProperties");

                    this.data.identityServiceUrl = _.get(_.filter(this.model.configDefault.stageConfigs, {
                        "name": "selfRegistration"
                    })[0], "identityServiceUrl");

                    this.initDefaultConfig();

                    this.data.advancedConfig.snapshotToken = this.model.configDefault.snapshotToken;

                    this.data.enableSelfService = false;

                    //Finds current managed object
                    this.model.currentManagedObject = this.findManagedSchema(this.model.managed, this.data.identityServiceUrl);

                    this.parentRender(_.bind(function () {
                        //Feed it list built off of current + missing required schema items
                        this.renderRegistrationProperties(this.model.currentManagedObject, registrationProperties);

                        this.$el.find("#userRegistrationConfigBody").hide();
                        this.$el.find(".nav-tabs").tabdrop();

                        if (callback) {
                            callback();
                        }
                    }, this));
                }
            }, this));
        },

        /**
         * Finds the current managed object based off of the identity resource, currently if it is a none managed resource it
         * returns an empty object allowing the UI to function
         *
         * @param managedList - managed object list
         * @param source - type of managed object based of of identity resource
         * @returns {*} Finds needed managed object based off of identity resource
         */
        findManagedSchema: function(managedList, source) {
            var managedObject,
                identityUrl = source.split("/");

            if(identityUrl[0] === "managed") {
                managedObject = _.find(managedList, (managedObject) => {
                    return managedObject.name === identityUrl[1];
                });
            } else {
                //For now we catch any non-managed identity resource set the UI into a state of not being able to break
                this.model.registrationProperties = [];
                managedObject = {
                    schema : {
                        properties : [],
                        required: []
                    }
                };
            }

            return _.clone(managedObject, true);
        },

        /**
         * This function generates two lists that are used to configure the display of the grid and current available items for the selectize registration field list
         *
         * @param managedProperties - Full list of managed properties based off the currently set identity resource
         * @param currentProperties - List of currently listed properties for registrationProperties
         * @returns {{listItems: Array, gridItems: Array}}
         */
        generatRegistrationLists: function(managedProperties, currentProperties) {
            var gridItems = [],
                listItems = [];

            if(currentProperties.length) {
                _.each(currentProperties, (prop) => {
                    let tempProperty = _.clone(managedProperties[prop]);

                    tempProperty.displayKey = prop;

                    gridItems.push(tempProperty);

                    delete managedProperties[prop];
                });

                _.each(managedProperties, (value, key) => {
                    let tempProperty = _.clone(managedProperties[key]);

                    if(key !== "_id" && _.isUndefined(tempProperty.encryption) && tempProperty.type === "string" && tempProperty.userEditable === true) {
                        tempProperty.displayKey = key;

                        listItems.push(tempProperty);
                    }
                });
            } else {
                _.each(managedProperties, (value, key) => {
                    let tempProperty = _.clone(managedProperties[key]);

                    if (key !== "_id" && _.isUndefined(tempProperty.encryption) && tempProperty.type === "string" && tempProperty.userEditable === true) {
                        tempProperty.displayKey = key;

                        listItems.push(tempProperty);
                    }
                });
            }

            return {
                "listItems" : listItems,
                "gridItems" : gridItems
            };
        },

        /**
         *  Renders the registration fields grid and selectize
         *
         * @param managedObject - The current managed object used with identity source
         * @param currentProperties - List of the current properties
         */
        renderRegistrationProperties: function(managedObject, currentProperties) {
            var _this = this,
                propertiesLists = this.generatRegistrationLists(managedObject.schema.properties, currentProperties);

            this.model.registrationFieldCollection = new Backbone.Collection();

            _.each(propertiesLists.gridItems, (item) => {
                if(_.indexOf(managedObject.schema.required, item.displayKey) !== -1) {
                    item.required = true;
                }

                this.model.registrationFieldCollection.add(item);
            });

            this.model.registrationFieldGrid = new Backgrid.Grid({
                className: "table backgrid backgrid-count backgrid-with-footer",
                emptyText: $.t("templates.selfservice.noRegistrationFields"),
                columns: BackgridUtils.addSmallScreenCell([
                    {
                        name: "Details",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            className: "col-sm-7",
                            render: function () {
                                var data = {
                                    "count" : this.model.collection.indexOf(this.model) + 1,
                                    "displayName" : this.model.attributes.title,
                                    "name" : this.model.attributes.displayKey
                                };

                                this.$el.html(Handlebars.compile("{{> selfservice/_registrationFormCell}}")(data));

                                return this;
                            }
                        })
                    },
                    {
                        name: "Required",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            className: "required-cell col-sm-4",
                            render: function () {
                                var display = "";

                                if(this.model.attributes.required) {
                                    display = $('<span class="text-success required"><i class="fa fa-check-circle"></i>' +$.t("templates.selfservice.required") +'</span>');
                                }

                                this.$el.html(display);

                                return this;
                            }
                        })
                    },
                    {
                        name: "",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            render: function () {
                                var display = $('<span class="btn-link"><i class="fa fa-arrows grid-icon move-registration-item"></i></span>');

                                this.$el.html(display);

                                return this;
                            }
                        })
                    },
                    {
                        name: "",
                        sortable: false,
                        editable: false,
                        cell: Backgrid.Cell.extend({
                            events: {
                                "click .remove-registration-item" : "removeRegistrationItem"
                            },
                            render: function () {
                                var display;

                                if(this.model.attributes.required) {
                                    display = $('<span class="btn-link disabled-item"><i class="fa fa-times grid-icon"></i></span>');
                                } else {
                                    display = $('<span class="btn-link"><i class="fa fa-times grid-icon remove-registration-item"></i></span>');
                                }

                                this.$el.html(display);

                                this.$el.find(".disabled-item").popover({
                                    content: function () {
                                        return '<span>' +$.t("templates.selfservice.requiredPropertiesMessage")  +'</span>';
                                    },
                                    placement:'top',
                                    container: 'body',
                                    html: 'true',
                                    title: ''
                                });

                                return this;
                            },
                            removeRegistrationItem: function(event) {
                                event.preventDefault();

                                _this.$el.find('.registration-select')[0].selectize.addOption(this.model.attributes);

                                _this.model.registrationProperties = _.reject(_this.model.registrationProperties, (value) => {
                                    return value === this.model.attributes.displayKey;
                                });

                                this.model.collection.remove(this.model);

                                //We need to preserve the dragAndDrop refrence so we must empty and not create a new array
                                _this.model.dragAndDropGridList.length = 0;

                                _.each(_this.model.registrationFieldCollection.models, (model) => {
                                    _this.model.dragAndDropGridList.push(model.attributes);
                                });

                                _this.saveConfig(_this.model.saveConfig);
                            }
                        })
                    }
                ]),
                collection: this.model.registrationFieldCollection
            });

            this.$el.find("#registrationFormBody").append(this.model.registrationFieldGrid.render().el);

            this.model.dragAndDropGridList = _.clone(propertiesLists.gridItems, true);

            //Setup drag and drop
            BackgridUtils.sortable({
                "containers": [this.$el.find("#registrationFormBody tbody")[0]],
                "handlesClassName" : "move-registration-item",
                "rows": this.model.dragAndDropGridList
            },
            (registrationProperties) => {
                this.moveRegistrationItem(registrationProperties);
            });

            //Setup selectize
            this.$el.find('.registration-select').selectize({
                valueField: 'displayKey',
                labelField: 'title',
                searchField: 'displayKey',
                create: false,
                render: {
                    option: function(item, selectizeEscape) {
                        var element = $('<div class="fr-search-option"></div>');

                        $(element).append('<span class="fr-search-primary">' +item.title +'</span>');
                        $(element).append('<span class="fr-search-secondary text-muted"> (' +item.displayKey + ')</span>');

                        return element.prop('outerHTML');
                    }
                }
            });

            //Load selectize
            _.each(propertiesLists.listItems, (property) => {
                this.$el.find('.registration-select')[0].selectize.addOption(property);
            });
        },

        /**
         * Reloads the grid with the newly arranged schema items (based off drag and drop results)
         *
         * @param registrationProperties List of newly arranged schema properties
         */
        moveRegistrationItem : function(registrationProperties) {
            var registrationList = [];

            this.model.registrationFieldCollection.reset(registrationProperties);

            _.each(registrationProperties, (prop) => {
                registrationList.push(prop.displayKey);
            });

            this.model.registrationProperties = registrationList;

            this.saveConfig(this.model.saveConfig);
        },

        /**
         * Adds a registration property to the grid and removes the corresponding property from selectize
         *
         * @param event - Click event from add registration property button
         */
        addRegistrationField: function(event) {
            event.preventDefault();

            var currentItem = this.$el.find('.registration-select').val(),
                currentProperty = _.clone(this.model.currentManagedObject.schema.properties[currentItem]),
                selectize = this.$el.find('.registration-select')[0].selectize;

            if(_.indexOf(this.model.currentManagedObject.schema.required, currentItem) !== -1) {
                currentProperty.required = true;
            }

            currentProperty.displayKey = currentItem;

            this.model.registrationProperties.push(currentItem);

            this.model.registrationFieldCollection.add(currentProperty);

            selectize.removeOption(selectize.getValue());

            this.model.dragAndDropGridList.length = 0;

            _.each(this.model.registrationFieldCollection.models, (model) => {
                this.model.dragAndDropGridList.push(model.attributes);
            });

            this.saveConfig(this.model.saveConfig);
        },

        renderAttributeGrid: function(propertiesList, requiredPropertiesList) {
            var promise = $.Deferred();

            if (!_.isUndefined(propertiesList)) {
                promise.resolve(propertiesList, requiredPropertiesList);
            } else {
                $.when(
                    AdminUtils.findPropertiesList(this.data.identityServiceUrl.split("/")),
                    AdminUtils.findPropertiesList(this.data.identityServiceUrl.split("/"), true)
                ).then(_.bind(function (propList, requiredProperties) {
                    promise.resolve(propList, requiredProperties);
                }, this));
            }

            $.when(promise).then(_.bind(function(propertiesList, requiredPropertiesList) {
                var availableObjects = {
                        source: {
                            fullName: "mapIdpCommonSchemaToManagedUser",
                            name: "mapIdpCommonSchemaToManagedUser",
                            properties: [
                                "honorificPrefix",
                                "givenName",
                                "middleName",
                                "familyName",
                                "honorificSuffix",
                                "fullName",
                                "nickname",
                                "displayName",
                                "title",
                                "email",
                                "postalAddress",
                                "addressLocality",
                                "addressRegion",
                                "postalCode",
                                "country",
                                "phone",
                                "id",
                                "username",
                                "profileUrl",
                                "photoUrl",
                                "preferredLanguage",
                                "locale",
                                "timezone",
                                "active",
                                "rawProfile"
                            ]
                        },
                        target: {
                            fullName: this.data.identityServiceUrl,
                            name: "user",
                            properties: _.chain(propertiesList).keys().sortBy().value()
                        }
                    },

                    staticSourceObject = {
                        IDMSampleMappingName: "mapIdpCommonSchemaToManagedUser",
                        "honorificPrefgitix": "Ms",
                        "givenName": "Emma",
                        "middleName": "Ann",
                        "familyName": "Smith",
                        "honorificSuffix": "Jr",
                        "fullName": "Emma Ann Smith Jr",
                        "nickname": "Em",
                        "displayName": "Emma Smith",
                        "title": "Business Analyst",
                        "email": "Emma.Smith@forgerock.com",
                        "postalAddress": "123 Fake Street",
                        "addressLocality": "San Francisco",
                        "addressRegion": "CA",
                        "postalCode": "94101",
                        "country": "USA",
                        "phone": "+1-555-555-5555",
                        "id": "123456",
                        "username": "Emma.smith",
                        "profileUrl": "http://www.fakesocialprovider.com/123456",
                        "photoUrl": "http://www.fakesocialprovider.com/123456/profilePhoto",
                        "preferredLanguage": "en",
                        "locale": "en_US",
                        "timezone": "UTC−08:00",
                        "active": true,
                        "rawProfile" : { }
                    };

                var _this = this;
                AttributesGridView.render({
                    usesDragIcon: false,
                    usesLinkQualifier: false,
                    usesDynamicSampleSource: false,
                    staticSourceSample: staticSourceObject,
                    availableObjects: availableObjects,
                    requiredProperties:  _.keys(requiredPropertiesList),
                    mapping: _.extend(this.model.propertyMap, {"name": "mapIdpCommonSchemaToManagedUser"}),
                    save: (mappingProperties) => {
                        _this.model.propertyMap.properties = mappingProperties;
                        ConfigDelegate.updateEntity("selfservice.propertymap", _this.model.propertyMap).then(() => {
                            _this.renderAttributeGrid(propertiesList, requiredPropertiesList);
                        });
                    }
                });

            }, this));
        },

        showDetailDialog: function(event) {
            var el,
                currentData = {},
                cardDetails;

            if ($(event.target).hasClass("self-service-card")) {
                el = $(event.target);
            } else {
                el = $(event.target).closest(".self-service-card");
            }

            cardDetails = this.getCardDetails(el);

            if (cardDetails.disabled && !this.$el.find(".all-check").val()) {
                return false;
            }

            if($(event.target).parents(".checkbox").length === 0 && cardDetails.editable === "true") {
                currentData = _.filter(this.model.saveConfig.stageConfigs, {"name" : cardDetails.type})[0];

                this.loadSelfServiceDialog(el, cardDetails.type, currentData);
            }
        },

        openRegistrationDetails(event) {
            if(event) {
                event.preventDefault();
            }

            var details = {},
                results;

            results = _.filter(this.model.saveConfig.stageConfigs, (stage) => {
                return (stage.name === "selfRegistration" || stage.name === "idmUserDetails");
            });

            $.extend(true, details, results[0], results[1]);

            AdminUtils.findPropertiesList(details.identityServiceUrl.split("/")).then((properties) => {
                details.identityEmailOptions = _.chain(properties).keys().sortBy().value();
                details.identityServiceOptions = this.model.resources;
                details.snapshotToken = this.model.saveConfig.snapshotToken;

                this.loadSelfServiceDialog(_.noop(), "idmUserDetails", details);
            });
        },

        loadSelfServiceDialog(el, type, data) {
            SelfServiceStageDialogView.render({
                "element" : el,
                "type" : type,
                "data" : data,
                "saveCallback" : (config, oldData) => {
                    _.extend(this.model.saveConfig.stageConfigs, config.stageConfigs);

                    if(config.snapshotToken) {
                        _.extend(this.model.saveConfig.snapshotToken, config.snapshotToken);
                    }

                    this.saveConfig(this.model.saveConfig, oldData);
                },
                "stageConfigs" : this.model.saveConfig.stageConfigs
            });
        },

        controlAllSwitch: function(event) {
            var check = this.$el.find(".all-check"),
                tempConfig,
                tempIdmUserDetails;

            this.data.enableSelfService = check.is(":checked");

            if (this.data.enableSelfService) {
                this.$el.find("#userRegistrationConfigBody").show();
                this.$el.find("#enableRegistrationModal").show();

                this.renderAttributeGrid();

                this.model.surpressSave = true;

                _.each(this.$el.find(".wide-card"), function(card) {
                    tempConfig = _.find(this.model.configList, function(config) {
                        return $(card).attr("data-type") === config.type;
                    }, this);

                    if (tempConfig.enabledByDefault) {
                        this.activateStage(this.model.emailServiceAvailable, card, $(card).attr("data-type"));
                    } else {
                        this.model.saveConfig.stageConfigs = _.reject(this.model.saveConfig.stageConfigs, function(stage) {
                            return stage.name === $(card).attr("data-type");
                        });
                    }
                }, this);

                this.model.surpressSave = false;

                this.model.uiConfig.configuration[this.model.uiConfigurationParameter] = true;

                this.openRegistrationDetails();

                tempIdmUserDetails= _.find(this.model.saveConfig.stageConfigs, {
                    "name": "idmUserDetails"
                });

                this.model.registrationProperties = _.clone(tempIdmUserDetails.registrationProperties);

                this.createConfig().then(_.bind(function() {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Save");
                }, this));
            } else {
                this.$el.find("#userRegistrationConfigBody").hide();
                this.$el.find("#enableRegistrationModal").hide();

                this.model.surpressSave = true;
                this.$el.find(".section-check:checked").prop("checked", false).trigger("change");
                this.model.surpressSave = false;
                this.model.uiConfig.configuration[this.model.uiConfigurationParameter] = false;

                this.deleteConfig().then(_.bind(function() {
                    this.initDefaultConfig();
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Delete");
                }, this));
            }
        },

        activateStage: function(emailServiceAvailable, card, type) {
            if(type !== "emailValidation") {
                $(card).find(".section-check").prop("checked", true).trigger("change");
            } else {
                if (emailServiceAvailable === true) {
                    $(card).find(".section-check").prop("checked", true).trigger("change");
                } else {
                    this.model.saveConfig.stageConfigs = _.reject(this.model.saveConfig.stageConfigs, function(stage) {
                        return stage.name === "emailValidation";
                    });
                }
            }
        },

        controlSectionSwitch: function(event) {
            var check = $(event.target),
                card = check.parents(".wide-card"),
                cardDetails = this.getCardDetails(card),
                removeConfig = false,
                currentData;

            if(check.is(":checked")) {
                this.model.saveConfig.stageConfigs = this.setSwitchOn(card, this.model.saveConfig.stageConfigs, this.model.configList, this.model.configDefault.stageConfigs, cardDetails.type);

                if(!this.model.surpressSave) {
                    this.showDetailDialog(event);

                    if(cardDetails.type !== "socialUserDetails") {
                        currentData = _.filter(this.model.saveConfig.stageConfigs, {"name": cardDetails.type})[0];

                        this.loadSelfServiceDialog(card, cardDetails.type, currentData);
                    } else {
                        ConfigDelegate.createEntity(this.model.accountClaimUrl, this.model.configAccountClaimDefault);
                    }
                }
            } else {
                this.model.saveConfig.stageConfigs = this.setSwitchOff(card, this.model.saveConfig.stageConfigs, this.model.configList, cardDetails.type);

                if(cardDetails.type === "socialUserDetails") {
                    ConfigDelegate.deleteEntity(this.model.accountClaimUrl);
                }
            }

            if (!this.model.surpressSave && !removeConfig) {
                this.saveConfig(this.model.saveConfig);
            }
        },

        /**
         * @param card {object}
         * @param stages {Array.<Object>}
         * @param configList {Array.<Object>}
         * @param defaultStages {Array.<Object>}
         * @param type {string}
         *
         * @returns {Array.<Object>}
         *
         * This function updates the html to the on status for a stage and returns the updated stage list
         */
        setSwitchOn: function(card, stages, configList, defaultStages, type) {
            var configItem,
                saveOrder,
                defaultLocation,
                currentStage;

            card.toggleClass("disabled", false);
            card.toggleClass("active", true);

            configItem = _.find(configList, function(config) { return config.type ===  type; });

            if(configItem) {
                configItem.toggledOn = true;
            }

            //socialRegistrationEnabled
            if(type !== "socialUserDetails") {
                if(_.filter(stages, {"name" : type}).length === 0) {
                    saveOrder = this.findPosition(configList, type);
                    defaultLocation = _.findIndex(defaultStages, function (stage) {
                        return stage.name === type;
                    });

                    stages.splice(saveOrder, 0, _.clone(defaultStages[defaultLocation]));
                }
            } else {
                currentStage = _.find(stages, function(stage){
                    return stage.name === "idmUserDetails";
                });

                this.$el.find("#attributesGrid").show();

                currentStage.socialRegistrationEnabled = true;
            }

            return stages;
        },

        /**
         * @param card {object}
         * @param stages {Array.<Object>}
         * @param configList {Array.<Object>}
         * @param type {string}
         *
         * @returns {Array.<Object>}
         *
         * This function updates the html to the off status for a stage and returns the updated stage list
         */
        setSwitchOff: function(card, stages, configList, type) {
            var configItem,
                currentStage;

            card.toggleClass("active", false);
            card.toggleClass("disabled", true);

            configItem = _.find(configList, function(config) { return config.type ===  type; });

            if(configItem) {
                configItem.toggledOn = false;
            }

            if(type !== "socialUserDetails") {
                return _.reject(stages, function (stage) {
                    return stage.name === type;
                });
            } else {
                currentStage = _.find(stages, function(stage) {
                    return stage.name === "idmUserDetails";
                });

                this.$el.find("#attributesGrid").hide();

                currentStage.socialRegistrationEnabled = false;

                return stages;
            }
        },

        /**
         * @param orderedList {Array.<Object>}
         * @param type {string}
         *
         * This function will find the correct position based on what stages are turned on vs its default recommended location
         */
        findPosition : function(orderedList, type) {
            var position = 0;

            _.each(orderedList, (item) => {
                if(item.type === type) {
                    return false;
                }

                if(item.toggledOn) {
                    position++;
                }
            });

            return position;
        },

        /**
         *
         * @param card {object}
         * @returns {{type: string, editable: boolean, disabled: boolean}}
         */
        getCardDetails: function(card) {
            return {
                "type" : card.attr("data-type"),
                "editable" : card.attr("data-editable"),
                "disabled" : card.hasClass("disabled")
            };
        },

        setKBADefinitionEnabled: function () {
            this.model.uiConfig.configuration.kbaDefinitionEnabled = !!_.find(this.model.saveConfig.stageConfigs, function (stage) {
                return stage.name === "kbaSecurityAnswerDefinitionStage";
            });
        },

        setKBAEnabled: function () {
            this.model.uiConfig.configuration.kbaEnabled =
                !!this.model.uiConfig.configuration.kbaDefinitionEnabled ||
                !!this.model.uiConfig.configuration.kbaVerificationEnabled;
        },

        getResources: function() {
            var resourcePromise = $.Deferred();

            if (!this.model.resources) {
                AdminUtils.getAvailableResourceEndpoints().then(_.bind(function (resources) {
                    resourcePromise.resolve(resources);
                }, this));
            } else {
                resourcePromise.resolve(this.model.resources);
            }

            return resourcePromise.promise();
        },

        createConfig: function () {
            this.setKBADefinitionEnabled();
            this.setKBAEnabled();

            return $.when(
                ConfigDelegate.createEntity(this.model.configUrl, this.model.saveConfig),
                ConfigDelegate.updateEntity("ui/configuration", this.model.uiConfig)
            ).then(function () {
                SiteConfigurationDelegate.updateConfiguration(function () {
                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                });
            });
        },

        deleteConfig: function () {
            this.setKBADefinitionEnabled();
            this.setKBAEnabled();

            return $.when(
                ConfigDelegate.deleteEntity(this.model.configUrl),
                ConfigDelegate.updateEntity("ui/configuration", this.model.uiConfig)
            ).then(function () {
                SiteConfigurationDelegate.updateConfiguration(function () {
                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                });
            });
        },

        saveConfig: function(config, oldData) {
            var saveData = {},
                idmUserDetailsIndex = _.findIndex(this.model.saveConfig.stageConfigs, {
                    "name": "idmUserDetails"
                }),
                idmRegistrationIndex = _.findIndex(this.model.saveConfig.stageConfigs, {
                    "name": "selfRegistration"
                }),
                refreshView = false,
                managedObject;

            this.setKBADefinitionEnabled();
            this.setKBAEnabled();

            $.extend(true, saveData, config);

            if(!_.isUndefined(oldData) && oldData.identityServiceUrl && saveData.stageConfigs[idmRegistrationIndex].identityServiceUrl !== oldData.identityServiceUrl) {
                refreshView = true;

                managedObject = this.findManagedSchema(this.model.managed, saveData.stageConfigs[idmRegistrationIndex].identityServiceUrl);

                //Changing schema will set the registration properties to everything currently set as required in the schema
                saveData.stageConfigs[idmUserDetailsIndex].registrationProperties = _.clone(managedObject.schema.required);
            } else {
                saveData.stageConfigs[idmUserDetailsIndex].registrationProperties = _.clone(this.model.registrationProperties);
            }

            return $.when(
                ConfigDelegate.updateEntity(this.model.configUrl, saveData),
                ConfigDelegate.updateEntity("ui/configuration", this.model.uiConfig)
            ).then(_.bind(function() {
                SiteConfigurationDelegate.updateConfiguration(function () {
                    EventManager.sendEvent(Constants.EVENT_UPDATE_NAVIGATION);
                });

                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, this.model.msgType +"Save");

                //If the identityServiceUrl changes we reload the view
                if(refreshView) {
                    this.render();
                }
            }, this));
        }
    });

    return new UserRegistrationConfigView();
});
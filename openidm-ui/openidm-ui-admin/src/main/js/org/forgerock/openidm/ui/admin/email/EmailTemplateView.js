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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "form2js",
    "handlebars",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "libs/codemirror/lib/codemirror",
    "libs/codemirror/mode/xml/xml",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/components/ChangesPending",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "bootstrap-dialog"
], function($, _, form2js,
            Handlebars,
            AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            codeMirror,
            xmlMode,
            UIUtils,
            ChangesPending,
            resourceDelegate,
            BootstrapDialog) {

    var EmailTemplateConfigView = AdminAbstractView.extend({
        template: "templates/admin/email/EmailTemplateViewTemplate.html",
        events: {
            "click #submitEmailTemplateConfigForm" : "save",
            "change #toggle-enabled" : "toggleEnabled",
            "change input,textarea" : "makeChanges",
            "keyup input" : "makeChanges",
            "click .undo" : "reload",
            "click #objectProperties" : "openObjectPropertiesDialog",
            "click #previewMessage" : "openPreviewDialog"
        },
        model: {},
        partials: [
            "partials/email/_emailTemplateBasicForm.html",
            "partials/email/_objectProperties.html"
        ],
        /**
        This view is the basic form for editing the configuration of email objects.
        Properties included in this form are => "enabled","from","subject","message".
        The basic form is extensible by adding an optional partial to partials/email/extensions
        using this naming convention for the file => partials/email/extensions/_resetPassword.html
        where "resetPassword" represents the id of the email template.
        **/
        render: function (args, callback) {
            var configId = args[0],
                configRoute = "emailTemplate/" + configId,
                configReadPromise = ConfigDelegate.readEntity(configRoute),
                extensionPartialPromise = this.loadExtensionPartial(configId),
                schemaPromise,
                sampleDataPromise;

            this.configRoute = configRoute;
            this.data.configId = configId;

            //TODO make the resource being used here dynamic
            this.resourcePath = "managed/user";
            schemaPromise = resourceDelegate.getSchema(this.resourcePath.split("/"));
            sampleDataPromise = resourceDelegate.serviceCall({
                "type": "GET",
                "serviceUrl": "/openidm/" + this.resourcePath,
                "url":  "?_queryFilter=true&_pageSize=1"
            });

            $.when(configReadPromise, extensionPartialPromise, schemaPromise, sampleDataPromise).then((config, partial, schema, sampleData) => {
                this.data.config = config;
                this.data.extensionPartial = partial;
                this.data.resourceName = schema.title.toLowerCase();
                this.propertiesList = this.getPropertiesList(schema);
                this.data.resourceSchema = schema;

                if (sampleData[0].result[0]) {
                    this.data.sampleData = sampleData[0].result[0];
                } else {
                    this.data.sampleData = this.generateSampleData(this.propertiesList);
                }

                this.parentRender(() => {
                    this.model.changesModule = ChangesPending.watchChanges({
                        element: this.$el.find(".changes-pending-container"),
                        undo: true,
                        watchedObj: _.clone(this.data.config, true)
                    });

                    this.cmBox = codeMirror.fromTextArea(this.$el.find("#templateSourceCode")[0], {
                        lineNumbers: true,
                        autofocus: true,
                        viewportMargin: Infinity,
                        theme: "forgerock",
                        mode: "xml",
                        htmlMode: true
                    });

                    //set the initial value of the codeMirror instance for message
                    this.cmBox.setValue(this.data.config.message.en);

                    this.cmBox.on("change", () => {
                        this.makeChanges();
                    });

                    if (callback) {
                        callback();
                    }
                });
            });
        },
        /**
        This function returns the result of form2js on the emailTemplateConfigForm
        **/
        getFormData: function () {
            return form2js("emailTemplateConfigForm",".", false);
        },
        /**
        This function grabs the current state the form via form2js
        and updates this.data.config with it's results. There is a special
        case for the message field because it is handled with codemirror.

        @param config {object} - an object representing the current state of this.data.config
        @returns {object} - the up to date object
        **/
        updateCurrentConfig: function (oldConfig) {
            var newConfig = _.cloneDeep(oldConfig);

            _.extend(newConfig, this.getFormData());

            //special case for message
            newConfig.message.en = this.cmBox.getValue();

            return newConfig;
        },


        save: function (e) {
            if (e) {
                e.preventDefault();
            }

            this.data.config = this.updateCurrentConfig(this.data.config);

            ConfigDelegate.updateEntity(this.configRoute, this.data.config).then(() => {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "emailTemplateConfigSaveSuccess");
                this.reload();
            });
        },

        reload: function () {
            this.render([this.data.configId]);
        },

        /**
        This function attempts to load a partial file from partials/email/extensions
        with the same name as the configId. If the partial exists it returns a promise
        resolving with a string representing the name of the partial to be used in the
        form template or false if no partial exists.

        @param configId {string}
        @returns promise
        **/
        loadExtensionPartial: function (configId) {
            var promise = $.Deferred(),
                partialName = "_" + configId,
                path = "partials/email/extensions/" + partialName + ".html";

            $.ajax({
                url: path,
                success: (data) => {
                    Handlebars.registerPartial(partialName, Handlebars.compile(data));
                    promise.resolve(partialName);
                },
                error: (e) => {
                    promise.resolve(false);
                }
            });

            return promise;
        },

        toggleEnabled: function () {
            this.$el.find("#emailTemplateConfigFormControls").slideToggle(!this.$el.find("#toggle-enabled").prop("checked"));
            this.cmBox.refresh();
        },
        /**
        This function is called any time the form is updated. It updates the current config,
        checks with the changes pending module for any differences with the original form,
        toggles the save button on when there are changes, and off when the form is current.
        **/
        makeChanges: function () {
            this.data.config = this.updateCurrentConfig(this.data.config);

            this.model.changesModule.makeChanges(_.clone(this.data.config));

            if (this.model.changesModule.isChanged()) {
                this.$el.find(".btn-save").prop("disabled",false);
            } else {
                this.$el.find(".btn-save").prop("disabled",true);
            }
        },
        /**
        This function takes a schema object loops over the order, makes a map of the properties
        which are of type "string", not the "_id" property and not protected (example "password").

        @param schema {object} - an object representing the schema of a resource
        @returns {array} - an array of schema property objects
        **/
        getPropertiesList: function (schema) {
            return _(schema.order)
                    .map( (propKey) => {
                        var prop = schema.properties[propKey];

                        //filter out any properties that are not strings, not the _id property
                        //and is not using encryption (in the case of password)
                        if (prop.type === "string" && propKey !== "_id" && !prop.encryption) {
                            prop.propName = propKey;
                            return prop;
                        }
                    })
                    .reject(function (val) {
                        return val === undefined;
                    })
                    .value();
        },

        openObjectPropertiesDialog: function () {
            var title = this.data.resourceSchema.title + " " + $.t("templates.emailConfig.properties"),
                dialogContent = Handlebars.compile("{{> email/_objectProperties}}")({ properties : this.propertiesList });

            this.showDialog(title, dialogContent);
        },

        openPreviewDialog: function () {
            var title = $.t("templates.emailConfig.preview"),
                dialogContent = Handlebars.compile(this.data.config.message.en)({ object : this.data.sampleData });

            this.showDialog(title, dialogContent);
        },

        showDialog: function (title, content) {
            BootstrapDialog.show({
                title: title,
                type: "type-default",
                message: content,
                id: "frConfirmationDialog",
                buttons: [
                    {
                        label: $.t('common.form.close'),
                        id: "frDialogBtnClose",
                        action: function(dialog){
                            dialog.close();
                        }
                    }
                ]
            });
        },
        /**
        This function takes in an array of schema properties and
        creates an object with random string values having key's representing
        each.

        @param properties {array} - array of schema property objects
        @returns {object} - an array of sample data for use in the message preview
        **/
        generateSampleData: function (properties) {
            var sampleData = {},
                randomStrings = ["lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed", "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua"];

            _.each(properties, function (prop) {
                sampleData[prop.propName] = randomStrings[Math.floor((Math.random() * 10) + 1)];
            });

            return sampleData;
        }
    });

    return new EmailTemplateConfigView();
});

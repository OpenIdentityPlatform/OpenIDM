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
 * Copyright 2015-2016 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "form2js",
    "faiconpicker",
    "org/forgerock/openidm/ui/admin/managed/AbstractManagedView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Router"
], function($, _,
            form2js,
            faiconpicker,
            AbstractManagedView,
            ValidatorsManager,
            ConfigDelegate,
            EventManager,
            Constants,
            Router) {

    var AddManagedView = AbstractManagedView.extend({
        template: "templates/admin/managed/AddManagedTemplate.html",
        events: {
            "submit #addManagedObjectForm" : "createManagedObject",
            "onValidate": "onValidate"
        },
        data: {},
        model: {},

        render: function(args, callback) {
            var managedPromise = ConfigDelegate.readEntity("managed"),
                repoCheckPromise = ConfigDelegate.getConfigList();

            $.when(managedPromise, repoCheckPromise).then(_.bind(function(managedObjects, configFiles) {
                this.checkRepo(configFiles[0], _.bind(function(){
                    this.model.managedObjects = managedObjects;

                    this.parentRender(_.bind(function () {
                        ValidatorsManager.bindValidators(this.$el.find("#addManagedObjectForm"));
                        this.$el.find('#managedObjectIcon').iconpicker({
                            hideOnSelect: true
                        });

                        if (callback) {
                            callback();
                        }
                    }, this));
                }, this));
            }, this));
        },

        createManagedObject: function(event) {
            event.preventDefault();

            var managedObject = form2js('addManagedObjectForm', '.', true),
                nameCheck;

            managedObject.schema = {};
            managedObject.schema.icon = this.$el.find("#managedObjectIcon").val();

            nameCheck = this.checkManagedName(managedObject.name, this.model.managedObjects.objects);

            if(!nameCheck) {
                this.model.managedObjects.objects.push(managedObject);

                this.saveManagedObject(managedObject, this.model.managedObjects, true);
            } else {
                this.$el.find("#managedErrorMessage .message").html($.t("templates.managed.duplicateNameError"));
                this.$el.find("#managedErrorMessage").show();
                this.$el.find("#addManagedObject").prop("disabled", true);
            }
        }
    });

    return new AddManagedView();
});

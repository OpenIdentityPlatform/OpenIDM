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
 * Copyright 2015-2017 ForgeRock AS.
 */

define([
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/managed/schema/dataTypes/ObjectTypeView",
    "org/forgerock/openidm/ui/admin/managed/schema/util/SchemaUtils"
], function($, _,
    AdminAbstractView,
    ObjectTypeView,
    SchemaUtils
) {
    var SchemaEditorView = AdminAbstractView.extend({
        template: "templates/admin/managed/schema/SchemaEditorViewTemplate.html",
        element: "#managedSchemaContainer",
        noBaseTemplate: true,
        model: {},
        events: {},

        render: function(args, callback) {
            this.parent = args[0];

            this.parentRender(() => {
                var wasJustSaved = false;

                if (
                    _.isObject(this.model.managedObjectSchema) &&
                    this.model.managedObjectSchema.data.wasJustSaved &&
                    this.parent.data.currentManagedObject.name === this.model.managedObjectSchema.model.propertyRoute
                ) {
                    wasJustSaved = true;
                }

                this.model.managedObjectSchema = new ObjectTypeView();

                this.model.managedObjectSchema.render({
                    elementId: "managedSchema",
                    schema: this.parent.data.currentManagedObject.schema,
                    saveSchema: _.bind(this.saveManagedSchema,this),
                    propertyRoute: this.parent.data.currentManagedObject.name,
                    topLevelObject: true,
                    wasJustSaved: wasJustSaved
                });


                if (callback) {
                    callback();
                }

            });

        },
        getManagedSchema: function() {
            var managedSchema = _.extend({
                "$schema": "http://forgerock.org/json-schema#",
                "type": "object",
                "title": this.parent.$el.find("#managedObjectTitle").val(),
                "description": this.parent.$el.find("#managedObjectDescription").val(),
                "icon": this.parent.$el.find("#managedObjectIcon").val()
            }, this.model.managedObjectSchema.getValue());

            return managedSchema;
        },
        saveManagedSchema: function(e, callback) {
            if (e) {
                e.preventDefault();
            }

            this.parent.data.currentManagedObject.schema = this.getManagedSchema();

            this.parent.saveManagedObject(this.parent.data.currentManagedObject, this.parent.data.managedObjects, false, () => {
                this.parent.$el.find('a[href="#managedSchemaContainer"]').tab('show');

                if (callback) {
                    callback();
                }
            });
        }
    });

    return new SchemaEditorView();
});

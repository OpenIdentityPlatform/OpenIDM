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
 * Copyright 2011-2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/common/resource/GenericEditResourceView", [
    "jquery",
    "underscore",
    "handlebars",
    "jsonEditor",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/openidm/ui/common/delegates/ResourceDelegate",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openidm/ui/common/resource/ResourceCollectionSearchDialog",
    "org/forgerock/openidm/ui/common/resource/RelationshipArrayView",
    "org/forgerock/openidm/ui/common/resource/ResourceCollectionRelationshipsView",
    "org/forgerock/openidm/ui/common/util/ResourceCollectionUtils",
    "org/forgerock/openidm/ui/common/linkedView/LinkedView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "bootstrap"
], function($, _, handlebars,JSONEditor,
        AbstractView,
        eventManager,
        constants,
        uiUtils,
        serviceInvoker,
        resourceDelegate,
        messagesManager,
        ResourceCollectionSearchDialog,
        RelationshipArrayView,
        ResourceCollectionRelationshipsView,
        resourceCollectionUtils,
        LinkedView,
        router,
        ValidatorsManager
    ) {
    var EditResourceView = AbstractView.extend({
        template: "templates/admin/resource/EditResourceViewTemplate.html",

        events: {
            "click #saveBtn": "save",
            "click #backBtn": "backToList",
            "click #deleteBtn": "deleteObject",
            "click #resetBtn": "reset",
            "onValidate": "onValidate"
        },
        partials: [
            "partials/resource/_relationshipDisplay.html"
        ],
        render: function(args, callback) {
            var resourceReadPromise,
                objectId = (args[0] === "managed") ? args[2] : args[3],
                displayField;


            resourceDelegate.getSchema(args).then(_.bind(function (schema) {
                var readUrl;

                this.data.args = args;

                this.data.objectType = args[0];
                this.data.isSystemResource = false;
                this.objectName = args[1];
                this.data.serviceUrl = resourceDelegate.getServiceUrl(args);

                readUrl = this.data.serviceUrl +"/" + objectId + "?_fields=" + resourceCollectionUtils.getFieldsToExpand(schema.properties);


                if (this.data.objectType === "system") {
                    this.data.isSystemResource = true;
                    this.objectName += "/" + args[2];
                    readUrl = this.data.serviceUrl +"/" + objectId;
                }

                if(objectId){
                    resourceReadPromise = serviceInvoker.restCall({
                        url: readUrl
                    });
                    this.objectId = objectId;
                    this.data.newObject = false;
                } else {
                    resourceReadPromise = $.Deferred().resolve({});
                    this.data.newObject = true;
                }

                resourceReadPromise.then(_.bind(function(resource){
                    this.data.objectTitle = schema.title || this.objectName;

                    this.data.schema = schema;

                    if(this.data.isSystemResource) {
                        this.data.objectTitle = this.objectName;
                    }

                    if(!this.data.newObject) {
                        if(this.data.isSystemResource) {
                            displayField = _.chain(schema.properties)
                                            .map(function(val, key) { val.name = key; return val; })
                                            .where({ nativeName: "__NAME__" })
                                            .value();

                            if(displayField) {
                                displayField = displayField[0].name;
                            } else {
                                displayField = _.keys(schema.properties)[0];
                            }
                        } else {
                            _.map(schema.order, function (propName) {
                                if(!displayField && schema.properties[propName].viewable) {
                                    displayField = propName;
                                }
                            });
                        }

                        this.data.objectDisplayText = resource[displayField];
                    }

                    this.data.backBtnText = $.t("templates.admin.ResourceEdit.backToList",{ objectTitle: this.data.objectTitle });

                    this.parentRender(function(){
                        this.setupEditor(resource, schema);


                        ValidatorsManager.bindValidators(
                            this.$el.find("#resource"),
                            [this.data.objectType,this.objectName,this.objectId || "*"].join("/"),
                            _.bind(function () {
                                if(!this.data.newObject) {
                                    this.linkedView = new LinkedView();
                                    this.linkedView.element = "#linkedView";

                                    this.linkedView.render({id: resource._id, resourcePath: this.data.objectType + "/" + this.objectName + "/" });
                                }

                                if(callback) {
                                    callback();
                                }
                            }, this)
                        );
                    });
                },this));
            },this));
        },
        setupEditor: function(resource, schema){
            var propCount = 0,
                filteredProperties,
                filteredObject = resource;

            this.oldObject = $.extend(true, {}, filteredObject);

            filteredProperties = resourceCollectionUtils.convertRelationshipTypes(_.omit(schema.properties,function(p) { return !p.viewable; }));

            if(!_.isEmpty(filteredProperties)){
                filteredObject = _.pick(filteredObject, _.keys(filteredProperties));
            }

            JSONEditor.defaults.options = {
                    theme: "bootstrap3",
                    iconlib: "fontawesome4",
                    disable_edit_json: true,
                    disable_array_reorder: true,
                    disable_collapse: true,
                    disable_properties: true,
                    show_errors: "never",
                    formHorizontal: true
            };

            if(schema.order){
                _.each(schema.order, _.bind(function(prop){
                    schema.properties[prop].propertyOrder = propCount++;
                    if(schema.properties[prop].viewable && !_.has(filteredObject, prop)){
                        filteredObject[prop] = null;
                    }
                }, this));
            }

            if (this.data.isSystemResource) {
                schema.title = this.data.objectTitle;
                if (this.data.newObject) {
                    _.each(schema.properties,function(p) {
                        p.required = true;
                    });
                }
            }

            this.editor = new JSONEditor(document.getElementById("resource"), { schema: _.omit(schema, "allSchemas") });
            this.editor.setValue(filteredObject);
            this.addTooltips();

            this.convertResourceCollectionFields(filteredObject,schema).then(_.bind(function () {

                this.editor.on('change', _.bind(function() {
                    this.showPendingChanges();
                }, this));

                this.$el.find(".json-editor-btn-collapse").prop("disabled", true);
            }, this));

            if (this.data.isSystemResource) {
                this.$el.find(".row select").hide();
                this.$el.find(".row input").prop("disabled", true);
            }
        },
        showPendingChanges : function() {
            var changedFields = [],
                newValue = _.extend({},this.oldObject, this.getFormValue());

            if(_.isEqual(newValue, this.oldObject)) {
                this.$el.find("#saveBtn").attr("disabled", true);
                this.$el.find("#resetBtn").attr("disabled", true);
                this.$el.find("#resourceChangesPending").hide();
            } else {
                if(!this.data.newObject) {
                    _.each(newValue, _.bind(function(val,key) {
                        var relationshipType = this.data.schema.properties[key] && this.data.schema.properties[key].typeRelationship,
                            hasVal = val && val.length;
                        if(
                                (!this.oldObject[key] && hasVal) ||
                                (!relationshipType && (this.oldObject[key] && !_.isEqual(this.oldObject[key], val))) ||
                                (relationshipType && hasVal && !_.isEqual(JSON.parse(val), this.oldObject[key]))
                          ) {
                            if(this.data.schema.properties && this.data.schema.properties[key] && this.data.schema.properties[key].title && this.data.schema.properties[key].title.length) {
                                changedFields.push(this.data.schema.properties[key].title);
                            } else {
                                changedFields.push(key);
                            }
                        }
                    }, this));

                    if(changedFields.length) {
                        this.$el.find("#changedFields").html("<br/>- " + changedFields.join("<br/>- "));

                        this.$el.find("#saveBtn").removeAttr("disabled");
                        this.$el.find("#resetBtn").removeAttr("disabled");

                        this.$el.find("#resourceChangesPending").show();
                    } else {
                        this.$el.find("#resourceChangesPending").hide();
                        this.$el.find("#saveBtn").attr("disabled", true);
                        this.$el.find("#resetBtn").attr("disabled", true);
                    }
                } else {
                    this.$el.find("#saveBtn").removeAttr("disabled");
                    this.$el.find("#resetBtn").removeAttr("disabled");

                }
            }
        },
        /* To accommodate a popover the addTooltips function transforms the following html:
         *
         * <div class=" form-group">
         *      <label class=" control-label">Username</label>
         *      <input type="text" class="form-control" name="root[userName]">
         *      <p class="help-block">The Username</p>
         * </div>
         *
         * into:
         *
         * <div class=" form-group">
         *      <label class=" control-label">Username</label> <i class="fa fa-info-circle info" title="" data-original-title="The Username"></i>
         *      <input type="text" class="form-control" name="root[userName]">
         *      <p class="help-block"></p>
         * </div>
         *
         */
        addTooltips: function() {
            var propertyDescriptionSpan = this.$el.find("p.help-block"),
                objectHeader = this.$el.find("#resource").find("h3:eq(0)"),
                objectDescriptionSpan = objectHeader.next(),
                // this text escaped since it's being inserted into an attribute
                tipDescription = handlebars.Utils.escapeExpression(objectDescriptionSpan.text()),
                iconElement = $('<i class="fa fa-info-circle info" />');

            $.each(propertyDescriptionSpan, function() {
                // this text escaped since it's being inserted into an attribute
                var tipDescription = handlebars.Utils.escapeExpression($(this).text());
                iconElement.attr('title', tipDescription);
                $(this).parent().find("label").after(iconElement);
                $(this).empty();
            });

            if (objectDescriptionSpan.text().length > 0) {
                iconElement.attr('title', tipDescription);
                objectHeader.append(iconElement);
                objectDescriptionSpan.empty();
            }

            this.$el.find(".info").popover({
                content: function () { return $(this).attr("data-original-title");},
                placement: 'top',
                container: 'body',
                html: 'true',
                template: '<div class="popover popover-info" role="tooltip"><div class="popover-content"></div></div>'
            });
        },
        getFormValue : function() {
            var formVal = this.editor.getValue();

            if(!this.data.newObject){
                /*
                The following _.each() was placed here to account for JSONEditor.setValue()
                turning a property that exists but has a null value into an empty text field.
                Upon calling JSONEditor.getValue() the previously null property will be set to and empty string.

                This loop filters out previously null values that have not been changed.
                */
                _.each(_.keys(formVal), function(key){
                    if ((this.oldObject[key] === null || this.oldObject[key] === undefined) && (!formVal[key] || !formVal[key].length)){
                        formVal[key] = this.oldObject[key];
                    }
                }, this);
            } else {
                _.each(this.$el.find(".resourceCollectionValue"), function(element) {
                    try {
                        formVal[$(element).attr("propname")] = JSON.parse($(element).val());
                    } catch (e) {}
                });
            }

            return formVal;
        },
        save: function(e, callback){
            var formVal = this.getFormValue(),
                successCallback = _.bind(function(editedObject){
                    var msg = (this.data.newObject) ? "templates.admin.ResourceEdit.addSuccess" : "templates.admin.ResourceEdit.editSuccess",
                        editRouteName = (!this.data.isSystemResource) ? "adminEditManagedObjectView" : "adminEditSystemObjectView";

                    messagesManager.messages.addMessage({"message": $.t(msg,{ objectTitle: this.data.objectTitle })});
                    this.data.editedObject = editedObject;

                    if(this.data.newObject) {
                        this.data.args.push(editedObject._id);
                        eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes[editRouteName], args: this.data.args, callback: callback});
                    } else {
                        this.render(this.data.args,callback);
                    }
                }, this);

            if(e) {
                e.preventDefault();
            }

            if ($(e.currentTarget).attr("disabled") === "disabled" ) {
                return false;
            }

            if(this.data.newObject){
                formVal = _.omit(formVal,function (val) { return val === "" || val === null; });
                resourceDelegate.createResource(this.data.serviceUrl, formVal._id, formVal, successCallback);
            } else {
                if (!this.data.isSystemResource) {
                    _.each(this.$el.find(".resourceCollectionValue"), function(element) {
                        var val = $(element).val();

                        if (val.length) {
                            val = JSON.parse($(element).val());
                        } else {
                            val = null;
                        }
                        formVal[$(element).attr("propname")] = val;
                    });
                    resourceDelegate.patchResourceDifferences(this.data.serviceUrl, {id: this.oldObject._id, rev: this.oldObject._rev}, this.oldObject, _.extend({}, this.oldObject, formVal), successCallback);
                } else {
                    resourceDelegate.updateResource(this.data.serviceUrl, this.oldObject._id, formVal, successCallback);
                }
            }
        },
        backToList: function(e){
            var routeName = (!this.data.isSystemResource) ? "adminListManagedObjectView" : "adminListSystemObjectView";

            if(e){
                e.preventDefault();
            }

            eventManager.sendEvent(constants.ROUTE_REQUEST, {routeName: routeName, args: this.data.args});
        },
        reset: function(e){
            if (e) {
                e.preventDefault();
            }

            if ($(e.currentTarget).attr("disabled") === "disabled" ) {
                return false;
            }

            this.render(this.data.args);
        },
        deleteObject: function(e, callback){
            if (e) {
                e.preventDefault();
            }

            uiUtils.confirmDialog($.t("templates.admin.ResourceEdit.confirmDelete",{ objectTitle: this.data.objectTitle }), "danger", _.bind(function(){
                resourceDelegate.deleteResource(this.data.serviceUrl, this.objectId, _.bind(function(){
                    messagesManager.messages.addMessage({"message": $.t("templates.admin.ResourceEdit.deleteSuccess",{ objectTitle: this.data.objectTitle })});
                    this.backToList();
                    if (callback) {
                        callback();
                    }
                }, this));
            }, this));
        },
        /**
         * looks through the resource's schema, finds all relationship fields, and either converts
         * the JSONEditor representation of the field to a relationship UI in the case of singleton relationships
         * or in the case of arrays of relationships it converts that into its own tab with it's own grid of data
         * and actions
         *
         * @param {Object} filteredObject
         * @param {Object} schema
         * @returns {promise}
         */
        convertResourceCollectionFields: function(filteredObject,schema){
                var _this = this,
                    getFields,
                    convertField,
                    convertArrayField,
                    showRelationships,
                    addTab;

                getFields = function(properties, parent) {
                    var promises;

                    promises = _.map(properties, function(prop,key) {
                        prop.propName = key;
                        if (prop.type === "object") {
                            if (parent) {
                                parent += "\\." + key;
                            } else {
                                parent = "\\." + key;
                            }
                            return getFields(prop.properties, parent);
                        }

                        if (parent) {
                            prop.selector =  parent + "\\." + key;
                        } else {
                            prop.selector = "\\." + key;
                        }

                        if (prop.type === "array") {
                            if(prop.items.resourceCollection && _.has(filteredObject,key)) {
                                prop.parentObjectId =  _this.objectId;
                                prop.relationshipUrl = _this.data.objectType + "/" + _this.objectName + "/" + _this.objectId + "/" + prop.propName;
                                prop.typeRelationship = true;
                                prop.parentDisplayText = _this.data.objectDisplayText;
                                return convertArrayField(prop);
                            }
                        }

                        if (prop.resourceCollection) {
                            return convertField(prop);
                        }

                        // nothing special needed for this field
                        return $.Deferred().resolve();
                    });

                    return $.when.apply($, promises);
                };

                /**
                 * converts a singleton relationship field into a button that opens an instance of ResourceCollectionSearchDialog
                 * if the property has no value the button will be a create button
                 * if the property has a value the button will be a link button with the related resource's display text and the resource's icon
                 */
                convertField = function (prop) {
                    var el = _this.$el.find("#0-root" + prop.selector.replace(/\./g, "-")),//this is the JSONEditor field to be hidden and changed by the button/dialog
                        buttonId = "relationshipLink-" + prop.propName,
                        button = $(handlebars.compile("{{> resource/_relationshipDisplay}}")({
                                "newRelationship": true,
                                "displayText" : $.t("templates.admin.ResourceEdit.addResource",{ resource: prop.title }),
                                "buttonId" : buttonId
                             })),
                        propertyValuePath,
                        iconClass,
                        resourceCollectionSchema,
                        resourceEditPath = function () {
                            var val = JSON.parse(el.val()),
                                route = "resource/",
                                pathArray = val._ref.split("/");

                            pathArray.pop();

                            route += pathArray.join("/") + "/edit/" + val._id;

                            return route;
                        };

                    if (el.val().length) {
                        propertyValuePath = resourceCollectionUtils.getPropertyValuePath(JSON.parse(el.val()));
                        resourceCollectionSchema = _.findWhere(_this.data.schema.allSchemas, { name : propertyValuePath.split("/")[propertyValuePath.split("/").length - 1] });

                        if (resourceCollectionSchema) {
                            iconClass = resourceCollectionSchema.schema.icon;
                        }

                        button = $(handlebars.compile("{{> resource/_relationshipDisplay}}")({
                            "iconClass": iconClass || "fa-cube",
                            "displayText": resourceCollectionUtils.getDisplayText(prop, JSON.parse(el.val()), resourceCollectionUtils.getResourceCollectionIndex(_this.data.schema,propertyValuePath, prop.propName)),
                            "editButtonText": $.t("templates.admin.ResourceEdit.updateResource",{ resource: prop.title }),
                            "propName": prop.propName,
                            "resourceEditPath": resourceEditPath()
                         }));

                    }

                    button.click(function (e) {
                        var opts = {
                                property: prop,
                                propertyValue: el.val(),
                                schema: _this.data.schema,
                                onChange: function (value, newText) {
                                    _this.editor.getEditor("root" + prop.selector.replace("\\","")).setValue(JSON.stringify(value));
                                    button.remove();
                                    convertField(prop);
                                    _this.$el.find("#resourceEditLink-" + prop.propName).text(newText);
                                }
                        };

                        if ($(e.target).attr("id") === buttonId || $(e.target).closest(".updateRelationshipButton").attr("id") === buttonId) {
                            e.preventDefault();
                            ResourceCollectionSearchDialog.render(opts);
                        }
                    });

                    el.attr("style","display: none !important");
                    el.attr("propname",prop.propName);
                    el.addClass("resourceCollectionValue");
                    el.after(button);

                    return $.Deferred().resolve();
                };

                convertArrayField = function(prop) {
                    _this.editor.getEditor('root' + prop.selector.replace("\\","")).destroy();

                    //in case this relationship array field is returned by default
                    //remove it from the original version of the resource
                    if (_this.oldObject[prop.propName]) {
                        delete _this.oldObject[prop.propName];
                    }

                    return addTab(prop, {
                        templateId : "tabContentTemplate",
                        tabView: new RelationshipArrayView(),
                        viewId: "relationshipArray-" + prop.propName,
                        contentId: "resource-" + prop.propName,
                        contentClass: "resourceCollectionArray",
                        headerText: prop.title
                    });
                };

                showRelationships = function(prop) {
                    return addTab(prop, {
                        templateId : "relationshipsTemplate",
                        tabView: new ResourceCollectionRelationshipsView(),
                        viewId: "resourceCollectionRelationship-" + prop.propName,
                        contentId: "relationship-" + prop.propName,
                        contentClass: "resourceCollectionRelationships",
                        headerText: prop.resourceCollection.label
                    });
                };

                addTab = function(prop, opts) {
                    var tabHeader = _this.$el.find("#tabHeaderTemplate").clone(),
                        tabContent = _this.$el.find("#" + opts.templateId).clone(),
                        promise = $.Deferred();

                    if (!_this.data.newObject) {
                        tabHeader.attr("id", "tabHeader_" + opts.contentId);
                        tabHeader.find("a").attr("href","#" + opts.contentId).text(opts.headerText);
                        tabHeader.show();

                        tabContent.attr("id",opts.contentId);
                        tabContent.find("." + opts.contentClass).attr("id", opts.viewId);

                        _this.$el.find("#linkedSystemsTabHeader").before(tabHeader);
                        _this.$el.find("#resource-linkedSystems").before(tabContent);

                        opts.tabView.render({ element: "#" + opts.viewId, prop: prop, schema: schema, onChange: opts.onChange }, function () {
                            promise.resolve();
                        });
                    } else {
                        promise.resolve();
                    }

                    return promise;
                };

                return getFields(schema.properties);
        }
    });

    return new EditResourceView();
});

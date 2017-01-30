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
    "handlebars",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "bootstrap-dialog",
    "org/forgerock/openidm/ui/admin/managed/schema/util/SchemaUtils",
    "backbone",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function($, _,
    handlebars,
    AbstractView,
    conf,
    uiUtils,
    BootstrapDialog,
    SchemaUtils,
    Backbone,
    Backgrid,
    BackgridUtils,
    UIUtils
) {
    var EditPolicyDialog = AbstractView.extend({
        template: "templates/admin/managed/schema/EditPolicyDialogTemplate.html",
        el: "#dialogs",
        model: {},
        /**
        * @param {object} args - two properties { policy: policyToBeEdited, savePolicy: functionToSaveThePolicy }
        * @param {function} callback - a function to be executed after load
        */
        render: function(args, onLoadCallback) {
            var self = this;

            if (args.policy) {
                this.data.policy = args.policy;
                this.dialogTitle = $.t("templates.managed.schemaEditor.editPolicy");
                this.saveButtonLabel = $.t('common.form.save');
            } else {
                this.data.policy = {
                    policyId: "",
                    params: {}
                };
                this.dialogTitle = $.t("templates.managed.schemaEditor.addPolicy");
                this.saveButtonLabel = $.t('common.form.add');
            }

            this.savePolicy = args.savePolicy;

            this.data.policyParams = _.map(_.keys(this.data.policy.params), (paramName) => {
                return {
                    paramName: paramName,
                    value: this.data.policy.params[paramName]
                };
            });

            $.when(
                UIUtils.preloadPartial("partials/managed/schema/_policyParamNewRow.html"),
                UIUtils.preloadPartial("partials/managed/schema/_policyParamEditableRow.html")
            ).then(() => {
                this.currentDialog = $('<div id="editPolicyDialog"></div>');

                $('#dialogs').append(this.currentDialog);

                //change dialog
                this.dialog = BootstrapDialog.show({
                    title: this.dialogTitle,
                    type: BootstrapDialog.TYPE_DEFAULT,
                    message: this.currentDialog,
                    size: BootstrapDialog.SIZE_WIDE,
                    cssClass : "objecttype-windows",
                    onshown : (dialogRef) => {
                        this.loadTemplate();

                        if (this.onLoadCallback) {
                            this.onLoadCallback();
                        }
                    },
                    buttons: [
                        {
                            label: $.t('common.form.cancel'),
                            id: "editPolicyDialogCloseBtn",
                            action: function(dialogRef){
                                dialogRef.close();
                            }
                        },
                        {
                            label: this.saveButtonLabel,
                            cssClass: "btn-primary",
                            id: "editPolicyDialogSaveBtn",
                            action: function(dialogRef) {
                                self.setPolicy();
                                dialogRef.close();
                                self.savePolicy(self.data.policy);
                            }
                        }
                    ]
                });
            });
        },
        loadTemplate: function () {
            uiUtils.renderTemplate(
                    this.template,
                    this.currentDialog,
                    _.extend({}, conf.globalData, this.data),
                    () => {
                        this.setupPolicyParamsGrid(this.data.policyParams);
                    },
                    "replace"
                );
        },
        /**
         *
         */
        setupPolicyParamsGrid: function (policyParams) {
            var self = this,
                listElement = this.currentDialog.find(".paramsList"),
                cols = [
                    {
                        name: "paramName",
                        label: $.t("templates.managed.schemaEditor.parameterName"),
                        cell: "string",
                        sortable: false,
                        editable: false
                    },
                    {
                        name: "value",
                        label: $.t("templates.managed.schemaEditor.value"),
                        cell: "string",
                        sortable: false,
                        editable: false
                    },
                    {
                        label: "",
                        cell: BackgridUtils.ButtonCell([
                            {
                                className: "fa fa-times grid-icon col-sm-1 pull-right",
                                callback: (e) => {
                                    var itemIndex = SchemaUtils.getClickedRowIndex(e);
                                    this.data.policyParams.splice(itemIndex,1);
                                    this.setPolicy();
                                    this.loadTemplate(true);
                                }
                            },
                            {
                                // No callback necessary, the row click will trigger the edit
                                className: "fa fa-pencil grid-icon col-sm-1 pull-right"
                            }
                        ]),
                        sortable: false,
                        editable: false
                    }
                ],
                policyParamsGrid,
                newRow;

            //empty the existing
            listElement.empty();

            newRow = $(handlebars.compile("{{> managed/schema/_policyParamNewRow}}")());

            this.model.policyParamsCollection = new Backbone.Collection(this.data.policyParams);

            policyParamsGrid = new Backgrid.Grid({
                className: "table backgrid table-hover",
                columns: BackgridUtils.addSmallScreenCell(cols),
                collection: this.model.policyParamsCollection,
                row: BackgridUtils.ClickableRow.extend({
                    callback: function(e) {
                        var row = $(e.target).closest("tr"),
                            paramName = this.model.get("paramName"),
                            editableRow = $(handlebars.compile("{{> managed/schema/_policyParamEditableRow}}")({
                                paramName : this.model.get("paramName"),
                                value : this.model.get("value")
                            }));

                        e.preventDefault();

                        //open policyDialog here
                        if (!$(e.target).hasClass("fa-times")) {
                            row.replaceWith(editableRow);

                            //hide the add row
                            editableRow.parent().find(".policyParamNewRow").hide();

                            editableRow.find(".cancelEditPolicyParam").click((e) => {
                                e.preventDefault();
                                self.loadTemplate();
                            });

                            editableRow.find(".saveEditPolicyParam").click((e) => {
                                e.preventDefault();
                                self.savePolicyParamRow(e);
                            });
                        }
                    }
                })
            });

            listElement.append(policyParamsGrid.render().el);

            listElement.find("tbody").append(newRow);

            newRow.find(".addNewPolicyParamButton").click((e) => {
                this.savePolicyParamRow(e, true);
            });
        },
        savePolicyParamRow: function(e, isNew) {
            var row = $(e.target).closest("tr"),
                paramName = row.find(".policyParamName").val(),
                paramValue = row.find(".policyParamValue").val(),
                rowIndex = SchemaUtils.getClickedRowIndex(e),
                param = {
                    paramName: paramName,
                    value: paramValue
                };

            if (paramName.length) {
                if (isNew) {
                    this.data.policyParams.push(param);
                } else {
                    this.data.policyParams[rowIndex] = param;
                }
                this.setPolicy();
                this.loadTemplate(true);
            }
        },
        setPolicy: function () {
            var params = {};
            this.data.policy.policyId = this.currentDialog.find(".policyId").val();

            _.each(this.data.policyParams, (param) => {
                params[param.paramName] = param.value;
            });

            this.data.policy.params = params;
        }
    });

    return new EditPolicyDialog();
});

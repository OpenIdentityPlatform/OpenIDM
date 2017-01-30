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
    "backbone",
    "org/forgerock/openidm/ui/admin/managed/AbstractManagedView",
    "org/forgerock/openidm/ui/admin/managed/schema/util/SchemaUtils",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openidm/ui/admin/managed/schema/EditPolicyDialog"
], function($, _,
    Backbone,
    AbstractManagedView,
    SchemaUtils,
    Backgrid,
    BackgridUtils,
    UIUtils,
    EditPolicyDialog
) {
    var PolicyView = AbstractManagedView.extend({
        template: "templates/admin/managed/schema/PolicyViewTemplate.html",
        element: "#policyContainer",
        noBaseTemplate: true,
        model: {},
        events: {
            "click #addNewPolicy" : "addNewPolicy"
        },
        partials: [
            "partials/managed/schema/_policyParamNewRow.html",
            "partials/managed/schema/_policyParamEditableRow.html"
        ],

        render: function(args, callback) {
            this.parent = args[0];
            this.data.policies = this.parent.data.property.policies || [];

            this.parentRender(() => {
                this.setupPoliciesGrid();

                if (callback) {
                    callback();
                }

            });

        },
        /**
         *
         */
        setupPoliciesGrid: function () {
            var self = this,
                listElement = this.$el.find(".policyList"),
                cols = [
                    {
                        name: "policyId",
                        label: $.t("templates.managed.schemaEditor.policies"),
                        cell: "string",
                        sortable: false,
                        editable: false
                    },
                    {
                        name: "params",
                        label: $.t("templates.managed.schemaEditor.parameters"),
                        cell: Backgrid.Cell.extend({
                            render: function () {
                                var params = this.model.get("params");

                                this.$el.html(_.keys(params).join(","));

                                return this;
                            }
                        }),
                        sortable: false,
                        editable: false
                    },
                    {
                        label: "",
                        cell: BackgridUtils.ButtonCell([
                            {
                                className: "fa fa-times grid-icon col-sm-1 pull-right",
                                callback: function(e) {
                                    var itemIndex = SchemaUtils.getClickedRowIndex(e);
                                    e.preventDefault();

                                    UIUtils.confirmDialog($.t("templates.managed.schemaEditor.confirmPolicyDelete", { policyId: this.model.get("policyId")}), "danger", () => {
                                        self.parent.data.property.policies.splice(itemIndex,1);
                                        self.saveProperty();
                                    });
                                }
                            },
                            {
                                // No callback necessary, the row click will trigger the edit
                                className: "fa fa-pencil grid-icon col-sm-1 pull-right"
                            },
                            {
                                className: "dragToSort fa fa-arrows grid-icon col-sm-1 pull-right"
                            }
                        ]),
                        sortable: false,
                        editable: false
                    }
                ],
                policiesGrid,
                makeSortable = () => {
                    BackgridUtils.sortable({
                        "containers": [listElement.find("tbody")[0]],
                        "rows": _.clone(this.data.policies, true)
                    }, (newOrder) => {
                        this.parent.data.property.policies = newOrder;
                        this.saveProperty();
                    });
                };

            //empty the existing
            listElement.empty();

            this.model.policiesCollection = new Backbone.Collection(this.data.policies);

            policiesGrid = new Backgrid.Grid({
                className: "table backgrid table-hover",
                emptyText: $.t("templates.admin.ResourceList.noData"),
                columns: BackgridUtils.addSmallScreenCell(cols),
                collection: this.model.policiesCollection,
                row: BackgridUtils.ClickableRow.extend({
                    callback: function(e) {
                        var itemIndex = SchemaUtils.getClickedRowIndex(e);

                        e.preventDefault();

                        //open policyDialog here
                        if (!$(e.target).hasClass("fa-times")) {
                            self.openEditPolicyDialog(this.model.toJSON(), itemIndex);
                        }
                    }
                })
            });

            listElement.append(policiesGrid.render().el);

            makeSortable();
        },
        addNewPolicy : function (e) {
            e.preventDefault();

            this.openEditPolicyDialog();
        },
        openEditPolicyDialog: function(policy, index) {
            var args = {
                savePolicy: (editedPolicy) => {
                    if (!_.isArray(this.parent.data.property.policies)) {
                        this.parent.data.property.policies = [];
                    }
                    this.parent.data.property.policies.push(editedPolicy);
                    this.saveProperty();
                }
            };

            if (policy) {
                args = {
                    policy: policy,
                    savePolicy: (editedPolicy) => {
                        this.parent.data.property.policies[index] = editedPolicy;
                        this.saveProperty();
                    }
                };
            }

            EditPolicyDialog.render(args);
        },
        saveProperty: function () {
            this.parent.saveProperty(false, () => {
                this.parent.$el.find('a[href="#policyContainer"]').tab('show');
            });
        }

    });

    return new PolicyView();
});

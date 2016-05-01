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
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/main/AbstractCollection",
    "backgrid",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils"
], function($, _,
            AdminAbstractView,
            AbstractModel,
            AbstractCollection,
            Backgrid,
            BackgridUtils) {
    var ProcessDefinitionsView = AdminAbstractView.extend({
        template: "templates/admin/workflow/ProcessDefinitionsViewTemplate.html",
        events: {
        },
        model : {

        },
        element: "#processDefinitions",
        render: function(args, callback) {
            this.parentRender(_.bind(function(){
                this.parentRender(_.bind(function() {
                    var processDefinitionGrid,
                        ProcessDefinitionModel = AbstractModel.extend({ "url": "/openidm/workflow/processdefinition" }),
                        Process = AbstractCollection.extend({ model: ProcessDefinitionModel });

                    this.model.processes = new Process();
                    this.model.processes.url = "/openidm/workflow/processdefinition?_queryId=filtered-query";
                    this.model.processes.state.pageSize = null;

                    processDefinitionGrid = new Backgrid.Grid({
                        className: "table backgrid",
                        emptyText: $.t("templates.workflows.processes.noProcessesDefinitions"),
                        columns: BackgridUtils.addSmallScreenCell([
                            {
                                name: "name",
                                label: $.t("templates.processInstance.definition"),
                                cell: Backgrid.Cell.extend({
                                    render: function () {
                                        this.$el.html('<a href="#workflow/processdefinition/' +this.model.id +'">' +this.model.get("name") +'<small class="text-muted"> (' +this.model.id +')</small></a>');

                                        this.delegateEvents();
                                        return this;
                                    }
                                }),
                                sortable: true,
                                editable: false
                            }], true),
                        collection: this.model.processes
                    });

                    this.$el.find("#processDefinitionGridHolder").append(processDefinitionGrid.render().el);

                    this.model.processes.getFirstPage();

                    if(callback) {
                        callback();
                    }
                }, this));
            }, this));
        }
    });

    return new ProcessDefinitionsView();
});
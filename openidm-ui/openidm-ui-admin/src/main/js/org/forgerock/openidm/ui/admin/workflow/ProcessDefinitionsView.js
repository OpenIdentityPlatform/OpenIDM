/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, _, Handlebars */

define("org/forgerock/openidm/ui/admin/workflow/ProcessDefinitionsView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/commons/ui/common/main/AbstractCollection",
    "backgrid"
], function(AdminAbstractView,
            AbstractModel,
            AbstractCollection,
            Backgrid) {
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
                        className: "table",
                        emptyText: $.t("templates.workflows.processes.noProcessesDefinitions"),
                        columns: [
                        {
                            name: "name",
                            label: $.t("templates.workflows.processes.processDefinitions"),
                            cell: "string",
                            sortable: true,
                            editable: false
                        }],
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
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

/*global define */

define("org/forgerock/openidm/ui/admin/workflow/ProcessListView", [
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/openidm/ui/admin/workflow/ActiveProcessesView",
    "org/forgerock/openidm/ui/admin/workflow/ProcessDefinitionsView",
    "org/forgerock/openidm/ui/admin/workflow/ProcessHistoryView",
    "org/forgerock/commons/ui/common/main/AbstractCollection"
], function(_,
            AdminAbstractView,
            ActiveProcessesView,
            ProcessDefinitionsView,
            ProcessHistoryView,
            AbstractCollection) {
    var ProcessListView = AdminAbstractView.extend({
        template: "templates/admin/workflow/ProcessListViewTemplate.html",
        events: {
            "change #processFilterType" : "filterType"
        },
        model : {

        },
        render: function(args, activeProcessCallback, processDefinitionsCallback, processHistoryCallback) {
            var processDefinition = {};

            this.parentRender(_.bind(function(){
                this.model.processDefinitions = new AbstractCollection();
                this.model.processDefinitions.url =  "/openidm/workflow/processdefinition?_queryId=filtered-query";

                this.model.processDefinitions.getFirstPage().then(function(processDefinitions){
                    processDefinition = _.chain(processDefinitions.result)
                        .map(function (pd) {
                            return _.pick(pd,"name","key");
                        })
                        .uniq(function (pdm) {
                            return pdm.name;
                        })
                        .value();

                    ActiveProcessesView.render([processDefinition] , activeProcessCallback);
                    ProcessDefinitionsView.render([], processDefinitionsCallback);
                    ProcessHistoryView.render([processDefinition], processHistoryCallback);
                });
            }, this));
        }
    });

    return new ProcessListView();
});

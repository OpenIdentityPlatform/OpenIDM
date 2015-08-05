/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/admin/workflow/ProcessDefinitionView", [
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/openidm/ui/admin/util/WorkflowUtils"
], function(_, AbstractView, eventManager, constants, UIUtils, AbstractModel, WorkflowUtils) {
    var ProcessModel = AbstractModel.extend({ url: "/openidm/workflow/processdefinition" }),
        ProcessDefinitionView = AbstractView.extend({
            template: "templates/admin/workflow/ProcessDefinitionViewTemplate.html",

            events: {

            },
            render: function(args, callback) {
                this.model = new ProcessModel();

                this.model.id = args[0];

                this.model.fetch().then(_.bind(function () {

                    this.data.processDefinition = this.model.toJSON();

                    this.data.diagramUrl = "/openidm/workflow/processdefinition/" + this.model.id + "?_fields=/diagram&_mimeType=image/png";

                    this.parentRender(_.bind(function(){

                        if (callback) {
                            callback();
                        }
                    },this));

                },this));
            }
        });

    return new ProcessDefinitionView();
});

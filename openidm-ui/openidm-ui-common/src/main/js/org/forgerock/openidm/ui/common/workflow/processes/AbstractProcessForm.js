/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/common/workflow/processes/AbstractProcessForm", [
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/workflow/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(_, AbstractView, validatorsManager, eventManager, constants, workflowManager, conf) {
    var AbstractProcessForm = AbstractView.extend({
        template: "templates/common/EmptyTemplate.html",
        element: "#processContent",

        events: {
            "onValidate": "onValidate"
        },

        postRender: function() {

        },

        prepareData: function(callback) {
            callback();
        },

        render: function(processDefinition, category, args, callback) {
            this.setElement(this.element);
            this.$el.unbind();
            this.delegateEvents();
            this.processDefinition = processDefinition;
            this.category = category;
            this.args = args;

            this.prepareData(_.bind(function() {
                this.parentRender(function() {
                    this.postRender(callback);
                    this.reloadData();
                });
            }, this));

        },

        reloadData: function() {
        }

    });

    return AbstractProcessForm;
});

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

define("org/forgerock/openidm/ui/common/workflow/processes/TemplateStartProcessForm", [
    "handlebars",
    "org/forgerock/openidm/ui/common/workflow/processes/AbstractProcessForm",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(Handlebars, AbstractProcessForm, DateUtil, conf, uiUtils) {

    var TemplateStartProcessForm = AbstractProcessForm.extend({

        template: "templates/common/EmptyTemplate.html",

        postRender: function(callback) {
            var t = Handlebars.compile(this.args)(this.processDefinition);

            this.$el.html(t);

            if (callback) {
                callback();
            }
        }

    });

    return new TemplateStartProcessForm();
});

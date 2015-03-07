/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

define("org/forgerock/openidm/ui/admin/sync/ReconScriptsView", [
    "org/forgerock/openidm/ui/admin/sync/MappingScriptsView"
], function(MappingScriptsView) {
    var ReconScriptsView = MappingScriptsView.extend({
        element: "#reconQueryView",
        noBaseTemplate: true,
        events: {
            "click .addScript": "addScript",
            "click .saveScripts": "saveScripts"
        },
        model: {
            scripts: ["result"],
            scriptEditors: {},
            successMessage: "triggeredByReconSaveSuccess"
        },

        /**
         * @param args {object}
         *      sync {object}
         *      mapping {object}
         *      mappingName {string}
         *
         */
        render: function(args) {
            this.model.sync = args.sync;
            this.model.mapping = args.mapping;
            this.model.mappingName = args.mappingName;

            this.parentRender(function () {
                this.init();
            });
        }
    });

    return new ReconScriptsView();
});

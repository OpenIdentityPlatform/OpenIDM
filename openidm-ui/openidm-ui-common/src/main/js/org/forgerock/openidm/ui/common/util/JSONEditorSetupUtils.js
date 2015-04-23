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

/*global define , _, $*/

/**
 * @author huck.elliott
 */
define("org/forgerock/openidm/ui/common/util/JSONEditorSetupUtils", [
    "jsonEditor"
], function (JSONEditor) {
        var JSONEditorPostBuild = JSONEditor.AbstractEditor.prototype.postBuild;
        JSONEditor.AbstractEditor.prototype.postBuild = function () {
            var ret = JSONEditorPostBuild.apply(this, arguments);
            if (this.path && this.input && this.label && !this.input.id && !this.label.htmlFor) {
                this.input.id = (this.jsoneditor.options.uuid || this.jsoneditor.uuid) + "." + this.path;
                this.label.htmlFor = (this.jsoneditor.options.uuid || this.jsoneditor.uuid) + "." + this.path;
            }
            return ret;
        };
        
});

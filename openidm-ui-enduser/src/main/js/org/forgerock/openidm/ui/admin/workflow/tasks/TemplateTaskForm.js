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

/*global define, $, form2js, _, js2form, Handlebars */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/workflow/tasks/TemplateTaskForm", [
    "org/forgerock/openidm/ui/admin/workflow/tasks/AbstractTaskForm",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractTaskForm, DateUtil, conf, uiUtils) {
    
    var TemplateTaskForm = AbstractTaskForm.extend({
        
        template: "templates/common/EmptyTemplate.html",
        
        postRender: function(callback) {            
            var t = Handlebars.compile(this.args)(this.task);
            
            this.$el.html(t);
            
            if (callback) {
                callback();
            }
        }
    }); 
    
    return new TemplateTaskForm();
});



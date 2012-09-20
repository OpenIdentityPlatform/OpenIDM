/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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

/*global define, $, form2js, _, js2form, document */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/tasks/ApplicationAcceptanceTask", [
    "org/forgerock/openidm/ui/admin/tasks/AbstractTaskForm",
    "org/forgerock/openidm/ui/common/util/DateUtil"
], function(AbstractTaskForm, DateUtil) {
    var ApplicationAcceptanceTask = AbstractTaskForm.extend({
        template: "templates/admin/tasks/ApplicationAcceptanceTemplate.html",
        
        reloadData: function() {
            js2form(document.getElementById(this.$el.attr("id")), this.task);
            this.$el.find("input[name=taskName]").val(this.task.name);
            this.$el.find("input[name=createTime]").val(DateUtil.formatDate(this.task.createTime));
            this.$el.find("input[name=saveButton]").val("Update");
            this.$el.find("input[name=backButton]").val("Back");
        }
            
    }); 
    
    return new ApplicationAcceptanceTask();
});



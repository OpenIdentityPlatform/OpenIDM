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

/*global define, $, form2js, _, js2form, document */

/**
 * @author mbilski
 */
define("org/forgerock/openidm/ui/admin/tasks/ApplicationAcceptanceTask", [
    "org/forgerock/openidm/ui/admin/tasks/AbstractTaskForm",
    "org/forgerock/commons/ui/common/util/DateUtil",
    "org/forgerock/openidm/ui/apps/delegates/UserApplicationLnkDelegate",
    "org/forgerock/commons/ui/user/delegates/UserDelegate",
    "org/forgerock/openidm/ui/apps/delegates/ApplicationDelegate",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractTaskForm, DateUtil, userApplicationLnkDelegate, userDelegate, applicationDelegate, conf) {
    var ApplicationAcceptanceTask = AbstractTaskForm.extend({
        template: "templates/admin/tasks/ApplicationAcceptanceTemplate.html",
        
        reloadData: function() {
            var self = this;
            js2form(document.getElementById(this.$el.attr("id")), this.task);
            this.$el.find("input[name=taskName]").val(this.task.name);
            this.$el.find("input[name=createTime]").val(DateUtil.formatDate(this.task.createTime));
            this.$el.find("input[name=saveButton]").val("Update");
            
            if(this.category === "all") {
                this.$el.find("input[name=claimButton]").val("Claim");                
                
                if(this.task.assignee === conf.loggedUser.userName) {
                    this.$el.find("input[name=saveButton]").show();
                } else {
                    this.$el.find("input[name=saveButton]").hide();
                }
            } else {
                this.$el.find("input[name=claimButton]").val("Unclaim");
                this.$el.find("input[name=saveButton]").show();
            }
            
            userApplicationLnkDelegate.readEntity(this.task.description, function(userAppLink) {
                
                userDelegate.readEntity(userAppLink.userId, function(user) {
                    self.$el.find("input[name=userData]").val(user.givenName + " " + user.familyName);
                });
                
                applicationDelegate.getApplicationDetails(userAppLink.applicationId, function(app) {
                    self.$el.find("input[name=requestedApplicationName]").val(app.name);
                });
                
            });
        }
    }); 
    
    return new ApplicationAcceptanceTask();
});



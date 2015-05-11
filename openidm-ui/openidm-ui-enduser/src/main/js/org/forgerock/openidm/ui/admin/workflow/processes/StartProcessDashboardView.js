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

/*global define, $, form2js, _ */

/**
 * @author jdabrowski
 */
define("org/forgerock/openidm/ui/admin/workflow/processes/StartProcessDashboardView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openidm/ui/admin/workflow/WorkflowDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/admin/workflow/processes/StartProcessView",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, workflowManager, eventManager, constants, startProcessView, conf) {
    var StartProcessDashboardView = AbstractView.extend({

        template: "templates/admin/workflow/processes/StartProcessDashboardTemplate.html",

        events: {
            "click .processName": "showStartProcessView"
        },

        element: "#processes",

        render: function(args) {
            var processId;
            if (args && args[0] && args[0] !== '') {
                processId = args[0];
            }
            workflowManager.getAllUniqueProcessDefinitions(conf.loggedUser._id, _.bind(function(processDefinitions) {
                var i;
                this.data.processDefinitions = processDefinitions;

                this.parentRender(function() {
                    if(processDefinitions.length === 0) {
                        $("#processContentBody").html("<h5 class='text-center'>" +$.t("openidm.ui.admin.tasks.StartProcessDashboardView.noProcesses") +"</h5>");
                    } else {
                        $("#processBadge").html(processDefinitions.length);
                        $("#processBadge").show();
                    }

                    if (processId) {
                        this.renderStartProcessView(processId);
                    }
                });


            }, this));
        },

        showStartProcessView: function(event) {
            event.preventDefault();
            var id = $(event.target).parent().find('[name="id"]').val();

            $("#processDetails").remove();
            $(".selected-process").removeClass('selected-process');
            $(event.target).closest('div').addClass('selected-process');
            $(event.target).closest('div').append('<div id="processDetails" style="margin-top: 10px;"></div>');

            this.renderStartProcessView(id);
        },

        renderStartProcessView: function(id) {
            startProcessView.render(id, "", function() {
                $("#processContent [disabled]:hidden").filter(function(){return $(this).siblings(":visible").length === 0;}).parent().hide();

                if($("#processContent").html() === "") {
                    $("#processContent").html($.t("openidm.ui.admin.tasks.StartProcessDashboardView.noDataRequired"));
                }
            });
        }

    });

    return new StartProcessDashboardView();
});



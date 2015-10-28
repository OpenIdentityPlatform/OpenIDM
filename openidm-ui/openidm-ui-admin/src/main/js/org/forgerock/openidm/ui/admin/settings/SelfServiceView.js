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
 * Copyright 2015 ForgeRock AS.
 */

/*global define */

define("org/forgerock/openidm/ui/admin/settings/SelfServiceView", [
    "underscore",
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager"
], function(_,
            AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            validatorsManager) {

    var SelfServiceView = AdminAbstractView.extend({
        template: "templates/admin/settings/SelfServiceTemplate.html",
        element: "#selfServiceContainer",
        noBaseTemplate: true,
        events: {
            "click #saveSelfServiceURL": "saveSelfServiceURL",
            "onValidate": "onValidate",
            "customValidate": "customValidate"
        },
        model: {
            uiContextObject: {}
        },
        data: {
            selfServiceURL: ""
        },

        render: function (args, callback) {
            this.data.docHelpUrl = constants.DOC_URL;

            ConfigDelegate.readEntity("ui.context/selfservice"). then(_.bind(function(data) {
                this.model.uiContextObject = data;
                this.data.selfServiceURL = data.urlContextRoot;

                this.parentRender(_.bind(function() {
                    validatorsManager.bindValidators(this.$el.find("#urlConfigBody"));
                    validatorsManager.validateAllFields(this.$el.find("#urlConfigBody"));
                }, this));
            }, this));
        },

        saveSelfServiceURL: function(e) {
            e.preventDefault();
            this.model.uiContextObject.urlContextRoot = this.$el.find("#selfServiceURL").val();

            ConfigDelegate.updateEntity("ui.context/selfservice", this.model.uiContextObject).then(_.bind(function () {
                eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "selfServiceSaveSuccess");
            }, this));
        },

        customValidate: function() {
            this.validationResult = validatorsManager.formValidated(this.$el.find("#urlConfigBody"));

            this.$el.find("#saveSelfServiceURL").prop('disabled', !this.validationResult);
        }
    });

    return new SelfServiceView();
});
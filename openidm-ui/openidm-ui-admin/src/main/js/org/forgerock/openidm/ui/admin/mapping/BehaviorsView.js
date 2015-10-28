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

define("org/forgerock/openidm/ui/admin/mapping/BehaviorsView", [
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/delegates/SchedulerDelegate",
    "org/forgerock/openidm/ui/admin/util/Scheduler",
    "org/forgerock/openidm/ui/admin/mapping/behaviors/PoliciesView",
    "org/forgerock/openidm/ui/admin/mapping/behaviors/SituationalEventScriptsView",
    "org/forgerock/openidm/ui/admin/mapping/behaviors/ReconciliationScriptView",
    "org/forgerock/openidm/ui/admin/mapping/behaviors/SingleRecordReconciliationView",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingUtils"

], function(_,
            MappingAdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            SchedulerDelegate,
            Scheduler,
            PoliciesView,
            SituationalEventScriptsView,
            ReconciliationScriptView,
            SingleRecordReconciliationView,
            mappingUtils) {

    var BehaviorsView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/BehaviorsTemplate.html",
        element: "#mappingContent",
        noBaseTemplate: true,
        mapping: null,

        render: function (args, callback) {
            this.data.docHelpUrl = constants.DOC_URL;
            this.model = {
                args: args,
                callback: callback
            };
            this.mapping = this.getCurrentMapping();

            this.data.mappingName = this.getMappingName();
            this.data.hideSituational = true;
            this.data.hideRecon = true;
            this.data.hideSingleRecordRecon = mappingUtils.readOnlySituationalPolicy(this.mapping.policies);
            this.data.borderHide = false;

            if(this.data.hideSingleRecordRecon || !this.getRecon()) {
                this.data.borderHide = true;
            }

            _.each(SituationalEventScriptsView.model.scripts, function (script) {
                if (_.has(SituationalEventScriptsView.model, "mapping")) {
                    if (_.has(SituationalEventScriptsView.model.mapping, script)) {
                        this.data.hideSituational = false;
                    }
                } else if (_.has(this.mapping, script)) {
                    this.data.hideSituational = false;
                }
            }, this);

            _.each(ReconciliationScriptView.model.scripts, function (script) {
                if (_.has(ReconciliationScriptView.model, "mapping")) {
                    if (_.has(ReconciliationScriptView.model.mapping, script)) {
                        this.data.hideRecon = false;
                    }
                } else if (_.has(this.mapping, script)) {
                    this.data.hideRecon = false;
                }
            }, this);

            this.parentRender(_.bind(function () {
                PoliciesView.render({
                    saveCallback: _.bind(function () {
                        this.render(this.model.args, this.model.callback);
                    }, this)
                });
                SituationalEventScriptsView.render();
                ReconciliationScriptView.render();
                SingleRecordReconciliationView.render({recon: this.getRecon()});

                if (callback) {
                    callback();
                }

            }, this));
        }
    });

    return new BehaviorsView();
});
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

/*global define, $, _ */

define("org/forgerock/openidm/ui/admin/sync/CorrelationView", [
    "org/forgerock/openidm/ui/admin/util/AdminAbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openidm/ui/common/delegates/ConfigDelegate",
    "org/forgerock/openidm/ui/admin/mapping/MappingBaseView",
    "org/forgerock/openidm/ui/admin/sync/AnalysisView",
    "org/forgerock/openidm/ui/admin/sync/ObjectFiltersView",
    "org/forgerock/openidm/ui/admin/sync/CorrelationQueryView"
], function(AdminAbstractView,
            eventManager,
            constants,
            ConfigDelegate,
            MappingBaseView,
            AnalysisView,
            ObjectFiltersView,
            CorrelationQueryView) {

    var CorrelationView = AdminAbstractView.extend({
        template: "templates/admin/sync/CorrelationTemplate.html",
        element: "#mappingContent",
        noBaseTemplate: true,
        events: {
            "click .correlationBody fieldset legend" : "sectionHideShow"
        },
        data: {},
        dataModel: {},

        render: function (args, callback) {
            MappingBaseView.child = this;
            MappingBaseView.render(args,_.bind(function(){
                this.loadData(args, callback);
            }, this));
        },
        loadData: function(args, callback){
            this.dataModel.sync = MappingBaseView.data.syncConfig;
            this.dataModel.mapping = _.omit(MappingBaseView.currentMapping(),"recon");
            this.dataModel.mappingName = this.mappingName = args[0];

            this.data.hideObjectFilters = true;
            _.each(ObjectFiltersView.model.scripts, function(script) {
                if (_.has(ObjectFiltersView.model, "mapping")) {
                    if (_.has(ObjectFiltersView.model.mapping, script)) {
                        this.data.hideObjectFilters = false;
                    }
                } else if (_.has(this.dataModel.mapping, script)) {
                    this.data.hideObjectFilters = false;
                }
            }, this);

            this.parentRender(_.bind(function () {
                AnalysisView.render({sync: this.dataModel.sync, mapping: this.dataModel.mapping, mappingName: this.dataModel.mappingName});
                CorrelationQueryView.render({sync: this.dataModel.sync, mapping: this.dataModel.mapping, mappingName: this.dataModel.mappingName, startSync: this.sync});
                ObjectFiltersView.render({sync: this.dataModel.sync, mapping: this.dataModel.mapping, mappingName: this.dataModel.mappingName});
                MappingBaseView.moveSubmenu();
                if(callback){
                    callback();
                }
            }, this));
        },

        sync: function () {
            MappingBaseView.syncNow();
        }
    });

    return new CorrelationView();
});

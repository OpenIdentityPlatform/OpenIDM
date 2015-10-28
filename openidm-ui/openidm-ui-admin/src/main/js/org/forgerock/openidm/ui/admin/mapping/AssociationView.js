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

define("org/forgerock/openidm/ui/admin/mapping/AssociationView", [
    "underscore",
    "org/forgerock/openidm/ui/admin/mapping/util/MappingAdminAbstractView",
    "org/forgerock/openidm/ui/admin/mapping/association/DataAssociationManagementView",
    "org/forgerock/openidm/ui/admin/mapping/association/IndividualRecordValidationView",
    "org/forgerock/openidm/ui/admin/mapping/association/ReconciliationQueryFiltersView",
    "org/forgerock/openidm/ui/admin/mapping/association/AssociationRuleView"
], function(_,
            MappingAdminAbstractView,
            DataAssociationManagementView,
            IndividualRecordValidationView,
            ReconciliationQueryFiltersView,
            AssociationRuleView) {

    var AssociationView = MappingAdminAbstractView.extend({
        template: "templates/admin/mapping/AssociationTemplate.html",
        element: "#mappingContent",
        noBaseTemplate: true,

        render: function (args, callback) {
            var mapping = this.getCurrentMapping();

            this.data.hideObjectFilters = true;
            _.each(IndividualRecordValidationView.model.scripts, function(script) {
                if (_.has(IndividualRecordValidationView.model, "mapping")) {
                    if (_.has(IndividualRecordValidationView.model.mapping, script)) {
                        this.data.hideObjectFilters = false;
                    }
                } else if (_.has(mapping, script)) {
                    this.data.hideObjectFilters = false;
                }
            }, this);

            this.data.hideReconQueries = !mapping.sourceQuery && !mapping.targetQuery;

            this.parentRender(_.bind(function () {
                DataAssociationManagementView.render();
                ReconciliationQueryFiltersView.render();
                IndividualRecordValidationView.render();
                AssociationRuleView.render({});

                if (callback) {
                    callback();
                }
            }, this));
        }
    });

    return new AssociationView();
});

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
 * Copyright 2016 ForgeRock AS.
 */
/*global define */

define("org/forgerock/openidm/ui/common/resource/ResourceCollection", [
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractModel",
    "org/forgerock/openidm/ui/admin/util/BackgridUtils",
    "org/forgerock/commons/ui/common/main/AbstractCollection"
], function(_, AbstractModel, BackgridUtils, AbstractCollection) {
    var ResourceCollection = AbstractCollection.extend({
        initialize: function(models, options) {
            this.url = options.url;
            this.model = AbstractModel.extend({ "url": options.url });
            this.state = options.state;
            _.extend(this.queryParams, BackgridUtils.getQueryParams({
                _queryFilter: options._queryFilter
            }, options.isSystemResource));
        }
    });
    return ResourceCollection;
});

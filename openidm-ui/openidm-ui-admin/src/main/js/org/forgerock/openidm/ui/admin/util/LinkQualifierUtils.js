/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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
 * your own identifying information:i
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define */

define("org/forgerock/openidm/ui/admin/util/LinkQualifierUtils", [
    "jquery",
    "underscore",
    "org/forgerock/openidm/ui/admin/delegates/ScriptDelegate"
], function ($, _, ScriptDelegate) {
    var obj = {};

    obj.model = {
        linkQualifier: []
    };

    obj.checkLinkQualifier = function (mapping) {
        var linkQualifierPromise = $.Deferred();

        if(mapping.linkQualifiers !== undefined) {

            if(mapping.linkQualifiers.type) {
                if(this.model.linkQualifier[mapping.name] !== null &&  this.model.linkQualifier[mapping.name] !== undefined) {
                    linkQualifierPromise.resolve(this.model.linkQualifier[mapping.name]);
                } else {
                    ScriptDelegate.evalLinkQualifierScript(mapping.linkQualifiers).then(_.bind(function(result){
                        this.model.linkQualifier[mapping.name] = result;

                        linkQualifierPromise.resolve(this.model.linkQualifier[mapping.name]);
                    }, this));
                }
            } else {
                this.model.linkQualifier[mapping.name] = mapping.linkQualifiers;

                linkQualifierPromise.resolve(this.model.linkQualifier[mapping.name]);
            }
        } else {
            this.model.linkQualifier[mapping.name] = ['default'];

            linkQualifierPromise.resolve(this.model.linkQualifier[mapping.name]);
        }

        return linkQualifierPromise;
    };

    obj.getLinkQualifier = function(mappingName) {
        return this.model.linkQualifier[mappingName];
    };

    obj.setLinkQualifier = function(linkQualifier, mappingName) {
        this.model.linkQualifier[mappingName] = linkQualifier;
    };

    return obj;
});

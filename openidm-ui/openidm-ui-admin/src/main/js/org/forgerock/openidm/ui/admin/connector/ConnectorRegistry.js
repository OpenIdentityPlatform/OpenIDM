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
 * Copyright 2014-2015 ForgeRock AS.
 */

/*global define, require */

define("org/forgerock/openidm/ui/admin/connector/ConnectorRegistry", [
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/util/ModuleLoader"
], function(_, AbstractConfigurationAware, ModuleLoader) {
    var obj = new AbstractConfigurationAware();

    obj.updateConfigurationCallback = function (conf) {
        this.configuration = conf;

        _.each(conf,function(val,key){
            require.config({"map":
                { "*":
                    { key : val }
                }
            });
        });
    };

    obj.getConnectorModule = function (type) {
        if(_.isUndefined(this.configuration[type])) {
            return ModuleLoader.load("org/forgerock/openidm/ui/admin/connector/ConnectorTypeView");
        } else {
            return ModuleLoader.load(this.configuration[type]);
        }
    };

    return obj;
});

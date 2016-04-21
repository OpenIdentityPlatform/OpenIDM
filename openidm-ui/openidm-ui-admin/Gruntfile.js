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

var common = require('../Gruntfile-common');

module.exports = function(grunt) {
    var forgeRockCommonsDirectory = process.env.FORGEROCK_UI_SRC + "/forgerock-ui-commons";

    common(grunt, {
        watchCompositionDirs: [
            forgeRockCommonsDirectory + "/src/main/js",
            forgeRockCommonsDirectory + "/src/main/resources"
        ],
        deployDirectory: "admin/default",
        eslintFormatter: require.resolve("eslint-formatter-warning-summary"),
        lessPlugins: [new (require("less-plugin-clean-css"))({})]
    });
};

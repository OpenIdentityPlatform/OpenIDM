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
 * Portions copyright 2026 3A Systems LLC.
 */

define([
    "org/forgerock/openidm/ui/common/util/Constants"
], function (Constants) {
    QUnit.module('Constants Tests');

    QUnit.test("Constants.context defaults to 'openidm'", function (assert) {
        assert.equal(Constants.context, "openidm",
            "The default context should be 'openidm'");
    });

    QUnit.test("Constants.context produces correct REST API path prefix", function (assert) {
        var contextPath = "/" + Constants.context;
        assert.equal(contextPath, "/openidm",
            "The default REST API path prefix should be '/openidm'");
    });

    QUnit.test("Constants.context can be overridden for custom REST path", function (assert) {
        var originalContext = Constants.context;
        Constants.context = "myidm";
        assert.equal(Constants.context, "myidm",
            "Constants.context should be overridable to a custom value");
        assert.equal("/" + Constants.context, "/myidm",
            "Custom context should produce the correct REST API path prefix");
        Constants.context = originalContext;
    });

    QUnit.test("window.__openidm_context_path overrides the default context", function (assert) {
        var originalValue = window.__openidm_context_path;
        window.__openidm_context_path = "myidm";
        var contextValue = (typeof window !== "undefined" && window.__openidm_context_path)
            ? window.__openidm_context_path
            : "openidm";
        assert.equal(contextValue, "myidm",
            "window.__openidm_context_path should override the default 'openidm' context");
        window.__openidm_context_path = originalValue;
    });

    QUnit.test("window.__openidm_context_path falls back to 'openidm' when unset", function (assert) {
        var originalValue = window.__openidm_context_path;
        delete window.__openidm_context_path;
        var contextValue = (typeof window !== "undefined" && window.__openidm_context_path)
            ? window.__openidm_context_path
            : "openidm";
        assert.equal(contextValue, "openidm",
            "Should fall back to 'openidm' when window.__openidm_context_path is not set");
        if (originalValue !== undefined) {
            window.__openidm_context_path = originalValue;
        }
    });
});

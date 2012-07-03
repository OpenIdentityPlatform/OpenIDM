/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2012 ForgeRock AS. All rights reserved.
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

/*global define, require */

/**
 * @author yaromin
 */

define(["app/util/UIUtils"],function(cut) {
    QUnit.module("UIUtils");
    
    QUnit.test("convertQueryParametersToJSON", function() {
        var result = cut.convertQueryParametersToJSON("action=user.SOME_EVENT")
        QUnit.equal(result.action, "user.SOME_EVENT");
        
        result = cut.convertQueryParametersToJSON("action=user.user.SOME_EVENT&otherparam=some")
        QUnit.equal(result.otherparam, "some");
    });
    
    QUnit.test("normalizeSubPath", function() {
        console.debug(cut.normalizeSubPath("/path/"));
        QUnit.equal(cut.normalizeSubPath("/path/"), "/path");
        QUnit.equal(cut.normalizeSubPath("/path"), "/path");
    });
});


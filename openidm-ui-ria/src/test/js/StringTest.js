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

define(["org/forgerock/common/js/typeextentions/String"],function(cut) {
    QUnit.module("Strings");
    
    QUnit.test("removeLastChar", function() {
        QUnit.equal("abcde".removeLastChars(1), "abcd");
        QUnit.equal("abcde".removeLastChars(), "abcd");
        QUnit.equal("abcde".removeLastChars(2), "abc");
    });
    
    QUnit.test("endsWith", function() {
        QUnit.ok(!"abcde".endsWith("abcd"));
        QUnit.ok("abcde".endsWith("de"));
        QUnit.ok("abcde".endsWith("e"));
        QUnit.ok(!"abcde".endsWith("aaa"));
    });
    
    QUnit.test("startsWith", function() {
        QUnit.ok(!"abcde".startsWith("bdsa"));
        QUnit.ok("abcde".startsWith("abc"));
        QUnit.ok("abcde".startsWith("abcde"));
        QUnit.ok(!"abcde".startsWith("abcdef"));
    });
});


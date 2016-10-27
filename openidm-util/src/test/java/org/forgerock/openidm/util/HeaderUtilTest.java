/*
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

package org.forgerock.openidm.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class HeaderUtilTest {

    @DataProvider
    public Object[][] testDecodeRfc5987Data() {
        return new Object[][]{
                // not detected to be in RFC 5987 format, so passes through unchanged
                { "utf-8", "utf-8", null },
                { "utf-8'", "utf-8'", null },
                { "utf-8''", "utf-8''", null },
                { "utf-8'''", "utf-8'''", null },
                // UTF-8 without lang-tag
                { "utf-8''emptyLangTag%C2%A3", "emptyLangTag£", null },
                // UTF-8 with lang-tag (has no effect on decoding)
                { "utf-8'en'hasLangTag%C2%A3", "hasLangTag£", null },
                // malformed percent-encoding
                { "utf-8''%", null, IllegalArgumentException.class },
                { "utf-8''%C", null, IllegalArgumentException.class },
                { "utf-8''%0_", null, IllegalArgumentException.class },
                { "utf-8''%_0", null, IllegalArgumentException.class },
                { "utf-8''£", null, IllegalArgumentException.class },
                // in addition to UTF-8, ISO-8859-1 is also supported
                { "ISO-8859-1''%ff", "ÿ", null },
        };
    }

    @Test(dataProvider = "testDecodeRfc5987Data")
    public void testDecodeRfc5987(final String input, final String expectedOutput,
            final Class<? extends Throwable> expectedException) {
        try {
            final String actualOutput = HeaderUtil.decodeRfc5987(input);
            if (expectedException != null) {
                failBecauseExceptionWasNotThrown(expectedException);
            }
            assertThat(actualOutput).isEqualTo(expectedOutput);
        } catch (final Exception e) {
            if (expectedException != null) {
                assertThat(e).isInstanceOf(expectedException);
            }
        }
    }

}

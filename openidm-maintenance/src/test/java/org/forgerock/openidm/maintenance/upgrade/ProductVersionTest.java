/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for
 * the specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file
 * and include the License file at legal/CDDLv1.0.txt. If applicable, add the following
 * below the CDDL Header, with the fields enclosed by brackets [] replaced by your
 * own identifying information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openidm.maintenance.upgrade;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit tests to cover behavior of {@link ProductVersion}.
 */
public class ProductVersionTest {
    private static ProductVersion baseVersion = new ProductVersion("5.0.0", "0");

    @DataProvider
    public Object[][] versions() {
        return new Object[][] {
                // @formatter:off
                { "5.0.0", "0", true },
                { "5.0.0-1", "0", false },
                { "5.0.1", "0", false },
                // The next 3 cases *should* match but ComparableVersion doesn't implement the behavior we want
                //{ "5.0.0-RC1", "0", true },
                //{ "5.0.0-SNAPSHOT", "0", true },
                //{ "5.0.0-RC3-SNAPSHOT", "0", true },
                { "5.0.0-1-SNAPSHOT", "0", false }
                // @formatter:on
        };
    }

    @Test(dataProvider = "versions")
    public void testVersionStringMatches(String version, String revision, boolean shouldMatch) {
        ProductVersion pv = new ProductVersion(version, revision);
        assertThat(pv.isSameAs(baseVersion)).isEqualTo(shouldMatch);
    }
}

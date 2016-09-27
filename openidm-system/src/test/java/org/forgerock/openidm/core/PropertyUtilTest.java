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
package org.forgerock.openidm.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openidm.core.PropertyUtil.*;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

/**
 * Tests the PropertyUtil class.
 *
 * @see PropertyUtil
 */
public class PropertyUtilTest {
    private Map<String, String> props = new HashMap<>();

    private PropertyAccessor propertyAccessor = new PropertyAccessor() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> T getProperty(String key, T defaultValue, Class<T> expected) {
            T value = null;
            if (null != key
                    && ((null != expected && expected.isAssignableFrom(
                    String.class)) || defaultValue instanceof String)) {
                value = (T) props.get(key);
            }
            return null == value
                    ? defaultValue
                    : value;
        }
    };

    @Test
    public void testContainsProperty() {
        assertThat(containsProperty("bla bla &{foo} bla bla", "foo")).isTrue();
        assertThat(containsProperty("bla bla ${foo} bla bla", "foo")).isTrue();
        assertThat(containsProperty("bla bla &{foo} bla bla", "bar")).isFalse();
        assertThat(containsProperty(null, "bar")).isFalse();
    }

    @Test
    public void testSubstVars() {
        props.put("replacement", "replaced");
        assertThat(substVars("the ${replacement} string", propertyAccessor, Delimiter.DOLLAR, false))
                .isEqualTo("the replaced string");
        assertThat(substVars("the &{replacement} string", propertyAccessor, Delimiter.AMPERSAND, false))
                .isEqualTo("the replaced string");
        assertThat(substVars("the &{replacement} string", propertyAccessor, true))
                .isEqualTo("the replaced string");
    }

}

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
package org.forgerock.openidm.osgi;

import org.osgi.service.component.ComponentContext;
import org.testng.annotations.Test;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the ComponentContextUtil methods.
 */
public class ComponentContextUtilTest {

    class UnmodifiableDictionary<K, V> extends Dictionary<K, V> {
        private final Dictionary<K, V> dictionary;

        UnmodifiableDictionary(Dictionary<K, V> dictionary) {
            this.dictionary = dictionary;
        }

        @Override
        public int size() {
            return dictionary.size();
        }

        @Override
        public boolean isEmpty() {
            return dictionary.isEmpty();
        }

        @Override
        public Enumeration<K> keys() {
            return dictionary.keys();
        }

        @Override
        public Enumeration<V> elements() {
            return dictionary.elements();
        }

        @Override
        public V get(Object key) {
            return dictionary.get(key);
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException("Dictionary cannot be modified");
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException("Dictionary cannot be modified");
        }
    }

    @Test
    public void testGetModifiableProperties() {
        ComponentContext context = mock(ComponentContext.class);
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("property", "value");
        Dictionary<String, Object> unmodifiable = new UnmodifiableDictionary<>(properties);
        when(context.getProperties()).thenReturn(unmodifiable);

        try {
            context.getProperties().remove("property");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(UnsupportedOperationException.class);
        }

        Dictionary<String, Object> modifiable = ComponentContextUtil.getModifiableProperties(context);
        assertThat(modifiable.remove("property")).isEqualTo("value");
    }

    @Test
    public void testGetFullPidWithFactoryId() {
        final String fullPid = "org.forgerock.openidm.osgi.factoryPid";
        ComponentContext contextMock = mock(ComponentContext.class);

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(ComponentContextUtil.COMPONENT_NAME, "org.forgerock.openidm.osgi");
        properties.put(ComponentContextUtil.COMPONENT_CONFIG_FACTORY_PID, "factoryPid");
        Dictionary<String, Object> dictionary = new UnmodifiableDictionary(properties);

        when(contextMock.getProperties()).thenReturn(dictionary);

        assertThat(ComponentContextUtil.getFullPid(contextMock)).isEqualTo(fullPid);
    }

    @Test
    public void testGetFullPidWithOutFactoryId() {
        final String fullPid = "org.forgerock.openidm.osgi";
        ComponentContext contextMock = mock(ComponentContext.class);

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(ComponentContextUtil.COMPONENT_NAME, "org.forgerock.openidm.osgi");
        Dictionary<String, Object> dictionary = new UnmodifiableDictionary(properties);

        when(contextMock.getProperties()).thenReturn(dictionary);

        assertThat(ComponentContextUtil.getFullPid(contextMock)).isEqualTo(fullPid);
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.provisioner.openicf.connector;

import org.identityconnectors.common.script.Script;
import org.identityconnectors.common.script.ScriptBuilder;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

import java.io.File;
import java.net.URI;

/**
 * @version $Revision$ $Date$
 */
public class TestConfiguration extends AbstractConfiguration {

    private String stringValue = "stringValue";
    private String[] stringArrayValue = new String[]{"stringArrayValue1", "stringArrayValue2"};
    private long primitiveLongValue = Long.MAX_VALUE;
    private long[] primitiveLongArrayValue = new long[]{Long.MIN_VALUE, Long.MAX_VALUE};
    private Long longValue = new Long(9223372036854775807l);
    private Long[] longArrayValue = new Long[]{new Long(9223372036854775807l), new Long(-9223372036854775808l)};
    private char charValue = 'c';
    private char[] charArrayValue = new char[]{'c', 'h', 'a', 'r'};
    private Character characterValue = new Character('d');
    private Character[] characterArrayValue = new Character[]{new Character('d'), new Character('i')};
    private double primitiveDoubleValue = Double.MAX_VALUE;
    private double[] primitiveDoubleArrayValue = new double[]{Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE};
    private Double doubleValue = new Double(3147483647.234d);
    private Double[] doubleArrayValue = new Double[]{new Double(4000000000.0003d), new Double(4000000000.300d)};
    private float primitiveFloatValue = Float.MAX_VALUE;
    private float[] primitivefloatArrayValue = new float[]{Float.MIN_VALUE, Float.MIN_NORMAL, Float.MAX_VALUE};
    private Float floatValue = new Float(2343.56f);
    private Float[] floatArrayValue = new Float[]{new Float(2343.56f), new Float(5373.89f)};
    private int intValue = Integer.MIN_VALUE;
    private int[] intArrayValue = new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
    private Integer integerValue = new Integer(4321);
    private Integer[] integerArrayValue = new Integer[]{new Integer(4321), new Integer(1234)};
    private boolean primitiveBooleanValue = true;
    private boolean[] primitiveBooleanArrayValue = new boolean[]{true, false};
    private Boolean booleanValue = Boolean.FALSE;
    private Boolean[] booleanArrayValue = new Boolean[]{Boolean.FALSE, Boolean.TRUE};
    private URI uriValue = null;
    private URI[] uriArrayValue = null;
    private File fileValue = new File("/usr/local/");
    private File[] fileArrayValue = new File[]{new File("/etc"), new File("/bin")};
    private GuardedByteArray guardedByteArrayValue = new GuardedByteArray("Passw0rd".getBytes());
    private GuardedByteArray[] guardedByteArrayArrayValue = new GuardedByteArray[]{new GuardedByteArray("Passw0rd1".getBytes()),new GuardedByteArray("Passw0rd2".getBytes())};
    private GuardedString guardedStringValue = new GuardedString("Password".toCharArray());
    private GuardedString[] guardedStringArrayValue = new GuardedString[]{new GuardedString("Password3".toCharArray()),new GuardedString("Password4".toCharArray())};
    private Script scriptValue;
    private Script[] scriptArrayValue;

    public TestConfiguration() {
        try {
            uriValue = new URI("https://wikis.forgerock.org/confluence/display/openidm/Home");
            uriArrayValue = new URI[]{new URI("https://wikis.forgerock.org/confluence/display/openidm/Home"),
                    new URI("https://wikis.forgerock.org/confluence/display/openam/Home")};

        } catch (Exception e) {
            //Do Nothing
        }

        ScriptBuilder builder = new ScriptBuilder();
        builder.setScriptLanguage("Groovy");
        builder.setScriptText("Groovy Script");
        scriptValue = builder.build();
        scriptArrayValue = new Script[2];
        builder.setScriptLanguage("JavaScript");
        builder.setScriptText("Java Script");
        scriptArrayValue[0] = builder.build();
        builder.setScriptLanguage("Boo");
        builder.setScriptText("Boo Script");
        scriptArrayValue[1] = builder.build();
    }

    @ConfigurationProperty(order = 1, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Boolean[] getBooleanArrayValue() {
        return booleanArrayValue;
    }

    public void setBooleanArrayValue(Boolean[] booleanArrayValue) {
        this.booleanArrayValue = booleanArrayValue;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public char[] getCharArrayValue() {
        return charArrayValue;
    }

    public void setCharArrayValue(char[] charArrayValue) {
        this.charArrayValue = charArrayValue;
    }

    @ConfigurationProperty(order = 4, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public char getCharValue() {
        return charValue;
    }

    public void setCharValue(char charValue) {
        this.charValue = charValue;
    }

    @ConfigurationProperty(order = 5, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Character[] getCharacterArrayValue() {
        return characterArrayValue;
    }

    public void setCharacterArrayValue(Character[] characterArrayValue) {
        this.characterArrayValue = characterArrayValue;
    }

    @ConfigurationProperty(order = 6, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Character getCharacterValue() {
        return characterValue;
    }

    public void setCharacterValue(Character characterValue) {
        this.characterValue = characterValue;
    }

    @ConfigurationProperty(order = 7, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Double[] getDoubleArrayValue() {
        return doubleArrayValue;
    }

    public void setDoubleArrayValue(Double[] doubleArrayValue) {
        this.doubleArrayValue = doubleArrayValue;
    }

    @ConfigurationProperty(order = 8, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    @ConfigurationProperty(order = 9, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public File[] getFileArrayValue() {
        return fileArrayValue;
    }

    public void setFileArrayValue(File[] fileArrayValue) {
        this.fileArrayValue = fileArrayValue;
    }

    @ConfigurationProperty(order = 10, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public File getFileValue() {
        return fileValue;
    }

    public void setFileValue(File fileValue) {
        this.fileValue = fileValue;
    }

    @ConfigurationProperty(order = 11, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Float[] getFloatArrayValue() {
        return floatArrayValue;
    }

    public void setFloatArrayValue(Float[] floatArrayValue) {
        this.floatArrayValue = floatArrayValue;
    }

    @ConfigurationProperty(order = 12, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Float getFloatValue() {
        return floatValue;
    }

    public void setFloatValue(Float floatValue) {
        this.floatValue = floatValue;
    }

    @ConfigurationProperty(order = 13, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public GuardedByteArray[] getGuardedByteArrayArrayValue() {
        return guardedByteArrayArrayValue;
    }

    public void setGuardedByteArrayArrayValue(GuardedByteArray[] guardedByteArrayArrayValue) {
        this.guardedByteArrayArrayValue = guardedByteArrayArrayValue;
    }

    @ConfigurationProperty(order = 14, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public GuardedByteArray getGuardedByteArrayValue() {
        return guardedByteArrayValue;
    }

    public void setGuardedByteArrayValue(GuardedByteArray guardedByteArrayValue) {
        this.guardedByteArrayValue = guardedByteArrayValue;
    }

    @ConfigurationProperty(order = 15, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public GuardedString[] getGuardedStringArrayValue() {
        return guardedStringArrayValue;
    }

    public void setGuardedStringArrayValue(GuardedString[] guardedStringArrayValue) {
        this.guardedStringArrayValue = guardedStringArrayValue;
    }

    @ConfigurationProperty(order = 16, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public GuardedString getGuardedStringValue() {
        return guardedStringValue;
    }

    public void setGuardedStringValue(GuardedString guardedStringValue) {
        this.guardedStringValue = guardedStringValue;
    }

    @ConfigurationProperty(order = 17, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public int[] getIntArrayValue() {
        return intArrayValue;
    }

    public void setIntArrayValue(int[] intArrayValue) {
        this.intArrayValue = intArrayValue;
    }

    @ConfigurationProperty(order = 17, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    @ConfigurationProperty(order = 19, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Integer[] getIntegerArrayValue() {
        return integerArrayValue;
    }

    public void setIntegerArrayValue(Integer[] integerArrayValue) {
        this.integerArrayValue = integerArrayValue;
    }

    @ConfigurationProperty(order = 20, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    @ConfigurationProperty(order = 21, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Long[] getLongArrayValue() {
        return longArrayValue;
    }

    public void setLongArrayValue(Long[] longArrayValue) {
        this.longArrayValue = longArrayValue;
    }

    @ConfigurationProperty(order = 22, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    @ConfigurationProperty(order = 23, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public boolean[] getPrimitiveBooleanArrayValue() {
        return primitiveBooleanArrayValue;
    }

    public void setPrimitiveBooleanArrayValue(boolean[] primitiveBooleanArrayValue) {
        this.primitiveBooleanArrayValue = primitiveBooleanArrayValue;
    }

    @ConfigurationProperty(order = 24, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public boolean isPrimitiveBooleanValue() {
        return primitiveBooleanValue;
    }

    public void setPrimitiveBooleanValue(boolean primitiveBooleanValue) {
        this.primitiveBooleanValue = primitiveBooleanValue;
    }

    @ConfigurationProperty(order = 25, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public double[] getPrimitiveDoubleArrayValue() {
        return primitiveDoubleArrayValue;
    }

    public void setPrimitiveDoubleArrayValue(double[] primitiveDoubleArrayValue) {
        this.primitiveDoubleArrayValue = primitiveDoubleArrayValue;
    }

    @ConfigurationProperty(order = 26, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public double getPrimitiveDoubleValue() {
        return primitiveDoubleValue;
    }

    public void setPrimitiveDoubleValue(double primitiveDoubleValue) {
        this.primitiveDoubleValue = primitiveDoubleValue;
    }

    @ConfigurationProperty(order = 27, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public float getPrimitiveFloatValue() {
        return primitiveFloatValue;
    }

    public void setPrimitiveFloatValue(float primitiveFloatValue) {
        this.primitiveFloatValue = primitiveFloatValue;
    }

    @ConfigurationProperty(order = 28, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public long[] getPrimitiveLongArrayValue() {
        return primitiveLongArrayValue;
    }

    public void setPrimitiveLongArrayValue(long[] primitiveLongArrayValue) {
        this.primitiveLongArrayValue = primitiveLongArrayValue;
    }

    @ConfigurationProperty(order = 29, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public long getPrimitiveLongValue() {
        return primitiveLongValue;
    }

    public void setPrimitiveLongValue(long primitiveLongValue) {
        this.primitiveLongValue = primitiveLongValue;
    }

    @ConfigurationProperty(order = 30, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public float[] getPrimitivefloatArrayValue() {
        return primitivefloatArrayValue;
    }

    public void setPrimitivefloatArrayValue(float[] primitivefloatArrayValue) {
        this.primitivefloatArrayValue = primitivefloatArrayValue;
    }

    @ConfigurationProperty(order = 31, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Script[] getScriptArrayValue() {
        return scriptArrayValue;
    }

    public void setScriptArrayValue(Script[] scriptArrayValue) {
        this.scriptArrayValue = scriptArrayValue;
    }

    @ConfigurationProperty(order = 32, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public Script getScriptValue() {
        return scriptValue;
    }

    public void setScriptValue(Script scriptValue) {
        this.scriptValue = scriptValue;
    }

    @ConfigurationProperty(order = 33, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public String[] getStringArrayValue() {
        return stringArrayValue;
    }

    public void setStringArrayValue(String[] stringArrayValue) {
        this.stringArrayValue = stringArrayValue;
    }


    @ConfigurationProperty(order = 34, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    @ConfigurationProperty(order = 35, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public URI[] getUriArrayValue() {
        return uriArrayValue;
    }

    public void setUriArrayValue(URI[] uriArrayValue) {
        this.uriArrayValue = uriArrayValue;
    }


    @ConfigurationProperty(order = 36, displayMessageKey = "TEST_PROPERTY_1", helpMessageKey = "TEST_PROPERTY_1_HELP")
    public URI getUriValue() {
        return uriValue;
    }

    public void setUriValue(URI uriValue) {
        this.uriValue = uriValue;
    }


    /**
     * {@inheritDoc}
     */
    public void validate() {
    }

}

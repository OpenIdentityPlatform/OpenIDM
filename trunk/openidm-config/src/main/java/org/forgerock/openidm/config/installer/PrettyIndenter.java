/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/**
 * Plug-in based on the AP 2.0 licensed Jackson Indenter
 * with changes to have configurable indentation.
 * 
 * The original Jackson source files do not have license headers
 * and the above was added to clearly identify the 
 * licensing of the original file. 
 */
package org.forgerock.openidm.config.installer;

import java.io.IOException;
import java.util.Arrays;

import org.codehaus.jackson.impl.Indenter;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonGenerationException;

/**
 * Indenter, part of formatting Jackson output in pretty print
 * Makes the number of spaces to use per indent configurable.
 *
 */
class PrettyIndenter implements Indenter {
    int noOfSpaces = 4; // Default to 4 spaces per level
    
    final static String SYSTEM_LINE_SEPARATOR;
    static {
        String lf = null;
        try {
            lf = System.getProperty("line.separator");
        } catch (Throwable t) { } // access exception?
        SYSTEM_LINE_SEPARATOR = (lf == null) ? "\n" : lf;
    }

    final static int SPACE_COUNT = 64;
    final static char[] SPACES = new char[SPACE_COUNT];
    static {
        Arrays.fill(SPACES, ' ');
    }

    /**
     * Configure how many spaces to use per indent. 
     * Default is 4 spaces.
     * 
     * @param noOfSpaces
     */
    public void setIndentSpaces(int noOfSpaces) {
        this.noOfSpaces = noOfSpaces;
    }

    public boolean isInline() { return false; }

    public void writeIndentation(JsonGenerator jg, int level)
        throws IOException, JsonGenerationException {
        jg.writeRaw(SYSTEM_LINE_SEPARATOR);
        level = level * noOfSpaces; 
        while (level > SPACE_COUNT) { // should never happen but...
            jg.writeRaw(SPACES, 0, SPACE_COUNT); 
            level -= SPACES.length;
        }
        jg.writeRaw(SPACES, 0, level);
    }
}
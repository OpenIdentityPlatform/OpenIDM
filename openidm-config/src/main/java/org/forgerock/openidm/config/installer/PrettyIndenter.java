/**
 * Plug-in based on the AP 2.0 licensed Jackson Indenter
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
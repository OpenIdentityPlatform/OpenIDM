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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.script.javascript;

// Java Standard Edition
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

// Mozilla Rhino
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

// ForgeRock OpenIDM Core
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.ScriptThrownException;

/**
 * A JavaScript script.
 * <p>
 * This implementation pre-compiles the provided script. Any syntax errors in the source code
 * will throw an exception during construction of the object.
 * <p>
 * This implementation provides 
 *
 * @author Paul C. Bryan
 */
public class JavaScript implements Script {

    /** A sealed shared scope to improve performance; avoids allocating standard objects on every exec call. */
    private static ScriptableObject SHARED_SCOPE = null; // lazily initialized

    /** The compiled script to execute. */
    private final org.mozilla.javascript.Script script;

    /** Indicates if this script instance should use the shared scope. */
    private final boolean sharedScope;

    /**
     * Compiles the JavaScript source code into an executable script. A sealed shared scope
     * containing standard JavaScript objects (Object, String, Number, Date, etc.) will be
     * used for script execution rather than allocating a new unsealed scope for each
     * execution.
     *
     * @param source the source code of the JavaScript script.
     * @throws ScriptException if there was an exception encountered while compiling the script.
     */
    public JavaScript(String source) throws ScriptException {
        this(source, true);
    }

    /**
     * Compiles the JavaScript source code into an executable script. If {@code useSharedScope}
     * is {@code true}, then a sealed shared scope containing standard JavaScript objects
     * (Object, String, Number, Date, etc.) will be used for script execution; otherwise a new
     * unsealed scope will be allocated for each execution.
     *
     * @param source the source code of the JavaScript script.
     * @param sharedScope if {@code true}, uses the shared scope, otherwise allocates new scope.
     * @throws ScriptException if there was an exception encountered while compiling the script.
     */
    public JavaScript(String source, boolean sharedScope) throws ScriptException {
        this.sharedScope = sharedScope;
        Context cx = Context.enter();
        try {
            script = cx.compileString(source, "line", 1, null);
        }
        catch (RhinoException re) {
            throw new ScriptException(re.getMessage());
        }
        finally {
            Context.exit();
        }
    }

    /**
     * TEMPORARY
     */
    public JavaScript(File file) throws ScriptException {
        this(file, true);
    }

    /**
     * TEMPORARY
     */
    public JavaScript(File file, boolean sharedScope) throws ScriptException {
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            this.sharedScope = sharedScope;
            Context cx = Context.enter();
            try {
                script = cx.compileReader(reader, file.getPath(), 1, null);
            } catch (RhinoException re) {
                throw new ScriptException(re);
            } finally {
                Context.exit();
            }
        } catch (IOException ioe) {
            throw new ScriptException(ioe);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // meaningless exception
                }
            }
        }
    }

    /**
     * Gets the JavaScript standard objects, either as the shared sealed scope or as a newly
     * allocated set of standard objects, depending on the value of {@code useSharedScope}.
     *
     * @param context The runtime context of the executing script.
     * @return the JavaScript standard objects.
     */
    private ScriptableObject getStandardObjects(Context context) {
        if (!sharedScope) {
            return context.initStandardObjects(); // somewhat expensive
        }
        if (SHARED_SCOPE == null) { // lazy initialization race condition is harmless
            ScriptableObject scope = context.initStandardObjects(null, true);
            scope.sealObject(); // seal the whole scope (not just standard objects)
            SHARED_SCOPE = scope;
        }
        return SHARED_SCOPE;
    }

    @Override
    public Object exec(Map<String, Object> scope) throws ScriptException {
        if (scope == null) {
            throw new NullPointerException();
        }
        Context context = Context.enter();
        try {
            Scriptable outer = new ScriptableMap(scope);
            outer.setPrototype(getStandardObjects(context)); // standard objects included with every box
            outer.setParentScope(null);
            Scriptable inner = context.newObject(outer); // inner transient scope for new properties
            inner.setPrototype(outer);
            inner.setParentScope(null);
            return Converter.convert(script.exec(context, inner));
        }
        catch (RhinoException re) {
            if (re instanceof JavaScriptException) { // thrown by the script itself
                throw new ScriptThrownException(Converter.convert(((JavaScriptException)re).getValue()));
            }
            else { // some other runtime exception encountered
                throw new ScriptException(re.getMessage());
            }
        }   
        finally {
            Context.exit();
        } 
    }
}

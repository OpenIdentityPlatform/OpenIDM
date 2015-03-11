/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.sync.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.forgerock.json.fluent.JsonValue;

/**
 * A Utils class is a collection of common methods used in the script library.
 *
 */
public class Utils {

    public static class Pair<A, B> {
        public final A fst;
        public final B snd;

        public Pair(final A a, final B b) {
            fst = a;
            snd = b;
        }

        public String toString() {
            return "Pair[" + fst + "," + snd + "]";
        }

        private static boolean equals(Object x, Object y) {
            return (x == null && y == null) || (x != null && x.equals(y));
        }

        public boolean equals(Object other) {
            return other instanceof Pair<?, ?> && equals(fst, ((Pair<?, ?>) other).fst)
                    && equals(snd, ((Pair<?, ?>) other).snd);
        }

        public int hashCode() {
            int result = fst != null ? fst.hashCode() : 0;
            result = 31 * result + (snd != null ? snd.hashCode() : 0);
            return result;
        }

        public static <A, B> Pair<A, B> of(final A a, final B b) {
            return new Pair<A, B>(a, b);
        }
    }

    private Utils() {
    }

    public static void copyURLToFile(URL in, File out) throws IOException {
        ReadableByteChannel inChannel = Channels.newChannel(in.openStream());
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            outChannel.transferFrom(inChannel, 0, 1 << 24);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    public static <T> T deepCopy(final T source) {
        if (source instanceof JsonValue) {
            return (T) new JsonValue(deepCopy(((JsonValue) source).getObject()));
        } else if (source instanceof Collection || source instanceof Map) {
            return (T) deepCopy(source, new Stack<Pair<Object, Object>>());
        } else {
            return source;
        }
    }

    @SuppressWarnings({ "unchecked" })
    private static Object deepCopy(Object source, final Stack<Pair<Object, Object>> valueStack) {
        Iterator<Pair<Object, Object>> i = valueStack.iterator();
        while (i.hasNext()) {
            Pair<Object, Object> next = i.next();
            if (next.fst == source) {
                return next.snd;
            }
        }

        if (source instanceof JsonValue) {
            return new JsonValue(deepCopy(((JsonValue) source).getObject(), valueStack));
        } else if (source instanceof Collection) {
            List<Object> copy = new ArrayList<Object>(((Collection) source).size());
            valueStack.push(Pair.of(source, (Object) copy));
            for (Object o : (Collection) source) {
                copy.add(deepCopy(o, valueStack));
            }
            // valueStack.pop();
            return copy;
        } else if (source instanceof Map) {
            Map copy = new LinkedHashMap(((Map) source).size());
            valueStack.push(Pair.of(source, (Object) copy));
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) source).entrySet()) {
                copy.put(entry.getKey(), deepCopy(entry.getValue(), valueStack));
            }
            // valueStack.pop();
            return copy;
        } else {
            return source;
        }
    }
}

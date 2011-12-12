package org.forgerock.openidm.shell.impl;

import org.apache.felix.gogo.runtime.Reflective;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.forgerock.openidm.shell.CustomCommandScope;

import java.util.List;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
public class CommandProxy implements Function {

    private CustomCommandScope tgt;
    private String function;

    public CommandProxy(CustomCommandScope tgt, String function) {
        this.tgt = tgt;
        this.function = function;
    }

    public Object execute(CommandSession session, List<Object> arguments) throws Exception {
        try {
            if (tgt instanceof Function) {
                return ((Function) tgt).execute(session, arguments);
            } else {
                return Reflective.invoke(session, tgt, function, arguments);
            }
        } finally {

        }
    }
}

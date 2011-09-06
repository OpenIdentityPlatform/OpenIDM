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
package org.forgerock.openidm.shell.felixgogo;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.StringMemberValue;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;


/**
 * Felix GoGo Commands Service class generator. It uses {@link AbstractFelixCommandsService} as super class and
 * adds public methods for each command name and in method body redirects call to original commands service
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class FelixGogoCommandsServiceGenerator {
    /**
     * Logger.
     */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FelixGogoCommandsServiceGenerator.class);

    /**
     * Classes pool
     */
    private static final ClassPool POOL = ClassPool.getDefault();

    /**
     * Initialization of Classes pool
     */
    static {
        POOL.appendClassPath(new ClassClassPath(AbstractFelixCommandsService.class));
    }


    /**
     * Generate CommandProvider class and instance for this class based on parameters
     *
     * @param service  commands service
     * @param commands commands map (name=help)
     * @param suffix   unique class suffix
     * @return generated CommandProvider instance
     * @throws Exception if something went wrong
     */
    public static Object generate(Object service, Map<String, String> commands, String suffix) throws Exception {
        // generate class with unique name
        CtClass ctClass = POOL.makeClass(AbstractFelixCommandsService.class.getName() + suffix);
        try {
            if (!ctClass.isFrozen()) {
                ClassFile ccFile = ctClass.getClassFile();
                ccFile.setVersionToJava5();
                ConstPool constPool = ccFile.getConstPool();

                // set superclass
                CtClass abstractCtClass = POOL.getCtClass(AbstractFelixCommandsService.class.getName());
                ctClass.setSuperclass(abstractCtClass);

                // add constructor
                CtClass serviceCtClass = POOL.getCtClass(Object.class.getName());
                CtConstructor ctConstructor = new CtConstructor(new CtClass[]{serviceCtClass}, ctClass);
                ctConstructor.setModifiers(Modifier.PUBLIC);
                ctConstructor.setBody("super($1);");
                ctClass.addConstructor(ctConstructor);

                // add method for each command
                CtClass sessionCtClass = POOL.getCtClass(CommandSession.class.getName());
                CtClass stringArrayCtClass = POOL.getCtClass(String[].class.getName());
                Set<String> names = commands.keySet();
                for (String name : names) {
                    if (isMethodAvailable(service, name)) {
                        CtMethod ctMethod = new CtMethod(CtClass.voidType, name, new CtClass[]{
                                sessionCtClass, stringArrayCtClass
                        }, ctClass);
                        ctMethod.setModifiers(Modifier.PUBLIC);
                        ctMethod.setBody("runCommand(\"" + name + "\", $1, $2);");
                        ctClass.addMethod(ctMethod);

                        // add GoGo descriptor for this shell command
                        AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
                        Annotation annotation = new Annotation(Descriptor.class.getName(), constPool);
                        annotation.addMemberValue("value", new StringMemberValue(commands.get(name), constPool));
                        annotationsAttribute.addAnnotation(annotation);
                        ctMethod.getMethodInfo().addAttribute(annotationsAttribute);
                    }
                }
            }

            // create new instance
            Class<?> aClass = ctClass.toClass(FelixGogoCommandsServiceGenerator.class.getClassLoader());
            Constructor<?> constructor = aClass.getConstructor(Object.class);
            return constructor.newInstance(service);
        } catch (Exception e) {
            ctClass.detach();
            throw e;
        }
    }

    private static boolean isMethodAvailable(Object commandsProvider, String methodName) {
        try {
            commandsProvider.getClass().getMethod(methodName, PrintWriter.class, String[].class);
            return true;
        } catch (NoSuchMethodException e) {
            logger.warn("Unable to find Console Command: {}", methodName, e);
            return false;
        }
    }

    /**
     * Detach generated class
     *
     * @param suffix unique class suffix
     */
    public static void clean(String suffix) {
        try {
            CtClass ctClass = POOL.getCtClass(AbstractFelixCommandsService.class.getName() + suffix);
            ctClass.defrost();
            ctClass.detach();
        } catch (NotFoundException e) {
            logger.warn("Unable to clean Console Service. {}", e.getMessage(), e);
        }
    }
}

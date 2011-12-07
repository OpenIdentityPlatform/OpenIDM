/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openidm.shell.felixgogo;

import org.forgerock.openidm.shell.CustomCommandScope;
import org.forgerock.openidm.shell.impl.Main;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;


/**
 * Felix GoGo Commands Service class generator. It uses {@link AbstractFelixCommandsService} as super class and
 * adds public methods for each command name and in method body redirects call to original commands service
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class FelixGogoCommandsServiceGenerator extends ClassLoader {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(FelixGogoCommandsServiceGenerator.class);

    /* *//**
     * Classes pool
     *//*
    private static final javassist.ClassPool POOL = javassist.ClassPool.getDefault();

    */
    /**
     * Initialization of Classes pool
     *//*
    static {
        POOL.appendClassPath(new javassist.ClassClassPath(AbstractFelixCommandsService.class));
    }*/

    private static StubClassLoader classLoader = new StubClassLoader();

    /**
     * Generate CommandProvider class and instance for this class based on parameters
     *
     * @param service  commands service
     * @param commands commands map (name=help)
     * @param suffix   unique class suffix
     * @return generated CommandProvider instance
     * @throws Exception if something went wrong
     */
    public static Object generate(CustomCommandScope service, Map<String, String> commands, String suffix) throws Exception {
        // generate class with unique name
        //javassist.CtClass ctClass = POOL.makeClass(AbstractFelixCommandsService.class.getName() + suffix);

        try {
            ClassWriter cw = new ClassWriter(0);
            MethodVisitor mv;
            AnnotationVisitor av0;
            String className = AbstractFelixCommandsService.class.getName().replace('.', '/') + suffix;
            cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, className, null, AbstractFelixCommandsService.class.getName().replace('.', '/'), null);

            //cw.visitSource("AbstractFelixCommandsServiceSample.java", null);

            {
                mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", null, null);
                mv.visitCode();
                Label l0 = new Label();
                mv.visitLabel(l0);
                mv.visitLineNumber(10, l0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, AbstractFelixCommandsService.class.getName().replace('.', '/'), "<init>", "(Ljava/lang/Object;)V");
                Label l1 = new Label();
                mv.visitLabel(l1);
                mv.visitLineNumber(11, l1);
                mv.visitInsn(RETURN);
                Label l2 = new Label();
                mv.visitLabel(l2);
                mv.visitLocalVariable("this", "L" + className + ";", null, l0, l2, 0);
                mv.visitLocalVariable("service", "Ljava/lang/Object;", null, l0, l2, 1);
                mv.visitMaxs(2, 2);
                mv.visitEnd();
            }

            /*javassist.bytecode.ClassFile ccFile = ctClass.getClassFile();
            ccFile.setVersionToJava5();
            javassist.bytecode.ConstPool constPool = ccFile.getConstPool();

            // set superclass
            javassist.CtClass abstractCtClass = POOL.getCtClass(AbstractFelixCommandsService.class.getName());
            ctClass.setSuperclass(abstractCtClass);

            // add constructor
            javassist.CtClass serviceCtClass = POOL.getCtClass(Object.class.getName());
            javassist.CtConstructor ctConstructor = new javassist.CtConstructor(new javassist.CtClass[]{serviceCtClass}, ctClass);
            ctConstructor.setModifiers(javassist.Modifier.PUBLIC);
            ctConstructor.setBody("super($1);");
            ctClass.addConstructor(ctConstructor);

            // add method for each command
            javassist.CtClass sessionCtClass = POOL.getCtClass(CommandSession.class.getName());
            javassist.CtClass stringArrayCtClass = POOL.getCtClass(String[].class.getName());*/
            Set<String> names = commands.keySet();
            for (String name : names) {
                if (isMethodAvailable(service, name)) {
                    mv = cw.visitMethod(ACC_PUBLIC, name, "(Lorg/apache/felix/service/command/CommandSession;[Ljava/lang/String;)V", null, null);
                    {
                        av0 = mv.visitAnnotation("Lorg/apache/felix/service/command/Descriptor;", true);
                        av0.visit("value", commands.get(name));
                        av0.visitEnd();
                    }
                    mv.visitCode();
                    Label l0 = new Label();
                    mv.visitLabel(l0);
                    mv.visitLineNumber(15, l0);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitLdcInsn(name);
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitMethodInsn(INVOKEVIRTUAL, className, "runCommand", "(Ljava/lang/String;Lorg/apache/felix/service/command/CommandSession;[Ljava/lang/String;)V");
                    Label l1 = new Label();
                    mv.visitLabel(l1);
                    mv.visitLineNumber(16, l1);
                    mv.visitInsn(RETURN);
                    Label l2 = new Label();
                    mv.visitLabel(l2);
                    mv.visitLocalVariable("this", "L" + className + ";", null, l0, l2, 0);
                    mv.visitLocalVariable("session", "Lorg/apache/felix/service/command/CommandSession;", null, l0, l2, 1);
                    mv.visitLocalVariable("args", "[Ljava/lang/String;", null, l0, l2, 2);
                    mv.visitMaxs(4, 3);
                    mv.visitEnd();

                    /*javassist.CtMethod ctMethod = new javassist.CtMethod(javassist.CtClass.voidType, name, new javassist.CtClass[]{
                            sessionCtClass, stringArrayCtClass
                    }, ctClass);
                    ctMethod.setModifiers(javassist.Modifier.PUBLIC);
                    ctMethod.setBody("runCommand(\"" + name + "\", $1, $2);");
                    ctClass.addMethod(ctMethod);

                    // add GoGo descriptor for this shell command
                    javassist.bytecode.AnnotationsAttribute annotationsAttribute = new javassist.bytecode.AnnotationsAttribute(constPool, javassist.bytecode.AnnotationsAttribute.visibleTag);
                    javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(Descriptor.class.getName(), constPool);
                    annotation.addMemberValue("value", new javassist.bytecode.annotation.StringMemberValue(commands.get(name), constPool));
                    annotationsAttribute.addAnnotation(annotation);
                    ctMethod.getMethodInfo().addAttribute(annotationsAttribute);*/
                }
            }

            cw.visitEnd();
            // create new instance
            /*Class<?> aClass = ctClass.toClass(FelixGogoCommandsServiceGenerator.class.getClassLoader(), null);
            */
            Class<?> aClass = classLoader.defineClass(className, cw.toByteArray());
            Constructor<?> constructor = aClass.getConstructor(Object.class);
            return constructor.newInstance(service);
        } catch (Exception e) {
            //ctClass.detach();
            throw e;
        }
    }

    private static boolean isMethodAvailable(CustomCommandScope commandsProvider, String methodName) {
        return null != Main.findCommandMethod(commandsProvider.getClass(), methodName);
    }


    /**
     * Detach generated class
     *
     * @param suffix unique class suffix
     */
    public static void clean(String suffix) {
        /*try {
            javassist.CtClass ctClass = POOL.getCtClass(AbstractFelixCommandsService.class.getName() + suffix);
            ctClass.defrost();
            ctClass.detach();
        } catch (javassist.NotFoundException e) {
            logger.warn("Unable to clean Console Service. {}", e.getMessage(), e);
        }*/
    }


    static class StubClassLoader extends ClassLoader {
        public Class defineClass(String name, byte[] b) {
            return defineClass(name.replace('/', '.'), b, 0, b.length);
        }
    }

}

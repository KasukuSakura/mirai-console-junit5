package com.kasukusakura.mirai.console.junit5.impl;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class JHookSupport {
    public static void premain(String opt, Instrumentation instrumentation) {
        instrumentation.addTransformer(new IsUnderJUnitTestingTransformer());
        instrumentation.addTransformer(new Transformer());
    }

    private static Class<?> findClass(String className) throws Throwable {
        return Class.forName(className);
    }

    private static Launcher createLauncher() {
        return new MiraiConsoleExecuteLauncher();
    }

    private static LauncherDiscoveryRequest createLauncherDiscoveryRequest(MethodHandles.Lookup lookup, Object thiz, List<String> testClasses) throws Throwable {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();

        for (Method method : lookup.lookupClass().getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (method.getParameterCount() != 1) continue;
            if (method.getParameterTypes()[0] != LauncherDiscoveryRequestBuilder.class) continue;

            lookup.unreflect(method).invoke(thiz, builder);
        }

        builder.selectors(testClasses.stream().map(LazyLoadClassDiscovery::new).collect(Collectors.toList()));

        return builder.build();
    }

    public static CallSite adapter(MethodHandles.Lookup lookup, String name, MethodType methodType) throws Throwable {
        MethodHandles.Lookup asSupport = MethodHandles.lookup();

        if (name.equals("loadClass")) {
            return new ConstantCallSite(asSupport.findStatic(JHookSupport.class, "findClass", methodType));
        }

        if (name.equals("createLauncher")) {
            MethodHandle createLauncher = asSupport.findStatic(JHookSupport.class, "createLauncher", MethodType.methodType(Launcher.class));

            if (methodType.parameterCount() != 0) {
                createLauncher = MethodHandles.dropArguments(createLauncher, 0, methodType.parameterArray());
            }

            return new ConstantCallSite(
                    createLauncher.asType(methodType)
            );
        }

        if (name.equals("createLauncherDiscoveryRequest")) {
            return new ConstantCallSite(
                    asSupport.findStatic(JHookSupport.class, "createLauncherDiscoveryRequest", MethodType.methodType(LauncherDiscoveryRequest.class, MethodHandles.Lookup.class, Object.class, List.class))
                            .bindTo(lookup)
                            .asType(methodType)
            );
        }

        throw new IllegalArgumentException("Failed to resolve " + name + methodType);
    }

    private static void dynCall(MethodVisitor mv, String name, String type) {
        mv.visitInvokeDynamicInsn(name, type, new Handle(
                Opcodes.H_INVOKESTATIC, "org/junit/miraiconsolejunit5/gradleadapter/GradleJUnitTestAdapter",
                "adapter", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false
        ));
    }

    private static class Transformer implements ClassFileTransformer {
        private static final String CollectAllTestClassesExecutor = "org/gradle/api/internal/tasks/testing/junitplatform/JUnitPlatformTestClassProcessor$CollectAllTestClassesExecutor";

        @Override
        public byte[] transform(
                ClassLoader loader,
                String className,
                Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain,
                byte[] classfileBuffer
        ) {


            if (className == null) return null;
            if ("org/gradle/api/internal/tasks/testing/junitplatform/JUnitPlatformTestClassProcessor".equals(className)) {
                ClassWriter writer = new ClassWriter(0);

                new ClassReader(classfileBuffer).accept(new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        MethodVisitor sup = super.visitMethod(access, name, descriptor, signature, exceptions);
                        if (name.equals("loadClass")) {

                            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
                            sup.visitVarInsn(Opcodes.ALOAD, isStatic ? 0 : 1);
                            dynCall(sup, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
                            sup.visitInsn(Opcodes.ARETURN);
                            sup.visitMaxs(2, 2);

                            return null;
                        }

                        if (name.equals("createLauncherDiscoveryRequest")) {
                            sup.visitVarInsn(Opcodes.ALOAD, 0);
                            Type metDesc = Type.getMethodType(descriptor);
                            int slot = 1;
                            for (Type argx : metDesc.getArgumentTypes()) {
                                sup.visitVarInsn(argx.getOpcode(Opcodes.ILOAD), slot);
                                slot += argx.getSize();
                            }

                            dynCall(sup, "createLauncherDiscoveryRequest", "(Ljava/lang/Object;" + descriptor.substring(1));
                            sup.visitInsn(metDesc.getReturnType().getOpcode(Opcodes.IRETURN));
                            sup.visitMaxs(2, 2);
                            return null;
                        }
                        return sup;
                    }
                }, 0);

                return writer.toByteArray();
            }
            if (CollectAllTestClassesExecutor.equals(className)) {

                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

                new ClassReader(classfileBuffer).accept(new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                        MethodVisitor sup = super.visitMethod(access, name, descriptor, signature, exceptions);
                        if (name.equals("processAllTestClasses")) {
                            return new MethodVisitor(api, sup) {
                                @Override
                                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                    if (opcode == Opcodes.INVOKESTATIC) {
                                        if (owner.endsWith("LauncherFactory")) {
                                            if (name.equals("create")) {
                                                dynCall(sup, "createLauncher", descriptor);
                                                return;
                                            }
                                        }
                                    }
                                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                }
                            };
                        }
                        if (name.equals("execute") && descriptor.equals("(Ljava/lang/String;)V")) {
                            sup.visitVarInsn(Opcodes.ALOAD, 0);
                            sup.visitFieldInsn(Opcodes.GETFIELD, CollectAllTestClassesExecutor, "testClasses", "Ljava/util/List;");
                            sup.visitVarInsn(Opcodes.ALOAD, 1);
                            sup.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                            sup.visitInsn(Opcodes.POP);
                            sup.visitInsn(Opcodes.RETURN);

                            sup.visitMaxs(0, 0);
                            return null;
                        }
                        return sup;
                    }
                }, 0);

                return writer.toByteArray();
            }

            return null;
        }
    }

    private static class IsUnderJUnitTestingTransformer implements ClassFileTransformer {
        private static final ClassLoader PLATFORM_CCL = ClassLoader.getSystemClassLoader().getParent();
        private static final ClassLoader APP_CCL = ClassLoader.getSystemClassLoader();


        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (loader == null) return null;
            if (loader == PLATFORM_CCL) return null;
            if (loader == APP_CCL) return null;

            AtomicBoolean modified = new AtomicBoolean(false);
            ClassWriter cw = new ClassWriter(0);


            new ClassReader(classfileBuffer).accept(new ClassVisitor(Opcodes.ASM9, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    MethodVisitor sup = super.visitMethod(access, name, descriptor, signature, exceptions);
                    if ((access & Opcodes.ACC_ABSTRACT) != 0) return sup;

                    if (Type.getReturnType(descriptor) != Type.BOOLEAN_TYPE) return sup;


                    return new MethodNode(Opcodes.ASM9, access, name, descriptor, signature, exceptions) {
                        private boolean hasJUnitTesting = false;

                        @Override
                        public AnnotationVisitor visitAnnotationDefault() {
                            return super.visitAnnotationDefault();
                        }

                        @Override
                        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {

                            AnnotationVisitor visitor = super.visitAnnotation(descriptor, visible);
                            if (descriptor.endsWith("/IsUnderJUnitTesting;") || descriptor.endsWith("$IsUnderJUnitTesting;")) {
                                hasJUnitTesting = true;
                                return visitor;
                            }
                            return visitor;
                        }


                        @Override
                        public void visitEnd() {
                            if (hasJUnitTesting) {
                                instructions = new InsnList();
                                tryCatchBlocks.clear();
                                localVariables.clear();

                                access ^= ~Opcodes.ACC_NATIVE;
                                visitInsn(Opcodes.ICONST_1);
                                visitInsn(Opcodes.IRETURN);
                                maxStack = 2;
                                maxLocals = Type.getArgumentsAndReturnSizes(descriptor) >> 2;

                                if ((access & Opcodes.ACC_STATIC) == 0) {
                                    maxLocals++;
                                }
                                modified.set(true);
                            }

                            accept(sup);
                        }
                    };
                }
            }, 0);

            if (modified.get()) return cw.toByteArray();

            return null;
        }
    }
}

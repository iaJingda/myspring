package org.myspring.expression.spel.standard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.myspring.core.asm.ClassWriter;
import org.myspring.core.asm.MethodVisitor;
import org.myspring.core.asm.Opcodes;
import org.myspring.core.util.ClassUtils;
import org.myspring.core.util.ConcurrentReferenceHashMap;
import org.myspring.expression.Expression;
import org.myspring.expression.spel.CodeFlow;
import org.myspring.expression.spel.CompiledExpression;
import org.myspring.expression.spel.ast.SpelNodeImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SpelCompiler implements Opcodes {

    private static final Log logger = LogFactory.getLog(SpelCompiler.class);

    private static final Map<ClassLoader, SpelCompiler> compilers =
            new ConcurrentReferenceHashMap<ClassLoader, SpelCompiler>();

    private final ChildClassLoader ccl;
    private final AtomicInteger suffixId = new AtomicInteger(1);
    private SpelCompiler(ClassLoader classloader) {
        this.ccl = new ChildClassLoader(classloader);
    }

    public CompiledExpression compile(SpelNodeImpl expression) {
        if (expression.isCompilable()) {
            if (logger.isDebugEnabled()) {
                logger.debug("SpEL: compiling " + expression.toStringAST());
            }
            Class<? extends CompiledExpression> clazz = createExpressionClass(expression);
            if (clazz != null) {
                try {
                    return clazz.newInstance();
                }
                catch (Throwable ex) {
                    throw new IllegalStateException("Failed to instantiate CompiledExpression", ex);
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("SpEL: unable to compile " + expression.toStringAST());
        }
        return null;
    }
    private int getNextSuffix() {
        return this.suffixId.incrementAndGet();
    }

    private Class<? extends CompiledExpression> createExpressionClass(SpelNodeImpl expressionToCompile) {
        // Create class outline 'spel/ExNNN extends org.springframework.expression.spel.CompiledExpression'
        String clazzName = "spel/Ex" + getNextSuffix();
        ClassWriter cw = new ExpressionClassWriter();
        cw.visit(V1_5, ACC_PUBLIC, clazzName, null, "org/springframework/expression/spel/CompiledExpression", null);

        // Create default constructor
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "org/springframework/expression/spel/CompiledExpression",
                "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // Create getValue() method
        mv = cw.visitMethod(ACC_PUBLIC, "getValue",
                "(Ljava/lang/Object;Lorg/springframework/expression/EvaluationContext;)Ljava/lang/Object;", null,
                new String[ ]{"org/springframework/expression/EvaluationException"});
        mv.visitCode();

        CodeFlow cf = new CodeFlow(clazzName, cw);

        // Ask the expression AST to generate the body of the method
        try {
            expressionToCompile.generateCode(mv, cf);
        }
        catch (IllegalStateException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(expressionToCompile.getClass().getSimpleName() +
                        ".generateCode opted out of compilation: " + ex.getMessage());
            }
            return null;
        }

        CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor());
        if ("V".equals(cf.lastDescriptor())) {
            mv.visitInsn(ACONST_NULL);
        }
        mv.visitInsn(ARETURN);

        mv.visitMaxs(0, 0);  // not supplied due to COMPUTE_MAXS
        mv.visitEnd();
        cw.visitEnd();

        cf.finish();

        byte[] data = cw.toByteArray();
        // TODO need to make this conditionally occur based on a debug flag
        // dump(expressionToCompile.toStringAST(), clazzName, data);
        return (Class<? extends CompiledExpression>) this.ccl.defineClass(clazzName.replaceAll("/", "."), data);
    }
    public static SpelCompiler getCompiler(ClassLoader classLoader) {
        ClassLoader clToUse = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
        synchronized (compilers) {
            SpelCompiler compiler = compilers.get(clToUse);
            if (compiler == null) {
                compiler = new SpelCompiler(clToUse);
                compilers.put(clToUse, compiler);
            }
            return compiler;
        }
    }

    public static boolean compile(Expression expression) {
        return (expression instanceof SpelExpression && ((SpelExpression) expression).compileExpression());
    }

    public static void revertToInterpreted(Expression expression) {
        if (expression instanceof SpelExpression) {
            ((SpelExpression) expression).revertToInterpreted();
        }
    }

    private static void dump(String expressionText, String name, byte[] bytecode) {
        String nameToUse = name.replace('.', '/');
        String dir = (nameToUse.indexOf('/') != -1 ? nameToUse.substring(0, nameToUse.lastIndexOf('/')) : "");
        String dumpLocation = null;
        try {
            File tempFile = File.createTempFile("tmp", null);
            dumpLocation = tempFile + File.separator + nameToUse + ".class";
            tempFile.delete();
            File f = new File(tempFile, dir);
            f.mkdirs();
            // System.out.println("Expression '" + expressionText + "' compiled code dumped to " + dumpLocation);
            if (logger.isDebugEnabled()) {
                logger.debug("Expression '" + expressionText + "' compiled code dumped to " + dumpLocation);
            }
            f = new File(dumpLocation);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytecode);
            fos.flush();
            fos.close();
        }
        catch (IOException ex) {
            throw new IllegalStateException(
                    "Unexpected problem dumping class '" + nameToUse + "' into " + dumpLocation, ex);
        }
    }

    private static class ChildClassLoader extends URLClassLoader {

        private static final URL[] NO_URLS = new URL[0];

        public ChildClassLoader(ClassLoader classLoader) {
            super(NO_URLS, classLoader);
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }


    private class ExpressionClassWriter extends ClassWriter {

        public ExpressionClassWriter() {
            super(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        }

        @Override
        protected ClassLoader getClassLoader() {
            return ccl;
        }
    }
}

package org.myspring.expression.spel.standard;

import org.myspring.core.convert.TypeDescriptor;
import org.myspring.core.util.Assert;
import org.myspring.expression.EvaluationContext;
import org.myspring.expression.EvaluationException;
import org.myspring.expression.Expression;
import org.myspring.expression.TypedValue;
import org.myspring.expression.common.ExpressionUtils;
import org.myspring.expression.spel.*;
import org.myspring.expression.spel.ast.SpelNodeImpl;
import org.myspring.expression.spel.support.StandardEvaluationContext;

public class SpelExpression implements Expression {

    private static final int INTERPRETED_COUNT_THRESHOLD = 100;

    // Number of times to try compiling an expression before giving up
    private static final int FAILED_ATTEMPTS_THRESHOLD = 100;


    private final String expression;

    private final SpelNodeImpl ast;

    private final SpelParserConfiguration configuration;

    // The default context is used if no override is supplied by the user
    private EvaluationContext evaluationContext;

    // Holds the compiled form of the expression (if it has been compiled)
    private CompiledExpression compiledAst;

    // Count of many times as the expression been interpreted - can trigger compilation
    // when certain limit reached
    private volatile int interpretedCount = 0;

    // The number of times compilation was attempted and failed - enables us to eventually
    // give up trying to compile it when it just doesn't seem to be possible.
    private volatile int failedAttempts = 0;

    public SpelExpression(String expression, SpelNodeImpl ast, SpelParserConfiguration configuration) {
        this.expression = expression;
        this.ast = ast;
        this.configuration = configuration;
    }

    public void setEvaluationContext(EvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
    }

    public EvaluationContext getEvaluationContext() {
        if (this.evaluationContext == null) {
            this.evaluationContext = new StandardEvaluationContext();
        }
        return this.evaluationContext;
    }

    @Override
    public String getExpressionString() {
        return this.expression;
    }

    @Override
    public Object getValue() throws EvaluationException {
        if (this.compiledAst != null) {
            try {
                EvaluationContext context = getEvaluationContext();
                return this.compiledAst.getValue(context.getRootObject().getValue(), context);
            }
            catch (Throwable ex) {
                // If running in mixed mode, revert to interpreted
                if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
                    this.interpretedCount = 0;
                    this.compiledAst = null;
                }
                else {
                    // Running in SpelCompilerMode.immediate mode - propagate exception to caller
                    throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
                }
            }
        }

        ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
        Object result = this.ast.getValue(expressionState);
        checkCompile(expressionState);
        return result;
    }

    public <T> T getValue(Class<T> expectedResultType) throws EvaluationException {
        if (this.compiledAst != null) {
            try {
                EvaluationContext context = getEvaluationContext();
                Object result = this.compiledAst.getValue(context.getRootObject().getValue(), context);
                if (expectedResultType == null) {
                    return (T) result;
                }
                else {
                    return ExpressionUtils.convertTypedValue(
                            getEvaluationContext(), new TypedValue(result), expectedResultType);
                }
            }
            catch (Throwable ex) {
                // If running in mixed mode, revert to interpreted
                if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
                    this.interpretedCount = 0;
                    this.compiledAst = null;
                }
                else {
                    // Running in SpelCompilerMode.immediate mode - propagate exception to caller
                    throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
                }
            }
        }

        ExpressionState expressionState = new ExpressionState(getEvaluationContext(), this.configuration);
        TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
        checkCompile(expressionState);
        return ExpressionUtils.convertTypedValue(
                expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
    }

    @Override
    public Object getValue(Object rootObject) throws EvaluationException {
        if (this.compiledAst != null) {
            try {
                return this.compiledAst.getValue(rootObject, getEvaluationContext());
            }
            catch (Throwable ex) {
                // If running in mixed mode, revert to interpreted
                if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
                    this.interpretedCount = 0;
                    this.compiledAst = null;
                }
                else {
                    // Running in SpelCompilerMode.immediate mode - propagate exception to caller
                    throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
                }
            }
        }

        ExpressionState expressionState =
                new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
        Object result = this.ast.getValue(expressionState);
        checkCompile(expressionState);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(Object rootObject, Class<T> expectedResultType) throws EvaluationException {
        if (this.compiledAst != null) {
            try {
                Object result = this.compiledAst.getValue(rootObject, getEvaluationContext());
                if (expectedResultType == null) {
                    return (T)result;
                }
                else {
                    return ExpressionUtils.convertTypedValue(
                            getEvaluationContext(), new TypedValue(result), expectedResultType);
                }
            }
            catch (Throwable ex) {
                // If running in mixed mode, revert to interpreted
                if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
                    this.interpretedCount = 0;
                    this.compiledAst = null;
                }
                else {
                    // Running in SpelCompilerMode.immediate mode - propagate exception to caller
                    throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
                }
            }
        }

        ExpressionState expressionState =
                new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
        TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
        checkCompile(expressionState);
        return ExpressionUtils.convertTypedValue(
                expressionState.getEvaluationContext(), typedResultValue, expectedResultType);
    }

    @Override
    public Object getValue(EvaluationContext context) throws EvaluationException {
        Assert.notNull(context, "EvaluationContext is required");

        if (this.compiledAst != null) {
            try {
                return this.compiledAst.getValue(context.getRootObject().getValue(), context);
            }
            catch (Throwable ex) {
                // If running in mixed mode, revert to interpreted
                if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
                    this.interpretedCount = 0;
                    this.compiledAst = null;
                }
                else {
                    // Running in SpelCompilerMode.immediate mode - propagate exception to caller
                    throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
                }
            }
        }

        ExpressionState expressionState = new ExpressionState(context, this.configuration);
        Object result = this.ast.getValue(expressionState);
        checkCompile(expressionState);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(EvaluationContext context, Class<T> expectedResultType) throws EvaluationException {
        Assert.notNull(context, "EvaluationContext is required");

        if (this.compiledAst != null) {
            try {
                Object result = this.compiledAst.getValue(context.getRootObject().getValue(), context);
                if (expectedResultType != null) {
                    return ExpressionUtils.convertTypedValue(context, new TypedValue(result), expectedResultType);
                }
                else {
                    return (T) result;
                }
            }
            catch (Throwable ex) {
                // If running in mixed mode, revert to interpreted
                if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
                    this.interpretedCount = 0;
                    this.compiledAst = null;
                }
                else {
                    // Running in SpelCompilerMode.immediate mode - propagate exception to caller
                    throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
                }
            }
        }

        ExpressionState expressionState = new ExpressionState(context, this.configuration);
        TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
        checkCompile(expressionState);
        return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
    }

    @Override
    public Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException {
        Assert.notNull(context, "EvaluationContext is required");

        if (this.compiledAst != null) {
            try {
                return this.compiledAst.getValue(rootObject, context);
            }
            catch (Throwable ex) {
                // If running in mixed mode, revert to interpreted
                if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
                    this.interpretedCount = 0;
                    this.compiledAst = null;
                }
                else {
                    // Running in SpelCompilerMode.immediate mode - propagate exception to caller
                    throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
                }
            }
        }

        ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
        Object result = this.ast.getValue(expressionState);
        checkCompile(expressionState);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(EvaluationContext context, Object rootObject, Class<T> expectedResultType)
            throws EvaluationException {

        Assert.notNull(context, "EvaluationContext is required");

        if (this.compiledAst != null) {
            try {
                Object result = this.compiledAst.getValue(rootObject, context);
                if (expectedResultType != null) {
                    return ExpressionUtils.convertTypedValue(context, new TypedValue(result), expectedResultType);
                }
                else {
                    return (T) result;
                }
            }
            catch (Throwable ex) {
                // If running in mixed mode, revert to interpreted
                if (this.configuration.getCompilerMode() == SpelCompilerMode.MIXED) {
                    this.interpretedCount = 0;
                    this.compiledAst = null;
                }
                else {
                    // Running in SpelCompilerMode.immediate mode - propagate exception to caller
                    throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_RUNNING_COMPILED_EXPRESSION);
                }
            }
        }

        ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
        TypedValue typedResultValue = this.ast.getTypedValue(expressionState);
        checkCompile(expressionState);
        return ExpressionUtils.convertTypedValue(context, typedResultValue, expectedResultType);
    }

    @Override
    public Class<?> getValueType() throws EvaluationException {
        return getValueType(getEvaluationContext());
    }

    @Override
    public Class<?> getValueType(Object rootObject) throws EvaluationException {
        return getValueType(getEvaluationContext(), rootObject);
    }

    @Override
    public Class<?> getValueType(EvaluationContext context) throws EvaluationException {
        Assert.notNull(context, "EvaluationContext is required");
        ExpressionState expressionState = new ExpressionState(context, this.configuration);
        TypeDescriptor typeDescriptor = this.ast.getValueInternal(expressionState).getTypeDescriptor();
        return (typeDescriptor != null ? typeDescriptor.getType() : null);
    }

    @Override
    public Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException {
        ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
        TypeDescriptor typeDescriptor = this.ast.getValueInternal(expressionState).getTypeDescriptor();
        return (typeDescriptor != null ? typeDescriptor.getType() : null);
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
        return getValueTypeDescriptor(getEvaluationContext());
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException {
        ExpressionState expressionState =
                new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration);
        return this.ast.getValueInternal(expressionState).getTypeDescriptor();
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException {
        Assert.notNull(context, "EvaluationContext is required");
        ExpressionState expressionState = new ExpressionState(context, this.configuration);
        return this.ast.getValueInternal(expressionState).getTypeDescriptor();
    }

    @Override
    public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject)
            throws EvaluationException {

        Assert.notNull(context, "EvaluationContext is required");
        ExpressionState expressionState = new ExpressionState(context, toTypedValue(rootObject), this.configuration);
        return this.ast.getValueInternal(expressionState).getTypeDescriptor();
    }

    @Override
    public boolean isWritable(Object rootObject) throws EvaluationException {
        return this.ast.isWritable(
                new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration));
    }

    @Override
    public boolean isWritable(EvaluationContext context) throws EvaluationException {
        Assert.notNull(context, "EvaluationContext is required");
        return this.ast.isWritable(new ExpressionState(context, this.configuration));
    }

    @Override
    public boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException {
        Assert.notNull(context, "EvaluationContext is required");
        return this.ast.isWritable(new ExpressionState(context, toTypedValue(rootObject), this.configuration));
    }

    @Override
    public void setValue(Object rootObject, Object value) throws EvaluationException {
        this.ast.setValue(
                new ExpressionState(getEvaluationContext(), toTypedValue(rootObject), this.configuration), value);
    }

    @Override
    public void setValue(EvaluationContext context, Object value) throws EvaluationException {
        Assert.notNull(context, "EvaluationContext is required");
        this.ast.setValue(new ExpressionState(context, this.configuration), value);
    }

    @Override
    public void setValue(EvaluationContext context, Object rootObject, Object value)
            throws EvaluationException {

        Assert.notNull(context, "EvaluationContext is required");
        this.ast.setValue(new ExpressionState(context, toTypedValue(rootObject), this.configuration), value);
    }

    private void checkCompile(ExpressionState expressionState) {
        this.interpretedCount++;
        SpelCompilerMode compilerMode = expressionState.getConfiguration().getCompilerMode();
        if (compilerMode != SpelCompilerMode.OFF) {
            if (compilerMode == SpelCompilerMode.IMMEDIATE) {
                if (this.interpretedCount > 1) {
                    compileExpression();
                }
            }
            else {
                // compilerMode = SpelCompilerMode.MIXED
                if (this.interpretedCount > INTERPRETED_COUNT_THRESHOLD) {
                    compileExpression();
                }
            }
        }
    }


    public boolean compileExpression() {
        if (this.failedAttempts > FAILED_ATTEMPTS_THRESHOLD) {
            // Don't try again
            return false;
        }
        if (this.compiledAst == null) {
            synchronized (this.expression) {
                // Possibly compiled by another thread before this thread got into the sync block
                if (this.compiledAst != null) {
                    return true;
                }
                SpelCompiler compiler = SpelCompiler.getCompiler(this.configuration.getCompilerClassLoader());
                this.compiledAst = compiler.compile(this.ast);
                if (this.compiledAst == null) {
                    this.failedAttempts++;
                }
            }
        }
        return (this.compiledAst != null);
    }

    public void revertToInterpreted() {
        this.compiledAst = null;
        this.interpretedCount = 0;
        this.failedAttempts = 0;
    }


    public SpelNode getAST() {
        return this.ast;
    }
    public String toStringAST() {
        return this.ast.toStringAST();
    }
    private TypedValue toTypedValue(Object object) {
        return (object != null ? new TypedValue(object) : TypedValue.NULL);
    }

}

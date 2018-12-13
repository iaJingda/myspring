package org.myspring.context.expression;

import org.myspring.beans.BeansException;
import org.myspring.beans.factory.BeanExpressionException;
import org.myspring.beans.factory.config.BeanExpressionContext;
import org.myspring.beans.factory.config.BeanExpressionResolver;
import org.myspring.core.convert.ConversionService;
import org.myspring.core.util.Assert;
import org.myspring.core.util.StringUtils;
import org.myspring.expression.Expression;
import org.myspring.expression.ExpressionParser;
import org.myspring.expression.ParserContext;
import org.myspring.expression.spel.SpelParserConfiguration;
import org.myspring.expression.spel.standard.SpelExpressionParser;
import org.myspring.expression.spel.support.StandardEvaluationContext;
import org.myspring.expression.spel.support.StandardTypeConverter;
import org.myspring.expression.spel.support.StandardTypeLocator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StandardBeanExpressionResolver  implements BeanExpressionResolver {

    /** Default expression prefix: "#{" */
    public static final String DEFAULT_EXPRESSION_PREFIX = "#{";

    /** Default expression suffix: "}" */
    public static final String DEFAULT_EXPRESSION_SUFFIX = "}";


    private String expressionPrefix = DEFAULT_EXPRESSION_PREFIX;

    private String expressionSuffix = DEFAULT_EXPRESSION_SUFFIX;

    private ExpressionParser expressionParser;

    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<String, Expression>(256);

    private final Map<BeanExpressionContext, StandardEvaluationContext> evaluationCache =
            new ConcurrentHashMap<BeanExpressionContext, StandardEvaluationContext>(8);

    private final ParserContext beanExpressionParserContext = new ParserContext() {
        @Override
        public boolean isTemplate() {
            return true;
        }
        @Override
        public String getExpressionPrefix() {
            return expressionPrefix;
        }
        @Override
        public String getExpressionSuffix() {
            return expressionSuffix;
        }
    };

    public StandardBeanExpressionResolver() {
        this.expressionParser = new SpelExpressionParser();
    }

    public StandardBeanExpressionResolver(ClassLoader beanClassLoader) {
        this.expressionParser = new SpelExpressionParser(new SpelParserConfiguration(null, beanClassLoader));
    }

    public void setExpressionPrefix(String expressionPrefix) {
        Assert.hasText(expressionPrefix, "Expression prefix must not be empty");
        this.expressionPrefix = expressionPrefix;
    }

    public void setExpressionSuffix(String expressionSuffix) {
        Assert.hasText(expressionSuffix, "Expression suffix must not be empty");
        this.expressionSuffix = expressionSuffix;
    }

    public void setExpressionParser(ExpressionParser expressionParser) {
        Assert.notNull(expressionParser, "ExpressionParser must not be null");
        this.expressionParser = expressionParser;
    }

    @Override
    public Object evaluate(String value, BeanExpressionContext evalContext) throws BeansException {
        if (!StringUtils.hasLength(value)) {
            return value;
        }
        try {
            Expression expr = this.expressionCache.get(value);
            if (expr == null) {
                expr = this.expressionParser.parseExpression(value, this.beanExpressionParserContext);
                this.expressionCache.put(value, expr);
            }
            StandardEvaluationContext sec = this.evaluationCache.get(evalContext);
            if (sec == null) {
                sec = new StandardEvaluationContext(evalContext);
                sec.addPropertyAccessor(new BeanExpressionContextAccessor());
                sec.addPropertyAccessor(new BeanFactoryAccessor());
                sec.addPropertyAccessor(new MapAccessor());
                sec.addPropertyAccessor(new EnvironmentAccessor());
                sec.setBeanResolver(new BeanFactoryResolver(evalContext.getBeanFactory()));
                sec.setTypeLocator(new StandardTypeLocator(evalContext.getBeanFactory().getBeanClassLoader()));
                ConversionService conversionService = evalContext.getBeanFactory().getConversionService();
                if (conversionService != null) {
                    sec.setTypeConverter(new StandardTypeConverter(conversionService));
                }
                customizeEvaluationContext(sec);
                this.evaluationCache.put(evalContext, sec);
            }
            return expr.getValue(sec);
        }
        catch (Throwable ex) {
            throw new BeanExpressionException("Expression parsing failed", ex);
        }
    }

    protected void customizeEvaluationContext(StandardEvaluationContext evalContext) {
    }
}

package org.myspring.expression.spel.standard;

import org.myspring.core.util.Assert;
import org.myspring.expression.ParseException;
import org.myspring.expression.ParserContext;
import org.myspring.expression.common.TemplateAwareExpressionParser;
import org.myspring.expression.spel.SpelParserConfiguration;

public class SpelExpressionParser extends TemplateAwareExpressionParser {
    private final SpelParserConfiguration configuration;

    public SpelExpressionParser() {
        this.configuration = new SpelParserConfiguration();
    }

    public SpelExpressionParser(SpelParserConfiguration configuration) {
        Assert.notNull(configuration, "SpelParserConfiguration must not be null");
        this.configuration = configuration;
    }

    public SpelExpression parseRaw(String expressionString) throws ParseException {
        return doParseExpression(expressionString, null);
    }

    @Override
    protected SpelExpression doParseExpression(String expressionString, ParserContext context) throws ParseException {
        return new InternalSpelExpressionParser(this.configuration).doParseExpression(expressionString, context);
    }

}

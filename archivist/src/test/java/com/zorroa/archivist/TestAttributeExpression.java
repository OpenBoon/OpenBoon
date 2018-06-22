package com.zorroa.archivist;

import com.zorroa.common.domain.Source;
import org.junit.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.io.File;

/**
 * Created by chambers on 7/14/16.
 */
public class TestAttributeExpression {


    @Test
    public void testParse() {

        Source source = new Source(new File("../unittest/resources/images/set01/faces.jpg"));

        SpelParserConfiguration config = new SpelParserConfiguration(true, true);
        ExpressionParser parser = new SpelExpressionParser(config);

        Expression expression = parser.parseExpression("getAttr('source.path')");
        String o = expression.getValue(source, String.class);

        System.out.println(o);



    }
}

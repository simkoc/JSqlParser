/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.expression;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;

/**
 * @author tw
 */
public class ExpressionVisitorAdapterTest {

    public ExpressionVisitorAdapterTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testInExpressionProblem() throws JSQLParserException {
        final List<Object> exprList = new ArrayList<>();
        Select select = (Select) CCJSqlParserUtil.parse("select * from foo where x in (?,?,?)");
        PlainSelect plainSelect = select.getSelectBody(PlainSelect.class);
        Expression where = plainSelect.getWhere();
        where.accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(InExpression expr) {
                super.visit(expr);
                exprList.add(expr.getLeftExpression());
                exprList.add(expr.getLeftItemsList());
                exprList.add(expr.getRightItemsList());
            }
        });

        assertTrue(exprList.get(0) instanceof Expression);
        assertNull(exprList.get(1));
        assertTrue(exprList.get(2) instanceof ItemsList);
    }

    @Test
    public void testInExpression() throws JSQLParserException {
        final List<Object> exprList = new ArrayList<>();
        Select select = (Select) CCJSqlParserUtil.
                parse("select * from foo where (a,b) in (select a,b from foo2)");
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        Expression where = plainSelect.getWhere();
        where.accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(InExpression expr) {
                super.visit(expr);
                exprList.add(expr.getLeftExpression());
                exprList.add(expr.getLeftItemsList());
                exprList.add(expr.getRightItemsList());
            }
        });

        assertNull(exprList.get(0));
        assertTrue(exprList.get(1) instanceof ItemsList);
        assertTrue(exprList.get(2) instanceof ItemsList);
    }

    @Test
    public void testXorExpression() throws JSQLParserException {
        final List<Expression> exprList = new ArrayList<>();
        Select select = (Select) CCJSqlParserUtil.
                parse("SELECT * FROM table WHERE foo XOR bar");
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        Expression where = plainSelect.getWhere();
        where.accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(XorExpression expr) {
                super.visit(expr);
                exprList.add(expr.getLeftExpression());
                exprList.add(expr.getRightExpression());
            }
        });

        assertEquals(2, exprList.size());
        assertTrue(exprList.get(0) instanceof Column);
        assertEquals("foo", ((Column) exprList.get(0)).getColumnName());
        assertTrue(exprList.get(1) instanceof Column);
        assertEquals("bar", ((Column) exprList.get(1)).getColumnName());
    }

    @Test
    public void testOracleHintExpressions() throws JSQLParserException {
        testOracleHintExpression("select --+ MYHINT \n * from foo", "MYHINT", true);
        testOracleHintExpression("select /*+ MYHINT */ * from foo", "MYHINT", false);
    }

    public static void testOracleHintExpression(String sql, String hint, boolean singleLine) throws JSQLParserException {
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        final OracleHint[] holder = new OracleHint[1];
        assertNotNull(plainSelect.getOracleHint());
        plainSelect.getOracleHint().accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(OracleHint hint) {
                super.visit(hint);
                holder[0] = hint;
            }
        });

        assertNotNull(holder[0]);
        assertEquals(singleLine, holder[0].isSingleLine());
        assertEquals(hint, holder[0].getValue());
    }

    @Test
    public void testCurrentTimestampExpression() throws JSQLParserException {
        final List<String> columnList = new ArrayList<String>();
        Select select = (Select) CCJSqlParserUtil.
                parse("select * from foo where bar < CURRENT_TIMESTAMP");
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        Expression where = plainSelect.getWhere();
        where.accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(Column column) {
                super.visit(column);
                columnList.add(column.getColumnName());
            }
        });

        assertEquals(1, columnList.size());
        assertEquals("bar", columnList.get(0));
    }

    @Test
    public void testCurrentDateExpression() throws JSQLParserException {
        final List<String> columnList = new ArrayList<String>();
        Select select = (Select) CCJSqlParserUtil.
                parse("select * from foo where bar < CURRENT_DATE");
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        Expression where = plainSelect.getWhere();
        where.accept(new ExpressionVisitorAdapter() {

            @Override
            public void visit(Column column) {
                super.visit(column);
                columnList.add(column.getColumnName());
            }
        });

        assertEquals(1, columnList.size());
        assertEquals("bar", columnList.get(0));
    }

    @Test
    public void testSubSelectExpressionProblem() throws JSQLParserException {
        Select select = (Select) CCJSqlParserUtil.
                parse("SELECT * FROM t1 WHERE EXISTS (SELECT * FROM t2 WHERE t2.col2 = t1.col1)");
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        Expression where = plainSelect.getWhere();
        ExpressionVisitorAdapter adapter = new ExpressionVisitorAdapter();
        adapter.setSelectVisitor(new SelectVisitorAdapter());
        try {
            where.accept(adapter);
        } catch (NullPointerException npe) {
            fail();
        }
    }

    @Test
    public void testCaseWithoutElse() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseExpression("CASE WHEN 1 then 0 END");
        ExpressionVisitorAdapter adapter = new ExpressionVisitorAdapter();
        expr.accept(adapter);
    }

    @Test
    public void testCaseWithoutElse2() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseExpression("CASE WHEN 1 then 0 ELSE -1 END");
        ExpressionVisitorAdapter adapter = new ExpressionVisitorAdapter();
        expr.accept(adapter);
    }

    @Test
    public void testCaseWithoutElse3() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseExpression("CASE 3+4 WHEN 1 then 0 END");
        ExpressionVisitorAdapter adapter = new ExpressionVisitorAdapter();
        expr.accept(adapter);
    }

    @Test
    public void testAnalyticFunctionWithoutExpression502() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseExpression("row_number() over (order by c)");
        ExpressionVisitorAdapter adapter = new ExpressionVisitorAdapter();
        expr.accept(adapter);
    }

    @Test
    public void testAtTimeZoneExpression() throws JSQLParserException {
        Expression expr = CCJSqlParserUtil.parseExpression("DATE(date1 AT TIME ZONE 'UTC' AT TIME ZONE 'australia/sydney')");
        ExpressionVisitorAdapter adapter = new ExpressionVisitorAdapter();
        expr.accept(adapter);
    }
}

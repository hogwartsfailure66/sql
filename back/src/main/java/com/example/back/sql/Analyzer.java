package com.example.back.sql;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;

public class Analyzer {
    public static void analyze(String sql, Node root) throws Exception {
        List<SQLStatement> statements;
        try {
            statements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        } catch (Exception e) {
            System.out.println("PARSE ERROR: " + e);
            throw new Exception("WRONG SQL");
        }
        //
        SQLStatement statement = statements.get(0);
        if (statement instanceof MySqlInsertStatement) {
//            System.out.println(((MySqlInsertStatement) statement).getTableName());
            String tableName = ((MySqlInsertStatement) statement).getTableName().toString();
            List<SQLExpr> insertItems = ((MySqlInsertStatement) statement).getColumns();
            SQLSelect selectQuery = ((MySqlInsertStatement) statement).getQuery();
            SQLSelectQueryBlock selectQueryBlock = selectQuery.getQueryBlock();
            List<SQLSelectItem> selectItems = selectQueryBlock.getSelectList();
//            System.out.println(insertItems);
//            System.out.println(selectItems);
            for (int i = 0; i < insertItems.size(); i++) {
                SQLExpr insertItem = insertItems.get(i);
                Column column = new Column();
                column.setType(Type.INSERTITEM);
                SQLSelectItem selectItem = selectItems.get(i);
                String resultString = selectItem.getAlias() == null ? selectItem.getExpr().toString() : selectItem.getAlias();
                resultString = resultString.replace("`", "");
                if (resultString.contains(".") && !resultString.contains("(")) {
                    int index = resultString.indexOf(".");
                    resultString = resultString.substring(index + 1);
                }
//                System.out.println(resultString);
                // 在下一层找resultCol
                column.setResultColumn(resultString);
                column.setExpr(insertItem.toString());
                column.setSourceTableName(tableName);
                Node node = new Node(column);
                // insert应该都是根节点？？先暂时这样
                root.addChild(node);
                analyze(selectQuery.toString(), node);
            }
        } else if (statement instanceof SQLCreateViewStatement) {
            SQLCreateViewStatement createViewStatement = (SQLCreateViewStatement) statement;
            String viewName = createViewStatement.getTableSource().toString();
            SQLSelect subQuery = createViewStatement.getSubQuery();
            SQLSelectQueryBlock subQueryBlock = subQuery.getQueryBlock();
            List<SQLSelectItem> selectItems = subQueryBlock.getSelectList();
            for (SQLSelectItem selectItem : selectItems) {
                // 在下一层找resultCol
                String resultString = selectItem.getAlias() == null ? selectItem.getExpr().toString() : selectItem.getAlias();
                resultString = resultString.replace("`", "");
                if (resultString.contains(".") && !resultString.contains("(")) {
                    int index = resultString.indexOf(".");
                    resultString = resultString.substring(index + 1);
                }
                Column column = new Column();
                column.setType(Type.VIEWITEM);
                column.setSourceTableName(viewName);
                column.setResultColumn(resultString);
                Node node = new Node(column);
                root.addChild(node);
                analyze(subQuery.toString(), node);
            }
        } else if (statement instanceof SQLSelectStatement) {
            SQLSelectQuery selectQuery = ((SQLSelectStatement) statement).getSelect().getQuery();
            if (selectQuery instanceof SQLSelectQueryBlock) {
                SQLSelectQueryBlock selectQueryBlock = (SQLSelectQueryBlock) selectQuery;
                System.out.println("===============SELECT QUERY BLOCK===============");
                System.out.println(selectQueryBlock);
                List<SQLSelectItem> selectItems = selectQueryBlock.getSelectList();
                for (SQLSelectItem selectItem : selectItems) {
                    String columnName, alias = selectItem.getAlias();
                    Column column = new Column();
                    column.type = Type.IDENTIFIER;    // 先暂时这样
                    if (alias != null) {
                        columnName = alias;
                        column.setType(Type.ALIAS);
                    } else {
                        columnName = selectItem.toString();
                    }
                    String sourceTableName = null;
                    columnName = columnName.replace("`", "");
                    if (columnName.contains(".") && !columnName.contains("(")) {
                        int index = columnName.indexOf(".");
                        sourceTableName = columnName.substring(0, index);
                        columnName = columnName.substring(index + 1);
                    }
                    SQLExpr expr = selectItem.getExpr();
                    column.setExpr(expr.toString());
                    column.setResultColumn(columnName);
                    column.setSourceTableName(sourceTableName);
                    System.out.println("selectItem: " + expr + ", col: " + columnName +
                            ", alias: " + alias + ", sourceTable: " + sourceTableName);
                    Node node = new Node(column);
                    if (root.getColumn().getType() == Type.ROOT || root.getColumn().getResultColumn().equals(columnName)) {
                        // 名字一样才加，不然不是这个result column的
                        root.addChild(node);
                    }
                    analyzeExpr(expr, node);
                }
                SQLTableSource table = selectQueryBlock.getFrom();
                if (table != null) {
                    analyzeTable(table, root);
                }
            } else if (selectQuery instanceof SQLUnionQuery) {
                SQLUnionQuery unionQuery = (SQLUnionQuery) selectQuery;
                SQLSelectQuery left = unionQuery.getLeft();
                SQLSelectQuery right = unionQuery.getRight();
                SQLSelectQueryBlock leftBlock = (SQLSelectQueryBlock) left;
                List<SQLSelectItem> selectItems = leftBlock.getSelectList();
                for (SQLSelectItem selectItem : selectItems) {
                    String resultString = selectItem.getAlias() == null ? selectItem.getExpr().toString() : selectItem.getAlias();
                    resultString = resultString.replace("`", "");
                    if (resultString.contains(".") && !resultString.contains("(")) {
                        int index = resultString.indexOf(".");
                        resultString = resultString.substring(index + 1);
                    }
                    Column column = new Column();
                    column.setType(Type.UNIONITEM);
                    column.setResultColumn(resultString);
                    Node node = new Node(column);
                    root.addChild(node);
                    analyze(leftBlock.toString(), node);
                    analyze(right.toString(), node);
                }
            } else {
                System.out.println("ERROR SELECT QUERY TYPE:" + selectQuery.getClass());
            }
        } else if (statement instanceof SQLUpdateStatement) {
            // 好像druid不是很支持update/delete ... select ..., 无语, 以后再看
//            PARSE ERROR: com.alibaba.druid.sql.parser.ParserException: syntax error, error in :'INNER JOIN (SELECT id, name
//            FR', expect IDENTIFIER, actual IDENTIFIER pos 1, line 1, column 2, token IDENTIFIER null
//            MySqlUpdateStatement updateStatement = (MySqlUpdateStatement) statement;
//            System.out.println(updateStatement);
//            System.out.println(updateStatement.getTableSource());
//            String tableName = updateStatement.getTableName().toString();
//            SQLTableSource table = updateStatement.getTableSource();
//            updateStatement.getItems(); // A.name = c.name
        } else if (statement instanceof SQLDeleteStatement) {
            // ...
        } else {
            System.out.println("OTHER STATEMENT TYPE: " + statement.getClass());
        }
    }

    public static void analyzeExpr(SQLExpr expr, Node root) {
        if (expr instanceof SQLIdentifierExpr) {
            // select id, name
            analyzeIdentifierExpr((SQLIdentifierExpr) expr, root);
        } else if (expr instanceof SQLPropertyExpr) {
            // select a.id, b.name
            analyzePropertyExpr((SQLPropertyExpr) expr, root);
        } else if (expr instanceof SQLAggregateExpr) {
            // count, min, max, sum, avg...
            analyzeAggregateExpr((SQLAggregateExpr) expr, root);
        } else if (expr instanceof SQLMethodInvokeExpr) {
            // concat...
            analyzeMethodInvokeExpr((SQLMethodInvokeExpr) expr, root);
        } else if (expr instanceof SQLCharExpr) {
            analyzeCharExpr((SQLCharExpr) expr, root);
        } else if (expr instanceof SQLIntegerExpr) {
            analyzeIntegerExpr((SQLIntegerExpr) expr, root);
        } else if (expr instanceof SQLBinaryOpExpr) {
            // xx + xx ...
            analyzeBinaryOpExpr((SQLBinaryOpExpr) expr, root);
        } else if (expr instanceof SQLBooleanExpr) {
            analyzeBooleanExpr((SQLBooleanExpr) expr, root);
        } else if (expr instanceof SQLNumericLiteralExpr) {
            analyzeNumericLiteralExpr((SQLNumericLiteralExpr) expr, root);
        } else if (expr instanceof SQLDateExpr) {
            analyzeDateExpr((SQLDateExpr) expr, root);
        } else if (expr instanceof SQLTextLiteralExpr) {
            analyzeTextLiteralExpr((SQLTextLiteralExpr) expr, root);
        } else if (expr instanceof SQLTimestampExpr) {
            analyzeTimestampExpr((SQLTimestampExpr) expr, root);
        } else {
            System.out.println(expr + "ERROR OTHER EXPR TYPE: " + expr.getClass());
        }
    }

    public static void analyzeIdentifierExpr(SQLIdentifierExpr expr, Node root) {
        // id, name ...
        String name = expr.getName();
        if (!(root.getColumn().getType() == Type.IDENTIFIER && root.getColumn().getResultColumn().equals(name))) {
            // 跟root一样就返回
            Column column = new Column();
            column.setResultColumn(name);
            column.setExpr(expr.toString());
            column.setType(Type.IDENTIFIER);
            if (!root.findChild(column)) {
                // 防止重复加
                root.addChild(column);
            }
        }
    }

    public static void analyzePropertyExpr(SQLPropertyExpr expr, Node root) {
        // temp.name
        String owner = expr.getOwnernName(), name = expr.getName();
//        System.out.println(owner);   // temp
//        System.out.println(name);    // name
        if (!(root.getColumn().getResultColumn().equals(name) && root.getColumn().getExpr().equals(expr.toString()))) {
            // 防止加当前节点一样的
            Column column = new Column();
            column.setSourceTableName(owner);
            column.setResultColumn(name);
            column.setExpr(expr.toString());
            column.setType(Type.PROPERTY);
            if (!root.findChild(column)) {
                // 防止重复加， e.g. "(A.sell_price - A.supply_price) / A.sell_price"
                root.addChild(column);
            }
        } else {
            root.getColumn().setType(Type.PROPERTY);
        }
    }

    public static void analyzeAggregateExpr(SQLAggregateExpr expr, Node root) {
//        if (root.getColumn().getType() == Type.ALIAS) {
//            Column column = new Column();
//            column.setType(Type.METHOD);
//            String exprString = root.getColumn().getExpr();
//            column.setExpr(exprString);
//            column.setResultColumn(exprString);
//            Node node = new Node(column);
//            root.addChild(node);
//            List<SQLExpr> exprList = expr.getArguments();
//            for (SQLExpr e : exprList) {
//                analyzeExpr(e, node);
//            }
//        } else {
//            root.getColumn().setType(Type.METHOD);
//            List<SQLExpr> exprList = expr.getArguments();
//            for (SQLExpr e : exprList) {
//                analyzeExpr(e, root);
//            }
//        }
        // 算了，alias好像也不影响，最后显示的时候来处理
        root.getColumn().setType(Type.METHOD);
        List<SQLExpr> exprList = expr.getArguments();
        for (SQLExpr e : exprList) {
            analyzeExpr(e, root);
        }
    }

    public static void analyzeMethodInvokeExpr(SQLMethodInvokeExpr expr, Node root) {
//        if (root.getColumn().getType() == Type.ALIAS) {
//            Column column = new Column();
//            column.setType(Type.METHOD);
//            String exprString = root.getColumn().getExpr();
//            column.setExpr(exprString);
//            column.setResultColumn(exprString);
//            Node node = new Node(column);
//            root.addChild(node);
//            List<SQLExpr> exprList = expr.getParameters();
//            for (SQLExpr e : exprList) {
//                analyzeExpr(e, node);
//            }
//        } else {
//            root.getColumn().setType(Type.METHOD);
//            List<SQLExpr> exprList = expr.getParameters();
//            for (SQLExpr e : exprList) {
//                analyzeExpr(e, root);
//            }
//        }
        root.getColumn().setType(Type.METHOD);
        List<SQLExpr> exprList = expr.getParameters();
        for (SQLExpr e : exprList) {
            analyzeExpr(e, root);
        }
    }

    public static void analyzeCharExpr(SQLCharExpr expr, Node root) {
//        System.out.println(expr.getText());
        String text = expr.getText();
        if (!root.findConstChild(text)) {
            Column column = new Column();
            column.setType(Type.CONST);
            column.setResultColumn(text);
            root.addChild(column);
        }
    }

    public static void analyzeIntegerExpr(SQLIntegerExpr expr, Node root) {
        String text = expr.getNumber().toString();
        if (!root.findConstChild(text)) {
            Column column = new Column();
            column.setType(Type.CONST);
            column.setResultColumn(text);
            root.addChild(column);
        }
    }

    public static void analyzeBinaryOpExpr(SQLBinaryOpExpr expr, Node root) {
        root.getColumn().setType(Type.BINARYOP);
        analyzeExpr(expr.getLeft(), root);
        analyzeExpr(expr.getRight(), root);
    }

    public static void analyzeBooleanExpr(SQLBooleanExpr expr, Node root) {
        String text = expr.toString();
        if (!root.findConstChild(text)) {
            Column column = new Column();
            column.setType(Type.CONST);
            column.setResultColumn(text);
            root.addChild(column);
        }
    }

    public static void analyzeNumericLiteralExpr(SQLNumericLiteralExpr expr, Node root) {
        String text = expr.getNumber().toString();
        if (!root.findConstChild(text)) {
            Column column = new Column();
            column.setType(Type.CONST);
            column.setResultColumn(text);
            root.addChild(column);
        }
    }

    public static void analyzeDateExpr(SQLDateExpr expr, Node root) {
        String text = expr.getLiteral().toString();
        if (!root.findConstChild(text)) {
            Column column = new Column();
            column.setType(Type.CONST);
            column.setResultColumn(text);
            root.addChild(column);
        }
    }

    public static void analyzeTextLiteralExpr(SQLTextLiteralExpr expr, Node root) {
        String text = expr.getText();
        if (!root.findConstChild(text)) {
            Column column = new Column();
            column.setType(Type.CONST);
            column.setResultColumn(text);
            root.addChild(column);
        }
    }

    public static void analyzeTimestampExpr(SQLTimestampExpr expr, Node root) {
        String text = expr.getLiteral();
        if (!root.findConstChild(text)) {
            Column column = new Column();
            column.setType(Type.CONST);
            column.setResultColumn(text);
            root.addChild(column);
        }
    }

    public static void analyzeTable(SQLTableSource table, Node root) throws Exception {
        if (table instanceof SQLSubqueryTableSource) {
            // 子查询
            analyzeSubqueryTable((SQLSubqueryTableSource) table, root);
        } else if (table instanceof SQLJoinTableSource) {
            analyzeJoinTable((SQLJoinTableSource) table, root);
        } else if (table instanceof SQLExprTableSource) {
            // 最终的table e.g. from student
            analyzeExprTable((SQLExprTableSource) table, root);
        } else if (table instanceof SQLUnionQueryTableSource) {
            analyzeUnionTable((SQLUnionQueryTableSource) table, root);
        } else {
            System.out.println("OTHER SQL TABLE: " + table.getClass());
        }
    }

    public static void analyzeExprTable(SQLExprTableSource table, Node root) {
        String dbName = null, tableName, alias = table.getAlias();
        SQLExpr expr = table.getExpr();
        if (expr instanceof SQLPropertyExpr) {
            // A.student 有dbname A
            dbName = ((SQLPropertyExpr) expr).getOwnernName();
            tableName = ((SQLPropertyExpr) expr).getName();
//            System.out.println(dbName);
//            System.out.println(tableName);
        } else {
            tableName = table.getExpr().toString();
//            System.out.println(tableName);
        }
        List<Node> children = root.getChildren();
        if (children.isEmpty()) {
            // 防止join出错
//            无语，好像不太对...之后再改
        }
        for (Node child : children) {
            // 有alias就有两层，这样应该是对的？？
            analyzeExprTable(table, child);
            if (child.getColumn().sourceTableName == null || child.getColumn().getSourceTableName().equals(tableName)
                    || child.getColumn().getSourceTableName().equals(alias)) {
                child.getColumn().setSourceTableName(tableName);
                child.getColumn().setSourceDbName(dbName);
//                // 防止join出错
//                // 无语，好像不太对...之后再改
            }
        }
    }

    public static void analyzeJoinTable(SQLJoinTableSource table, Node root) throws Exception {
        // JOIN好像有问题，以后改
        SQLTableSource leftTable = table.getLeft(), rightTable = table.getRight();
        List<Node> leaf = root.getLeaf();
        for (Node node : leaf) {
            if (node.getColumn().getType() == Type.CONST) {
                continue;
            }
            analyzeTable(leftTable, node);
            analyzeTable(rightTable, node);
        }
    }

    public static void analyzeSubqueryTable(SQLSubqueryTableSource table, Node root) throws Exception {
        List<Node> leaf = root.getLeaf();
        for (Node node : leaf) {
            if (node.getColumn().getType() == Type.CONST) {
                continue;
            }
            analyze(table.toString(), node);
        }
    }

    public static void analyzeUnionTable(SQLUnionQueryTableSource table, Node root) throws Exception {
        List<Node> leaf = root.getLeaf();
        for (Node node : leaf) {
            if (node.getColumn().getType() == Type.CONST) {
                continue;
            }
            analyze(table.getUnion().toString(), node);
        }
    }
}

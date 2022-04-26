package com.example.back.sql;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Node {
    Column column;
    List<Node> children;
    Node parent;
    Boolean terminal;

    public Node(Column column) {
        this.column = column;
        children = new ArrayList<>();
        parent = null;
        terminal = false;
    }

    public List<Node> getChildren() {
        return children;
    }

    public Column getColumn() {
        return column;
    }

    public Boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }

    public void addChild(Node node) {
        node.parent = this;
        this.children.add(node);
    }

    public void addChild(Column column) {
        Node child = new Node(column);
        child.parent = this;
        this.children.add(child);
    }

    public boolean findConstChild(String s) {
        for (Node child : children) {
            if (child.getColumn().getType() == Type.CONST && child.getColumn().getResultColumn().equals(s)) {
                return true;
            }
        }
        return false;
    }

    public boolean findChild(Column column) {
        //        System.out.println("findchildren:");
//        System.out.println("column:" + column.getType() + "," + column.getExpr() + "," + column.getResultColumn() + "," + column.getSourceTableName());
        for (Node child : children) {
//            Node.printNode(child);
            if (child.getColumn().getType() == column.getType() && child.getColumn().getExpr().equals(column.getExpr())
                    && child.getColumn().getResultColumn().equals(column.getResultColumn())
                    && child.getColumn().getExpr().equals(column.getExpr())) {
                if (column.getSourceTableName() == null) {
//                    System.out.println("true");
                    return true;
                } else {
                    if (child.getColumn().getSourceTableName().equals(column.getSourceTableName())) {
//                        System.out.println("true");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<Node> getLeaf() {
        List<Node> res = new ArrayList<>();
        if (children.size() == 0) {
            res.add(this);
        } else {
            for (Node child : children) {
                res.addAll(child.getLeaf());
            }
        }
        return res;
    }

    public int getLevel() {
        if (getColumn().getType() == Type.ROOT) return 0;
        else return parent.getLevel() + 1;
    }

    public static void printNode(Node node) {
        System.out.println("     ".repeat(Math.max(0, node.getLevel() - 1)) + JSONObject.toJSONString(node.getColumn()));
    }

    public void printTree() {
        Stack<Node> stack = new Stack<>();
        stack.add(this);
        while (!stack.empty()) {
            Node tmp = stack.pop();
            printNode(tmp);
            stack.addAll(tmp.getChildren());
        }
    }

    public static void printTree(Node node) {
        printNode(node);
        List<Node> nodes = node.getChildren();
        if (!nodes.isEmpty()) {
            for (Node n : nodes) {
                printTree(n);
            }
        }
    }
}

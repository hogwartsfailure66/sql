package com.example.back.service;

import com.example.back.entity.GeneralNode;
import com.example.back.entity.MyRelationship;
import com.example.back.repository.NodeRepository;
import com.example.back.repository.RelationshipRepository;
import com.example.back.sql.Column;
import com.example.back.sql.Node;
import com.example.back.sql.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NodeService {
    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    RelationshipRepository relationshipRepository;

    public GeneralNode findByName(String name) {
        return nodeRepository.findByName(name);
    }

    public Iterable<GeneralNode> findAll() {
        return nodeRepository.findAll();
    }

    public void deleteAll() {
        nodeRepository.deleteAll();
    }

    public void save(GeneralNode node) {
        nodeRepository.save(node);
    }

    public GeneralNode search(String name) {
        GeneralNode ret = nodeRepository.findByName(name);
        if (ret == null) {
            ret = new GeneralNode(name);
            nodeRepository.save(ret);
        }
        return ret;
    }

    public GeneralNode search(Node node) {
        Column column = node.getColumn();
        Type type = column.getType();
        String name = "";
        if (type == Type.PROPERTY) {
            name += column.getExpr();
        } else if (type == Type.METHOD) {
            if (column.getResultColumn().equals(column.getExpr())) {
                // no alias
                name += column.getExpr();
            } else {
                name += column.getResultColumn() + "[func:" + column.getExpr() + "]";
            }
        } else if (type == Type.BINARYOP) {
            if (column.getResultColumn().equals(column.getExpr())) {
                // no alias
                name += column.getExpr();
            } else {
                name += column.getResultColumn() + "[OP:" + column.getExpr() + "]";
            }
        } else if (type == Type.CONST) {
            name += "'" + column.getResultColumn() + "'";
        } else if (type == Type.ALIAS) {
            if (column.getSourceTableName() != null) {
                name += column.getSourceTableName() + ".";
            }
            name += column.getResultColumn() + "[ALIAS]";
        } else if (type == Type.INSERTITEM) {
            name += column.getSourceTableName() + "." + column.getExpr();
        } else if (type == Type.IDENTIFIER) {
            if (column.getSourceTableName() != null) {
                name += column.getSourceTableName() + ".";
            }
            name += column.getExpr();
        } else if (type == Type.VIEWITEM) {
            name += column.getSourceTableName() + "." + column.getResultColumn();
        } else if (type == Type.UNIONITEM) {
            name += column.getResultColumn() + "[UNION]";
        }
        return search(name);
    }

    public void handleInNeo4j(Node node) {
        Column column = node.getColumn();
        if (column.getType() == Type.ROOT) {
            List<Node> children = node.getChildren();
            for (Node child : children) {
                handleInNeo4j(child);
            }
        } else {
            GeneralNode end = search(node);
            List<Node> children = node.getChildren();
            for (Node child : children) {
                MyRelationship relationship = new MyRelationship();
                GeneralNode start = search(child);
                if (!start.getRelatedNodes().contains(end)) {
                    start.addRelated(end);
                }
                relationship.setEndNode(end);
                relationship.setStartNode(start);
                relationshipRepository.save(relationship);
                handleInNeo4j(child);
            }
        }
    }
}

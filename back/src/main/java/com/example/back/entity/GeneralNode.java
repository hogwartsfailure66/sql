package com.example.back.entity;

import lombok.Data;
import org.neo4j.ogm.annotation.*;

import java.util.HashSet;
import java.util.Set;

@Data
@NodeEntity
public class GeneralNode {
    @Id
    @GeneratedValue
    private Long id;

    @Property
    String name;

//    @Relationship(type = "MYRELATIONSHIP", direction = Relationship.OUTGOING)
    private Set<GeneralNode> relatedNodes = new HashSet<>();

    public GeneralNode(String name) {
        this.name = name;
    }

    public void addRelated(GeneralNode node) {
        relatedNodes.add(node);
    }
}

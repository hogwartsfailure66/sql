package com.example.back.entity;

import lombok.Data;
import org.neo4j.ogm.annotation.*;

@RelationshipEntity
@Data
public class MyRelationship {
    @Id
    @GeneratedValue
    Long id;

    @StartNode
    GeneralNode startNode;

    @EndNode
    GeneralNode endNode;

    @Property
    String relation;
}
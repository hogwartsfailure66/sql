package com.example.back.repository;

import com.example.back.entity.MyRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface RelationshipRepository extends Neo4jRepository<MyRelationship, Long> {
}

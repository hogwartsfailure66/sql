package com.example.back.repository;

import com.example.back.entity.GeneralNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface NodeRepository extends Neo4jRepository<GeneralNode, Long> {
    GeneralNode findByName(String name);

    Iterable<GeneralNode> findAll();
}

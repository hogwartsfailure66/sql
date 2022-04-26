package com.example.back.service;

import com.example.back.entity.MyRelationship;
import com.example.back.repository.RelationshipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RelationshipService {
    @Autowired
    RelationshipRepository relationshipRepository;

    public void deleteAll() {
        relationshipRepository.deleteAll();
    }

    public void save(MyRelationship relationship) {
        relationshipRepository.save(relationship);
    }
}

package com.example.back.controller;

import com.example.back.entity.GeneralNode;
import com.example.back.entity.Link;
import com.example.back.service.NodeService;
import com.example.back.service.RelationshipService;
import com.example.back.sql.Analyzer;
import com.example.back.sql.Column;
import com.example.back.sql.Node;
import com.example.back.sql.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/api")
public class mainController {
    @Autowired
    NodeService nodeService;

    @Autowired
    RelationshipService relationshipService;

    @CrossOrigin
    @PostMapping("/analyze")
    public HashMap<String, Object> Analyze(@RequestBody String sql) {
        System.out.println(sql);
        Node root = new Node(new Column());
        root.getColumn().setType(Type.ROOT);
        HashMap<String, Object> map = new HashMap<>();
//        Analyzer.analyze(sql, root);
        try {
            Analyzer.analyze(sql, root);
        } catch (Exception e) {
            map.put("error", "WRONG SQL");
            map.put("nodes", null);
            map.put("links", null);
//            System.out.println("ERROR:" + e);
            return map;
        }
        Node.printTree(root);
        relationshipService.deleteAll();
        nodeService.deleteAll();
        nodeService.handleInNeo4j(root);
        Iterable<GeneralNode> nodes = nodeService.findAll();
        List<Link> links = new ArrayList<>();
        for (GeneralNode node : nodes) {
            Set<GeneralNode> related = node.getRelatedNodes();
            for (GeneralNode r : related) {
                Link link = new Link(node.getName(), r.getName(), 1);
                links.add(link);
            }
        }
        map.put("nodes", nodes);
        map.put("links", links);
        map.put("error", "");
        return map;
    }

/*
测试用
 */

//    @GetMapping("/search/{name}")
//    public GeneralNode findByName(@PathVariable("name") String name) {
////        System.out.println(name);
//        GeneralNode node = nodeService.findByName(name);
////        System.out.println(node.getRelatedNodes().size());
//        return nodeService.findByName(name);
//    }
//
//    @GetMapping("/all")
//    @ResponseBody
//    public HashMap<String, Object> findAll() {
//        Iterable<GeneralNode> nodes = nodeService.findAll();
//        List<Link> links = new ArrayList<>();
//        for (GeneralNode node : nodes) {
//            Set<GeneralNode> related = node.getRelatedNodes();
//            for (GeneralNode r : related) {
//                Link link = new Link(node.getName(), r.getName(), 1);
//                links.add(link);
//            }
//        }
//        HashMap<String, Object> map = new HashMap<>();
//        map.put("nodes", nodes);
//        map.put("links", links);
//        return map;
//    }
}

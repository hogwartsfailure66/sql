package com.example.back.entity;

import lombok.Data;

@Data
public class Link {
    private String source;
    private String target;
    private int value;

    public Link(String source, String target, int value) {
        this.source = source;
        this.target = target;
        this.value = value;
    }
}

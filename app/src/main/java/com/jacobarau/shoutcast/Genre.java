package com.jacobarau.shoutcast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jacob on 10/16/16.
 */
public class Genre {
    private String name;
    private int id;
    private List<Genre> children;

    public Genre(String name, int id, Genre parent) {
        this.name = name;
        this.id = id;
        this.children = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Genre> getChildren() {
        return children;
    }

    public void addChildren(List<Genre> children) {
        this.children.addAll(children);
    }

    public void addChild(Genre child) {
        this.children.add(child);
    }
}

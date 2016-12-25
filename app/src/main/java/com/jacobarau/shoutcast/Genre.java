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

    @Override
    public String toString() {
        return "Genre{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", children=" + children +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Genre genre = (Genre) o;

        if (id != genre.id) return false;
        if (!name.equals(genre.name)) return false;
        return children.equals(genre.children);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + id;
        result = 31 * result + children.hashCode();
        return result;
    }
}

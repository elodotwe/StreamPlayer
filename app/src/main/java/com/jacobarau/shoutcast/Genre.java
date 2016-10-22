package com.jacobarau.shoutcast;

/**
 * Created by jacob on 10/16/16.
 */
public class Genre {
    private String name;
    private int id;
    private int parentID;
    private boolean hasChildren;

    public Genre(String name, int id, int parentID, boolean hasChildren) {
        this.name = name;
        this.id = id;
        this.parentID = parentID;
        this.hasChildren = hasChildren;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Parent ID of 0 means there is no parent--this genre is a top-level genre.
     * @return
     */
    public int getParentID() {
        return parentID;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }
}

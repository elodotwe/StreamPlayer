package com.jacobarau.streamplayer;

/**
 * Created by jacob on 13/02/17.
 */

public class StationPreset {
    public String name;
    public String url;

    public StationPreset(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StationPreset that = (StationPreset) o;

        if (!name.equals(that.name)) return false;
        return url.equals(that.url);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + url.hashCode();
        return result;
    }
}

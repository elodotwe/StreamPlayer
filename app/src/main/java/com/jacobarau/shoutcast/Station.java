package com.jacobarau.shoutcast;

/**
 * Created by jacob on 10/14/16.
 */
public class Station {
    private String name;
    private String mediaType;
    private int id;
    private int bitrate;
    private String genre;
    private String nowPlaying;
    private int listeners;
    private int maxListeners;

    public Station(String name, String mediaType, int id, int bitrate, String genre, String currentTrack, int listeners, int maxListeners) {
        this.name = name;
        this.mediaType = mediaType;
        this.id = id;
        this.bitrate = bitrate;
        this.genre = genre;
        this.nowPlaying = currentTrack;
        this.listeners = listeners;
        this.maxListeners = maxListeners;
    }

    public String getName() {
        return name;
    }

    public String getMediaType() {
        return mediaType;
    }

    public int getId() {
        return id;
    }

    public int getBitrate() {
        return bitrate;
    }

    public String getGenre() {
        return genre;
    }

    public String getNowPlaying() {
        return nowPlaying;
    }

    public int getListeners() {
        return listeners;
    }

    public int getMaxListeners() {
        return maxListeners;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Station station = (Station) o;

        if (id != station.id) return false;
        if (bitrate != station.bitrate) return false;
        if (listeners != station.listeners) return false;
        if (maxListeners != station.maxListeners) return false;
        if (!name.equals(station.name)) return false;
        if (!mediaType.equals(station.mediaType)) return false;
        if (!genre.equals(station.genre)) return false;
        return nowPlaying.equals(station.nowPlaying);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + mediaType.hashCode();
        result = 31 * result + id;
        result = 31 * result + bitrate;
        result = 31 * result + genre.hashCode();
        result = 31 * result + nowPlaying.hashCode();
        result = 31 * result + listeners;
        result = 31 * result + maxListeners;
        return result;
    }
}

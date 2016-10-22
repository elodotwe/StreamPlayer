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

    public Station(String name, String mediaType, int id, int bitrate, String genre, String currentTrack, int listeners) {
        this.name = name;
        this.mediaType = mediaType;
        this.id = id;
        this.bitrate = bitrate;
        this.genre = genre;
        this.nowPlaying = currentTrack;
        this.listeners = listeners;
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
}

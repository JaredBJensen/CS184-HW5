package edu.ucsb.cs.cs184.jaredbjensen.jjensengeotweet;


import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

public class Tweet {
    private ArrayList<LatLng> path;
    private String postId;
    private String author;
    private String content;
    private double timestamp;
    private LatLng location;
    private LatLng lastLocation;
    private int likes;
    private Marker marker;

    public Tweet(String postId, String author, String content, double timestamp, double lat, double lon, int likes) {
        path = new ArrayList<>();
        this.postId = postId;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
        this.location = new LatLng(lat, lon);
        this.lastLocation = location;
        this.likes = likes;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public void setLastLocation(LatLng lastLocation) {
        this.lastLocation = lastLocation;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public ArrayList<LatLng> getPath() {
        return path;
    }

    public String getPostId() {
        return postId;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public LatLng getLocation() {
        return location;
    }

    public LatLng getLastLocation() {
        return lastLocation;
    }

    public int getLikes() {
        return likes;
    }

    public Marker getMarker() {
        return marker;
    }
}

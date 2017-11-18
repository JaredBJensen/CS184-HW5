package edu.ucsb.cs.cs184.jaredbjensen.jjensengeotweet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.nio.file.Path;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, FirebaseHelper.TweetListener {

    private GoogleMap mMap;
    private ArrayList<Marker> markers;
    private TweetDialogFragment dialog;
    int notificationId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FirebaseHelper.Initialize(this);

        markers = new ArrayList<>();
        notificationId = 0;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMarkerClickListener(this);

        LatLng ucsb = new LatLng(34.412936, -119.847863);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(ucsb));
        mMap.setMinZoomPreference(15.3f);
    }

    public void onFirstTweetsAdded(Tweet tweet) {
        addMarker(tweet);
    }

    public void onTweetAdded(Tweet tweet) {
        addMarker(tweet);
        sendNotification(tweet);
    }

    public void onTweetUpdated(Tweet tweet) {
        mMap.addPolyline((new PolylineOptions())
                .add(tweet.getLastLocation(), tweet.getLocation())
                .width(7)
                .color(Color.BLACK)
                .visible(true));
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(tweet.getLocation())
                .title(String.valueOf(tweet.getTimestamp())));
        tweet.getMarker().remove();
        markers.remove(tweet.getMarker());
        tweet.setMarker(marker);
        markers.add(marker);
        if (dialog != null && dialog.isVisible() && dialog.timestamp == tweet.getTimestamp()) {
            String likesText = tweet.getLikes() + " likes";
            dialog.likesText.setText(likesText);
        }
    }

    public void onTweetRemoved(ArrayList<Tweet> tweets, Tweet deleteTweet) {
        mMap.clear();
        for (Tweet tweet : tweets) {
            ArrayList<LatLng> path = tweet.getPath();
            if (path.size() > 1) {
                LatLng previous = path.get(0);
                LatLng current;
                for (int i = 1; i < path.size(); i++) {
                    current = path.get(i);
                    mMap.addPolyline((new PolylineOptions())
                            .add(previous, current)
                            .width(7)
                            .color(Color.BLACK)
                            .visible(true));
                    previous = current;
                }
            }
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(tweet.getLocation())
                    .title(String.valueOf(tweet.getTimestamp())));
            tweet.setMarker(marker);
            markers.add(marker);
        }
        if (dialog != null && dialog.isVisible() && dialog.timestamp == deleteTweet.getTimestamp()) {
            dialog.likeButton.setEnabled(false);
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        for (Tweet tweet : FirebaseHelper.tweets) {
            if (String.valueOf(tweet.getTimestamp()).equals(marker.getTitle())) {
                dialog = new TweetDialogFragment();
                Bundle args = new Bundle();
                args.putString("id", tweet.getPostId());
                args.putString("author", tweet.getAuthor());
                args.putString("content", tweet.getContent());
                args.putInt("likes", tweet.getLikes());
                args.putDouble("timestamp", tweet.getTimestamp());
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), "");
                return true;
            }
        }
        return false;
    }

    public void addMarker(Tweet tweet) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(tweet.getLocation())
                .title(String.valueOf(tweet.getTimestamp())));
        tweet.setMarker(marker);
        markers.add(marker);
    }

    public void sendNotification(Tweet tweet) {
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(tweet.getAuthor())
                .setContentText(tweet.getContent())
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher);

        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("author", tweet.getAuthor());
        intent.putExtra("content", tweet.getContent());
        intent.putExtra("likes", tweet.getLikes());
        intent.putExtra("postId", tweet.getPostId());
        intent.putExtra("timestamp", tweet.getTimestamp());

        PendingIntent pending = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pending);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notificationId++, builder.build());
    }

    @Override
    public void onNewIntent(Intent intent) {
        dialog = new TweetDialogFragment();
        Bundle args = new Bundle();
        args.putString("id", intent.getStringExtra("postId"));
        args.putString("author", intent.getStringExtra("author"));
        args.putString("content", intent.getStringExtra("content"));
        args.putInt("likes", intent.getIntExtra("likes", 0));
        args.putDouble("timestamp", intent.getDoubleExtra("timestamp", 0));
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "");
    }
}

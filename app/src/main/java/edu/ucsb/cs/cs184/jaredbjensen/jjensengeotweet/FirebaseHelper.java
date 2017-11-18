package edu.ucsb.cs.cs184.jaredbjensen.jjensengeotweet;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a Firebase helper starter class we have created for you
 * In your Activity, please call FirebaseHelper.Initialize() to setup the Firebase
 * Put your application logic in OnDatabaseInitialized where you'll have the database object initialized
 */
public class FirebaseHelper {
    /** This is a message data structure that mirrors our Firebase data structure for your convenience */
    public static class Message implements Serializable {
        public double longitude;
        public double latitude;
        public String author;
        public String content;
        public double timestamp;
        public int likes;
    }

    /** ============================================================================================
     * Retrieve Firebase access tokens from a server we setup.
     * You should call FirebaseHelper.Initialize() when your activity starts to initiate the database helper.
     * You don't need to change the code in this section unless we instruct you to.
     */
    private static class RetrieveFirebaseTokensTask extends AsyncTask<Void, Void, ArrayList<String>> {
        private FirebaseTokensListener listener;

        public interface FirebaseTokensListener {
            void onTokens(String url, String apiKey, String applicationID);
        }

        public RetrieveFirebaseTokensTask(FirebaseTokensListener listener) {
            super();
            this.listener = listener;
        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            try {
                // Retrieve data from the url
                URL pageUrl = new URL("http://cs.jalexander.ninja:8080/file/firebase.txt");
                URLConnection connection = pageUrl.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String url = reader.readLine().trim();
                String apikey = reader.readLine().trim();
                String applicationID = reader.readLine().trim();
                ArrayList<String> result = new ArrayList<>();
                result.add(url);
                result.add(apikey);
                result.add(applicationID);
                reader.close();
                return result;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if (listener != null) {
                listener.onTokens(result.get(0), result.get(1), result.get(2));
            }
        }
    }

    /** Keep track of initialized state, so we don't initialize multiple times */
    private static boolean initialized = false;

    /** The Firebase database object */
    private static FirebaseDatabase db;

    /** Initialize the firebase instance */
    public static void Initialize(final Context context) {
        if (!initialized) {
            initialized = true;
            // We retrieve the database access tokens from a server because we may change the actual database url if we run out of quota.
            // This is a very simple example of a "configuration server" -- instead of hardcoding the database configuration, we retrieve them from a server.
            // This approach makes it easy for us to change the database url without having you modify your code.
            RetrieveFirebaseTokensTask retrieveTokensTask = new RetrieveFirebaseTokensTask(new RetrieveFirebaseTokensTask.FirebaseTokensListener() {
                @Override
                public void onTokens(String url, String apiKey, String applicationID) {
                    // Once we get the tokens from our configuration server, we initialize the database API as follows:
                    FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                            .setDatabaseUrl(url)
                            .setApiKey(apiKey)
                            .setProjectId("cs184-hw5")
                            .setApplicationId(applicationID)
                            .build()
                    );
                    // Call the OnDatabaseInitialized to setup application logic
                    OnDatabaseInitialized();
                }
            });
            retrieveTokensTask.execute();
            setTweetListener(context);
        }
    }
    /** ============================================================================================
     */

    public static ArrayList<Tweet> tweets;

    /** This is called once we initialize the firebase database object */
    private static void OnDatabaseInitialized() {
        db = FirebaseDatabase.getInstance();
        DatabaseReference fb = db.getReference();
        DatabaseReference posts = fb.child("posts");

        tweets = new ArrayList<>();

        posts.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Message message = dataSnapshot.getValue(Message.class);
                Tweet tweet = new Tweet(dataSnapshot.getKey(), message.author, message.content, message.timestamp, message.latitude, message.longitude, message.likes);
                tweet.getPath().add(tweet.getLocation());
                tweets.add(tweet);
                tweetListener.onFirstTweetsAdded(tweet);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        posts.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Message message = dataSnapshot.getValue(Message.class);
                Tweet tweet = new Tweet(dataSnapshot.getKey(), message.author, message.content, message.timestamp, message.latitude, message.longitude, message.likes);
                tweet.getPath().add(tweet.getLocation());
                tweets.add(tweet);
                tweetListener.onTweetAdded(tweet);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Message message = dataSnapshot.getValue(Message.class);
                Tweet newTweet = new Tweet(dataSnapshot.getKey(), message.author, message.content, message.timestamp, message.latitude, message.longitude, message.likes);
                for (Tweet tweet : tweets) {
                    if (tweet.getPostId().equals(newTweet.getPostId())) {
                        tweet.getPath().add(newTweet.getLocation());
                        tweet.setLastLocation(tweet.getLocation());
                        tweet.setLocation(newTweet.getLocation());
                        tweet.setLikes(newTweet.getLikes());
                        tweetListener.onTweetUpdated(tweet);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Message message = dataSnapshot.getValue(Message.class);
                Tweet deleteTweet = new Tweet(dataSnapshot.getKey(), message.author, message.content, message.timestamp, message.latitude, message.longitude, message.likes);
                for (int i=0; i<tweets.size(); i++) {
                    if (tweets.get(i).getPostId().equals(deleteTweet.getPostId())) {
                        tweets.remove(i);
                    }
                }
                tweetListener.onTweetRemoved(tweets, deleteTweet);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("", "Error getting data from firebase");
            }
        });
    }

    public interface TweetListener {
        void onFirstTweetsAdded(Tweet tweet);
        void onTweetAdded(Tweet tweet);
        void onTweetUpdated(Tweet tweet);
        void onTweetRemoved(ArrayList<Tweet> tweets, Tweet tweet);
    }

    private static TweetListener tweetListener;

    private static void setTweetListener(Context context) {
        tweetListener = (MapsActivity)context;
    }

    public static void increaseLikes(final String postId) {
        DatabaseReference fb = db.getReference();
        DatabaseReference post = fb.child("posts").child(postId);
        final DatabaseReference likes = post.child("likes");
        likes.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                int l = mutableData.getValue(Integer.class);
                if (l == 0) {
                    return Transaction.success(mutableData);
                }

                l++;
                mutableData.setValue(l);
                return Transaction.success(mutableData);
            }
            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }
}

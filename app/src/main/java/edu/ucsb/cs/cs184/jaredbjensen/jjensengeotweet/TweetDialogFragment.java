package edu.ucsb.cs.cs184.jaredbjensen.jjensengeotweet;

import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class TweetDialogFragment extends DialogFragment {

    View rootView;

    TextView authorText;
    TextView contentText;
    TextView likesText;
    Button likeButton;

    String postId;
    String author;
    String content;
    double timestamp;
    int likes;

    public TweetDialogFragment() {}

    @Override
    public void onResume() {
        super.onResume();

        authorText = rootView.findViewById(R.id.authorText);
        contentText = rootView.findViewById(R.id.contentText);
        likesText = rootView.findViewById(R.id.likesText);
        likeButton = rootView.findViewById(R.id.likeButton);

        authorText.setText(author);
        contentText.setText(content);

        String likeString = likes + " likes";
        likesText.setText(likeString);

        likeButton.setText("+1");

        boolean deleted = true;
        for (Tweet tweet : FirebaseHelper.tweets) {
            if (tweet.getPostId().equals(postId)) {
                deleted = false;
            }
        }
        if (deleted) {
            likeButton.setEnabled(false);
        }
        else {
            likeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseHelper.increaseLikes(postId);
                    likes++;
                }
            });
        }

        Window window = getDialog().getWindow();
        Point dimensions = new Point();
        Display display = window.getWindowManager().getDefaultDisplay();
        display.getSize(dimensions);

        window.setLayout((int)(dimensions.x*.7), (int)(dimensions.y*.7));
        window.setGravity(Gravity.CENTER);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        postId = getArguments().getString("id");
        author = getArguments().getString("author");
        content = getArguments().getString("content");
        likes = getArguments().getInt("likes");
        timestamp = getArguments().getDouble("timestamp");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_dialog_tweet, container, false);
        return rootView;
    }

}

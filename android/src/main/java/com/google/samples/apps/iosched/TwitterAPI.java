package com.google.samples.apps.iosched;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterAPI {

    public static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    public static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    public static final String PREF_KEY_OAUTH_DONE = "oauth_token_done";

    static final String TWITTER_CONSUMER_KEY = "";
    static final String TWITTER_CONSUMER_SECRET = "";
    public static final String TWITTER_CALLBACK_URL = "xda14://devcon.com";

    public static Twitter mTwitter;
    public static RequestToken mToken;

    private static void twitterLogin(Context mContext) {
        // I deserve to be killed with fire, but I don't want to implement an AsyncTask for this...
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        mTwitter = factory.getInstance();

        try {
            mToken = mTwitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(mToken.getAuthenticationURL()));
            mContext.startActivity(i);
            ((Activity) mContext).finish();
        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    public static void createTweetDialog(final Context mContext) {
        if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(PREF_KEY_OAUTH_DONE, false)) {
            twitterLogin(mContext);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        AlertDialog dialog;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        builder.setView(inflater.inflate(R.layout.dialog_twitter, null))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog f = (Dialog) dialog;
                        EditText tweet = (EditText) f.findViewById(R.id.dialog_tweet);
                        sendTweet(mContext, tweet.getText().toString());
                        Toast toast = Toast.makeText(mContext, "Tweet Sent", Toast.LENGTH_LONG);
                        toast.show();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialog = builder.create();
        dialog.show();
    };

    public static void createTweetDialog(final Context mContext, final String hashtag) {
        if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(PREF_KEY_OAUTH_DONE, false)) {
            twitterLogin(mContext);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        AlertDialog dialog;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        builder.setView(inflater.inflate(R.layout.dialog_twitter, null))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog f = (Dialog) dialog;
                        EditText tweet = (EditText) f.findViewById(R.id.dialog_tweet);
                        sendTweet(mContext, "#" + hashtag + " " + tweet.getText().toString());
                        Toast toast = Toast.makeText(mContext, "Tweet Sent", Toast.LENGTH_LONG);
                        toast.show();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialog = builder.create();
        dialog.show();
    };

    private static void sendTweet(final Context mContext, final String tweet) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ConfigurationBuilder cb = new ConfigurationBuilder();
                        cb.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
                        cb.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
                        cb.setOAuthAccessToken(PreferenceManager.getDefaultSharedPreferences(mContext).getString(PREF_KEY_OAUTH_TOKEN, ""));
                        cb.setOAuthAccessTokenSecret(PreferenceManager.getDefaultSharedPreferences(mContext).getString(PREF_KEY_OAUTH_SECRET, ""));
                        Twitter twitter = new TwitterFactory(cb.build()).getInstance();
                        twitter.updateStatus(tweet);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
    }
}

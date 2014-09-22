package com.google.samples.apps.iosched.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.samples.apps.iosched.TwitterAPI;

import twitter4j.auth.AccessToken;

public class TwitterReceiver extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receiveOAuth();
        goBack();
    }

    public void receiveOAuth() {
        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(TwitterAPI.TWITTER_CALLBACK_URL)) {
            String verifier = uri.getQueryParameter("oauth_verifier");
            try {
                AccessToken accessToken = TwitterAPI.mTwitter.getOAuthAccessToken(TwitterAPI.mToken, verifier);
                SharedPreferences.Editor mEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                mEditor.putString(TwitterAPI.PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
                mEditor.putString(TwitterAPI.PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
                mEditor.putBoolean(TwitterAPI.PREF_KEY_OAUTH_DONE, true);
                mEditor.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void goBack() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(TwitterAPI.PREF_KEY_OAUTH_DONE, false)) {
            Toast toast = Toast.makeText(this, "Successful login to Twitter", Toast.LENGTH_LONG);
            toast.show();
        } else {
            Toast toast = Toast.makeText(this, "We couldn't login to Twitter", Toast.LENGTH_LONG);
            toast.show();
        }
        Intent intent = new Intent(this, AllScheduleActivity.class);
        startActivity(intent);
        finish();
    }
}

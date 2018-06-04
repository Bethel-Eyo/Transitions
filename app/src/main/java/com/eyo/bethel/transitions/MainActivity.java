package com.eyo.bethel.transitions;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;


import com.eyo.bethel.transitions.Logs.LogFragment;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import android.support.v7.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Basic application that recognises activity transitions

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    // Intents action that will be fired when transitions are triggered
    private final String TRANSITION_ACTION_RECEIVER =
            BuildConfig.APPLICATION_ID + "TRANSITION_ACTION_RECEIVER";
    private LogFragment mLogFragment;
    private PendingIntent mPendingIntent;
    private myTransitionReceiver mTransitionsReceiver;

    private static String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.WALKING:
                return "WALKING";
            default:
                return "UNKNOWN";
        }
    }

    private static String toTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "ENTER";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "EXIT";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = new Intent(TRANSITION_ACTION_RECEIVER);
        mPendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        mTransitionsReceiver = new myTransitionReceiver();
        registerReceiver(mTransitionsReceiver, new IntentFilter(TRANSITION_ACTION_RECEIVER));

        mLogFragment = (LogFragment) getSupportFragmentManager().findFragmentById(R.id.log_fragment);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpTransitions();
    }

    @Override
    protected void onPause() {
        // Unregister the transitions:
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mPendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Transitions successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Transitions could not be unregistered: " + e);
                    }
                });

        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mTransitionsReceiver != null) {
            unregisterReceiver(mTransitionsReceiver);
            mTransitionsReceiver = null;
        }
        super.onStop();
    }

    private void setUpTransitions(){
        List<ActivityTransition> transitions = new ArrayList<>();

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        // Register for Transitions Updates.
        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, mPendingIntent);
        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "Transitions Api was successfully registered.");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Transitions Api could not be registered: " + e);
                    }
                });
    }

    public class myTransitionReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.equals(TRANSITION_ACTION_RECEIVER, intent.getAction())){
                mLogFragment.getLogView()
                        .echo("Unsupported action received in myTransitionReceiver class: action="
                        + intent.getAction());
                return;
            }

            if (ActivityTransitionResult.hasResult(intent)){
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                for (ActivityTransitionEvent event : result.getTransitionEvents()){
                    String theActivity = toActivityString(event.getActivityType());
                    String transType = toTransitionType(event.getTransitionType());
                    mLogFragment.getLogView()
                            .echo("Transition: "
                                    + theActivity + " (" + transType + ")" + "   "
                                    + new SimpleDateFormat("HH:mm:ss", Locale.UK)
                                    .format(new Date()));
                }
            }
        }
    }
}

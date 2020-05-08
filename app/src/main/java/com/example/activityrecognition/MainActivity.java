package com.example.activityrecognition;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    private GoogleApiClient mGoogleApiClient;
    private TextView mDetectedActivityTextView;
    private ActivityDetectionBroadcastReceiver mBroadcastReceiver;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpannableString s = new SpannableString("Google Activity Recognition");
        Typeface myTypeface = Typeface.create(ResourcesCompat.getFont(this, R.font.nunito_extralight), Typeface.BOLD);
        s.setSpan(new TypefaceSpan(myTypeface), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(0xFF000000), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        setTitle(s);

        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();
        mDetectedActivityTextView = (TextView) findViewById(R.id.detected_activities_textview);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();
    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();//just before the activity is visible to the user
    }

    //activity is in the foreground and the user can interact with it
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter("com.example.activityrecognition.OUR_ACTION"));
    }

    //activity is completely hidden
    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public String getDetectedActivity(int detectedActivityType) {
        Resources resources = this.getResources();
        switch(detectedActivityType) {
            case DetectedActivity.TILTING:
                return resources.getString(R.string.activity_tilting);
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.activity_on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.activity_on_foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.activity_running);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.activity_walking);
            case DetectedActivity.STILL:
                return resources.getString(R.string.activity_still);
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.activity_in_vehicle);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.activity_unknown);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }

    //activity is partially obscured by another activity
    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected.");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended!");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }


    public void requestUpdatesHandler(View view) {
            if (!mGoogleApiClient.isConnected()) {
                Toast.makeText(this, "GoogleAPIClient not connected.", Toast.LENGTH_SHORT).show();
            } else {
                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 0, getActivityDetectionPendingIntent()).setResultCallback(this);
            }
    }

    public void removeActivityUpdates(View view) {
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, getActivityDetectionPendingIntent()).setResultCallback(this);
        mDetectedActivityTextView.setText("");
    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent mIntent = new Intent(this, ActivityDetectionClass.class);
        //The flag indicates that if the described PendingIntent already exists, then keep it but replace its extra data with what is in this new Intent.
        return PendingIntent.getService(this, 0, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void onResult(Status statusOfActivity) {
        if (statusOfActivity.isSuccess()) {
            Log.e(TAG, "Successful!");
        } else {
            Log.e(TAG, "Error: " + statusOfActivity.getStatusMessage());
        }
    }


    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> detectedActivityArrayList = intent.getParcelableArrayListExtra("ActivityNameArrayList");
            String activityString = "";
            for(DetectedActivity activity: detectedActivityArrayList){
                activityString +=  "Detected Activity: " + getDetectedActivity(activity.getType()) + " | Confidence: " + activity.getConfidence() + "%\n";
            }
            mDetectedActivityTextView.setText(activityString);
        }
    }
}
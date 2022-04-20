package com.example.mapalarmclock;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.Timer;


public class CheckService extends Service {

    private String CHANNEL_ID = "10";
    private int notificationId = 10;
    private int GPSUpdateTime = 60;
    private LatLng selected;
    private boolean isSetting = false;
    private double distanceSetting = 0.0f;
    private int countdownTimeSetting = 0;
    private double now_distance = 0.0f;
    private int now_countdown = 0;
    private String state = "Countdown";

    NotificationManager notificationManager;
    Handler handler = new Handler();

    private void reset(){
        CHANNEL_ID = "10";
        notificationId = 10;
        GPSUpdateTime = 60;
        distanceSetting = 0.0f;
        countdownTimeSetting = 0;
        now_distance = 0.0f;
        now_countdown = 0;
        state = "Countdown";
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Create an explicit intent for an Activity in your app
            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("isSetting", false);
            intent.putExtra("notificationId", CHANNEL_ID);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);

            switch (state) {
                // Notification countDown
                case "Countdown":
                    String notiCountdownText = now_countdown + "s";
                    builder.setSmallIcon(R.drawable.common_google_signin_btn_text_light)
                            .setContentTitle("偵測倒數通知")
                            .setContentText(notiCountdownText)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            // Set the intent that will fire when the user taps the notification
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .addAction(R.drawable.quantum_ic_clear_grey600_24, "Cancel", pendingIntent);
                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(notificationId, builder.build());
                    handler.postDelayed(this, 1000);
                    // Countdown Time is Over, Change to detect location
                    if (now_countdown <= 0) {
                        state = "Detection";
                    }
                    break;
                // Detection Countdown
                case "DetectionCountdown":
                    builder.addAction(R.drawable.quantum_ic_clear_grey600_24, "Cancel", pendingIntent);
                    CountDownTimer timer =  new CountDownTimer(GPSUpdateTime*1000, 10000) {
                        public void onTick(long millisUntilFinished) {
                            builder.setSmallIcon(R.drawable.common_google_signin_btn_text_light)
                                    .setContentTitle("監控中")
                                    .setContentText(String.format("距離 %s 公尺，下次更新 %d 秒", Math.round(now_distance*1.0)/1.0, (int)(millisUntilFinished/1000)))
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                    // Set the intent that will fire when the user taps the notification
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true);
                            // notificationId is a unique int for each notification that you must define
                            notificationManager.notify(notificationId, builder.build());
                            now_countdown = (int)(millisUntilFinished/1000);
                        }
                        public void onFinish() {
                            state = "Detection";
                            handler.postDelayed(runnable, 0);
                        }
                    };
                    timer.start();
                    break;
                // Detection
                case "Detection":
                    LocationManager locMan = (LocationManager) getSystemService(LOCATION_SERVICE);

                    Location GPSLocation = null;
                    if (locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        else{
                            // TODO: get location permission failed
                        }
                        GPSLocation = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        LatLng nowLocation = new LatLng(GPSLocation.getLatitude(), GPSLocation.getLongitude());
                        now_distance = SphericalUtil.computeDistanceBetween(selected, nowLocation);
                    }
                    else{
                        // TODO: get Location Failed
                    }

                    // Notification distance
                    if (now_distance <= distanceSetting){
                        builder.setSmallIcon(R.drawable.common_google_signin_btn_text_light)
                                .setContentTitle("抵達")
                                .setContentText("您已抵達該地點")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setVibrate(new long[]{1000, 1000})
                                // Set the intent that will fire when the user taps the notification
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true);
                    }
                    else{
                        builder.setSmallIcon(R.drawable.common_google_signin_btn_text_light)
                                .setContentTitle("監控中")
                                .setContentText(String.format("距離 %s 公尺", Math.round(now_distance*1.0)/1.0))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                // Set the intent that will fire when the user taps the notification
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true)
                                .addAction(R.drawable.quantum_ic_clear_grey600_24, "Cancel", pendingIntent);
                        state = "DetectionCountdown";
                    }

                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(notificationId, builder.build());
                    handler.postDelayed(this, 1000);
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // initial
        state = "Countdown";
        isSetting = true;
        handler.removeCallbacks(runnable);

        // get value from MapsActivity
        double tmp_Lat = intent.getDoubleExtra("selectedLat", 0.0f);
        double tmp_Lng = intent.getDoubleExtra("selectedLng", 0.0f);
        selected = new LatLng(tmp_Lat, tmp_Lng);
        distanceSetting = intent.getDoubleExtra("distance", 0.0f);
        countdownTimeSetting = intent.getIntExtra("countdownTime", 0);

        // timer start
        CountDownTimer timer =  new CountDownTimer(countdownTimeSetting*1000, 1000) {
            public void onTick(long millisUntilFinished) {
                now_countdown = (int)(millisUntilFinished/1000);
                Log.d("Timer", "seconds remaining: " + millisUntilFinished / 1000);
            }
            public void onFinish() {
                Log.d("Timer", "Done!");
            }
        };
        timer.start();
        handler.postDelayed(runnable,1000);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate(){
        super.onCreate();

        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channelCheckService = new NotificationChannel("checkService", "CheckService", NotificationManager.IMPORTANCE_DEFAULT);
            channelCheckService.enableVibration(true);
            channelCheckService.setVibrationPattern(new long[]{0});
            channelCheckService.enableLights(false);
            channelCheckService.setSound(null, null);
            notificationManager.createNotificationChannel(channelCheckService);
            NotificationChannel channelCanAdd = new NotificationChannel("canAdd", "CanAdd", NotificationManager.IMPORTANCE_DEFAULT);
            channelCanAdd.setDescription("通知");
            channelCanAdd.enableVibration(true);
            channelCanAdd.setVibrationPattern(new long[]{0, 1000, 100, 1000, 100, 1000, 100, 1000, 100, 1000, 100});
            channelCanAdd.enableLights(true);
            channelCanAdd.setLightColor(Color.RED);
            notificationManager.createNotificationChannel(channelCanAdd);
        }

        Notification noti = new NotificationCompat.Builder(getApplicationContext(), "checkService")
                .setContentTitle("服務開始")
                .setContentText(String.format("歡迎使用本服務"))
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .build();

        startForeground(notificationId, noti);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(runnable);
        stopForeground(true);
        reset();
        super.onDestroy();
    }

}

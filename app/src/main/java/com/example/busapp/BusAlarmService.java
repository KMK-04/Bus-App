package com.example.busapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class BusAlarmService extends Service {
    private Handler handler;
    private Runnable checkRunnable;
    private String routeId, stationId,name,routeName;
    private int alarmMinutes;
    private NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "bus_alarm_channel";
    private static boolean playSound;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        routeId = intent.getStringExtra("routeId");
        playSound= intent.getBooleanExtra("playSound", false);
        stationId = intent.getStringExtra("stationId");
        routeName = intent.getStringExtra("routeName");
        name = intent.getStringExtra("Name");
        alarmMinutes = intent.getIntExtra("alarmMinutes", 5);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        startForeground(NOTIFICATION_ID, createNotification("정보 조회 중..."));

        handler = new Handler();
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                new BusArrivalCheckTask().execute();
                handler.postDelayed(this, 30 * 1000); // 30초마다 체크
            }
        };
        handler.post(checkRunnable);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (handler != null && checkRunnable != null) handler.removeCallbacks(checkRunnable);
        super.onDestroy();
    }

    // 알림 채널 생성
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "버스 알람", NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    // 상단바 알림 생성
    private Notification createNotification(String content) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(name+"\n"+routeName)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .build();
    }

    // 알림 내용 업데이트
    private void updateNotification(String newContent) {
        Notification notification = createNotification(newContent);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    // AsyncTask로 도착정보 조회
    private class BusArrivalCheckTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            int predictTime1 = -1;
            try {
                String urlStr = "https://apis.data.go.kr/6410000/busarrivalservice/v2/getBusArrivalItemv2" +
                        "?serviceKey=" +
                        "&stationId=" + URLEncoder.encode(stationId, "UTF-8") +
                        "&routeId=" + URLEncoder.encode(routeId, "UTF-8") +
                        "&staOrder=1" +
                        "&format=xml";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                    parser.setInput(new InputStreamReader(conn.getInputStream()));
                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && "predictTime1".equals(parser.getName())) {
                            String pt1 = parser.nextText();
                            if (pt1 != null && !pt1.isEmpty())
                                predictTime1 = Integer.parseInt(pt1.trim());
                            break;
                        }
                        eventType = parser.next();
                    }
                }
            } catch (Exception e) { }
            return predictTime1;
        }

        @Override
        protected void onPostExecute(Integer predictTime1) {
            if (predictTime1 > 0) {
                String content = "도착 예정: " + predictTime1 + "분 전";
                updateNotification(content);

                if (predictTime1 <= alarmMinutes) {
                    showAlarmNotification(predictTime1);
                    stopSelf();
                }
            } else {
                updateNotification("도착 정보 없음");
            }
        }
    }

    // 지정 시간 이하 도달 시 푸시 알림
    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

    private void showAlarmNotification(int minutes) {
        // 소리 재생 옵션
        if (playSound) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("버스 도착 알림")
                    .setContentText(minutes + "분 이내에 버스가 도착합니다!")
                    .setSmallIcon(R.drawable.ic_alarm)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            notificationManager.notify(1002, builder.build());

            File audioFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "m.mp3");

            // 알람음 재생 (MediaPlayer)
            if (audioFile.exists()) {
                try {
                    MediaPlayer player = new MediaPlayer();
                    FileInputStream fis = new FileInputStream(audioFile);
                    player.setDataSource(fis.getFD());
                    player.prepare();
                    player.start();
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    // 오류 발생 시 기본 알림음 재생
                    MediaPlayer.create(this, R.raw.my_alarm).start();
                }
            } else {
                MediaPlayer.create(this, R.raw.my_alarm).start();
            }
        }else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("버스 도착 알림")
                    .setContentText(minutes + "분 이내에 버스가 도착합니다!")
                    .setSmallIcon(R.drawable.ic_alarm)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            notificationManager.notify(1002, builder.build());

        }
    }
    @Override
    public IBinder onBind(Intent intent) { return null; }
}

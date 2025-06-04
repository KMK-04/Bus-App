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

public class busalarm2Service extends Service {
    private Handler handler;
    private Runnable checkRunnable;
    private String routeId, stationId, name, routeName, busname;
    private int alarmMinutes;
    private NotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "bus_alarm_channel";
    private static boolean playSound;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        routeId = intent.getStringExtra("routeId");
        playSound = intent.getBooleanExtra("playSound", false);
        stationId = intent.getStringExtra("stationId");
        routeName = intent.getStringExtra("routeName");
        name = intent.getStringExtra("Name");
        alarmMinutes = intent.getIntExtra("alarmMinutes", 5);
        busname = intent.getStringExtra("busname"); // 추가

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
                .setContentTitle(name + "\n" + routeName)
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

    // 차량번호 4자리만 표시
    private String getLastFourDigits(String plateNo) {
        return (plateNo != null && plateNo.length() > 4)
                ? plateNo.substring(plateNo.length() - 4)
                : plateNo;
    }

    // 도착정보 클래스
    public static class BusArrivalInfo {
        public int predictTime1, predictTime2;
        public String plateNo1, plateNo2;
        public BusArrivalInfo(int predictTime1, int predictTime2, String plateNo1, String plateNo2) {
            this.predictTime1 = predictTime1;
            this.predictTime2 = predictTime2;
            this.plateNo1 = plateNo1;
            this.plateNo2 = plateNo2;
        }
    }

    // AsyncTask로 도착정보 조회 (plateNo1, plateNo2 포함)
    private class BusArrivalCheckTask extends AsyncTask<Void, Void, BusArrivalInfo> {
        @Override
        protected BusArrivalInfo doInBackground(Void... voids) {
            int predictTime1 = -1, predictTime2 = -1;
            String plateNo1 = "", plateNo2 = "";
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
                    String tag = "";
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            tag = parser.getName();
                        } else if (eventType == XmlPullParser.TEXT) {
                            switch (tag) {
                                case "predictTime1":
                                    String pt1 = parser.getText();
                                    if (pt1 != null && !pt1.isEmpty())
                                        predictTime1 = Integer.parseInt(pt1.trim());
                                    break;
                                case "predictTime2":
                                    String pt2 = parser.getText();
                                    if (pt2 != null && !pt2.isEmpty())
                                        predictTime2 = Integer.parseInt(pt2.trim());
                                    break;
                                case "plateNo1":
                                    plateNo1 = parser.getText() != null ? parser.getText().trim() : "";
                                    break;
                                case "plateNo2":
                                    plateNo2 = parser.getText() != null ? parser.getText().trim() : "";
                                    break;
                            }
                        }
                        eventType = parser.next();
                    }
                }
            } catch (Exception e) {
            }
            return new BusArrivalInfo(predictTime1, predictTime2, plateNo1, plateNo2);
        }

        @Override
        protected void onPostExecute(BusArrivalInfo info) {
            String plateInfo = "";
            if (!info.plateNo1.isEmpty() && !info.plateNo2.isEmpty()) {
                plateInfo = "버스번호: " + getLastFourDigits(info.plateNo1) + ", " + getLastFourDigits(info.plateNo2);
            } else if (!info.plateNo1.isEmpty()) {
                plateInfo = "버스번호: " + getLastFourDigits(info.plateNo1);
            } else if (!info.plateNo2.isEmpty()) {
                plateInfo = "버스번호: " + getLastFourDigits(info.plateNo2);
            }

            // busname과 plateNo1, plateNo2 비교 (마지막 4자리 기준)
            boolean isAlarm = false;
            int alarmTime = -1;
            if (busname != null) {
                String busname4 = getLastFourDigits(busname);
                if (busname4.equals(getLastFourDigits(info.plateNo1)) && info.predictTime1 > 0 && info.predictTime1 <= alarmMinutes) {
                    isAlarm = true;
                    alarmTime = info.predictTime1;
                } else if (busname4.equals(getLastFourDigits(info.plateNo2)) && info.predictTime2 > 0 && info.predictTime2 <= alarmMinutes) {
                    isAlarm = true;
                    alarmTime = info.predictTime2;
                }
            }

            if (isAlarm) {
                String content = "도착 예정: " + alarmTime + "분 전\n" + plateInfo;
                updateNotification(content);
                showAlarmNotification(alarmTime, plateInfo);
                stopSelf();
            } else {
                // 도착 시간 정보 구성
                StringBuilder timeInfo = new StringBuilder();
                if (info.predictTime1 > 0 || info.predictTime2 > 0) {
                    timeInfo.append("도착 예정: ");
                    if (info.predictTime1 > 0) timeInfo.append(info.predictTime1).append("분");
                    if (info.predictTime2 > 0) {
                        if (info.predictTime1 > 0) timeInfo.append(", ");
                        timeInfo.append(info.predictTime2).append("분");
                    }
                } else {
                    timeInfo.append("도착 정보 없음");
                }

                // 알림 내용 조합
                String content;
                if (busname != null) {
                    content = "대기 중 (" + busname + ")\n"
                            + timeInfo.toString();
                } else {
                    content = "대기 중\n"
                            + timeInfo.toString();
                }
                updateNotification(content);
            }
        }
    }

    // 지정 시간 이하 도달 시 푸시 알림 (버스번호 포함)
    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

    private void showAlarmNotification(int minutes, String plateInfo) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("버스 도착 알림")
                .setContentText(minutes + "분 이내에 버스가 도착합니다!\n" + plateInfo)
                .setSmallIcon(R.drawable.ic_alarm)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1002, builder.build());

        if (playSound) {
            // 내부 저장공간/Music/m.mp3 경로
            File audioFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "m.mp3");

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
        }
    }
    @Override
    public IBinder onBind(Intent intent) { return null; }
}

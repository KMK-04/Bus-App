package com.example.busapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
public class StationActivity extends AppCompatActivity {

    private TextView tvStationName, tvRouteName, tvDestination;
    private TextView tvBus1Time, tvBus1Location, tvBus1Number;
    private TextView tvBus2Time, tvBus2Location, tvBus2Number;
    private TextView tvNoBus;
    private LinearLayout layoutBus1, layoutBus2;
    private String stationId, routeId, routeName, stationName;
    private int staOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.station_activity);

        // 뷰 초기화
        tvStationName = findViewById(R.id.tv_station_name);
        tvRouteName = findViewById(R.id.tv_route_name);
        tvDestination = findViewById(R.id.tv_destination);

        tvBus1Time = findViewById(R.id.tv_bus1_time);
        tvBus1Location = findViewById(R.id.tv_bus1_location);
        tvBus1Number = findViewById(R.id.tv_bus1_number);

        tvBus2Time = findViewById(R.id.tv_bus2_time);
        tvBus2Location = findViewById(R.id.tv_bus2_location);
        tvBus2Number = findViewById(R.id.tv_bus2_number);

        tvNoBus = findViewById(R.id.tv_no_bus);
        layoutBus1 = findViewById(R.id.layout_bus1);
        layoutBus2 = findViewById(R.id.layout_bus2);
        Button btn3 = findViewById(R.id.btn3);
        Button btn4 = findViewById(R.id.btn4);
        // 인텐트 데이터 추출
        Intent intent = getIntent();
        stationId = intent.getStringExtra("stationId");
        routeId = intent.getStringExtra("routeId");
        routeName = intent.getStringExtra("routeName");
        stationName = intent.getStringExtra("stationName");
        staOrder = intent.getIntExtra("staOrder", 2);

        tvStationName.setText(stationName);
        tvRouteName.setText(routeName);

        new BusArrivalTask().execute();
        btn3.setOnClickListener(v -> {
            // 현재 액티비티 새로고침
            recreate();
        });
        btn4.setOnClickListener(v -> {
            // 커스텀 다이얼로그 레이아웃 생성
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 10);

            TextView message = new TextView(this);
            message.setText("버스: " + routeName + "\n정류장: " + stationName);
            layout.addView(message);

            EditText etName = new EditText(this);
            etName.setHint("즐겨찾기 이름 입력");
            layout.addView(etName);

            new AlertDialog.Builder(this)
                    .setTitle("즐겨찾기 등록")
                    .setView(layout)
                    .setPositiveButton("등록", (dialog, which) -> {
                        String customName = etName.getText().toString().trim();
                        if (customName.isEmpty()) {
                            customName = stationName; // 기본값으로 정류장명 사용
                        }
                        saveFavorite(routeId, routeName, stationName, stationId, customName);
                    })
                    .setNegativeButton("취소", null)
                    .show();
            // return true;  // <-- 이 줄을 삭제하세요!
        });
        layoutBus1.setOnLongClickListener(v -> {
            // 배경색 효과
            Drawable originalBackground = v.getBackground();
            v.setBackgroundColor(Color.parseColor("#CCCCCC"));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                v.setBackground(originalBackground);
            }, 300);

            // 다이얼로그로 시간 입력 받기
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_minutes, null);
            EditText editMinutes = dialogView.findViewById(R.id.editMinutes);

            new AlertDialog.Builder(this)
                    .setTitle("알람 시간 설정")
                    .setMessage("알람을 몇 분 전에 울릴까요?\n(입력하지 않으면 5분 전으로 설정됩니다)")
                    .setView(dialogView)
                    .setPositiveButton("확인", (dialog, which) -> {
                        int minutes = 5; // 기본값
                        String input = editMinutes.getText().toString().trim();
                        if (!input.isEmpty()) {
                            try {
                                int customMinutes = Integer.parseInt(input);
                                if (customMinutes > 0) {
                                    minutes = customMinutes;
                                }
                            } catch (NumberFormatException e) {
                                Toast.makeText(this, "숫자를 올바르게 입력하세요.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        String busname = tvBus1Number.getText().toString();
                        Intent intent2 = new Intent(StationActivity.this, busalarm2Service.class);
                        intent2.putExtra("routeId", routeId);
                        intent2.putExtra("alarmMinutes", minutes);
                        intent2.putExtra("routeName", routeName);
                        intent2.putExtra("stationId", stationId);
                        intent2.putExtra("Name", stationName);
                        intent2.putExtra("playSound", true);
                        intent2.putExtra("end", true);
                        intent2.putExtra("busname", busname);
                        startService(intent2);
                        Toast.makeText(this, minutes + "분 전 알람이 설정되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("취소", null)
                    .show();

            return true;
        });
        layoutBus2.setOnLongClickListener(v -> {
            // 배경색 효과
            Drawable originalBackground = v.getBackground();
            v.setBackgroundColor(Color.parseColor("#CCCCCC"));
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                v.setBackground(originalBackground);
            }, 200);

            // 다이얼로그로 시간 입력 받기
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_minutes, null);
            EditText editMinutes = dialogView.findViewById(R.id.editMinutes);

            new AlertDialog.Builder(this)
                    .setTitle("알람 시간 설정")
                    .setMessage("알람을 몇 분 전에 울릴까요?\n(입력하지 않으면 5분 전으로 설정됩니다)")
                    .setView(dialogView)
                    .setPositiveButton("확인", (dialog, which) -> {
                        int minutes = 5; // 기본값
                        String input = editMinutes.getText().toString().trim();
                        if (!input.isEmpty()) {
                            try {
                                int customMinutes = Integer.parseInt(input);
                                if (customMinutes > 0) {
                                    minutes = customMinutes;
                                }
                            } catch (NumberFormatException e) {
                                Toast.makeText(this, "숫자를 올바르게 입력하세요.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        String busname = tvBus2Number.getText().toString();
                        Intent intent2 = new Intent(StationActivity.this, busalarm2Service.class);
                        intent2.putExtra("routeId", routeId);
                        intent2.putExtra("alarmMinutes", minutes);
                        intent2.putExtra("routeName", routeName);
                        intent2.putExtra("stationId", stationId);
                        intent2.putExtra("Name", stationName);
                        intent2.putExtra("playSound", true);
                        intent2.putExtra("end", true);
                        intent2.putExtra("busname", busname);
                        startService(intent2);
                        Toast.makeText(this, minutes + "분 전 알람이 설정되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("취소", null)
                    .show();

            return true;
        });
    }
        private void saveFavorite(String routeId, String routeName, String stationName, String stationId, String customName) {
        SharedPreferences prefs = getSharedPreferences("favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = routeId + "_" + stationId;
        String value = routeId + "," + routeName + "," + stationName + "," + stationId + "," + customName;
        editor.putString(key, value);
        editor.apply();
        Toast.makeText(this, "즐겨찾기에 등록되었습니다!", Toast.LENGTH_SHORT).show();
    }
    private class BusArrivalTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String serviceKey = "";
                String apiUrl = "https://apis.data.go.kr/6410000/busarrivalservice/v2/getBusArrivalItemv2";

                String urlStr = apiUrl + "?serviceKey=" + serviceKey +
                        "&stationId=" + URLEncoder.encode(stationId, "UTF-8") +
                        "&routeId=" + URLEncoder.encode(routeId, "UTF-8") +
                        "&staOrder=" + staOrder +
                        "&format=xml";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    return response.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                parseXmlData(result);
            } else {
                showError("데이터를 불러오지 못했습니다");
            }
        }
    }

    private void parseXmlData(String xmlData) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlData));

            int eventType = parser.getEventType();
            String tag = "";
            boolean isItemTag = false;

            // 파싱 변수
            int predictTime1 = -1, predictTime2 = -1;
            int locationNo1 = -1, locationNo2 = -1;
            String plateNo1 = "", plateNo2 = "";
            String routeDestName = "";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if ("busArrivalItem".equals(tag)) {
                            isItemTag = true;
                        }
                        break;

                    case XmlPullParser.TEXT:
                        if (isItemTag) {
                            switch (tag) {
                                case "predictTime1":
                                    String pt1 = parser.getText();
                                    predictTime1 = (pt1 != null && !pt1.trim().isEmpty()) ? Integer.parseInt(pt1.trim()) : -1;
                                    break;
                                case "predictTime2":
                                    String pt2 = parser.getText();
                                    predictTime2 = (pt2 != null && !pt2.trim().isEmpty()) ? Integer.parseInt(pt2.trim()) : -1;
                                    break;
                                case "locationNo1":
                                    String ln1 = parser.getText();
                                    locationNo1 = (ln1 != null && !ln1.trim().isEmpty()) ? Integer.parseInt(ln1.trim()) : -1;
                                    break;
                                case "locationNo2":
                                    String ln2 = parser.getText();
                                    locationNo2 = (ln2 != null && !ln2.trim().isEmpty()) ? Integer.parseInt(ln2.trim()) : -1;
                                    break;
                                case "plateNo1":
                                    plateNo1 = parser.getText() != null ? parser.getText().trim() : "";
                                    break;
                                case "plateNo2":
                                    plateNo2 = parser.getText() != null ? parser.getText().trim() : "";
                                    break;
                                case "routeDestName":
                                    routeDestName = parser.getText() != null ? parser.getText().trim() : "";
                                    break;
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if ("busArrivalItem".equals(parser.getName())) {
                            isItemTag = false;
                        }
                        break;
                }
                eventType = parser.next();
            }

            // final 변수로 복사 (람다에서 사용)
            final int finalPredictTime1 = predictTime1;
            final int finalLocationNo1 = locationNo1;
            final String finalPlateNo1 = plateNo1;
            final int finalPredictTime2 = predictTime2;
            final int finalLocationNo2 = locationNo2;
            final String finalPlateNo2 = plateNo2;
            final String finalRouteDestName = routeDestName;

            runOnUiThread(() -> updateUI(
                    finalPredictTime1, finalLocationNo1, finalPlateNo1,
                    finalPredictTime2, finalLocationNo2, finalPlateNo2,
                    finalRouteDestName
            ));

        } catch (Exception e) {
            showError("데이터 파싱 오류");
        }
    }

    private void updateUI(int time1, int loc1, String plate1,
                          int time2, int loc2, String plate2,
                          String destName) {
        // 방면 정보
        tvDestination.setText(destName.isEmpty() ? "" : destName + " 방면");

        // 첫번째 버스
        if (time1 > 0 && !plate1.isEmpty()) {
            layoutBus1.setVisibility(View.VISIBLE);
            tvBus1Time.setText(time1 + "분");
            tvBus1Location.setText(loc1 > 0 ? loc1 + "번째 전" : "-");
            tvBus1Location.setVisibility(View.VISIBLE);
            tvBus1Number.setText(getLastFourDigits(plate1));
            tvBus1Number.setVisibility(View.VISIBLE);
        } else {
            layoutBus1.setVisibility(View.GONE);
        }

        // 두번째 버스
        if (time2 > 0 && !plate2.isEmpty()) {
            layoutBus2.setVisibility(View.VISIBLE);
            tvBus2Time.setText(time2 + "분");
            tvBus2Location.setText(loc2 > 0 ? loc2 + "번째 전" : "-");
            tvBus2Location.setVisibility(View.VISIBLE);
            tvBus2Number.setText(getLastFourDigits(plate2));
            tvBus2Number.setVisibility(View.VISIBLE);
        } else {
            layoutBus2.setVisibility(View.VISIBLE);
            tvBus2Time.setText("회차지 대기");
            tvBus2Location.setVisibility(View.GONE);
            tvBus2Number.setVisibility(View.GONE);
        }

        // 버스 없을 때
        if ((time1 <= 0 || plate1.isEmpty()) && (time2 <= 0 || plate2.isEmpty())) {
            tvNoBus.setVisibility(View.VISIBLE);
            layoutBus1.setVisibility(View.GONE);
            layoutBus2.setVisibility(View.GONE);
        } else {
            tvNoBus.setVisibility(View.GONE);
        }
    }

    private String getLastFourDigits(String plateNo) {
        return (plateNo != null && plateNo.length() > 4)
                ? plateNo.substring(plateNo.length() - 4)
                : plateNo;
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            tvNoBus.setVisibility(View.VISIBLE);
            layoutBus1.setVisibility(View.GONE);
            layoutBus2.setVisibility(View.GONE);
        });
    }
}

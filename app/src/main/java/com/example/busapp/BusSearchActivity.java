package com.example.busapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BusSearchActivity extends AppCompatActivity {
    private LinearLayout upStationListContainer, downStationListContainer;
    private TextView tvRouteName, tvRegionName, tvRouteId;
    private String routeId, routeName;
    private Set<String> busStations = new HashSet<>(); // 실시간 버스 위치 정류장ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_search);

        upStationListContainer = findViewById(R.id.up_station_list_container);
        downStationListContainer = findViewById(R.id.down_station_list_container);
        tvRouteName = findViewById(R.id.tv_route_name);
        tvRegionName = findViewById(R.id.tv_region_name);
        tvRouteId = findViewById(R.id.tv_route_id);
        Button btnFavorite = findViewById(R.id.button2); // 즐겨찾기 버튼
        Button btnRefresh = findViewById(R.id.button);   // 새로고침 버튼

        routeId = getIntent().getStringExtra("routeId");
        routeName = getIntent().getStringExtra("routeName");
        String regionName = getIntent().getStringExtra("regionName");

        tvRouteName.setText(routeName != null ? routeName : "노선명 정보 없음");
        tvRegionName.setText(regionName != null ? "지역: " + regionName : "지역 정보 없음");
        tvRouteId.setText(routeId != null ? "노선 ID: " + routeId : "노선 ID 정보 없음");

        if (routeId != null) {
            new FetchBusLocationsTask().execute(routeId);            // 1. 실시간 버스 위치 정보 먼저 요청
            new BusRouteStationListTask().execute(routeId);          // 2. 정류장 리스트 요청
        }
        btnFavorite.setOnClickListener(v -> {
            // 노선 정보(버스 ID, 이름 등) 저장
            saveFavorite(routeId, routeName, "NULL", "NULL","NULL");
        });
        btnRefresh.setOnClickListener(v -> {
            // 현재 액티비티 새로고침
            recreate();
        });
    }
    private void saveBusFavorite(String routeId, String routeName) {
        SharedPreferences prefs = getSharedPreferences("bus_favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 여러 개 저장하려면 JSON 배열 등으로 관리하는 것이 좋지만, 여기서는 간단히 routeId를 키로 사용
        editor.putString(routeId, routeName);
        editor.apply();

        Toast.makeText(this, "이 버스가 즐겨찾기에 등록되었습니다!", Toast.LENGTH_SHORT).show();
    }
    // 1. 실시간 버스 위치 정보 요청
    private class FetchBusLocationsTask extends AsyncTask<String, Void, Set<String>> {
        @Override
        protected Set<String> doInBackground(String... params) {
            String routeId = params[0];
            Set<String> busStations = new HashSet<>();
            try {
                String apiUrl = "https://apis.data.go.kr/6410000/buslocationservice/v2/getBusLocationListv2";
                String serviceKey = "";
                String urlStr = apiUrl + "?serviceKey=" + serviceKey +
                        "&routeId=" + URLEncoder.encode(routeId, "UTF-8") +
                        "&format=xml";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                    parser.setInput(new InputStreamReader(is));

                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG &&
                                "busLocationList".equals(parser.getName())) {

                            String stationId = null;
                            while (!(eventType == XmlPullParser.END_TAG &&
                                    "busLocationList".equals(parser.getName()))) {
                                if (eventType == XmlPullParser.START_TAG &&
                                        "stationId".equals(parser.getName())) {
                                    stationId = parser.nextText();
                                    busStations.add(stationId);
                                }
                                eventType = parser.next();
                            }
                        }
                        eventType = parser.next();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return busStations;
        }

        @Override
        protected void onPostExecute(Set<String> result) {
            busStations = result;
            // 정류장 리스트가 이미 그려졌다면 색상 업데이트
            updateStationColor(upStationListContainer, busStations);
            updateStationColor(downStationListContainer, busStations);
        }
    }

    // 2. 정류장 리스트 요청
    private class BusRouteStationListTask extends AsyncTask<String, Void, List<Station>> {
        @Override
        protected List<Station> doInBackground(String... params) {
            String routeId = params[0];
            String apiUrl = "https://apis.data.go.kr/6410000/busrouteservice/v2/getBusRouteStationListv2";
            StringBuilder response = new StringBuilder();
            List<Station> stationList = new ArrayList<>();

            try {
                String serviceKey = "";
                String urlStr = apiUrl + "?serviceKey=" + serviceKey +
                        "&routeId=" + URLEncoder.encode(routeId, "UTF-8") +
                        "&format=xml";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    reader.close();
                } else {
                    return stationList; // 빈 리스트 반환
                }
            } catch (Exception e) {
                return stationList;
            }

            // XML 파싱
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(new StringReader(response.toString()));

                int eventType = parser.getEventType();
                Station station = null;
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "busRouteStationList".equals(parser.getName())) {
                        station = new Station();
                    } else if (station != null && eventType == XmlPullParser.START_TAG) {
                        switch (parser.getName()) {
                            case "stationName":
                                station.name = parser.nextText();
                                break;
                            case "mobileNo":
                                station.number = parser.nextText();
                                break;
                            case "regionName":
                                station.regionName = parser.nextText();
                                break;
                            case "stationSeq":
                                try { station.seq = Integer.parseInt(parser.nextText()); } catch (Exception ignore) {}
                                break;
                            case "turnYn":
                                station.turnYn = parser.nextText();
                                break;
                            case "stationId":
                                station.stationId = parser.nextText();
                                break;
                        }
                    } else if (eventType == XmlPullParser.END_TAG && "busRouteStationList".equals(parser.getName())) {
                        stationList.add(station);
                        station = null;
                    }
                    eventType = parser.next();
                }
            } catch (Exception e) {
                // 파싱 오류 무시
            }
            return stationList;
        }

        @Override
        protected void onPostExecute(List<Station> stationList) {
            upStationListContainer.removeAllViews();
            downStationListContainer.removeAllViews();

            // 방향별 분리
            List<Station> upList = new ArrayList<>();
            List<Station> downList = new ArrayList<>();
            boolean isTurnPointFound = false;

            for (Station station : stationList) {
                if (!isTurnPointFound) {
                    upList.add(station);
                    if ("Y".equals(station.turnYn)) {
                        isTurnPointFound = true;
                    }
                } else {
                    downList.add(station);
                }
            }

            // 상행(기점→종점) 표시
            if (upList.isEmpty()) {
                addEmptyView(upStationListContainer, "정류장 정보 없음");
            } else {
                for (Station s : upList) {
                    addStationView(upStationListContainer, s, routeId, routeName);
                }
            }

            // 하행(종점→기점) 표시
            if (downList.isEmpty()) {
                addEmptyView(downStationListContainer, "정류장 정보 없음");
            } else {
                for (Station s : downList) {
                    addStationView(downStationListContainer, s, routeId, routeName);
                }
            }

            // 버스가 있는 정류장 색상 업데이트
            updateStationColor(upStationListContainer, busStations);
            updateStationColor(downStationListContainer, busStations);
        }
    }

    // 정류장 정보 표시용 뷰 추가 + 롱클릭(즐겨찾기)
    private void addStationView(LinearLayout container, Station station, String routeId, String routeName) {
        TextView tv = new TextView(this);
        String stationText = "[" + station.seq + "] " + station.name + " (" + station.number + ")";
        tv.setText(stationText);
        tv.setTag(station.stationId); // 정류장 ID를 tag에 저장
        tv.setTextSize(16);
        tv.setPadding(24, 16, 24, 16);
        tv.setBackgroundColor(Color.parseColor("#EEEEEE"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        tv.setLayoutParams(params);

        // regionName과 stationText 비교하여 색상 설정
        String regionNameFromIntent = getIntent().getStringExtra("regionName");
        if (regionNameFromIntent != null && regionNameFromIntent.equals(station.name + "(" + station.number + ")")) {
            tv.setBackgroundColor(Color.parseColor("#FFC107"));
        }
        // 롱클릭 리스너 추가
        tv.setOnLongClickListener(v -> {
            // 커스텀 다이얼로그 레이아웃 생성
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 10);

            TextView message = new TextView(this);
            message.setText("버스: " + routeName + "\n정류장: " + station.name);
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
                            customName = station.name; // 기본값으로 정류장명 사용
                        }
                        saveFavorite(routeId, routeName, station.name, station.stationId, customName);
                    })
                    .setNegativeButton("취소", null)
                    .show();
            return true;
        });

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BusSearchActivity.this, StationActivity.class);
                intent.putExtra("routeId", routeId);          // 노선 ID
                intent.putExtra("routeName", routeName);
                intent.putExtra("stationId", station.stationId);      // 노선 이름
                intent.putExtra("stationName", station.name);
                intent.putExtra("staOrder", "2");
                startActivity(intent);
            }
        });

        container.addView(tv);
    }
    private void addEmptyView(LinearLayout container, String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.GRAY);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(24, 24, 24, 24);
        container.addView(tv);
    }

    // 즐겨찾기 저장
    private void saveFavorite(String routeId, String routeName, String stationName, String stationId, String customName) {
        SharedPreferences prefs = getSharedPreferences("favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = routeId + "_" + stationId;
        String value = routeId + "," + routeName + "," + stationName + "," + stationId + "," + customName;
        editor.putString(key, value);
        editor.apply();
        Toast.makeText(this, "즐겨찾기에 등록되었습니다!", Toast.LENGTH_SHORT).show();
    }


    // 실시간 버스가 있는 정류장 파란색 표시
    private void updateStationColor(LinearLayout container, Set<String> busStations) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View view = container.getChildAt(i);
            if (view instanceof TextView) {
                String stationId = (String) view.getTag();
                if (busStations != null && busStations.contains(stationId)) {
                    ((TextView) view).setTextColor(Color.BLUE);
                } else {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
            }
        }
    }

    // Station 데이터 클래스
    static class Station {
        String name;
        String number;
        String regionName;
        int seq;
        String turnYn;
        String stationId;
    }
}

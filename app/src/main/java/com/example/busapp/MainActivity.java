package com.example.busapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private LinearLayout searchBarContainer;
    private int selectedStationTarget = 0; // 1: 출발, 2: 도착
    private EditText etStation1, etStation2;
    private HorizontalScrollView tagScroll;
    private EditText etSearch;
    private LinearLayout tagContainer;
    private FrameLayout frameContent;
    private Button btnSearch,btn5,setting;

    private TextView tvFavorite, tvBus, tvStation;
    private LinearLayout navFavorite, navBus, navStation,layer3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn5 = findViewById(R.id.button5);
        setting = findViewById(R.id.setting);
        btn5.setVisibility(View.GONE);
        searchBarContainer = findViewById(R.id.search_bar_container);
        tagScroll = findViewById(R.id.tag_scroll);
        tagContainer = findViewById(R.id.tag_container);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        frameContent = findViewById(R.id.frame_content);

        etStation1 = findViewById(R.id.et_station1);
        etStation2 = findViewById(R.id.et_station2);

        tvFavorite = findViewById(R.id.tv_favorite);
        tvBus = findViewById(R.id.tv_bus);
        tvStation = findViewById(R.id.tv_station);

        navFavorite = findViewById(R.id.nav_favorite);
        navBus = findViewById(R.id.nav_bus);
        navStation = findViewById(R.id.nav_station);

        // 처음엔 즐겨찾기 화면
        showFavorite();

        navFavorite.setOnClickListener(v -> showFavorite());
        navBus.setOnClickListener(v -> showBus());
        navStation.setOnClickListener(v -> showStation());
        selectedStationTarget = 1;
        etStation1.setOnClickListener(v -> {
            selectedStationTarget = 1;
            searchBarContainer.setVisibility(View.VISIBLE);
            etSearch.setText("");
            frameContent.removeAllViews();
        });

        etStation2.setOnClickListener(v -> {
            selectedStationTarget = 2;
            searchBarContainer.setVisibility(View.VISIBLE);
            etSearch.setText("");
            frameContent.removeAllViews();
        });
        setting.setOnClickListener(v -> {
            Intent intent3 = new Intent(MainActivity.this, setActivity.class);
            intent3.putExtra("alarmMinutes", 5);
            startActivity(intent3);
        });
    }
    private class StationSearchTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String keyword = params[0];
            String apiUrl = "https://apis.data.go.kr/6410000/busstationservice/v2/getBusStationListv2";
            String serviceKey = "";
            StringBuilder response = new StringBuilder();

            try {
                String urlStr = apiUrl + "?serviceKey=" + serviceKey +
                        "&keyword=" + URLEncoder.encode(keyword, "UTF-8") +
                        "&format=xml";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            frameContent.removeAllViews();

            ScrollView scrollView = new ScrollView(MainActivity.this);
            scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            LinearLayout resultContainer = new LinearLayout(MainActivity.this);
            resultContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            resultContainer.setOrientation(LinearLayout.VERTICAL);
            resultContainer.setPadding(16, 16, 16, 16);

            scrollView.addView(resultContainer);
            frameContent.addView(scrollView);

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(new StringReader(result));

                boolean hasResult = false;
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "busStationList".equals(parser.getName())) {
                        String stationId = "";
                        String stationName = "";
                        String regionName = "";
                        String mobileNo = "";

                        while (!(eventType == XmlPullParser.END_TAG && "busStationList".equals(parser.getName()))) {
                            if (eventType == XmlPullParser.START_TAG) {
                                switch (parser.getName()) {
                                    case "stationId":
                                        stationId = parser.nextText();
                                        break;
                                    case "stationName":
                                        stationName = parser.nextText();
                                        break;
                                    case "regionName":
                                        regionName = parser.nextText();
                                        break;
                                    case "mobileNo":
                                        mobileNo = parser.nextText();
                                        break;
                                }
                            }
                            eventType = parser.next();
                        }

                        // 결과 표시 (CardView 등 원하는 스타일로)
                        CardView cardView = new CardView(MainActivity.this);
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        cardParams.setMargins(0, 0, 0, 16);
                        cardView.setLayoutParams(cardParams);
                        cardView.setCardBackgroundColor(Color.WHITE);
                        cardView.setRadius(16);
                        cardView.setCardElevation(8);
                        cardView.setUseCompatPadding(true);

                        LinearLayout infoLayout = new LinearLayout(MainActivity.this);
                        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        infoLayout.setOrientation(LinearLayout.VERTICAL);
                        infoLayout.setPadding(24, 24, 24, 24);

                        TextView nameView = new TextView(MainActivity.this);
                        nameView.setText(stationName);
                        nameView.setTextSize(20);
                        nameView.setTypeface(null, Typeface.BOLD);
                        infoLayout.addView(nameView);

                        TextView detailView = new TextView(MainActivity.this);
                        detailView.setText("정류소ID: " + stationId + "\n지역: " + regionName + "\n정류소번호: " + mobileNo);
                        detailView.setTextSize(15);
                        infoLayout.addView(detailView);

                        final String stationName1=stationName;
                        final String stationId1=stationId;
                        final String mobileNO1=mobileNo;

                        cardView.setOnClickListener(view -> {
                            if (selectedStationTarget == 1) {
                                etStation1.setText(stationName1+"("+mobileNO1+")");
                                etStation1.setTag(stationId1); // 정류장ID 저장
                            } else if (selectedStationTarget == 2) {
                                etStation2.setText(stationName1);
                                etStation2.setTag(stationId1);
                            }
                            searchBarContainer.setVisibility(View.GONE);
                            frameContent.removeAllViews();
                        });

                        cardView.addView(infoLayout);
                        resultContainer.addView(cardView);

                        hasResult = true;
                    }
                    eventType = parser.next();
                }
                if (!hasResult) {
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText("검색 결과가 없습니다.");
                    tv.setTextSize(16);
                    tv.setTextColor(Color.parseColor("#D0D0D0"));
                    tv.setGravity(Gravity.CENTER);
                    resultContainer.addView(tv);
                }
            } catch (Exception e) {
                TextView errorView = new TextView(MainActivity.this);
                errorView.setText("파싱 오류: " + e.getMessage());
                errorView.setTextColor(Color.RED);
                scrollView.addView(errorView);
            }
        }
    }
    private class BusRouteTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String busNumber = params[0];
            String apiUrl = "https://apis.data.go.kr/6410000/busrouteservice/v2/getBusRouteListv2";
            StringBuilder response = new StringBuilder();

            try {
                String serviceKey = "";
                String urlStr = apiUrl + "?serviceKey=" + serviceKey +
                        "&keyword=" + URLEncoder.encode(busNumber, "UTF-8") +
                        "&format=xml";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    reader.close();
                } else {
                    return "서버 응답 오류: " + responseCode;
                }
            } catch (Exception e) {
                return "연결 실패: " + e.getMessage();
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            frameContent.removeAllViews();

            // 스크롤 뷰 추가
            ScrollView scrollView = new ScrollView(MainActivity.this);
            scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            // 결과를 담을 LinearLayout
            LinearLayout resultContainer = new LinearLayout(MainActivity.this);
            resultContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            resultContainer.setOrientation(LinearLayout.VERTICAL);
            resultContainer.setPadding(16, 16, 16, 16);

            scrollView.addView(resultContainer);
            frameContent.addView(scrollView);

            // XML 파싱
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(new StringReader(result));

                boolean hasResult = false;
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && "busRouteList".equals(parser.getName())) {
                        String routeId = "";
                        String routeName = "";
                        String regionName = "";

                        while (!(eventType == XmlPullParser.END_TAG && "busRouteList".equals(parser.getName()))) {
                            if (eventType == XmlPullParser.START_TAG) {
                                switch (parser.getName()) {
                                    case "routeId":
                                        routeId = parser.nextText();
                                        break;
                                    case "routeName":
                                        routeName = parser.nextText();
                                        break;
                                    case "regionName":
                                        regionName = parser.nextText();
                                        break;
                                }
                            }
                            eventType = parser.next();
                        }

                        // CardView로 각 노선 정보 표시
                        CardView cardView = new CardView(MainActivity.this);
                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        cardParams.setMargins(0, 0, 0, 16); // 아래쪽 마진
                        cardView.setLayoutParams(cardParams);
                        cardView.setCardBackgroundColor(Color.WHITE);
                        cardView.setRadius(16); // 모서리 둥글게
                        cardView.setCardElevation(8); // 그림자 효과
                        cardView.setUseCompatPadding(true);

                        // 노선 정보를 담을 내부 레이아웃
                        LinearLayout infoLayout = new LinearLayout(MainActivity.this);
                        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        infoLayout.setOrientation(LinearLayout.VERTICAL);
                        infoLayout.setPadding(24, 24, 24, 24);

                        // 노선 이름 (굵게)
                        TextView routeNameView = new TextView(MainActivity.this);
                        routeNameView.setText(routeName);
                        routeNameView.setTextSize(20);
                        routeNameView.setTypeface(null, Typeface.BOLD);
                        routeNameView.setPadding(0, 0, 0, 8);
                        infoLayout.addView(routeNameView);

                        // 지역 이름
                        TextView regionNameView = new TextView(MainActivity.this);
                        regionNameView.setText("지역: " + regionName);
                        regionNameView.setTextSize(16);
                        infoLayout.addView(regionNameView);

                        // ID 정보는 태그로만 저장하고 화면에는 표시하지 않음
                        infoLayout.setTag(routeId);

                        final String finalRouteId = routeId;
                        final String finalRouteName = routeName;
                        final String finalregionName = regionName;

                        cardView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(MainActivity.this, BusSearchActivity.class);
                                intent.putExtra("routeId", finalRouteId);
                                intent.putExtra("routeName", finalRouteName);
                                intent.putExtra("regionName", finalregionName );
                                startActivity(intent);
                            }
                        });

                        cardView.addView(infoLayout);
                        resultContainer.addView(cardView);
                        hasResult = true;
                    }
                    eventType = parser.next();
                }

                if (!hasResult) {
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText("검색 결과가 없습니다.");
                    tv.setTextSize(16);
                    tv.setTextColor(Color.parseColor("#D0D0D0"));
                    tv.setGravity(Gravity.CENTER);
                    resultContainer.addView(tv);
                }
            } catch (Exception e) {
                TextView errorView = new TextView(MainActivity.this);
                errorView.setText("파싱 오류: " + e.getMessage());
                errorView.setTextColor(Color.RED);
                scrollView.addView(errorView);
            }
        }
    }
    private void showFavorite() {
        btn5.setVisibility(View.GONE);
        layer3=findViewById(R.id.layer3);
        layer3.setVisibility(View.GONE);
        Button btn4 = findViewById(R.id.button4);
        btn4.setVisibility(View.GONE);
        // 색상 변경
        tvFavorite.setTextColor(Color.parseColor("#0090F9"));
        tvBus.setTextColor(Color.parseColor("#B0B0B0"));
        tvStation.setTextColor(Color.parseColor("#B0B0B0"));

        // 검색바, 태그 숨김
        searchBarContainer.setVisibility(View.GONE);
        tagScroll.setVisibility(View.GONE);

        // 본문 내용
        frameContent.removeAllViews();

        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 32, 32, 32);
        scrollView.addView(container);

        // 1. 버스 즐겨찾기
        SharedPreferences busPrefs = getSharedPreferences("favorites", MODE_PRIVATE);
        Map<String, ?> busFavorites = busPrefs.getAll();

        TextView busTitle = new TextView(this);
        busTitle.setText("🚌 버스 즐겨찾기");
        busTitle.setTextSize(20);
        busTitle.setTypeface(null, Typeface.BOLD);
        busTitle.setPadding(0, 0, 0, 16);
        container.addView(busTitle);

        if (busFavorites.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("등록된 버스 즐겨찾기가 없습니다.");
            tv.setTextSize(16);
            tv.setTextColor(Color.GRAY);
            tv.setPadding(0, 0, 0, 24);
            container.addView(tv);
        } else {
            for (Object valueObj : busFavorites.values()) {
                String value = valueObj.toString(); // "routeId,routeName,stationId,stationName"
                String[] parts = value.split(",");
                String routeId = parts.length > 0 ? parts[0] : "";
                String routeName = parts.length > 1 ? parts[1] : "";
                String stationId = parts.length > 2 ? parts[3] : "";
                String stationName = parts.length > 3 ? parts[2] : "";
                String customName = parts.length > 4 ? parts[4] : ""; // 사용자 지정 이름

                CardView cardView = new CardView(this);
                cardView.setCardBackgroundColor(Color.parseColor("#FFF4E0"));
                cardView.setRadius(16);
                cardView.setCardElevation(6);
                cardView.setUseCompatPadding(true);
                LinearLayout infoLayout = new LinearLayout(this);
                infoLayout.setOrientation(LinearLayout.VERTICAL);
                infoLayout.setPadding(32, 32, 32, 32);

                if (!customName.isEmpty()) {
                    TextView tvCustomName = new TextView(this);
                    tvCustomName.setText(customName);
                    tvCustomName.setTextSize(20);
                    tvCustomName.setTypeface(null, Typeface.BOLD);
                    tvCustomName.setTextColor(Color.parseColor("#C3564A"));
                    tvCustomName.setPadding(0, 0, 0, 8);
                    infoLayout.addView(tvCustomName);
                }
                TextView tvRoute = new TextView(this);
                tvRoute.setText("버스: " + routeName);
                tvRoute.setTextSize(18);
                tvRoute.setTypeface(null, Typeface.BOLD);
                infoLayout.addView(tvRoute);

                TextView tvStation = new TextView(this);
                tvStation.setText("정류장: " + stationName);
                tvStation.setTextSize(15);
                infoLayout.addView(tvStation);

                // 도착정보 표시
                TextView tvArrival = new TextView(this);
                tvArrival.setTextSize(15);
                tvArrival.setTextColor(Color.parseColor("#1565C0"));
                infoLayout.addView(tvArrival);

                if (!routeId.isEmpty() && !stationId.isEmpty()) {
                    new BusArrivalTask(tvArrival, routeId, stationId).execute();
                }

                cardView.addView(infoLayout);
                container.addView(cardView);

                // 도착정보 조회
                if (!routeId.isEmpty() && !stationId.isEmpty()) {
                    new BusArrivalTask(tvArrival, routeId, stationId).execute();
                }

                // 클릭 시 상세화면 이동
                cardView.setOnClickListener(v -> {
                    if(!stationName.equals("NULL")) {
                        Intent intent = new Intent(MainActivity.this, StationActivity.class);
                        intent.putExtra("routeId", routeId);
                        intent.putExtra("routeName", routeName);
                        intent.putExtra("stationId", stationId);
                        intent.putExtra("stationName", stationName);
                        startActivity(intent);
                    }else {
                        Intent intent = new Intent(MainActivity.this, BusSearchActivity.class);
                        intent.putExtra("routeId", routeId);
                        intent.putExtra("routeName", routeName);
                        intent.putExtra("stationId", stationId);
                        intent.putExtra("stationName", stationName);
                        startActivity(intent);
                    }
                });
                cardView.setOnLongClickListener(v -> {
                    // 커스텀 다이얼로그 레이아웃 인플레이트
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_alarm_setting, null);

                    RadioGroup timeRadioGroup = dialogView.findViewById(R.id.timeRadioGroup);
                    CheckBox soundCheckbox = dialogView.findViewById(R.id.soundCheckbox);
                    EditText editMinutes = dialogView.findViewById(R.id.editMinutes);

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("알람 설정")
                            .setView(dialogView)
                            .setPositiveButton("확인", (dialog, which) -> {
                                int selectedId = timeRadioGroup.getCheckedRadioButtonId();
                                int minutes = -1;

                                // 1. 라디오 버튼이 선택된 경우
                                if (selectedId == R.id.radio5) minutes = 5;
                                else if (selectedId == R.id.radio10) minutes = 10;
                                else if (selectedId == R.id.radio30) minutes = 30;

                                // 2. EditText에 값이 입력된 경우 우선 적용
                                String editValue = editMinutes.getText().toString().trim();
                                if (!editValue.isEmpty()) {
                                    try {
                                        int customMinutes = Integer.parseInt(editValue);
                                        if (customMinutes > 0) {
                                            minutes = customMinutes;
                                        }
                                    } catch (NumberFormatException e) {
                                        Toast.makeText(MainActivity.this, "숫자를 올바르게 입력하세요.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }

                                if (minutes <= 0) {
                                    Toast.makeText(MainActivity.this, "알람 시간을 선택하거나 입력하세요.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                boolean playSound = soundCheckbox.isChecked();

                                // 서비스 시작
                                Intent intent = new Intent(MainActivity.this, BusAlarmService.class);
                                intent.putExtra("routeId", routeId);
                                intent.putExtra("alarmMinutes", minutes);
                                intent.putExtra("routeName", routeName);
                                intent.putExtra("stationId", stationId);
                                intent.putExtra("Name", customName);
                                intent.putExtra("playSound", playSound); // 소리 재생 여부 전달
                                startService(intent);

                                Toast.makeText(MainActivity.this,
                                        minutes + "분 이하 도착시 알람" + (playSound ? " (소리 포함)" : ""),
                                        Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("취소", null)
                            .show();
                    return true;
                });

            }
        }

        // 2. 정류장(경로) 즐겨찾기
        SharedPreferences stationPrefs = getSharedPreferences("station_favorites", MODE_PRIVATE);
        Map<String, ?> stationFavorites = stationPrefs.getAll();

        TextView stationTitle = new TextView(this);
        stationTitle.setText("🗺️ 경로(정류장) 즐겨찾기");
        stationTitle.setTextSize(20);
        stationTitle.setTypeface(null, Typeface.BOLD);
        stationTitle.setPadding(0, 32, 0, 16);
        container.addView(stationTitle);

        if (stationFavorites.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("등록된 정류장 경로 즐겨찾기가 없습니다.");
            tv.setTextSize(16);
            tv.setTextColor(Color.GRAY);
            tv.setPadding(0, 0, 0, 24);
            container.addView(tv);
        } else {
            for (Map.Entry<String, ?> entry : stationFavorites.entrySet()) {
                String key = entry.getKey(); // "startId_endId"
                String value = entry.getValue().toString(); // "startName,endName"
                String[] names = value.split(",");
                String startName = names.length > 0 ? names[0] : "";
                String endName = names.length > 1 ? names[1] : "";
                String[] ids = key.split("_");
                String startId = ids.length > 0 ? ids[0] : "";
                String endId = ids.length > 1 ? ids[1] : "";

                CardView cardView = new CardView(this);
                cardView.setCardBackgroundColor(Color.parseColor("#E0F7FA"));
                cardView.setRadius(16);
                cardView.setCardElevation(6);
                cardView.setUseCompatPadding(true);

                LinearLayout infoLayout = new LinearLayout(this);
                infoLayout.setOrientation(LinearLayout.VERTICAL);
                infoLayout.setPadding(32, 32, 32, 32);

                TextView tvPath = new TextView(this);
                tvPath.setText("출발: " + startName + "\n도착: " + endName);
                tvPath.setTextSize(18);
                tvPath.setTypeface(null, Typeface.BOLD);
                infoLayout.addView(tvPath);

                cardView.addView(infoLayout);
                container.addView(cardView);

                // 클릭 시 출발/도착 정류장에 값 적용 + 경유 버스 검색 실행
                cardView.setOnClickListener(v -> {
                    etStation1.setText(startName);
                    etStation1.setTag(startId);
                    etStation2.setText(endName);
                    etStation2.setTag(endId);
                    showStation();
                    // new CommonRoutesTask().execute(startId, endId); // 자동 검색 실행하고 싶으면 이 라인 사용
                });
            }
        }

        frameContent.removeAllViews();
        frameContent.addView(scrollView);
    }
    private void showBus() {
        btn5.setVisibility(View.GONE);
        layer3=findViewById(R.id.layer3);
        layer3.setVisibility(View.GONE);
        Button btn4 = findViewById(R.id.button4);
        btn4.setVisibility(View.GONE);
        // 색상 변경
        tvFavorite.setTextColor(Color.parseColor("#B0B0B0"));
        tvBus.setTextColor(Color.parseColor("#0090F9"));
        tvStation.setTextColor(Color.parseColor("#B0B0B0"));

        // 검색바, 태그 보임
        searchBarContainer.setVisibility(View.VISIBLE);
        tagScroll.setVisibility(View.VISIBLE);

        // 검색창 힌트 변경
        etSearch.setHint("버스 번호 검색");

        // 태그 예시
        tagContainer.removeAllViews();

        // 본문 내용
        frameContent.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("버스 검색 결과가 없습니다.");
        tv.setTextSize(16);
        tv.setTextColor(Color.parseColor("#D0D0D0"));
        tv.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        tv.setLayoutParams(lp);
        frameContent.addView(tv);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String busNumber = etSearch.getText().toString().trim();
                if (!busNumber.isEmpty()) {
                    new BusRouteTask().execute(busNumber);
                } else {
                    Toast.makeText(MainActivity.this, "버스 번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showStation() {
        btn5.setVisibility(View.GONE);
        Button btn4 = findViewById(R.id.button4);
        btn4.setVisibility(View.VISIBLE);
        Button btn3 = findViewById(R.id.button3);
        btn3.setOnClickListener(v -> {
            String startStationId = (String) etStation1.getTag();
            String endStationId = (String) etStation2.getTag();

            if (startStationId == null || startStationId.isEmpty()) {
                Toast.makeText(this, "출발 정류장을 선택하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (endStationId == null || endStationId.isEmpty()) {
                // 도착 정류장 미선택: 출발 정류장 경유 노선만 조회
                new RouteViaStationTask(startStationId).execute();
            } else {
                // 두 정류장 모두 선택: 공통 노선 조회
                new CommonRoutesTask().execute(startStationId, endStationId);
            }
        });

        btn4.setOnClickListener(v -> {
            String startStationId = (String) etStation1.getTag();
            String startStationName = etStation1.getText().toString();
            String endStationId = (String) etStation2.getTag();
            String endStationName = etStation2.getText().toString();

            if (startStationId == null || startStationId.isEmpty()) {
                Toast.makeText(this, "출발 정류장을 선택하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (endStationId == null || endStationId.isEmpty()) {
                Toast.makeText(this, "도착 정류장을 선택하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 저장 키: "출발ID_도착ID", 값: "출발이름,도착이름"
            String key = startStationId + "_" + endStationId;
            String value = startStationName + "," + endStationName;

            SharedPreferences prefs = getSharedPreferences("station_favorites", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key, value);
            editor.apply();

            Toast.makeText(this, startStationId+startStationName, Toast.LENGTH_SHORT).show();
        });

        layer3=findViewById(R.id.layer3);
        layer3.setVisibility(View.VISIBLE);

        // 색상 변경
        tvFavorite.setTextColor(Color.parseColor("#B0B0B0"));
        tvBus.setTextColor(Color.parseColor("#B0B0B0"));
        tvStation.setTextColor(Color.parseColor("#0090F9"));

        // 검색바 보임, 태그 숨김
        searchBarContainer.setVisibility(View.GONE);
        tagScroll.setVisibility(View.GONE);

        // 검색창 힌트 변경
        etSearch.setHint("정류장 이름/번호 검색");

        // 본문 내용
        frameContent.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("정류장 검색 결과가 없습니다.");
        tv.setTextSize(16);
        tv.setTextColor(Color.parseColor("#D0D0D0"));
        tv.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        tv.setLayoutParams(lp);
        frameContent.addView(tv);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = etSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    new StationSearchTask().execute(keyword);
                } else {
                    Toast.makeText(MainActivity.this, "정류장 이름/번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private class CommonRoutesTask extends AsyncTask<String, Void, List<BusRoute>> {
        private String startId; // 출발 정류장 ID 저장용

        private List<BusRoute> fetchRoutesForStation(String stationId) {
            List<BusRoute> routes = new ArrayList<>();
            try {
                String apiUrl = "https://apis.data.go.kr/6410000/busstationservice/v2/getBusStationViaRouteListv2";
                String serviceKey = "";

                String urlStr = apiUrl + "?serviceKey=" + serviceKey +
                        "&stationId=" + URLEncoder.encode(stationId, "UTF-8") +
                        "&format=xml";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // XML 파싱 로직 (기존 RouteViaStationTask와 동일)
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(new StringReader(response.toString()));

                    int eventType = parser.getEventType();
                    BusRoute route = null;
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && "busRouteList".equals(parser.getName())) {
                            route = new BusRoute();
                        } else if (route != null && eventType == XmlPullParser.START_TAG) {
                            switch (parser.getName()) {
                                case "routeId": route.routeId = parser.nextText(); break;
                                case "routeName": route.routeName = parser.nextText(); break;
                                case "regionName": route.regionName = parser.nextText(); break;
                                case "routeTypeName": route.routeTypeName = parser.nextText(); break;
                                case "routeDestName": route.routeDestName = parser.nextText(); break;
                            }
                        } else if (eventType == XmlPullParser.END_TAG && "busRouteList".equals(parser.getName())) {
                            routes.add(route);
                            route = null;
                        }
                        eventType = parser.next();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }
        @Override
        protected List<BusRoute> doInBackground(String... params) {
            startId = params[0]; // 출발 정류장 ID 저장
            String endId = params[1];

            List<BusRoute> startRoutes = fetchRoutesForStation(startId);
            List<BusRoute> endRoutes = fetchRoutesForStation(endId);

            // 공통 노선 필터링 (기존 코드 동일)
            Set<String> startIds = new HashSet<>();
            for (BusRoute r : startRoutes) startIds.add(r.routeId);

            List<BusRoute> common = new ArrayList<>();
            for (BusRoute r : endRoutes) {
                if (startIds.contains(r.routeId)) {
                    common.add(r);
                }
            }
            return common;
        }

        @Override
        protected void onPostExecute(List<BusRoute> commonRoutes) {
            frameContent.removeAllViews();
            ScrollView scrollView = new ScrollView(MainActivity.this);
            LinearLayout resultContainer = new LinearLayout(MainActivity.this);
            resultContainer.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(resultContainer);

            if (commonRoutes.isEmpty()) {
                TextView tv = new TextView(MainActivity.this);
                tv.setText("두 정류장을 모두 경유하는 노선이 없습니다.");
                tv.setTextColor(Color.GRAY);
                tv.setPadding(24, 24, 24, 24);
                resultContainer.addView(tv);
            } else {
                for (BusRoute r : commonRoutes) {
                    CardView cardView = new CardView(MainActivity.this);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    cardParams.setMargins(0, 0, 0, 16);
                    cardView.setLayoutParams(cardParams);
                    cardView.setCardBackgroundColor(Color.WHITE);
                    cardView.setRadius(16);
                    cardView.setCardElevation(8);
                    cardView.setUseCompatPadding(true);

                    final String routeId = r.routeId;
                    final String routeName = r.routeName;
                    final String asdf=etStation1.getText().toString();
                    cardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(MainActivity.this, BusSearchActivity.class);
                            intent.putExtra("routeId", routeId);
                            intent.putExtra("routeName", routeName);
                            intent.putExtra("regionName", asdf);
                            startActivity(intent);
                        }
                    });
                    // 카드 내부

                    LinearLayout infoLayout = new LinearLayout(MainActivity.this);
                    infoLayout.setOrientation(LinearLayout.VERTICAL);
                    infoLayout.setPadding(24, 24, 24, 24);

                    // 노선명 및 유형
                    TextView routeNameView = new TextView(MainActivity.this);
                    routeNameView.setText(r.routeName);
                    routeNameView.setTextSize(18);
                    routeNameView.setTypeface(null, Typeface.BOLD);

                    // 색상 설정
                    switch(r.routeTypeName) {
                        case "직행좌석형시내버스":
                            routeNameView.setTextColor(Color.RED);
                            break;
                        case "일반형시내버스":
                            routeNameView.setTextColor(Color.BLUE);
                            break;
                        default:
                            routeNameView.setTextColor(Color.GREEN);
                    }
                    infoLayout.addView(routeNameView);

                    // 방면 정보
                    TextView destView = new TextView(MainActivity.this);
                    destView.setText("방면: " + r.routeDestName + "\n지역: " + r.regionName);
                    destView.setTextSize(15);
                    infoLayout.addView(destView);

                    // 도착 정보 표시용 TextView 추가
                    TextView arrivalView = new TextView(MainActivity.this);
                    arrivalView.setText("도착정보 불러오는 중...");
                    arrivalView.setTextSize(14);
                    arrivalView.setTextColor(Color.parseColor("#666666"));
                    infoLayout.addView(arrivalView);

                    // 도착정보 조회 (출발 정류장 기준)
                    new BusArrivalTask(arrivalView, r.routeId, startId).execute();

                    cardView.addView(infoLayout);
                    resultContainer.addView(cardView);
                }
            }
            frameContent.addView(scrollView);
        }

        // fetchRoutesForStation() 메서드는 기존 코드와 동일
    }

    private class RouteViaStationTask extends AsyncTask<String, Void, List<BusRoute>> {
        private final String stationId;
        public RouteViaStationTask(String stationId) {
            this.stationId = stationId;
        }

        @Override
        protected List<BusRoute> doInBackground(String... params) {
            List<BusRoute> routeList = new ArrayList<>();
            try {
                String apiUrl = "https://apis.data.go.kr/6410000/busstationservice/v2/getBusStationViaRouteListv2";
                String serviceKey = "";
                String urlStr = apiUrl + "?serviceKey=" + serviceKey +
                        "&stationId=" + URLEncoder.encode(stationId, "UTF-8") +
                        "&format=xml";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // XML 파싱
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(new StringReader(response.toString()));

                    int eventType = parser.getEventType();
                    BusRoute route = null;
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            String tag = parser.getName();
                            if ("busRouteList".equals(tag)) {
                                route = new BusRoute();
                            } else if (route != null) {
                                switch (tag) {
                                    case "routeId":
                                        route.routeId = parser.nextText();
                                        break;
                                    case "routeName":
                                        route.routeName = parser.nextText();
                                        break;
                                    case "routeTypeName":
                                        route.routeTypeName = parser.nextText();
                                        break;
                                    case "routeDestName":
                                        route.routeDestName = parser.nextText();
                                        break;
                                }
                            }
                        } else if (eventType == XmlPullParser.END_TAG && "busRouteList".equals(parser.getName())) {
                            routeList.add(route);
                            route = null;
                        }
                        eventType = parser.next();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routeList;
        }

        @Override
        protected void onPostExecute(List<BusRoute> routes) {
            frameContent.removeAllViews();
            ScrollView scrollView = new ScrollView(MainActivity.this);
            LinearLayout resultContainer = new LinearLayout(MainActivity.this);
            resultContainer.setOrientation(LinearLayout.VERTICAL);
            resultContainer.setPadding(16, 16, 16, 16);
            scrollView.addView(resultContainer);

            if (routes.isEmpty()) {
                TextView tv = new TextView(MainActivity.this);
                tv.setText("경유 노선이 없습니다");
                tv.setTextColor(Color.GRAY);
                tv.setGravity(Gravity.CENTER);
                resultContainer.addView(tv);
            } else {
                for (BusRoute r : routes) {
                    // 카드뷰 생성
                    CardView cardView = new CardView(MainActivity.this);
                    cardView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    cardView.setCardElevation(8);
                    cardView.setRadius(16);
                    cardView.setContentPadding(24, 24, 24, 24);
                    cardView.setCardBackgroundColor(Color.WHITE);

                    final String routeId = r.routeId;
                    final String routeName = r.routeName;
                    final String asdf=etStation1.getText().toString();
                    cardView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(MainActivity.this, BusSearchActivity.class);
                            intent.putExtra("routeId", routeId);
                            intent.putExtra("routeName", routeName);
                            intent.putExtra("regionName", asdf);
                            startActivity(intent);
                        }
                    });
                    // 카드 내부 레이아웃
                    LinearLayout layout = new LinearLayout(MainActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    // 노선 정보
                    TextView tvRoute = new TextView(MainActivity.this);
                    tvRoute.setText(r.routeName);
                    // 버스 유형에 따른 색상 설정
                    switch(r.routeTypeName) {
                        case "직행좌석형시내버스":
                            tvRoute.setTextColor(Color.RED);
                            break;
                        case "일반형시내버스":
                            tvRoute.setTextColor(Color.BLUE);
                            break;
                        default:
                            tvRoute.setTextColor(Color.GREEN);
                    }
                    tvRoute.setTextSize(18);
                    tvRoute.setTypeface(null, Typeface.BOLD);

                    // 방면 정보
                    TextView tvDest = new TextView(MainActivity.this);
                    tvDest.setText(r.routeDestName + " 방면");
                    tvDest.setTextColor(Color.parseColor("#666666"));
                    tvDest.setTextSize(14);

                    // 도착 정보 (BusArrivalTask로 업데이트)
                    TextView tvArrival = new TextView(MainActivity.this);
                    tvArrival.setText("도착정보 불러오는 중...");
                    tvArrival.setTextSize(14);
                    tvArrival.setTextColor(Color.parseColor("#0090F9"));

                    layout.addView(tvRoute);
                    layout.addView(tvDest);
                    layout.addView(tvArrival);
                    cardView.addView(layout);
                    resultContainer.addView(cardView);

                    // 도착정보 조회
                    new BusArrivalTask(tvArrival, r.routeId, stationId).execute();
                }
            }
            frameContent.addView(scrollView);
        }
    }

    private class BusArrivalTask extends AsyncTask<Void, Void, String> {
        private final TextView targetView;
        private final String routeId, stationId;

        public BusArrivalTask(TextView targetView, String routeId, String stationId) {
            this.targetView = targetView;
            this.routeId = routeId;
            this.stationId = stationId;
        }

        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder result = new StringBuilder();
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
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                    parser.setInput(new InputStreamReader(is));

                    int predictTime1 = -1;
                    int predictTime2 = -1;
                    int eventType = parser.getEventType();

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            switch (parser.getName()) {
                                case "predictTime1":
                                    String pt1 = parser.nextText();
                                    if (pt1 != null && !pt1.isEmpty()) {
                                        predictTime1 = Integer.parseInt(pt1.trim());
                                    }
                                    break;
                                case "predictTime2":
                                    String pt2 = parser.nextText();
                                    if (pt2 != null && !pt2.isEmpty()) {
                                        predictTime2 = Integer.parseInt(pt2.trim());
                                    }
                                    break;
                            }
                        }
                        eventType = parser.next();
                    }

                    if (predictTime1 > 0) {
                        result.append("첫번째: ").append(predictTime1).append("분 후 도착");
                    } else {
                        result.append("첫번째: 회차지 대기");
                    }

                    if (predictTime2 > 0) {
                        result.append("\n두번째: ").append(predictTime2).append("분 후 도착");
                    } else {
                        result.append("\n두번째: 회차지 대기");
                    }
                }
            } catch (Exception e) {
                result.append("도착정보 없음");
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            targetView.setText(result);
        }
    }
    // BusRoute 데이터 클래스
    static class BusRoute {
        String routeId;
        String routeName;
        String regionName;
        String routeTypeName;
        String routeDestName;
    }

    private void addTag(String text, boolean selected) {
        TextView tag = new TextView(this);
        tag.setText(text);
        tag.setTextSize(14);
        tag.setPadding(32, 8, 32, 8);
        tag.setBackgroundResource(selected ? android.R.color.holo_blue_light : android.R.color.darker_gray);
        tag.setTextColor(selected ? Color.parseColor("#00B4FF") : Color.parseColor("#B0B0B0"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 16, 8);
        tag.setLayoutParams(params);
        tagContainer.addView(tag);
    }
    private class FavoriteArrivalTask extends AsyncTask<String, Void, int[]> {
        private final TextView tvArrival1, tvArrival2;

        public FavoriteArrivalTask(TextView tvArrival1, TextView tvArrival2) {
            this.tvArrival1 = tvArrival1;
            this.tvArrival2 = tvArrival2;
        }

        @Override
        protected int[] doInBackground(String... params) {
            String routeId = params[0];
            String stationId = params[1];
            String staOrder = params[2];

            String apiUrl = "https://apis.data.go.kr/6410000/busarrivalservice/v2/getBusArrivalItemv2";
            String serviceKey = "";
            int predictTime1 = -1, predictTime2 = -1;

            try {
                String urlStr = apiUrl + "?serviceKey=" + serviceKey +
                        "&routeId=" + URLEncoder.encode(routeId, "UTF-8") +
                        "&stationId=" + URLEncoder.encode(stationId, "UTF-8") +
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

                    // XML 파싱
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(new StringReader(response.toString()));

                    int eventType = parser.getEventType();
                    String tag = "";
                    boolean isItemTag = false;

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            tag = parser.getName();
                            if ("busArrivalItem".equals(tag)) {
                                isItemTag = true;
                            }
                        } else if (isItemTag && eventType == XmlPullParser.TEXT) {
                            switch (tag) {
                                case "predictTime1":
                                    String pt1 = parser.getText();
                                    predictTime1 = (pt1 != null && !pt1.isEmpty()) ? Integer.parseInt(pt1) : -1;
                                    break;
                                case "predictTime2":
                                    String pt2 = parser.getText();
                                    predictTime2 = (pt2 != null && !pt2.isEmpty()) ? Integer.parseInt(pt2) : -1;
                                    break;
                            }
                        }
                        eventType = parser.next();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new int[]{predictTime1, predictTime2};
        }

        @Override
        protected void onPostExecute(int[] times) {
            int predictTime1 = times[0];
            int predictTime2 = times[1];

            if (predictTime1 > 0) {
                tvArrival1.setText("첫번째 버스: " + predictTime1 + "분 후 도착");
            } else {
                tvArrival1.setText("첫번째 버스: 회차지 대기");
            }

            if (predictTime2 > 0) {
                tvArrival2.setVisibility(View.VISIBLE);
                tvArrival2.setText("두번째 버스: " + predictTime2 + "분 후 도착");
            } else {
                tvArrival2.setVisibility(View.VISIBLE);
                tvArrival2.setText("두번째 버스: 회차지 대기");
            }

            if (predictTime1 <= 0 && predictTime2 <= 0) {
                tvArrival1.setText("도착 예정 버스가 없습니다");
                tvArrival2.setVisibility(View.GONE);
            }
        }
    }
}


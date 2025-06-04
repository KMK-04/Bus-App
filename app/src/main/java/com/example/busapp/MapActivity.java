package com.example.busapp;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MapActivity extends AppCompatActivity {

    private double latitude;
    private double longitude;

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> stationList = new ArrayList<>();
    private ArrayList<String> stationNameList = new ArrayList<>();

    private final String SERVICE_KEY = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        listView = findViewById(R.id.stationListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stationList);
        listView.setAdapter(adapter);

        Intent intent = getIntent();
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String stationName = stationNameList.get(position);
            // 공백을 URL 인코딩 (간단히 %20으로 대체)
            String encodedName = stationName.replace(" ", "%20");
            String url = "https://map.naver.com/p/bus/bus-station/" + encodedName + "?c=14.00,0,0,0,dh";

            Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent1);
        });

        new NearbyStationTask().execute();
    }

    private class NearbyStationTask extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            ArrayList<String> result = new ArrayList<>();
            stationNameList.clear();

            try {
                String urlStr = "https://apis.data.go.kr/6410000/busstationservice/v2/getBusStationAroundListv2" +
                        "?serviceKey=" + SERVICE_KEY +
                        "&x=" + longitude +
                        "&y=" + latitude +
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

                    int eventType = parser.getEventType();
                    String stationName = "", mobileNo = "", distance = "";

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            switch (parser.getName()) {
                                case "stationName":
                                    stationName = parser.nextText().trim();
                                    break;
                                case "mobileNo":
                                    mobileNo = parser.nextText().trim();
                                    break;
                                case "distance":
                                    distance = parser.nextText().trim();
                                    break;
                            }
                        } else if (eventType == XmlPullParser.END_TAG && parser.getName().equals("busStationAroundList")) {
                            result.add(stationName + " (" + mobileNo + ") - 거리: " + distance + "m");
                            stationNameList.add(stationName);
                            stationName = mobileNo = distance = "";
                        }
                        eventType = parser.next();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                result.add("정류소 정보를 불러오는 데 실패했습니다.");
            }

            return result;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            stationList.clear();
            stationList.addAll(result);
            adapter.notifyDataSetChanged();

            if (result.isEmpty()) {
                Toast.makeText(MapActivity.this, "근처 정류소가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

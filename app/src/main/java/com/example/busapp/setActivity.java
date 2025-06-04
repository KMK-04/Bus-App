package com.example.busapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Map;

public class setActivity extends AppCompatActivity {

    private LinearLayout favoritesContainer;
    private SharedPreferences favoritesPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set);

        Button btnMusic = findViewById(R.id.btn_music);
        Button btnw = findViewById(R.id.button6);
        btnMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // musicActivity 실행
                Intent intent = new Intent(setActivity.this, musicActivity.class);
                startActivity(intent);
            }
        });
        btnw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // FusedLocationProviderClient 초기화
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(setActivity.this);

                // 위치 권한 확인
                if (ActivityCompat.checkSelfPermission(setActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(setActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // 권한이 없는 경우 권한 요청
                    ActivityCompat.requestPermissions(setActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    Toast.makeText(setActivity.this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 마지막으로 알려진 위치 가져오기
                fusedLocationClient.getLastLocation().addOnSuccessListener(setActivity.this, location -> {
                    if (location != null) {
                        // 위치 정보가 있을 경우 위도와 경도 출력
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        Log.d("afd", "현재 위치: 위도 " + latitude + ", 경도 " + longitude);
                        Toast.makeText(setActivity.this, "현재 위치: 위도 " + latitude + ", 경도 " + longitude, Toast.LENGTH_LONG).show();

                        // MapActivity로 현재 위치 전달
                        Intent intent = new Intent(setActivity.this, MapActivity.class);
                        intent.putExtra("latitude", latitude);
                        intent.putExtra("longitude", longitude);
                        startActivity(intent);

                    } else {
                        // 위치 정보가 없는 경우
                        Toast.makeText(setActivity.this, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(setActivity.this, e -> {
                    // 위치 가져오기 실패 시
                    Toast.makeText(setActivity.this, "위치 가져오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        favoritesContainer = findViewById(R.id.favorites_container);
        favoritesPrefs = getSharedPreferences("favorites", MODE_PRIVATE);

        loadFavorites();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "위치 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void loadFavorites() {
        // 기존에 추가된 뷰 모두 제거 (중복 방지)
        favoritesContainer.removeAllViews();

        // SharedPreferences에서 모든 즐겨찾기 데이터 가져오기
        Map<String, ?> allFavorites = favoritesPrefs.getAll();

        if (allFavorites.isEmpty()) {
            // 즐겨찾기가 없으면 안내 메시지 표시
            TextView tv = new TextView(this);
            tv.setText("등록된 즐겨찾기가 없습니다.");
            tv.setTextSize(16);
            tv.setTextColor(Color.GRAY);
            favoritesContainer.addView(tv);
        } else {
            // 즐겨찾기 데이터 반복문으로 카드 추가
            for (Map.Entry<String, ?> entry : allFavorites.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();
                String[] parts = value.split(",");
                String routeId = parts[0];
                String routeName = parts[1];
                String stationId = parts[2];
                String stationName = parts[3];
                String customName = parts.length > 4 ? parts[4] : "";

                addFavoriteCard(key, routeId, routeName, stationId, stationName, customName);
            }
        }
    }


    private void addFavoriteCard(String key, String routeId, String routeName,
                                 String stationId, String stationName, String customName) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 16, 16, 16);
        cardView.setLayoutParams(cardParams);
        cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
        cardView.setRadius(16);
        cardView.setCardElevation(8);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        // 사용자 지정 이름 표시
        TextView tvCustomName = new TextView(this);
        tvCustomName.setText(customName.isEmpty() ? "이름 없음" : customName);
        tvCustomName.setTextSize(18);
        tvCustomName.setTypeface(null, Typeface.BOLD);
        layout.addView(tvCustomName);

        // 버스 및 정류장 정보
        TextView tvInfo = new TextView(this);
        tvInfo.setText("버스: " + routeName + "\n정류장: " + stationName);
        tvInfo.setTextSize(14);
        layout.addView(tvInfo);

        // 삭제 및 이름 변경 버튼
        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnDelete = new Button(this);
        btnDelete.setText("삭제");
        btnDelete.setOnClickListener(v -> deleteFavorite(key));

        Button btnRename = new Button(this);
        btnRename.setText("이름 변경");
        btnRename.setOnClickListener(v -> showRenameDialog(key, customName));

        btnLayout.addView(btnDelete);
        btnLayout.addView(btnRename);
        layout.addView(btnLayout);

        cardView.addView(layout);
        favoritesContainer.addView(cardView);
    }

    private void deleteFavorite(String key) {
        SharedPreferences.Editor editor = favoritesPrefs.edit();
        editor.remove(key);
        editor.apply();
        Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show();
        loadFavorites(); // 목록 새로고침
    }

    private void showRenameDialog(String key, String currentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("이름 변경");

        final EditText input = new EditText(this);
        input.setText(currentName);
        builder.setView(input);

        builder.setPositiveButton("확인", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            updateFavoriteName(key, newName);
        });
        builder.setNegativeButton("취소", null);
        builder.show();
    }

    private void updateFavoriteName(String key, String newName) {
        String originalValue = favoritesPrefs.getString(key, "");
        String[] parts = originalValue.split(",");
        if (parts.length >= 4) {
            // 기존 값에 새 이름 추가
            String updatedValue = parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3] + "," + newName;
            SharedPreferences.Editor editor = favoritesPrefs.edit();
            editor.putString(key, updatedValue);
            editor.apply();
            loadFavorites(); // 목록 새로고침
        }
    }

}

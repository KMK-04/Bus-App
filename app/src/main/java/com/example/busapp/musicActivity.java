package com.example.busapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class musicActivity extends AppCompatActivity {

    private static final int REQUEST_READ_STORAGE = 100;
    private List<File> musicFiles = new ArrayList<>();
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_music);

        TextView header = findViewById(R.id.header);
        header.setText("알림음 선택 (" + getString(R.string.app_name) + ")");

        // 저장소 권한 체크 및 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        REQUEST_READ_STORAGE);
            } else {
                loadMusicList();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_STORAGE);
            } else {
                loadMusicList();
            }
        }
    }

    private void loadMusicList() {
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File[] files = musicDir.listFiles();

        musicFiles.clear();

        // 지원 확장자 목록
        List<String> supportedExtensions = Arrays.asList(".mp3", ".m4a", ".wav", ".ogg", ".flac");

        // m.mp3(알림음) 파일을 맨 위에 추가
        File alarmFile = new File(musicDir, "m.mp3");
        if (alarmFile.exists()) {
            musicFiles.add(alarmFile);
        }

        if (files != null) {
            for (File file : files) {
                String name = file.getName().toLowerCase();
                if (file.isFile() && supportedExtensions.stream().anyMatch(name::endsWith)) {
                    if (!file.equals(alarmFile)) {
                        musicFiles.add(file);
                    }
                }
            }
        }

        ListView listView = findViewById(R.id.listView);
        MusicListAdapter adapter = new MusicListAdapter(musicFiles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            File selectedFile = musicFiles.get(position);
            playAndConfirm(selectedFile);
        });
    }

    private void playAndConfirm(File file) {
        // 기존 재생 중지
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            new AlertDialog.Builder(this)
                    .setTitle("알림음 설정")
                    .setMessage("이 음악을 알림음으로 사용하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> {
                        copyAndRenameFile(file);
                        if (mediaPlayer != null) {
                            try { mediaPlayer.stop(); } catch (Exception ignored) {}
                            mediaPlayer.release();
                            mediaPlayer = null;
                        }
                    })
                    .setNegativeButton("아니오", (dialog, which) -> {
                        if (mediaPlayer != null) {
                            try { mediaPlayer.stop(); } catch (Exception ignored) {}
                            mediaPlayer.release();
                            mediaPlayer = null;
                        }
                    })
                    .setOnDismissListener(d -> {
                        if (mediaPlayer != null) {
                            try { mediaPlayer.stop(); } catch (Exception ignored) {}
                            mediaPlayer.release();
                            mediaPlayer = null;
                        }
                    })
                    .show();

        } catch (IOException e) {
            Toast.makeText(this, "재생 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyAndRenameFile(File sourceFile) {
        File targetFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), "m.mp3");
        try {
            // 기존 파일 삭제
            if (targetFile.exists()) {
                if (!targetFile.delete()) {
                    throw new IOException("기존 알림음 삭제 실패");
                }
            }
            // 파일 복사
            FileInputStream in = new FileInputStream(sourceFile);
            FileOutputStream out = new FileOutputStream(targetFile);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();

            Toast.makeText(this, "알림음이 설정되었습니다", Toast.LENGTH_SHORT).show();
            loadMusicList(); // 목록 갱신

        } catch (IOException e) {
            Toast.makeText(this, "설정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 음악 목록을 CardView로 예쁘게 보여주는 커스텀 어댑터
    private class MusicListAdapter extends ArrayAdapter<File> {
        MusicListAdapter(List<File> files) {
            super(musicActivity.this, 0, files);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.music_list_item, parent, false);
            }

            File file = getItem(position);
            CardView card = convertView.findViewById(R.id.card);
            TextView title = convertView.findViewById(R.id.title);
            TextView info = convertView.findViewById(R.id.info);

            // m.mp3(알림음) 강조
            if (file.getName().equals("m.mp3")) {
                card.setCardBackgroundColor(0xFFE3F2FD);
                title.setText("알림음: " + file.getName());
            } else {
                card.setCardBackgroundColor(Color.WHITE);
                title.setText(file.getName());
            }

            // 음악 길이 등 추가 정보 표시
            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(file.getPath());
                mp.prepare();
                int duration = mp.getDuration();
                info.setText(formatDuration(duration));
            } catch (Exception e) {
                info.setText("정보 불러오기 실패");
            } finally {
                mp.release();
            }

            return convertView;
        }

        private String formatDuration(int milliseconds) {
            int seconds = (milliseconds / 1000) % 60;
            int minutes = (milliseconds / (1000 * 60)) % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // 권한 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicList();
            } else {
                Toast.makeText(this, "저장소 접근 권한이 필요합니다 (grantResults[0]: " + grantResults[0] + ")", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}

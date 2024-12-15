package com.example.imageviewer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class LogViewerActivity extends AppCompatActivity {
    private LinearLayout logContainer;
    private Button btnRefresh;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_viewer);

        logContainer = findViewById(R.id.logContainer);
        btnBack = findViewById(R.id.logBackToMain);
        btnBack.setOnClickListener(v -> back());
        btnRefresh = findViewById(R.id.btnRefresh);

        btnRefresh.setOnClickListener(v -> refreshLogs());

        // 초기 로그 로딩
        refreshLogs();
    }

    private void refreshLogs() {
        // 기존 로그 클리어
        logContainer.removeAllViews();

        // 로그 데이터 가져오기 (예시)
        List<LogEntry> logs = fetchLogs();

        // 로그 표시
        for (LogEntry log : logs) {
            addLogEntry(log);
        }
    }

    private void back() {
        this.finish();
    }

    private void addLogEntry(LogEntry log) {
        View logView = getLayoutInflater().inflate(R.layout.log_item, null);

        TextView tvDateTime = logView.findViewById(R.id.tvDateTime);
        TextView tvLogContent = logView.findViewById(R.id.tvLogContent);

        tvDateTime.setText(log.getDateTime());
        tvLogContent.setText(log.getContent());

        logContainer.addView(logView);
    }

    // 로그 데이터 클래스
    private static class LogEntry {
        private String dateTime;
        private String content;

        public LogEntry(String dateTime, String content) {
            this.dateTime = dateTime;
            this.content = content;
        }

        public String getDateTime() { return dateTime; }
        public String getContent() { return content; }
    }

    // 실제 로그를 가져오는 메서드 (구현 필요)
    private List<LogEntry> fetchLogs() {
        List<LogEntry> logs = new ArrayList<>();
        // TODO: 실제 로그 데이터를 가져오는 로직 구현
        // 예시 데이터
        logs.add(new LogEntry("2024/03/15 14:30:22", "애플리케이션 시작됨"));
        logs.add(new LogEntry("2024/03/15 14:30:25", "데이터베이스 연결 성공"));
        return logs;
    }
}
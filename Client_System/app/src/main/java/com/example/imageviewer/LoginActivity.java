package com.example.imageviewer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;

    String host = "http://10.0.2.2:8000";
    // String host = "https://thinking.pythonanywhere.com";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 뷰 초기화
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        // 로그인 버튼 클릭 리스너
        loginButton.setOnClickListener(v -> attemptLogin());

        // 엔터 키 처리
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
    }

    private void attemptLogin() {
        // 입력값 가져오기
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // 입력 검증
        if (username.isEmpty()) {
            usernameEditText.setError("아이디를 입력하세요");
            usernameEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("비밀번호를 입력하세요");
            passwordEditText.requestFocus();
            return;
        }

        // 로그인 처리
        performLogin(username, password);
    }

    private void performLogin(String username, String password) {
        new SignInTask().execute(username, password);
    }

    private void loginSuccess(String _token) {
        // GalleryActivity로 이동
        Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra("token", _token);
        startActivity(intent);
        finish(); // LoginActivity 종료
    }

    private void loginFailed() {
        Toast.makeText(this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show();
        passwordEditText.setText("");
        passwordEditText.requestFocus();
    }

    private class SignInTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... params) {
            try {
                String endpoint = "/api-token-auth/";
                URL url = new URL(host + endpoint);
                String id = params[0];
                String password = params[1];
                String body = String.format("username=%s&password=%s", id, password);
                byte[] rawBody = body.getBytes(StandardCharsets.UTF_8);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setDoOutput(true);
                conn.getOutputStream().write(rawBody);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    String _token = result.toString();
                    Log.d(null, _token);
                    return _token;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String _token) {
            String token;
            if (_token == null) {
                loginFailed();
                return;
            }
            try {
                JSONObject jsonObj = new JSONObject(_token);
                token = jsonObj.get("token").toString();
                loginSuccess(token);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            Toast.makeText(getApplicationContext(), "Sign In Success " + token, Toast.LENGTH_LONG).show();
        }
    }
}
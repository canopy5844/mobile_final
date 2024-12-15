package com.example.imageviewer;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GalleryActivity extends AppCompatActivity {
    private LinearLayout imageContainer;
    private EditText searchEditText;
    private EditText yearEditText;
    private EditText monthEditText;
    private EditText dayEditText;
    private ImageButton calendarButton;
    private CheckBox allPeriodCheckbox;
    private Button searchButton;
    private Button logButton;
    private List<String> imageUrls; // 이미지 경로 리스트
    String host = "http://10.0.2.2:8000";
    // String host = "https://thinking.pythonanywhere.com";
    String token = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        imageUrls = new ArrayList<>();
        // 뷰 초기화
        imageContainer = findViewById(R.id.imageContainer);
        searchEditText = findViewById(R.id.searchEditText);
        yearEditText = findViewById(R.id.yearEditText);
        monthEditText = findViewById(R.id.monthEditText);
        dayEditText = findViewById(R.id.dayEditText);
        calendarButton = findViewById(R.id.calendarButton);
        allPeriodCheckbox = findViewById(R.id.allPeriodCheckbox);
        searchButton = findViewById(R.id.searchButton);
        // logButton = findViewById(R.id.logButton);

        // 날짜 입력 이벤트 설정
        setupDateInputs();

        // 캘린더 버튼 클릭 리스너
        calendarButton.setOnClickListener(v -> showDatePicker());

        // 검색 버튼 클릭 리스너
        searchButton.setOnClickListener(v -> performSearch());

        // 로그 버튼 클릭 리스너
        // logButton.setOnClickListener(v -> showLog());

        // 전체기간 체크박스 리스너
        allPeriodCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            yearEditText.setEnabled(!isChecked);
            monthEditText.setEnabled(!isChecked);
            dayEditText.setEnabled(!isChecked);
            calendarButton.setEnabled(!isChecked);
        });

        // 초기 이미지 로드
        loadImages();
    }

    private void setupDateInputs() {
        // 연도 입력 제한
        yearEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    int year = Integer.parseInt(s.toString());
                    if (year < 1900 || year > Calendar.getInstance().get(Calendar.YEAR)) {
                        yearEditText.setError("유효한 연도를 입력하세요");
                    } else {
                        yearEditText.setError(null);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 월 입력 제한
        monthEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    int month = Integer.parseInt(s.toString());
                    if (month < 1 || month > 12) {
                        monthEditText.setError("1-12 사이의 값을 입력하세요");
                    } else {
                        monthEditText.setError(null);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 일 입력 제한
        dayEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    int day = Integer.parseInt(s.toString());
                    if (day < 1 || day > 31) {
                        dayEditText.setError("1-31 사이의 값을 입력하세요");
                    } else {
                        dayEditText.setError(null);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadImages() {
        new TaskLoadImage().execute("{}");
    }

    private class TaskLoadImage extends AsyncTask<String, Integer, List<ImageArticle>> {
        private byte[] build(String query) throws IOException {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            stream.write(query.getBytes());
            return stream.toByteArray();
        }

        protected List<ImageArticle> doInBackground(String... params) {
            ByteArrayOutputStream rawDataStream = new ByteArrayOutputStream();
            List<ImageArticle> imageArticleList = new ArrayList<>();
            try {
                String query = params[0];
                rawDataStream.write(build(query));

                byte[] rawData = rawDataStream.toByteArray();
                String endpoint = "/api_root/search/q/";
                URL url = new URL(host + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);

                conn.setRequestProperty("Content-Length", String.valueOf(rawData.length));
                conn.setRequestProperty("Content-Type", "application/json");

                conn.setRequestMethod("POST");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setDoOutput(true);
                OutputStream outputStream = conn.getOutputStream();
                outputStream.write(rawData);
                outputStream.flush();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);
                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject post_json = (JSONObject) aryJson.get(i);
                        String title = post_json.getString("title");
                        String text = post_json.getString("text");
                        String imageUrl = post_json.getString("image");
                        String publishedDate = post_json.getString("published_date");
                        if (!imageUrl.isEmpty()) {
                            URL myImageUrl = new URL(imageUrl);
                            conn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = conn.getInputStream();
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);

                            ImageArticle imgArticle = new ImageArticle(imageBitmap, title, text, publishedDate);

                            imageArticleList.add(imgArticle);
                            ImageArticles.getInstance().addArticle(imgArticle); // 이미지 리스트에 추가
                            imgStream.close();
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return imageArticleList;
        }


        @Override
        protected void onPostExecute(List<ImageArticle> imageArticleList) {
            if (imageArticleList.isEmpty()) {
                imageContainer.removeAllViews();
                Log.d(null, "imageArticleList.isEmpty()");
            } else {
                imageContainer.removeAllViews();
                // 3x3
                for (int i = 0; i < imageArticleList.size(); i += 3) {
                    LinearLayout row = createImageRow();
                    for (int j = 0; j < 3 && (i + j) < imageArticleList.size(); j++) {
                        ImageView imageView = (ImageView) row.getChildAt(j);
                        Bitmap bitmap = imageArticleList.get(i + j).getImageBitmap();
                        // 이미지 뷰를 정사각형으로 만들기
                        makeSquareImageView(imageView);
                        imageView.setImageBitmap(bitmap);

                        final int index = i + j;
                        imageView.setOnClickListener(v -> onImageClick(index));
                    }

                    imageContainer.addView(row);
                }
            }
            ImageArticles.getInstance().clear();
            for (int i = 0; i < imageArticleList.size(); i++) {
                ImageArticles.getInstance().addArticle(imageArticleList.get(i));
            }
        }
    }

    private LinearLayout createImageRow() {
        LinearLayout row = new LinearLayout(this);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(rowParams);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setWeightSum(3);

        // 3개의 ImageView 추가
        for (int i = 0; i < 3; i++) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
            );
            imageParams.setMargins(1, 1, 1, 1);
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            row.addView(imageView);
        }

        return row;
    }

    private void makeSquareImageView(final ImageView imageView) {
        imageView.post(() -> {
            int width = imageView.getWidth();
            ViewGroup.LayoutParams params = imageView.getLayoutParams();
            params.height = width;
            imageView.setLayoutParams(params);
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        try {
            if (!yearEditText.getText().toString().isEmpty()) {
                year = Integer.parseInt(yearEditText.getText().toString());
            }
            if (!monthEditText.getText().toString().isEmpty()) {
                month = Integer.parseInt(monthEditText.getText().toString()) - 1;
            }
            if (!dayEditText.getText().toString().isEmpty()) {
                day = Integer.parseInt(dayEditText.getText().toString());
            }
        } catch (NumberFormatException e) {
            // 파싱 오류 시 현재 날짜 사용
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    yearEditText.setText(String.valueOf(selectedYear));
                    monthEditText.setText(String.format(Locale.getDefault(), "%02d", selectedMonth + 1));
                    dayEditText.setText(String.format(Locale.getDefault(), "%02d", selectedDayOfMonth));
                },
                year,
                month,
                day
        );

        datePickerDialog.getDatePicker().setMinDate(getMinDate());
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private long getMinDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(1900, 0, 1);
        return calendar.getTimeInMillis();
    }

    private void performSearch() {
        String searchQuery = searchEditText.getText().toString().trim();
        boolean isAllPeriod = allPeriodCheckbox.isChecked();
        boolean hasDateFilter = false;
        int year = -1, month = -1, day = -1;

        if (!isAllPeriod) {
            try {
                if (!yearEditText.getText().toString().isEmpty() &&
                        !monthEditText.getText().toString().isEmpty() &&
                        !dayEditText.getText().toString().isEmpty()) {

                    year = Integer.parseInt(yearEditText.getText().toString());
                    month = Integer.parseInt(monthEditText.getText().toString());
                    day = Integer.parseInt(dayEditText.getText().toString());
                    hasDateFilter = true;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "날짜 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (searchQuery.isEmpty() && !hasDateFilter && !isAllPeriod) {
            Toast.makeText(this, "검색어나 날짜를 입력하거나 전체기간을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        String json_query = buildSearchQuery(searchQuery, year, month, day, hasDateFilter, isAllPeriod);
        new TaskLoadImage().execute(json_query);
    }


    public String buildSearchQuery(String searchQuery, int year, int month, int day,
                                   boolean hasDateFilter, boolean isAllPeriod) {
        List<String> o = new ArrayList<>();
        if (isAllPeriod) {
            o.add("\"isAllPeriod\": " + String.valueOf(true));
        } else if (hasDateFilter) {
            o.add("\"hasDateFilter\": " + String.valueOf(true));
            if (year != -1) {
                o.add("\"year\": " + String.valueOf(year));
            }
            if (month != -1) {
                o.add("\"month\": " + String.valueOf(month));
            }
            if (day != -1) {
                o.add("\"day\": " + String.valueOf(day));
            }
        }
        if (!searchQuery.isEmpty()) {
            o.add("\"query\":\"" + searchQuery + "\"");
        }
        String s = "{";
        int l = o.size();
        for (int i = 0; i < l; i++) {
            s += o.get(i);
            if (i < l - 1) {
                s += ", ";
            }
        }
        s += "}";
        return s;
    }


    private List<String> filterImages(String searchQuery, int year, int month, int day,
                                      boolean hasDateFilter, boolean isAllPeriod) {
        // int hasDateFilterInt = hasDateFilter? 1:0;
        // int isAllPeriodInt = isAllPeriod? 1:0;
        String json_query = buildSearchQuery(searchQuery, year, month, day, hasDateFilter, isAllPeriod);
        Toast.makeText(this,
                json_query,
                Toast.LENGTH_SHORT).show();
//        Toast.makeText(this,
//                String.format("%s %d %d %d %d %d", searchQuery, year, month, day, hasDateFilterInt, isAllPeriodInt),
//                Toast.LENGTH_SHORT).show();
        List<String> filteredUrls = new ArrayList<>();
        for (String imagePath : imageUrls) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    imagePath.toLowerCase().contains(searchQuery.toLowerCase());
            boolean matchesDate = isAllPeriod || !hasDateFilter ||
                    matchesDateFilter(imagePath, year, month, day);

            if (matchesSearch && matchesDate) {
                filteredUrls.add(imagePath);
            }
        }
        return filteredUrls;
    }

//    private void showLog() {
//        Context ctx = this.getApplicationContext();
//        Intent intent = new Intent(ctx, LogViewerActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        ctx.startActivity(intent);
//    }

    private boolean matchesDateFilter(String imagePath, int year, int month, int day) {
        try {
            File imageFile = new File(imagePath);
            Date lastModified = new Date(imageFile.lastModified());
            Calendar imageDate = Calendar.getInstance();
            imageDate.setTime(lastModified);

            return imageDate.get(Calendar.YEAR) == year &&
                    imageDate.get(Calendar.MONTH) == month &&
                    imageDate.get(Calendar.DAY_OF_MONTH) == day;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateImages(List<String> newUrls) {
        imageUrls = newUrls;
        loadImages();
        Toast.makeText(this,
                String.format("검색 결과: %d개의 이미지", newUrls.size()),
                Toast.LENGTH_SHORT).show();
    }

    private void onImageClick(int index) {
        Context ctx = this.getApplicationContext();

        Intent intent = new Intent(ctx, DetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("index", index);
        ctx.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 메모리 정리
        recycleBitmaps();
    }

    private void recycleBitmaps() {
        for (int i = 0; i < imageContainer.getChildCount(); i++) {
            View view = imageContainer.getChildAt(i);
            if (view instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) view;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View childView = row.getChildAt(j);
                    if (childView instanceof ImageView) {
                        ImageView imageView = (ImageView) childView;
                        Drawable drawable = imageView.getDrawable();
                        if (drawable instanceof BitmapDrawable) {
                            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                            if (bitmap != null && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                        }
                        imageView.setImageDrawable(null);
                    }
                }
            }
        }
    }
}

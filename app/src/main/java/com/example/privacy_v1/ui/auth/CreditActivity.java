package com.example.privacy_v1.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privacy_v1.R;
import com.example.privacy_v1.model.CreditAdapter;
import com.example.privacy_v1.server.model.CreditInfoRequest;
import com.example.privacy_v1.server.model.UsageItem;
import com.example.privacy_v1.server.network.ApiService;
import com.example.privacy_v1.server.network.RetrofitClient;
import com.example.privacy_v1.ui.dialog.CustomCalendarDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreditActivity extends AppCompatActivity {
    private static final String TAG = "CreditActivity";
    private TextView selectDate, credit_num;
    private RecyclerView rv;
    private CreditAdapter adapter;
    private ApiService apiService;
    private String device_id;
    private Button btnToggleSort;
    // true = 오름차순, false = 내림차순
    private boolean isAscending = false;
    private List<UsageItem> currentList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_info);

        selectDate = findViewById(R.id.selectDate);
        rv = findViewById(R.id.usedHistory);
        btnToggleSort = findViewById(R.id.btnSortAsc);
        credit_num = findViewById(R.id.credit_num);

        // RecyclerView 설정
        adapter = new CreditAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        SharedPreferences tk_prefs = getSharedPreferences("tk_prefs", MODE_PRIVATE);
        // Retrofit 초기화
        apiService = RetrofitClient.getInstance().create(ApiService.class);


        // 초기 날짜 표시 및 클릭 리스너 설정

        Calendar now = Calendar.getInstance();
        int year  = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        device_id = tk_prefs.getString("device_id", null);
        selectDate.setText(String.format("%d년 %02d월 ▼", year, month));
        selectDate.setOnClickListener(v -> showYearMonthPicker());
        String req_month = String.format("%d-%02d", year, month);

        // 정렬 토글 버튼 클릭
        btnToggleSort.setOnClickListener(v -> {
            isAscending = !isAscending;
            // 버튼 텍스트 변경
            btnToggleSort.setText(isAscending ? "최신 순" : "오래된 순");
            List<UsageItem> displayList;
            if (isAscending) {
                // 원본 currentList 복사 후 뒤집기
                displayList = new ArrayList<>(currentList);
                Collections.reverse(displayList);
            } else {
                // 원본 순서 그대로
                displayList = new ArrayList<>(currentList);
            }
            // 어댑터에 새 리스트 세팅 (notifyDataSetChanged() 내부 호출)
            adapter.setItems(displayList);
        });

        // 첫 데이터 로드
        fetchUsageHistory(device_id,req_month);

        ImageButton beforeButton = findViewById(R.id.creditinfo_before);
        beforeButton.setOnClickListener(v -> {
            startActivity(new Intent(CreditActivity.this, MyPageActivity.class));
            finish();
        });

    }

    private void showYearMonthPicker() {
        // 1) 현재 연·월 구하기
        Calendar now = Calendar.getInstance();
        int year  = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);  // 0~11

// 2) 다이얼로그 생성 & 리스너 등록
        CustomCalendarDialog calendarDialog =
                CustomCalendarDialog.newInstance(year, month);

        calendarDialog.setOnYearMonthSetListener((y, m) -> {
            // m은 1~12
            selectDate.setText(String.format("%d년 %02d월 ▼", y, m));
            String show_req_month = String.format("%d-%02d", y, m);;
            fetchUsageHistory(device_id, show_req_month);
        });

        calendarDialog.show(getSupportFragmentManager(), "YearMonthPicker");
    }
    private void fetchUsageHistory(String id, String month) {
        CreditInfoRequest credit_req = new CreditInfoRequest(id, month);

        apiService.creditinfo(credit_req)
                .enqueue(new Callback<List<UsageItem>>() {
                    @Override
                    public void onResponse(Call<List<UsageItem>> call,
                                           Response<List<UsageItem>> response) {

                        // 응답이 오면 기존 리스트는 무조건 먼저 비움
                        currentList.clear();

                        if (response.isSuccessful() && response.body() != null) {

                            List<UsageItem> list = response.body();

                            // 서버 응답 리스트 추가
                            currentList.addAll(list);

                            if (!currentList.isEmpty()) {
                                UsageItem lastItem = currentList.get(currentList.size() - 1);
                                int lastIndexValue = lastItem.getAmount();

                                credit_num.setText(String.valueOf(lastIndexValue));
                            } else {
                                // 서버 응답이 []인 경우
                                credit_num.setText("0");
                            }

                        } else {
                            // 서버 오류 또는 body null
                            credit_num.setText("0");
                        }

                        // 성공 / 빈 배열 / 실패 응답 모두 여기서 RecyclerView 갱신
                        adapter.setItems(currentList);
                    }

                    @Override
                    public void onFailure(Call<List<UsageItem>> call,
                                          Throwable t) {

                        currentList.clear();
                        credit_num.setText("0");

                        // 통신 실패 시에도 RecyclerView 비움
                        adapter.setItems(currentList);
                        Log.e("CreditActivity", "creditinfo 통신 실패", t);
                    }
                });
    }

//    private void fetchUsageHistory(String id, String month) {
//        CreditInfoRequest credit_req = new CreditInfoRequest(id, month);
//
//        apiService.creditinfo(credit_req)
//                .enqueue(new Callback<List<UsageItem>>() {
//                    @Override
//                    public void onResponse(Call<List<UsageItem>> call,
//                                           Response<List<UsageItem>> response) {
//                        if (response.isSuccessful() && response.body() != null) {
//                            List<UsageItem> list = response.body();
//                            currentList.clear();
//                            currentList.addAll(list);
//
//                            // 1) 리스트가 비어 있지 않다면
//                            if (!currentList.isEmpty()) {
//                                // 2) 마지막 요소의 index 값 꺼내기
//                                UsageItem lastItem = currentList.get(currentList.size() - 1);
//                                int lastIndexValue = lastItem.getAmount(); // 또는 getIdx(), 모델에 맞춰서
//
//                                // 3) TextView 등에 표시
//                                credit_num.setText(String.valueOf(lastIndexValue));
//                                adapter.setItems(currentList);
//                            } else {
//                                credit_num.setText("0"); // 혹은 적절한 기본값
//                            }
//                        } else {
//                            // 에러 처리
//                            currentList.clear();
//                            adapter.setItems(currentList);
//                            credit_num.setText("0");
//                        }
//                    }
////                    @Override
////                    public void onResponse(
////                            Call<List<UsageItem>> call,
////                            Response<List<UsageItem>> response
////                    ) {
////                        if (response.isSuccessful() && response.body() != null) {
////                            // 바로 body()가 List<UsageItem>
////                            currentList.clear();
////                            currentList.addAll(response.body());
////                        } else {
////                            currentList.clear();
////                        }
////                        sortByTime();  // 정렬 및 adapter.setItems 호출
////                    }
////                        if (response.isSuccessful() && response.body() != null) {
////                            CreditInfoResponse res = response.body();
////                            if (res.isSuccess()) {
//////                                adapter.setItems(res.getData());
////                                currentList.clear();
////                                currentList.addAll(res.getData());
////                                sortByTime();
////                            } else {
////                                currentList.clear();
////                                adapter.setItems(Collections.emptyList());
////                            }
////                        } else {
////                            currentList.clear();
////                            adapter.setItems(Collections.emptyList());
////                        }
//
//                    @Override
//                    public void onFailure(
//                            Call<List<UsageItem>> call,
//                            Throwable t
//                    ) {
//                        currentList.clear();
//                        adapter.setItems(currentList);
//                        adapter.setItems(Collections.emptyList());
//                    }
//                });
//    }

}
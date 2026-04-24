package com.example.autorotate.UI;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autorotate.Model.AppsInfo;
import com.example.autorotate.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.autorotate.ViewModel.AppsRepository.nameComparator;

public class AppSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_PICKED_PACKAGE = "extra_picked_package";
    public static final String EXTRA_PICKED_NAME = "extra_picked_name";

    private RecyclerView appListRecycler;
    private ProgressBar loadingBar;
    private EditText searchField;
    private AppSelectionAdapter adapter;

    private final List<AppsInfo> appsInfoList = new ArrayList<>();
    private final Set<String> selectedPackageSet = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_selection);

        appListRecycler = findViewById(R.id.app_list_recycler);
        loadingBar = findViewById(R.id.app_list_loading);
        searchField = findViewById(R.id.app_list_search);

        appListRecycler.setLayoutManager(new LinearLayoutManager(this));

        MaterialToolbar toolbar = findViewById(R.id.app_selection_toolbar);
        toolbar.setTitle(getString(R.string.select_apps_screen_title));
        toolbar.setNavigationOnClickListener(v -> finish());

        ArrayList<String> selectedPackages = getIntent().getStringArrayListExtra(MainActivity.EXTRA_SELECTED_PACKAGES);
        if (selectedPackages != null) {
            selectedPackageSet.addAll(selectedPackages);
        }

        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.submitQuery(s != null ? s.toString() : "");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadAppListAsync();
    }

    private void loadAppListAsync() {
        loadingBar.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            PackageManager packageManager = getPackageManager();
            Intent launchIntent = new Intent(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = packageManager.queryIntentActivities(launchIntent, 0);

            appsInfoList.clear();
            Set<String> addedPackages = new HashSet<>();

            for (ResolveInfo info : infos) {
                if (info.activityInfo == null || info.activityInfo.packageName == null) {
                    continue;
                }

                String packageName = info.activityInfo.packageName;
                if (!addedPackages.add(packageName)) {
                    continue;
                }

                CharSequence label = info.loadLabel(packageManager);
                String name = label != null ? label.toString() : null;
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }

                appsInfoList.add(new AppsInfo(name, packageName));
            }

            Collections.sort(appsInfoList, nameComparator);

            runOnUiThread(() -> {
                loadingBar.setVisibility(View.GONE);
                adapter = new AppSelectionAdapter(
                        appsInfoList,
                        selectedPackageSet,
                        packageManager,
                        this::onAppPicked
                );
                appListRecycler.setAdapter(adapter);
            });
        });
        executor.shutdown();
    }

    private void onAppPicked(AppsInfo appInfo) {
        if (appInfo == null || appInfo.getAppPackageName() == null || appInfo.getAppPackageName().isEmpty()) {
            return;
        }

        selectedPackageSet.add(appInfo.getAppPackageName());

        Intent data = new Intent();
        data.putStringArrayListExtra(MainActivity.EXTRA_SELECTED_PACKAGES, new ArrayList<>(selectedPackageSet));
        data.putExtra(EXTRA_PICKED_PACKAGE, appInfo.getAppPackageName());
        data.putExtra(EXTRA_PICKED_NAME, appInfo.getAppName());
        setResult(RESULT_OK, data);
        finish();
    }
}

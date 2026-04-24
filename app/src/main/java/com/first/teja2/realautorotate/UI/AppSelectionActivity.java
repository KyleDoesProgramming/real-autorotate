package com.first.teja2.realautorotate.UI;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.first.teja2.realautorotate.Model.AppsInfo;
import com.first.teja2.realautorotate.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.first.teja2.realautorotate.ViewModel.AppsRepository.nameComparator;

public class AppSelectionActivity extends AppCompatActivity {

    private RecyclerView appListRecycler;
    private ProgressBar loadingBar;
    private MaterialButton saveButton;
    private AppSelectionAdapter adapter;

    private final List<AppsInfo> appsInfoList = new ArrayList<>();
    private final Set<String> selectedPackageSet = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_selection);

        appListRecycler = findViewById(R.id.app_list_recycler);
        loadingBar = findViewById(R.id.app_list_loading);
        saveButton = findViewById(R.id.app_list_save_button);

        appListRecycler.setLayoutManager(new LinearLayoutManager(this));

        MaterialToolbar toolbar = findViewById(R.id.app_selection_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ArrayList<String> selectedPackages = getIntent().getStringArrayListExtra(MainActivity.EXTRA_SELECTED_PACKAGES);
        if (selectedPackages != null) {
            selectedPackageSet.addAll(selectedPackages);
        }

        saveButton.setOnClickListener(v -> {
            Intent data = new Intent();
            data.putStringArrayListExtra(MainActivity.EXTRA_SELECTED_PACKAGES, new ArrayList<>(selectedPackageSet));
            setResult(RESULT_OK, data);
            finish();
        });

        loadAppListAsync();
    }

    private void loadAppListAsync() {
        loadingBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            PackageManager packageManager = getPackageManager();
            List<ApplicationInfo> infos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            appsInfoList.clear();

            for (ApplicationInfo info : infos) {
                if (packageManager.getLaunchIntentForPackage(info.packageName) == null) {
                    continue;
                }

                String name = (String) info.loadLabel(packageManager);
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }

                appsInfoList.add(new AppsInfo(name, info.packageName));
            }

            Collections.sort(appsInfoList, nameComparator);

            runOnUiThread(() -> {
                loadingBar.setVisibility(View.GONE);
                saveButton.setEnabled(true);
                adapter = new AppSelectionAdapter(appsInfoList, selectedPackageSet, packageManager);
                appListRecycler.setAdapter(adapter);
            });
        });
        executor.shutdown();
    }
}

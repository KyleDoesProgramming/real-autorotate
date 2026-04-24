package com.example.autorotate.UI;

/**
 * Created by
 * Bala Guna Teja Karlapudi
 */

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autorotate.Model.AppsInfo;
import com.example.autorotate.R;
import com.example.autorotate.Service.realAutorotateService;
import com.example.autorotate.Util.PermissionGate;
import com.example.autorotate.Util.UsageStatsHelper;
import com.example.autorotate.ViewModel.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.autorotate.ViewModel.AppsRepository.nameComparator;


public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PACKAGES = "extra_selected_packages";

    List<AppsInfo> selectedAppsList = new ArrayList<>();

    private MainViewModel mMainViewModel;
    View emptyState;
    ProgressBar pb;
    private RecyclerView mRecyclerView;
    private ItemAdapter mAdapter;
    MaterialSwitch materialSwitch;

    private final ActivityResultLauncher<Intent> appSelectionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }

                ArrayList<String> packages = result.getData().getStringArrayListExtra(EXTRA_SELECTED_PACKAGES);
                if (packages == null) {
                    return;
                }

                String pickedPackage = result.getData().getStringExtra(AppSelectionActivity.EXTRA_PICKED_PACKAGE);
                String pickedName = result.getData().getStringExtra(AppSelectionActivity.EXTRA_PICKED_NAME);

                applySelectedPackages(packages, pickedPackage, pickedName);
            });

    @Override
    protected void onResume() {
        super.onResume();

        if (!hasRequiredPermissions()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            return;
        }

        if (getStatus() == 1 && !selectedAppsList.isEmpty()) {
            stopService(new Intent(this, realAutorotateService.class));
            startService(new Intent(this, realAutorotateService.class));
        }

        List<AppsInfo> removedApps = new ArrayList<>();
        for (AppsInfo appsInfo : selectedAppsList) {
            if (appsInfo.isDefaultEntry()) {
                continue;
            }
            if (!isPackageInstalled(appsInfo.getAppPackageName(), getPackageManager())) {
                removedApps.add(appsInfo);
            }
        }

        if (!removedApps.isEmpty()) {
            for (AppsInfo appsInfo : removedApps) {
                mMainViewModel.setSelectedApps(this, selectedAppsList, appsInfo);
            }

            if (getStatus() == 1) {
                stopService(new Intent(this, realAutorotateService.class));
                startService(new Intent(this, realAutorotateService.class));
            }
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClicked);

        pb = findViewById(R.id.loadingBar);
        pb.setVisibility(View.INVISIBLE);

        emptyState = findViewById(R.id.empty_state);
        emptyState.setVisibility(View.GONE);

        materialSwitch = findViewById(R.id.switch1);

        mRecyclerView = findViewById(R.id.recyclerView);

        mMainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mMainViewModel.initSavedApps(getApplicationContext());

        List<AppsInfo> savedApps = mMainViewModel.getSavedApps().getValue();
        selectedAppsList = savedApps != null ? savedApps : new ArrayList<>();

        mMainViewModel.getSavedApps().observe(this, new Observer<List<AppsInfo>>() {
            @Override
            public void onChanged(@Nullable List<AppsInfo> appsList) {

                selectedAppsList = appsList != null ? appsList : new ArrayList<>();

                if (selectedAppsList.size() > 0) {

                    materialSwitch.setEnabled(true);

                    if (getStatus() == 1)
                        materialSwitch.setChecked(true);
                    else
                        materialSwitch.setChecked(false);


                    initRecyclerView();

                    appsSelectedVisibilitySettings();

                    stopService(new Intent(MainActivity.this, realAutorotateService.class));
                    startService(new Intent(MainActivity.this, realAutorotateService.class));

                } else {

                    noAppsVisibilitySettings();

                    materialSwitch.setChecked(false);
                    materialSwitch.setEnabled(false);

                }
            }
        });


        materialSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {

                setStatus(1);

                startService(new Intent(MainActivity.this, realAutorotateService.class));
                materialSwitch.setChecked(true);

            } else {

                setStatus(0);

                stopService(new Intent(MainActivity.this, realAutorotateService.class));
            }
        });

        int status = getStatus();

        if (selectedAppsList.size() > 0) {

            initRecyclerView();

            if (status == 1)
                materialSwitch.setChecked(true);

        }

        FloatingActionButton fab = findViewById(R.id.fab);
        applySystemInsets(fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!hasRequiredPermissions()) {
                    startActivity(new Intent(MainActivity.this, OnboardingActivity.class));
                    return;
                }

                Intent intent = new Intent(MainActivity.this, AppSelectionActivity.class);
                ArrayList<String> selectedPackages = new ArrayList<>();
                for (AppsInfo appsInfo : selectedAppsList) {
                    if (appsInfo.isDefaultEntry()) {
                        continue;
                    }
                    selectedPackages.add(appsInfo.getAppPackageName());
                }
                intent.putStringArrayListExtra(EXTRA_SELECTED_PACKAGES, selectedPackages);
                appSelectionLauncher.launch(intent);

            }
        });

    }

    private boolean onMenuItemClicked(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return false;
    }

    private void applySelectedPackages(List<String> packages, String pickedPackage, String pickedName) {
        pb.setVisibility(View.VISIBLE);

        List<AppsInfo> currentApps = new ArrayList<>(selectedAppsList);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            PackageManager packageManager = getPackageManager();
            List<AppsInfo> updatedApps = new ArrayList<>();
            HashMap<String, Integer> existingModes = new HashMap<>();
            HashMap<String, List<Integer>> existingRotations = new HashMap<>();
            AppsInfo defaultEntry = null;

            for (AppsInfo appsInfo : currentApps) {
                if (appsInfo.isDefaultEntry()) {
                    defaultEntry = appsInfo;
                    continue;
                }
                existingModes.put(appsInfo.getAppPackageName(), appsInfo.getRotationMode());
                existingRotations.put(appsInfo.getAppPackageName(), appsInfo.getSpecificRotations());
            }

            if (defaultEntry == null) {
                defaultEntry = AppsInfo.createDefaultEntry();
            }

            updatedApps.add(defaultEntry);

            HashSet<String> added = new HashSet<>();
            for (String packageName : packages) {
                if (added.contains(packageName)) {
                    continue;
                }
                try {
                    CharSequence appName = packageManager.getApplicationLabel(
                            packageManager.getApplicationInfo(packageName, 0)
                    );
                    if (appName != null) {
                        AppsInfo appsInfo = new AppsInfo(appName.toString(), packageName);
                        Integer rotationMode = existingModes.get(packageName);
                        if (rotationMode != null) {
                            appsInfo.setRotationMode(rotationMode);
                        }
                        List<Integer> specificRotations = existingRotations.get(packageName);
                        if (specificRotations != null) {
                            appsInfo.setSpecificRotations(specificRotations);
                        }
                        updatedApps.add(appsInfo);
                        added.add(packageName);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }

            Collections.sort(updatedApps, nameComparator);

            runOnUiThread(() -> {
                pb.setVisibility(View.INVISIBLE);
                mMainViewModel.setSelectedApps(MainActivity.this, updatedApps, null);
                materialSwitch.setChecked(true);
                setStatus(1);
                appsSelectedVisibilitySettings();
                initRecyclerView();

                if (pickedPackage != null && !pickedPackage.isEmpty()) {
                    String resolvedName = pickedName;
                    for (AppsInfo appsInfo : updatedApps) {
                        if (pickedPackage.equals(appsInfo.getAppPackageName())) {
                            resolvedName = appsInfo.getAppName();
                            break;
                        }
                    }
                    openRotationConfig(resolvedName != null ? resolvedName : "", pickedPackage);
                }
            });
        });
        executor.shutdown();
    }

    private void openRotationConfig(String appName, String packageName) {
        Intent intent = new Intent(this, AppRotationConfigActivity.class);
        intent.putExtra(AppRotationConfigActivity.EXTRA_APP_NAME, appName);
        intent.putExtra(AppRotationConfigActivity.EXTRA_APP_PACKAGE, packageName);
        intent.putExtra(AppRotationConfigActivity.EXTRA_IS_DEFAULT, false);
        startActivity(intent);
    }

    private void applySystemInsets(FloatingActionButton fab) {
        final int marginEnd = ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).rightMargin;
        final int marginBottom = ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.rightMargin = marginEnd + navInsets.right;
            params.bottomMargin = marginBottom + navInsets.bottom;
            v.setLayoutParams(params);
            return insets;
        });
    }

    private boolean hasRequiredPermissions() {
        return UsageStatsHelper.hasUsageStatsPermission(this)
            && UsageStatsHelper.hasBatteryOptimizationExemption(this)
            && (!PermissionGate.needsWriteSettings(this) || Settings.System.canWrite(this));
    }

    private boolean isPackageInstalled(String packagename, PackageManager packageManager) {
        try {
            packageManager.getPackageGids(packagename);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    int getStatus() {

        return PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getInt("status", -1);

    }

    void setStatus(int num) {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .edit()
                .putInt("status", num)
                .apply();
    }

    void noAppsVisibilitySettings() {
        emptyState.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);

    }

    void appsSelectedVisibilitySettings() {
        emptyState.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);

    }

    void initRecyclerView() {

        mAdapter = new ItemAdapter(selectedAppsList, this, mMainViewModel);
        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

}

package com.first.teja2.realautorotate.UI;

/**
 * Created by
 * Bala Guna Teja Karlapudi
 */

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.first.teja2.realautorotate.Model.AppsInfo;
import com.first.teja2.realautorotate.R;
import com.first.teja2.realautorotate.Service.realAutorotateService;
import com.first.teja2.realautorotate.Util.UsageStatsHelper;
import com.first.teja2.realautorotate.ViewModel.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.first.teja2.realautorotate.ViewModel.AppsRepository.nameComparator;


public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_PACKAGES = "extra_selected_packages";

    TextView title;
    List<AppsInfo> selectedAppsList = new ArrayList<>();

    private MainViewModel mMainViewModel;
    ImageView imageView;
    TextView tv, tv2;
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

                applySelectedPackages(packages);
            });

    @Override
    protected void onResume() {
        super.onResume();

        if (!hasRequiredPermissions()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            return;
        }

        if (selectedAppsList == null) {
            selectedAppsList = new ArrayList<>();
        }

        List<AppsInfo> removedApps = new ArrayList<>();
        for (AppsInfo appsInfo : selectedAppsList) {
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

        title = findViewById(R.id.toolbar_title);

        tv = findViewById(R.id.textView);
        tv.setVisibility(View.INVISIBLE);

        tv2 = findViewById(R.id.textView2);
        tv2.setVisibility(View.INVISIBLE);

        pb = findViewById(R.id.loadingBar);
        pb.setVisibility(View.INVISIBLE);

        imageView = findViewById(R.id.imageView);
        imageView.setVisibility(View.GONE);

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
                    selectedPackages.add(appsInfo.getAppPackageName());
                }
                intent.putStringArrayListExtra(EXTRA_SELECTED_PACKAGES, selectedPackages);
                appSelectionLauncher.launch(intent);

            }
        });

    }

    private void applySelectedPackages(List<String> packages) {
        if (packages.isEmpty()) {
            selectedAppsList.clear();
            mMainViewModel.setSelectedApps(getApplicationContext(), selectedAppsList, null);
            noAppsVisibilitySettings();
            Snackbar.make(findViewById(R.id.cLayout), R.string.no_apps_selected, Snackbar.LENGTH_LONG).show();
            materialSwitch.setChecked(false);
            return;
        }

        pb.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            PackageManager packageManager = getPackageManager();
            List<AppsInfo> updatedApps = new ArrayList<>();

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
                        updatedApps.add(new AppsInfo(appName.toString(), packageName));
                        added.add(packageName);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }

            Collections.sort(updatedApps, nameComparator);

            runOnUiThread(() -> {
                pb.setVisibility(View.INVISIBLE);
                mMainViewModel.setSelectedApps(getApplicationContext(), updatedApps, null);
                materialSwitch.setChecked(true);
                setStatus(1);
                appsSelectedVisibilitySettings();
                initRecyclerView();
            });
        });
        executor.shutdown();
    }

    private void applySystemInsets(FloatingActionButton fab) {
        View toolbar = findViewById(R.id.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), statusInsets.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

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
        return UsageStatsHelper.hasUsageStatsPermission(this) && Settings.System.canWrite(this);
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

        tv.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);
        tv2.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);

    }

    void appsSelectedVisibilitySettings() {

        tv.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
        tv2.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);

    }

    void initRecyclerView() {

        mAdapter = new ItemAdapter(selectedAppsList, getApplicationContext(), mMainViewModel);
        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

}

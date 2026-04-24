package com.example.autorotate.UI;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autorotate.Model.AppsInfo;
import com.example.autorotate.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Locale;

public class AppSelectionAdapter extends RecyclerView.Adapter<AppSelectionAdapter.ViewHolder> {

    interface AppPickedListener {
        void onAppPicked(AppsInfo appInfo);
    }

    private final List<AppsInfo> allApps;
    private final List<AppsInfo> filteredApps;
    private final Set<String> selectedPackageSet;
    private final PackageManager packageManager;
    private final AppPickedListener appPickedListener;

    public AppSelectionAdapter(
            List<AppsInfo> allApps,
            Set<String> selectedPackageSet,
            PackageManager packageManager,
            AppPickedListener appPickedListener
    ) {
        this.allApps = new ArrayList<>(allApps);
        this.filteredApps = new ArrayList<>(allApps);
        this.selectedPackageSet = selectedPackageSet;
        this.packageManager = packageManager;
        this.appPickedListener = appPickedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppsInfo appInfo = filteredApps.get(position);
        holder.appName.setText(appInfo.getAppName());

        try {
            Drawable icon = packageManager.getApplicationIcon(appInfo.getAppPackageName());
            holder.appIcon.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        boolean alreadyAdded = selectedPackageSet.contains(appInfo.getAppPackageName());
        holder.actionIcon.setImageResource(alreadyAdded ? R.drawable.ms_tune_24 : R.drawable.ms_add_24);
        holder.actionIcon.setAlpha(alreadyAdded ? 0.85f : 1f);

        holder.itemView.setOnClickListener(v -> {
            if (appPickedListener != null) {
                appPickedListener.onAppPicked(appInfo);
            }
        });
    }

    void submitQuery(String query) {
        filteredApps.clear();
        if (TextUtils.isEmpty(query)) {
            filteredApps.addAll(allApps);
            notifyDataSetChanged();
            return;
        }

        String needle = query.toLowerCase(Locale.ROOT).trim();
        for (AppsInfo appsInfo : allApps) {
            String appName = appsInfo.getAppName() != null ? appsInfo.getAppName() : "";
            String packageName = appsInfo.getAppPackageName() != null ? appsInfo.getAppPackageName() : "";
            if (appName.toLowerCase(Locale.ROOT).contains(needle)
                    || packageName.toLowerCase(Locale.ROOT).contains(needle)) {
                filteredApps.add(appsInfo);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return filteredApps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView appIcon;
        private final TextView appName;
        private final ImageView actionIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.selection_app_icon);
            appName = itemView.findViewById(R.id.selection_app_name);
            actionIcon = itemView.findViewById(R.id.selection_app_action);
        }
    }
}

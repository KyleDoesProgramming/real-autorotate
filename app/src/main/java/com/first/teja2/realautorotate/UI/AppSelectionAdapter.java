package com.first.teja2.realautorotate.UI;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.first.teja2.realautorotate.Model.AppsInfo;
import com.first.teja2.realautorotate.R;

import java.util.List;
import java.util.Set;

public class AppSelectionAdapter extends RecyclerView.Adapter<AppSelectionAdapter.ViewHolder> {

    private final List<AppsInfo> itemList;
    private final Set<String> selectedPackageSet;
    private final PackageManager packageManager;

    public AppSelectionAdapter(List<AppsInfo> itemList, Set<String> selectedPackageSet, PackageManager packageManager) {
        this.itemList = itemList;
        this.selectedPackageSet = selectedPackageSet;
        this.packageManager = packageManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppsInfo appInfo = itemList.get(position);
        holder.appName.setText(appInfo.getAppName());

        try {
            Drawable icon = packageManager.getApplicationIcon(appInfo.getAppPackageName());
            holder.appIcon.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        boolean checked = selectedPackageSet.contains(appInfo.getAppPackageName());
        holder.checkBox.setChecked(checked);

        View.OnClickListener toggleSelection = v -> {
            if (selectedPackageSet.contains(appInfo.getAppPackageName())) {
                selectedPackageSet.remove(appInfo.getAppPackageName());
                holder.checkBox.setChecked(false);
            } else {
                selectedPackageSet.add(appInfo.getAppPackageName());
                holder.checkBox.setChecked(true);
            }
        };

        holder.itemView.setOnClickListener(toggleSelection);
        holder.checkBox.setOnClickListener(toggleSelection);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView appIcon;
        private final TextView appName;
        private final CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.selection_app_icon);
            appName = itemView.findViewById(R.id.selection_app_name);
            checkBox = itemView.findViewById(R.id.selection_app_checkbox);
        }
    }
}

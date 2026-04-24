package com.example.autorotate.UI;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.autorotate.Model.AppsInfo;
import com.example.autorotate.R;
import com.example.autorotate.Service.realAutorotateService;
import com.example.autorotate.ViewModel.MainViewModel;

import java.util.List;

/**
 * Adapter for the RecyclerView
 * -
 * Created by
 * Bala Guna Teja Karlapudi
 */


public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    List<AppsInfo> itemList;
    Context context;
    MainViewModel mainViewModel;

    public ItemAdapter(List<AppsInfo> itemList, Context context, MainViewModel mainViewModel) {
        this.itemList = itemList;
        this.context = context;
        this.mainViewModel = mainViewModel;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        AppsInfo appInfo = itemList.get(position);

        holder.name.setText(appInfo.getAppName());
        holder.mode.setText(getRotationModeLabel(appInfo));

        if (appInfo.isDefaultEntry()) {
            holder.delete.setVisibility(View.GONE);
            holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
        } else {
            holder.delete.setVisibility(View.VISIBLE);
            try {
                Drawable icon = context.getPackageManager().getApplicationIcon(appInfo.getAppPackageName());
                holder.appIcon.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }

        holder.itemView.setOnClickListener(v -> openRotationConfig(appInfo));

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return;
                }

                mainViewModel.setSelectedApps(context, itemList, itemList.get(adapterPosition));
                context.stopService(new Intent(context, realAutorotateService.class));
                context.startService(new Intent(context, realAutorotateService.class));
            }
        });

        holder.appIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRotationConfig(appInfo);
            }
        });

    }

    private void openRotationConfig(AppsInfo appInfo) {
        Intent intent = new Intent(context, AppRotationConfigActivity.class);
        intent.putExtra(AppRotationConfigActivity.EXTRA_APP_NAME, appInfo.getAppName());
        intent.putExtra(AppRotationConfigActivity.EXTRA_APP_PACKAGE, appInfo.getAppPackageName());
        intent.putExtra(AppRotationConfigActivity.EXTRA_IS_DEFAULT, appInfo.isDefaultEntry());
        context.startActivity(intent);
    }

    private String getRotationModeLabel(AppsInfo appInfo) {
        int rotationMode = appInfo.getRotationMode();
        if (rotationMode == AppsInfo.MODE_ALLOW_ROTATION) {
            return context.getString(R.string.rotation_rule_force_rotate);
        } else if (rotationMode == AppsInfo.MODE_LOCK_ROTATION) {
            return context.getString(R.string.rotation_rule_force_lock);
        }
        return context.getString(R.string.rotation_rule_follow_default);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView name;
        private TextView mode;
        private ImageView delete;
        private ImageView appIcon;

        public ViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.title);
            mode = itemView.findViewById(R.id.rotationMode);
            delete = itemView.findViewById(R.id.deleteApp);
            appIcon = itemView.findViewById(R.id.appIcon);

        }
    }

}



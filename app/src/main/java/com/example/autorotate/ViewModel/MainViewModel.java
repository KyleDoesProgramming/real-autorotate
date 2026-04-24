package com.example.autorotate.ViewModel;

/**
 * View Model, which accesses data from the repository
 * -
 * Created by
 * Bala Guna Teja Karlapudi
 */


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;

import com.example.autorotate.Model.AppsInfo;

import java.util.List;

public class MainViewModel extends ViewModel {

    private MutableLiveData<List<AppsInfo>> mSavedApps;
    private AppsRepository mRepo;

    public void initSavedApps(Context context) {

        if (mSavedApps != null && mSavedApps.getValue().size()>0) {
            return;
        }

        mRepo = AppsRepository.getInstance();
        mSavedApps = mRepo.getSavedApps(context);

    }

    public void setSelectedApps(Context context, List<AppsInfo> apps, AppsInfo appsInfo) {

        mSavedApps = mRepo.setSavedApps(context, apps, appsInfo);

    }

    public LiveData<List<AppsInfo>> getSavedApps() {
        return mSavedApps;
    }

}

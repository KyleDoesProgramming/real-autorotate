package com.example.autorotate.Util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.autorotate.IRotationService;
import com.example.autorotate.R;
import com.example.autorotate.Service.RotationService;

import rikka.shizuku.Shizuku;

public final class RotationBackend {

    private static final String TAG = "RotationBackend";

    private static final Object LOCK = new Object();

    private static IRotationService service;
    private static boolean bindingRequested;

    private static final Shizuku.UserServiceArgs USER_SERVICE_ARGS = new Shizuku.UserServiceArgs(
            new ComponentName("com.example.autorotate", RotationService.class.getName()))
            .daemon(false)
            .processNameSuffix("rotation")
            .tag("rotation");

    private static final ServiceConnection CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            synchronized (LOCK) {
                service = IRotationService.Stub.asInterface(binder);
                bindingRequested = false;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (LOCK) {
                service = null;
                bindingRequested = false;
            }
        }
    };

    private RotationBackend() {
    }

    @NonNull
    private static OperationMode resolveMode(@NonNull Context context) {
        OperationMode mode = OperationModeStore.get(context);
        if (mode == OperationMode.AUTO) {
            return OperationModeStore.detectMode();
        }

        return mode;
    }

    public static boolean canUsePrivileged(@NonNull Context context) {
        OperationMode mode = resolveMode(context);
        if (mode == OperationMode.ACCESSIBILITY) {
            return false;
        }

        try {
            if (Shizuku.getBinder() == null) {
                return false;
            }

            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return false;
            }

            int uid = Shizuku.getUid();
            if (mode == OperationMode.ROOT) {
                return uid == 0;
            }

            return uid == 2000;
        } catch (Throwable tr) {
            Log.d(TAG, "Privileged backend unavailable", tr);
            return false;
        }
    }

    public static void ensureBound(@NonNull Context context) {
        if (!canUsePrivileged(context)) {
            return;
        }

        synchronized (LOCK) {
            if (service != null || bindingRequested) {
                return;
            }

            try {
                if (Shizuku.getVersion() < 10) {
                    return;
                }

                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                int versionCode;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    versionCode = (int) packageInfo.getLongVersionCode();
                } else {
                    versionCode = packageInfo.versionCode;
                }

                boolean debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                USER_SERVICE_ARGS.version(versionCode).debuggable(debuggable);

                bindingRequested = true;
                Shizuku.bindUserService(USER_SERVICE_ARGS, CONNECTION);
            } catch (Throwable tr) {
                bindingRequested = false;
                Log.d(TAG, "Unable to bind privileged service", tr);
            }
        }
    }

    public static boolean apply(@NonNull Context context, int accelerometerRotation, int userRotation) {
        OperationMode mode = resolveMode(context);
        if (mode != OperationMode.ACCESSIBILITY) {
            ensureBound(context);
            if (applyPrivileged(accelerometerRotation, userRotation)) {
                return true;
            }
        }

        return RotationSettings.apply(context, accelerometerRotation, userRotation);
    }

    @NonNull
    public static String describeBackend(@NonNull Context context) {
        OperationMode storedMode = OperationModeStore.get(context);
        OperationMode mode = storedMode == OperationMode.AUTO ? OperationModeStore.detectMode() : storedMode;

        if (storedMode == OperationMode.AUTO && mode == OperationMode.ACCESSIBILITY) {
            return context.getString(R.string.backend_status_auto_unavailable);
        }

        if (storedMode == OperationMode.AUTO) {
            if (mode == OperationMode.ROOT) {
                return context.getString(R.string.backend_status_root_ready);
            }

            if (mode == OperationMode.SHIZUKU) {
                return context.getString(R.string.backend_status_shizuku_ready);
            }

            return context.getString(R.string.backend_status_auto_unavailable);
        }

        if (mode == OperationMode.ACCESSIBILITY) {
            return context.getString(R.string.backend_status_accessibility);
        }

        if (canUsePrivileged(context)) {
            if (mode == OperationMode.ROOT) {
                return context.getString(R.string.backend_status_root_ready);
            }

            return context.getString(R.string.backend_status_shizuku_ready);
        }

        if (mode == OperationMode.ROOT) {
            return context.getString(R.string.backend_status_root_unavailable);
        }

        return context.getString(R.string.backend_status_shizuku_unavailable);
    }

    private static boolean applyPrivileged(int accelerometerRotation, int userRotation) {
        synchronized (LOCK) {
            if (service == null) {
                return false;
            }

            try {
                service.applyRotation(accelerometerRotation, userRotation);
                return true;
            } catch (Throwable tr) {
                Log.d(TAG, "Privileged rotation update failed", tr);
                service = null;
                bindingRequested = false;
                return false;
            }
        }
    }
}
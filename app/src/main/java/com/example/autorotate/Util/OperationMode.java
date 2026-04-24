package com.example.autorotate.Util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum OperationMode {

    AUTO("auto"),
    ACCESSIBILITY("accessibility"),
    ROOT("root"),
    SHIZUKU("shizuku");

    private final String value;

    OperationMode(String value) {
        this.value = value;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    @NonNull
    public static OperationMode fromValue(@Nullable String value) {
        if (AUTO.value.equals(value)) {
            return AUTO;
        }

        if (ROOT.value.equals(value)) {
            return ROOT;
        }

        if (SHIZUKU.value.equals(value)) {
            return SHIZUKU;
        }

        return AUTO;
    }
}
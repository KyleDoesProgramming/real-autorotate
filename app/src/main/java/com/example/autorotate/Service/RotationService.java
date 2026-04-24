package com.example.autorotate.Service;

import com.example.autorotate.IRotationService;

import java.io.IOException;

public class RotationService extends IRotationService.Stub {

    @Override
    public void applyRotation(int accelerometerRotation, int userRotation) {
        try {
            applyWindowRotation(accelerometerRotation, userRotation);
            runSettings("accelerometer_rotation", String.valueOf(accelerometerRotation));
            runSettings("user_rotation", String.valueOf(userRotation));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    private void applyWindowRotation(int accelerometerRotation, int userRotation) throws InterruptedException {
        if (accelerometerRotation == 1) {
            runCommandAllowFailure("/system/bin/cmd", "window", "user-rotation", "free");
            return;
        }

        runCommandAllowFailure("/system/bin/cmd", "window", "user-rotation", "lock", String.valueOf(userRotation));
    }

    private void runCommandAllowFailure(String... command) throws InterruptedException {
        try {
            runCommand(command);
        } catch (IOException ignored) {
        }
    }

    private void runSettings(String key, String value) throws IOException, InterruptedException {
        int exitCode = runCommand("/system/bin/settings", "put", "system", key, value);
        if (exitCode != 0) {
            throw new IOException("settings exited with " + exitCode + " for " + key);
        }
    }

    private int runCommand(String... command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        return process.waitFor();
    }
}
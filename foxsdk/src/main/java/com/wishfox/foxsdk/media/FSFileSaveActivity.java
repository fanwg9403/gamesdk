package com.wishfox.foxsdk.media;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** API 21～28 图片保存的透明系统文件选择器代理页。 */
public final class FSFileSaveActivity extends Activity {
    public static final String EXTRA_PICKER_ID = "fs_picker_id";
    private static final int REQUEST_CREATE_DOCUMENT = 0x4701;

    private String pickerId;
    private boolean pickerStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickerId = getIntent() == null ? null : getIntent().getStringExtra(EXTRA_PICKER_ID);
        FSMediaSaveCoordinator.PickerRequest request = FSMediaSaveCoordinator.picker(pickerId);
        if (request == null) {
            finish();
            return;
        }
        pickerStarted = savedInstanceState != null && savedInstanceState.getBoolean("picker_started", false);
        if (!pickerStarted) openPicker(request);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("picker_started", pickerStarted);
    }

    private void openPicker(FSMediaSaveCoordinator.PickerRequest request) {
        try {
            pickerStarted = true;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(request.mimeType)
                    .putExtra(Intent.EXTRA_TITLE, request.fileName)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CREATE_DOCUMENT);
        } catch (Throwable failure) {
            FSMediaSaveCoordinator.completePicker(
                    pickerId, false, null, "GALLERY_WRITE_FAILED");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CREATE_DOCUMENT) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            FSMediaSaveCoordinator.completePicker(pickerId, false, null, "USER_CANCELLED");
            finish();
            return;
        }
        Uri target = data.getData();
        FSMediaSaveCoordinator.PickerRequest request = FSMediaSaveCoordinator.picker(pickerId);
        if (request == null) {
            finish();
            return;
        }
        ExecutorService worker = Executors.newSingleThreadExecutor();
        worker.execute(() -> {
            boolean success = false;
            try (InputStream input = new FileInputStream(request.source);
                 OutputStream output = getContentResolver().openOutputStream(target, "w")) {
                if (output == null) throw new IllegalStateException("output unavailable");
                byte[] buffer = new byte[32768];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                output.flush();
                success = true;
            } catch (Throwable ignored) { }
            boolean completed = success;
            runOnUiThread(() -> {
                FSMediaSaveCoordinator.completePicker(pickerId, completed,
                        completed ? target.toString() : null,
                        completed ? null : "GALLERY_WRITE_FAILED");
                finish();
            });
            worker.shutdown();
        });
    }

    @Override
    public void onBackPressed() {
        if (!pickerStarted) {
            FSMediaSaveCoordinator.completePicker(pickerId, false, null, "USER_CANCELLED");
        }
        super.onBackPressed();
    }
}

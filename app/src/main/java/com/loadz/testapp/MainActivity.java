package com.loadz.testapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.loadz.AbstractLoadzListener;
import com.loadz.Download;
import com.loadz.DownloaderType;
import com.loadz.Loadz;
import com.loadz.LoadzConfiguration;
import com.loadz.LoadzRequest;
import com.loadz.hls.HlsVariant;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Loadz loadz;
    private DownloadAdapter adapter;

    private EditText urlInput, fileNameInput, headersInput;
    private RecyclerView recyclerView;
    private Button btnDownloadNormal, btnDownloadHls;

    // FIX 3: Define the listener as a class variable so we can safely remove it on destroy
    private final AbstractLoadzListener globalListener = new AbstractLoadzListener() {
        @Override
        public void onQueued(@NotNull Download download) {
            adapter.addOrUpdate(download);
        }

        @Override
        public void onProgress(@NotNull Download download, int progress) {
            adapter.addOrUpdate(download);
        }

        @Override
        public void onConversion(@NotNull Download download, int progress) {
            adapter.addOrUpdate(download);
        }

        @Override
        public void onPaused(@NotNull Download download) {
            adapter.addOrUpdate(download);
        }

        @Override
        public void onResumed(@NotNull Download download) {
            adapter.addOrUpdate(download);
        }

        @Override
        public void onCompleted(@NotNull Download download) {
            adapter.addOrUpdate(download);
        }

        @Override
        public void onCancelled(@NotNull Download download) {
            adapter.addOrUpdate(download);
        }

        @Override
        public void onFailed(@NotNull Download download, @NotNull String error) {
            adapter.addOrUpdate(download);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();

        LoadzConfiguration config = new LoadzConfiguration.Builder(this)
                .setNamespace("test_app_downloads")
                .enableLogging(true)
                .build();

        Loadz.Companion.setDefaultConfiguration(config);
        loadz = Loadz.Companion.getDefaultInstance();

        urlInput = findViewById(R.id.urlInput);
        fileNameInput = findViewById(R.id.fileNameInput);
        headersInput = findViewById(R.id.headersInput);

        btnDownloadNormal = findViewById(R.id.btnDownloadNormal);
        btnDownloadHls = findViewById(R.id.btnDownloadHls);
        recyclerView = findViewById(R.id.recyclerView);

        findViewById(R.id.btnViewLogs).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LogActivity.class))
        );

        adapter = new DownloadAdapter(new DownloadAdapter.DownloadActions() {
            @Override
            public void onPause(int id) {
                loadz.pause(id);
            }

            @Override
            public void onResume(int id) {
                loadz.resume(id);
            }

            @Override
            public void onCancel(int id) {
                loadz.cancel(id);
            }

            @Override
            public void onRemove(int id) {
                loadz.remove(id);
                adapter.remove(id);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        String normalDemoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";
        String hlsDemoUrl = "https://sderzk.ydc1wes.me/hls2/01/00011/1lujibe9qe3u_l/index-v1-a1.m3u8?t=juFqxgWa8oRfmCHjWX3LMZ2bj2duIFL0zCx1vvFcnJE&s=1791875947&e=21600&f=55637&i=0.0&sp=0";

        urlInput.setText(hlsDemoUrl);
        headersInput.setText("{\"Referer\": \"https://speedostream1.com/\"}");

        // Attach the global listener
        loadz.addListener(globalListener);

        btnDownloadNormal.setOnClickListener(v -> {
            debounceButton(btnDownloadNormal); // FIX 1: Prevent double-tap popups

            String url = urlInput.getText().toString();
            if (url.isEmpty()) url = normalDemoUrl;

            String fileName = getFileName(url, ".mp4");
            int uniqueId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            startDownload(url, DownloaderType.NORMAL, fileName, uniqueId, getHeaders());
        });

        btnDownloadHls.setOnClickListener(v -> {
            debounceButton(btnDownloadHls); // FIX 1: Prevent double-tap popups

            String inputUrl = urlInput.getText().toString();
            if (inputUrl.isEmpty()) inputUrl = hlsDemoUrl;

            final String finalUrl = inputUrl;
            String fileNameStr = getFileName(finalUrl, ".mp4");
            if (fileNameStr.endsWith(".m3u8") || fileNameStr.endsWith(".ts")) {
                fileNameStr = fileNameStr.substring(0, fileNameStr.lastIndexOf('.')) + ".mp4";
            }
            final String fileName = fileNameStr;
            Map<String, String> headers = getHeaders();
            int uniqueId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

            loadz.fetchHlsQualities(finalUrl, headers, (variants, exception) -> {
                if (isFinishing() || isDestroyed())
                    return kotlin.Unit.INSTANCE; // FIX 4: Prevent async dialog crashes

                if (exception != null || variants == null || variants.isEmpty()) {
                    startDownload(finalUrl, DownloaderType.HLS, fileName, uniqueId, headers);
                } else {
                    CharSequence[] options = new CharSequence[variants.size()];
                    for (int i = 0; i < variants.size(); i++) {
                        options[i] = variants.get(i).getDisplayName();
                    }

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Select Quality")
                            .setItems(options, (dialog, which) -> {
                                HlsVariant selected = variants.get(which);
                                startDownload(selected.getUrl(), DownloaderType.HLS, fileName, uniqueId, headers);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
                return kotlin.Unit.INSTANCE;
            });
        });

        loadz.getDownloads(downloads -> {
            for (Download d : downloads) {
                adapter.addOrUpdate(d);
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    // Helper to prevent rapid double-clicks on buttons
    private void debounceButton(Button button) {
        button.setEnabled(false);
        button.postDelayed(() -> button.setEnabled(true), 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // FIX 3: Clean up the listener so rotating the phone doesn't leak memory
        if (loadz != null) {
            loadz.removeListener(globalListener);
        }
    }

    private void startDownload(String url, DownloaderType type, String fileName, int uniqueId, Map<String, String> headers) {
        File dir = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager())
                ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                : getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        if (dir != null && !dir.exists()) dir.mkdirs();

        File targetFile = new File(dir, fileName);

        LoadzRequest request = new LoadzRequest(
                url, targetFile.getAbsolutePath(), uniqueId, "test_app", type,
                headers, com.loadz.NetworkType.ALL, com.loadz.Priority.NORMAL, 0
        );

        loadz.enqueue(request,
                req -> kotlin.Unit.INSTANCE,
                err -> {
                    if (err == com.loadz.LoadzError.FILE_ALREADY_EXISTS) {
                        if (!isFinishing() && !isDestroyed()) { // FIX 4: Prevent async dialog crashes
                            showRenameDialog(url, type, fileName, headers);
                        }
                    }
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    private void showRenameDialog(String url, DownloaderType type, String originalFileName, Map<String, String> headers) {
        EditText input = new EditText(this);
        input.setText(suggestNewFileName(originalFileName));
        input.setSingleLine(true);

        // Make the EditText look professional with margins
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, 0, margin, 0);
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("File Already Exists")
                .setMessage("A file with this name already exists. Please choose a new name:")
                .setView(container)
                .setPositiveButton("Download", (dialog, which) -> {
                    String newFileName = input.getText().toString().trim();
                    if (!newFileName.isEmpty()) {
                        // FIX 2: Generate a brand new ID for the retry attempt to avoid database constraint crashes
                        int freshUniqueId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                        startDownload(url, type, newFileName, freshUniqueId, headers);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getFileName(String url, String fallbackExt) {
        String inputName = fileNameInput.getText().toString().trim();
        if (!inputName.isEmpty()) return inputName;

        String guessedName = URLUtil.guessFileName(url, null, null);
        if (guessedName == null || guessedName.isEmpty() || guessedName.equals("downloadfile")) {
            guessedName = "download_" + System.currentTimeMillis() + fallbackExt;
        } else if (guessedName.endsWith(".m3u8")) {
            guessedName = guessedName.substring(0, guessedName.lastIndexOf('.')) + fallbackExt;
        }
        return guessedName;
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        String input = headersInput.getText().toString().trim();
        if (input.isEmpty()) return headers;

        try {
            JSONObject json = new JSONObject(input);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                headers.put(key, json.getString(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return headers;
    }

    private String suggestNewFileName(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex == -1) {
            return originalFileName + " (1)";
        }

        String nameWithoutExt = originalFileName.substring(0, dotIndex);
        String ext = originalFileName.substring(dotIndex);

        if (nameWithoutExt.matches(".* \\(\\d+\\)$")) {
            int openParen = nameWithoutExt.lastIndexOf("(");
            int closeParen = nameWithoutExt.lastIndexOf(")");
            String numStr = nameWithoutExt.substring(openParen + 1, closeParen);
            try {
                int num = Integer.parseInt(numStr);
                return nameWithoutExt.substring(0, openParen) + "(" + (num + 1) + ")" + ext;
            } catch (NumberFormatException ignored) {
            }
        }

        return nameWithoutExt + " (1)" + ext;
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 101);
            }
        }
    }
}
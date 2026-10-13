package com.loadz.testapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.loadz.Loadz;
import com.loadz.Status;

import java.util.ArrayList;

public class LogActivity extends AppCompatActivity {

    private LogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new LogAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadFailedDownloads();
    }

    private void loadFailedDownloads() {
        Loadz loadz = Loadz.Companion.getDefaultInstance();
        if (loadz != null) {
            loadz.getDownloadsWithStatus(Status.FAILED, downloads -> {
                adapter.updateData(downloads);
                return kotlin.Unit.INSTANCE;
            });
        }
    }
}
package com.loadz.testapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.loadz.Download;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private List<Download> failedDownloads;

    public LogAdapter(List<Download> failedDownloads) {
        this.failedDownloads = failedDownloads;
    }

    public void updateData(List<Download> newDownloads) {
        this.failedDownloads = newDownloads;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        Download download = failedDownloads.get(position);
        
        holder.titleTextView.setText(download.getFilePath() != null ? download.getFilePath() : download.getUrl());
        holder.sizeTextView.setText("ID: " + download.getId());
        
        if (download.getError() != null) {
            String errorName = download.getError().name();
            String fullStatus = "Reason: " + errorName;
            
            if (download.getError().getThrowable() != null) {
                String exceptionMsg = download.getError().getThrowable().getMessage();
                if (exceptionMsg != null && !exceptionMsg.isEmpty()) {
                    fullStatus += "\nDetails: " + exceptionMsg;
                }
            }
            holder.statusTextView.setText(fullStatus);
            holder.statusTextView.setMaxLines(6); // allow error text to wrap
            holder.statusTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        } else {
            holder.statusTextView.setText("Unknown Error");
        }

        holder.itemView.setOnClickListener(v -> {
            String fullError = "Failed: " + download.getUrl() + "\n";
            if (download.getError() != null) {
                fullError += download.getError().name();
                if (download.getError().getThrowable() != null) {
                    fullError += "\n" + download.getError().getThrowable().toString();
                }
            }
            Toast.makeText(v.getContext(), fullError, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public int getItemCount() {
        return failedDownloads == null ? 0 : failedDownloads.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView sizeTextView;
        TextView statusTextView;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            sizeTextView = itemView.findViewById(R.id.sizeTextView);
            statusTextView = itemView.findViewById(R.id.status_TextView);
        }
    }
}
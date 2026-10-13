package com.loadz.testapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.loadz.Download;
import com.loadz.DownloaderType;
import com.loadz.Status;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.DownloadViewHolder> {

    private final List<Download> downloads = new ArrayList<>();
    private final DownloadActions actions;

    // FIX 2: Declare DecimalFormat once to prevent memory leaks and UI lag during progress updates
    private static final DecimalFormat df = new DecimalFormat("#,##0.#");
    private static final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};

    public interface DownloadActions {
        void onPause(int id);

        void onResume(int id);

        void onCancel(int id);

        void onRemove(int id);
    }

    public DownloadAdapter(DownloadActions actions) {
        this.actions = actions;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addOrUpdate(Download download) {
        for (int i = 0; i < downloads.size(); i++) {
            if (downloads.get(i).getId() == download.getId()) {
                downloads.set(i, download);
                notifyItemChanged(i);
                return;
            }
        }
        downloads.add(0, download);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_card, parent, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        Download download = downloads.get(position);

        File file = new File(download.getFilePath());
        holder.titleTextView.setText(file.getName());
        holder.sizeTextView.setText("(" + download.getDownloaderType().name() + ")");

        String statusText = download.getStatus().name();
        String downloadedSizeText = "";

        if (download.getStatus() == Status.CONVERTING) {
            statusText = "Converting... " + download.getProgress() + "%";
            if (download.getTotalBytes() > 0) {
                downloadedSizeText = formatBytes(download.getTotalBytes()) + " (Total est)";
            }
        } else if (download.getStatus() == Status.DOWNLOADING) {
            if (download.getDownloadedBytes() > 0) {
                String downloadedStr = formatBytes(download.getDownloadedBytes());
                String totalStr = formatBytes(download.getTotalBytes());
                downloadedSizeText = downloadedStr + " / " + totalStr;
                if (download.getDownloaderType() == DownloaderType.HLS) {
                    downloadedSizeText += " (~est)";
                }
            }
            if (download.getBytesPerSecond() > 0) {
                statusText = formatBytes(download.getBytesPerSecond()) + "/s";
            } else {
                statusText = "DOWNLOADING - " + download.getProgress() + "%";
            }
        } else if (download.getStatus() == Status.COMPLETED) {
            statusText = "COMPLETED";
            if (download.getTotalBytes() > 0) {
                downloadedSizeText = formatBytes(download.getTotalBytes());
            } else if (file.exists()) {
                downloadedSizeText = formatBytes(file.length());
            }
        } else if (download.getStatus() == Status.FAILED && download.getError() != null) {
            statusText = download.getError().name();
        }

        holder.statusTextView.setText(statusText);
        holder.downloadedSizeTextView.setText(downloadedSizeText);
        holder.progressBar.setProgress(download.getProgress());

        // FIX 1: Reset the click listener first to prevent RecyclerView recycling bugs
        holder.actionButton.setOnClickListener(null);

        // Action Button States
        boolean canPause = download.getStatus() == Status.DOWNLOADING || download.getStatus() == Status.QUEUED;
        boolean canResume = download.getStatus() == Status.PAUSED || download.getStatus() == Status.FAILED;
        boolean isConverting = download.getStatus() == Status.CONVERTING;
        boolean isCancelled = download.getStatus() == Status.CANCELLED;

        if (canPause) {
            holder.actionButton.setImageResource(R.drawable.ic_pause_circle);
            holder.actionButton.setOnClickListener(v -> actions.onPause(download.getId()));
        } else if (canResume) {
            holder.actionButton.setImageResource(R.drawable.ic_play_circle);
            holder.actionButton.setOnClickListener(v -> actions.onResume(download.getId()));
        } else if (isConverting) {
            // FIX 3: Explicitly handle CONVERTING so users can cancel FFmpeg, and to prevent recycled Share buttons
            holder.actionButton.setImageResource(R.drawable.ic_cancel);
            holder.actionButton.setOnClickListener(v -> actions.onCancel(download.getId()));
        } else if (download.getStatus() == Status.COMPLETED) {
            holder.actionButton.setImageResource(R.drawable.ic_share);
            holder.actionButton.setOnClickListener(v -> shareFile(v.getContext(), file));
        } else if (isCancelled) {
            holder.actionButton.setImageResource(R.drawable.ic_cancel);
            holder.actionButton.setOnClickListener(v -> actions.onRemove(download.getId()));
        }

        holder.itemView.setOnClickListener(v -> {
            if (download.getStatus() == Status.COMPLETED) {
                openFile(v.getContext(), file);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 0, "Delete");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    actions.onRemove(download.getId());
                    return true;
                }
                return false;
            });
            popup.show();
            return true;
        });
    }

    // FIX 4: Use dynamic MimeTypes to prevent intent crashes on unknown files
    private String getMimeType(File file) {
        String mimeType = "*/*";
        String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        if (extension != null && !extension.isEmpty()) {
            String mappedMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (mappedMime != null) mimeType = mappedMime;
        }
        return mimeType;
    }

    private void shareFile(Context context, File file) {
        if (!file.exists()) return;
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(getMimeType(file));
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share File"));
        } catch (Exception e) {
            Toast.makeText(context, "Could not share file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(Context context, File file) {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, getMimeType(file));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void remove(int id) {
        for (int i = 0; i < downloads.size(); i++) {
            if (downloads.get(i).getId() == id) {
                downloads.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    static class DownloadViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon, actionButton;
        TextView titleTextView, sizeTextView, downloadedSizeTextView, statusTextView;
        ProgressBar progressBar;

        public DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            sizeTextView = itemView.findViewById(R.id.sizeTextView);
            progressBar = itemView.findViewById(R.id.progressBar);
            actionButton = itemView.findViewById(R.id.actionButton);
            downloadedSizeTextView = itemView.findViewById(R.id.downloadedSizeTextView);
            statusTextView = itemView.findViewById(R.id.status_TextView);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return df.format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
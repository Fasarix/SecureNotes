package com.example.securenotes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securenotes.R;
import com.example.securenotes.model.relation.FileWithTag;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FilesAdapter extends ListAdapter<FileWithTag, FilesAdapter.FileViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(FileWithTag file);
    }

    public interface OnFileOptionsClickListener {
        void onFileOptionsClick(FileWithTag file);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private OnFileClickListener clickListener;
    private OnFileOptionsClickListener optionsClickListener;
    private OnSelectionChangedListener selectionChangedListener;

    private final Set<Long> selectedFileIds = new HashSet<>();

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnFileOptionsClickListener(OnFileOptionsClickListener listener) {
        this.optionsClickListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void clearSelection() {
        if (!selectedFileIds.isEmpty()) {
            List<Long> toNotify = new ArrayList<>(selectedFileIds);
            selectedFileIds.clear();
            for (Long id : toNotify) {
                int pos = findPositionById(id);
                if (pos != -1) notifyItemChanged(pos);
            }
            notifySelectionChanged();
        }
    }

    public List<Long> getSelectedFileIds() {
        return new ArrayList<>(selectedFileIds);
    }

    public boolean isSelectionMode() {
        return !selectedFileIds.isEmpty();
    }

    private void notifySelectionChanged() {
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedFileIds.size());
        }
    }

    private int findPositionById(long fileId) {
        List<FileWithTag> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            FileWithTag fwt = currentList.get(i);
            if (fwt.file != null && fwt.file.id != null && fwt.file.id == fileId) {
                return i;
            }
        }
        return -1;
    }

    public void toggleSelection(long fileId) {
        int pos = findPositionById(fileId);
        if (pos == -1) return;

        if (!selectedFileIds.add(fileId)) {
            selectedFileIds.remove(fileId);
        }

        notifyItemChanged(pos);
        notifySelectionChanged();
    }

    public FilesAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<FileWithTag> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull FileWithTag oldItem, @NonNull FileWithTag newItem) {
            return oldItem.file.id != null && oldItem.file.id.equals(newItem.file.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull FileWithTag oldItem, @NonNull FileWithTag newItem) {
            boolean sameTag = (oldItem.tag == null && newItem.tag == null)
                    || (oldItem.tag != null && oldItem.tag.equals(newItem.tag));

            return oldItem.file.decryptedTitle.equals(newItem.file.decryptedTitle)
                    && oldItem.file.decryptedFileType.equals(newItem.file.decryptedFileType)
                    && oldItem.file.decryptedFilePath.equals(newItem.file.decryptedFilePath)
                    && oldItem.file.creationDate.equals(newItem.file.creationDate)
                    && oldItem.file.lastModifiedDate.equals(newItem.file.lastModifiedDate)
                    && sameTag;
        }
    };

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileWithTag fileWithTag = getItem(position);
        boolean isSelected = selectedFileIds.contains(fileWithTag.file.id);
        holder.bind(fileWithTag, isSelected);
    }

    class FileViewHolder extends RecyclerView.ViewHolder {
        private final TextView fileNameTextView;
        private final TextView fileTypeTextView;
        private final TextView filePathTextView;
        private final TextView fileTagTextView;
        private final TextView fileCreatedTextView;
        private final TextView fileUpdatedTextView;
        private final View resIdView;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.file_title);
            fileTypeTextView = itemView.findViewById(R.id.file_type);
            filePathTextView = itemView.findViewById(R.id.file_path);
            fileTagTextView = itemView.findViewById(R.id.file_tag);
            fileCreatedTextView = itemView.findViewById(R.id.file_created);
            fileUpdatedTextView = itemView.findViewById(R.id.file_updated);
            resIdView = itemView.findViewById(R.id.file_tag_color);
            ImageView fileOptionsImageView = itemView.findViewById(R.id.file_options);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                FileWithTag file = getItem(pos);
                if (isSelectionMode()) {
                    toggleSelection(file.file.id);
                } else if (clickListener != null) {
                    clickListener.onFileClick(file);
                } else {
                    Toast.makeText(itemView.getContext(), "ClickListener not set!", Toast.LENGTH_SHORT).show();
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;

                FileWithTag file = getItem(pos);
                toggleSelection(file.file.id);
                return true;
            });

            fileOptionsImageView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                FileWithTag file = getItem(pos);
                if (isSelectionMode()) {
                    toggleSelection(file.file.id);
                } else if (optionsClickListener != null) {
                    optionsClickListener.onFileOptionsClick(file);
                } else {
                    Toast.makeText(itemView.getContext(), "Options click listener not set!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        public void bind(FileWithTag fileWithTag, boolean isSelected) {
            fileNameTextView.setText(fileWithTag.file.decryptedTitle != null ? fileWithTag.file.decryptedTitle : "");
            fileTypeTextView.setText("Tipo: " + (fileWithTag.file.decryptedFileType != null ? fileWithTag.file.decryptedFileType : ""));
            filePathTextView.setText("Path: " + (fileWithTag.file.decryptedFilePath != null ? fileWithTag.file.decryptedFilePath : ""));

            if (fileWithTag.tag != null) {
                fileTagTextView.setText(fileWithTag.tag.name);
                fileTagTextView.setVisibility(View.VISIBLE);
                resIdView.setBackgroundResource(fileWithTag.tag.iconId);
                resIdView.setVisibility(View.VISIBLE);
            } else {
                fileTagTextView.setText("");
                fileTagTextView.setVisibility(View.GONE);
                resIdView.setVisibility(View.GONE);
            }

            fileCreatedTextView.setText("Creato: " + dateFormat.format(new Date(fileWithTag.file.creationDate)));
            fileUpdatedTextView.setText("Modificato: " + dateFormat.format(new Date(fileWithTag.file.lastModifiedDate)));

            itemView.setSelected(isSelected);
        }
    }
}

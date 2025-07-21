package com.example.securenotes.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.securenotes.R;
import com.example.securenotes.model.relation.NoteWithTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NotesAdapter extends ListAdapter<NoteWithTag, NotesAdapter.NoteViewHolder> {

    public interface OnNoteClickListener {
        void onNoteClick(NoteWithTag noteWithTag);
    }

    public interface OnNoteLongClickListener {
        void onNoteLongClick(NoteWithTag noteWithTag);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private OnNoteClickListener clickListener;
    private OnNoteLongClickListener longClickListener;
    private OnSelectionChangedListener selectionChangedListener;

    private final Set<Long> selectedNoteIds = new HashSet<>();

    public void setOnNoteClickListener(OnNoteClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnNoteLongClickListener(OnNoteLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void clearSelection() {
        if (!selectedNoteIds.isEmpty()) {
            selectedNoteIds.clear();
            notifyDataSetChanged();
            notifySelectionChanged();
        }
    }

    public int getSelectedCount() {
        return selectedNoteIds.size();
    }

    public List<Long> getSelectedNoteIds() {
        return new ArrayList<>(selectedNoteIds);
    }

    public NotesAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteWithTag currentNoteWithTag = getItem(position);
        boolean isSelected = selectedNoteIds.contains(currentNoteWithTag.note.id);
        holder.bind(currentNoteWithTag, isSelected);
    }

    private static final DiffUtil.ItemCallback<NoteWithTag> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull NoteWithTag oldItem, @NonNull NoteWithTag newItem) {
            return oldItem.note.id == newItem.note.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull NoteWithTag oldItem, @NonNull NoteWithTag newItem) {
            return oldItem.note.id == newItem.note.id &&
                    oldItem.note.titleEncrypted.equals(newItem.note.titleEncrypted) &&
                    oldItem.note.contentEncrypted.equals(newItem.note.contentEncrypted) &&
                    ((oldItem.note.tagId == null && newItem.note.tagId == null) ||
                            (oldItem.note.tagId != null && oldItem.note.tagId.equals(newItem.note.tagId))) &&
                    oldItem.note.createdAt == newItem.note.createdAt &&
                    oldItem.note.updatedAt == newItem.note.updatedAt &&
                    (Objects.equals(oldItem.note.decryptedTitle, newItem.note.decryptedTitle)) &&
                    (Objects.equals(oldItem.note.decryptedContent, newItem.note.decryptedContent));
        }
    };

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView contentTextView;
        private final TextView tagTextView;
        private final View resIdView;
        private final TextView selfDestructTimerTextView;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.note_title);
            contentTextView = itemView.findViewById(R.id.note_content);
            tagTextView = itemView.findViewById(R.id.note_tag);
            resIdView = itemView.findViewById(R.id.note_color_indicator);
            selfDestructTimerTextView = itemView.findViewById(R.id.note_self_destruct_timer);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    NoteWithTag note = getItem(position);
                    if (getSelectedCount() == 0) {
                        if (clickListener != null) {
                            clickListener.onNoteClick(note);
                        }
                    } else {
                        toggleSelection(note.note.id);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    NoteWithTag note = getItem(position);
                    if (longClickListener != null) {
                        longClickListener.onNoteLongClick(note);
                    } else {
                        toggleSelection(note.note.id);
                    }
                    return true;
                }
                return false;
            });
        }

        public void bind(NoteWithTag noteWithTag, boolean isSelected) {
            titleTextView.setText(noteWithTag.note.decryptedTitle);
            contentTextView.setText(noteWithTag.note.decryptedContent);

            if (noteWithTag.tag != null) {
                tagTextView.setText(noteWithTag.tag.name);
                tagTextView.setVisibility(View.VISIBLE);
                resIdView.setBackgroundResource(noteWithTag.tag.iconId);
                resIdView.setVisibility(View.VISIBLE);
            } else {
                tagTextView.setText("");
                tagTextView.setVisibility(View.GONE);
                resIdView.setVisibility(View.GONE);
            }

            if (noteWithTag.note.selfDestructAt != null) {
                long millisLeft = noteWithTag.note.selfDestructAt - System.currentTimeMillis();
                if (millisLeft > 0) {
                    long hours = millisLeft / (1000 * 60 * 60);
                    long minutes = (millisLeft / (1000 * 60)) % 60;
                    StringBuilder timeText = new StringBuilder("Scade tra: ");
                    if (hours > 0) timeText.append(hours).append("h ");
                    timeText.append(minutes).append("m");
                    selfDestructTimerTextView.setText(timeText.toString());
                    selfDestructTimerTextView.setVisibility(View.VISIBLE);
                } else {
                    selfDestructTimerTextView.setText("Scaduta");
                    selfDestructTimerTextView.setVisibility(View.VISIBLE);
                }
            } else {
                selfDestructTimerTextView.setVisibility(View.GONE);
            }

            itemView.setSelected(isSelected);
        }
    }

    public void toggleSelection(long noteId) {
        int index = -1;
        List<NoteWithTag> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).note.id == noteId) {
                index = i;
                break;
            }
        }
        if (index == -1) return;

        if (selectedNoteIds.contains(noteId)) {
            selectedNoteIds.remove(noteId);
        } else {
            selectedNoteIds.add(noteId);
        }
        notifyItemChanged(index);
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedNoteIds.size());
        }
    }
}

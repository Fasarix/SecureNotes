package com.example.securenotes.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.securenotes.ui.home.FilesFragment;
import com.example.securenotes.ui.home.NotesFragment;
import com.example.securenotes.ui.home.SettingsFragment;

public class HomePagerAdapter extends FragmentStateAdapter {

    public HomePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch(position) {
            case 0: return new NotesFragment();
            case 1: return new FilesFragment();
            case 2 : return new SettingsFragment();
            default: return new NotesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}

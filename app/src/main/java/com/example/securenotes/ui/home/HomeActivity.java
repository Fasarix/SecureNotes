package com.example.securenotes.ui.home;

import static com.example.securenotes.utils.Constants.INITIAL_DELAY_SECONDS;

import android.os.Bundle;

import androidx.viewpager2.widget.ViewPager2;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.securenotes.R;
import com.example.securenotes.adapter.HomePagerAdapter;
import com.example.securenotes.ui.BaseActivity;
import com.example.securenotes.worker.BackupWorker;
import com.example.securenotes.worker.NoteCheckWorker;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.concurrent.TimeUnit;

public class HomeActivity extends BaseActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme(this);
        super.onCreate(savedInstanceState);
        System.loadLibrary("sqlcipher");

        setContentView(R.layout.activity_home);

        setupViewPagerAndTabs();
        scheduleSelfDestructWorker();
        scheduleBackupWorker();
    }

    private void setupViewPagerAndTabs() {
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        HomePagerAdapter adapter = new HomePagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0 : tab.setText("Note");break;
                case 1 : tab.setText("File");break;
                case 2 : tab.setText("Settings");break;
            }
        }).attach();
    }

    private void scheduleSelfDestructWorker() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(NoteCheckWorker.class, 15, TimeUnit.MINUTES)
                .build();

        OneTimeWorkRequest initialWork = new OneTimeWorkRequest.Builder(NoteCheckWorker.class)
                .setInitialDelay(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
                .build();

        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(initialWork);
        wm.enqueueUniquePeriodicWork("self_destruct_worker", ExistingPeriodicWorkPolicy.KEEP, periodicWork);
    }

    private void scheduleBackupWorker() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(BackupWorker.class, 1, TimeUnit.DAYS)
                .build();

        OneTimeWorkRequest initialWork = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setInitialDelay(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
                .build();

        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(initialWork);
        wm.enqueueUniquePeriodicWork("auto_backup_worker", ExistingPeriodicWorkPolicy.KEEP, periodicWork);
    }
}

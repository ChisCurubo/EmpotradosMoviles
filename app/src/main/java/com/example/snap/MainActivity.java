package com.example.snap;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupNavigation();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupNavigation() {
        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Disable swipe to change tab if desired, or keep it.
        // viewPager.setUserInputEnabled(false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNavigationView.setSelectedItemId(R.id.action_text);
                        break;
                    case 1:
                        bottomNavigationView.setSelectedItemId(R.id.action_camera);
                        break;
                    case 2:
                        bottomNavigationView.setSelectedItemId(R.id.action_audio);
                        break;
                }
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_text) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.action_camera) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.action_audio) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });

        // Set default selection (Audio or specific tab?)
        // Original app main feature seems to be Voice/Audio.
        // But the menu order is Text, Camera, Audio.
        // Let's stick to the menu order (Text=0, Camera=1, Audio=2).
        // Since this branch is "translation-voice", maybe default to Audio?
        // Let's default to Audio to match user context.
        viewPager.setCurrentItem(2, false);
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new TextFragment();
                case 1:
                    return new CameraFragment();
                case 2:
                    return new VoiceFragment();
                default:
                    return new VoiceFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}

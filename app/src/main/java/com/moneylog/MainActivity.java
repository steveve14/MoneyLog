package com.moneylog;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.moneylog.databinding.ActivityMainBinding;
import com.moneylog.util.LocaleHelper;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment =
            (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // 온보딩 완료 시 — 현재 온보딩에 있으면 대시보드로 스킵
            if (LocaleHelper.isOnboardingDone(this)
                    && navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.onboardingFragment) {
                navController.navigate(R.id.action_onboarding_to_dashboard);
            }

            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }
    }
}

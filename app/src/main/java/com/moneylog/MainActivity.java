package com.moneylog;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.RecyclerView;

import com.moneylog.databinding.ActivityMainBinding;
import com.moneylog.util.LocaleHelper;

import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ViewTreeObserver.OnScrollChangedListener currentScrollListener;
    private View currentScrollTarget;

    private static final Set<Integer> MAIN_DESTINATIONS = Set.of(
            R.id.dashboardFragment, R.id.transactionFragment,
            R.id.statisticsFragment, R.id.settingsFragment);

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

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.onboardingFragment) {
                    binding.bottomNavigation.setVisibility(View.GONE);
                    binding.fabScrollTop.setVisibility(View.GONE);
                } else {
                    binding.bottomNavigation.setVisibility(View.VISIBLE);
                }
                // 페이지 전환 시 FAB 숨기고 이전 리스너 제거
                binding.fabScrollTop.setVisibility(View.GONE);
                detachScrollListener();

                // 메인 탭이면 스크롤 리스너 재연결 (뷰가 준비된 후)
                if (MAIN_DESTINATIONS.contains(destination.getId())) {
                    binding.getRoot().post(this::attachScrollListener);
                }
            });
        }

        binding.fabScrollTop.setOnClickListener(v -> scrollToTop());
    }

    private void attachScrollListener() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;
        View fragmentView = null;
        if (navHostFragment.getChildFragmentManager().getFragments().size() > 0) {
            fragmentView = navHostFragment.getChildFragmentManager().getFragments().get(0).getView();
        }
        if (fragmentView == null) return;

        // ScrollView 찾기
        ScrollView scrollView = findScrollView(fragmentView);
        if (scrollView != null) {
            currentScrollTarget = scrollView;
            currentScrollListener = () -> {
                int scrollY = scrollView.getScrollY();
                updateFabVisibility(scrollY > 400);
            };
            scrollView.getViewTreeObserver().addOnScrollChangedListener(currentScrollListener);
            return;
        }

        // RecyclerView 찾기
        RecyclerView recyclerView = findRecyclerView(fragmentView);
        if (recyclerView != null) {
            currentScrollTarget = recyclerView;
            RecyclerView.OnScrollListener rvListener = new RecyclerView.OnScrollListener() {
                private int totalDy = 0;
                @Override
                public void onScrolled(RecyclerView rv, int dx, int dy) {
                    totalDy += dy;
                    if (totalDy < 0) totalDy = 0;
                    updateFabVisibility(totalDy > 400);
                }
            };
            recyclerView.addOnScrollListener(rvListener);
            // 태그로 저장해서 나중에 제거
            recyclerView.setTag(R.id.fabScrollTop, rvListener);
        }
    }

    private void detachScrollListener() {
        if (currentScrollTarget instanceof ScrollView && currentScrollListener != null) {
            currentScrollTarget.getViewTreeObserver().removeOnScrollChangedListener(currentScrollListener);
        } else if (currentScrollTarget instanceof RecyclerView) {
            RecyclerView rv = (RecyclerView) currentScrollTarget;
            Object tag = rv.getTag(R.id.fabScrollTop);
            if (tag instanceof RecyclerView.OnScrollListener) {
                rv.removeOnScrollListener((RecyclerView.OnScrollListener) tag);
            }
        }
        currentScrollListener = null;
        currentScrollTarget = null;
    }

    private void updateFabVisibility(boolean show) {
        if (show) {
            if (binding.fabScrollTop.getVisibility() != View.VISIBLE) {
                binding.fabScrollTop.setVisibility(View.VISIBLE);
                binding.fabScrollTop.setAlpha(0f);
                binding.fabScrollTop.animate().alpha(1f).setDuration(200).start();
            }
        } else {
            if (binding.fabScrollTop.getVisibility() == View.VISIBLE) {
                binding.fabScrollTop.animate().alpha(0f).setDuration(150)
                        .withEndAction(() -> binding.fabScrollTop.setVisibility(View.GONE)).start();
            }
        }
    }

    private void scrollToTop() {
        if (currentScrollTarget instanceof ScrollView) {
            ((ScrollView) currentScrollTarget).smoothScrollTo(0, 0);
        } else if (currentScrollTarget instanceof RecyclerView) {
            ((RecyclerView) currentScrollTarget).smoothScrollToPosition(0);
        }
    }

    private ScrollView findScrollView(View root) {
        if (root instanceof ScrollView) return (ScrollView) root;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                ScrollView sv = findScrollView(vg.getChildAt(i));
                if (sv != null) return sv;
            }
        }
        return null;
    }

    private RecyclerView findRecyclerView(View root) {
        if (root instanceof RecyclerView) return (RecyclerView) root;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                RecyclerView rv = findRecyclerView(vg.getChildAt(i));
                if (rv != null) return rv;
            }
        }
        return null;
    }
}

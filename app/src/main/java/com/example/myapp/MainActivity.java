package com.example.myapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.myapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity.java
 * ------------------
 * 程序主活动：
 * 1. 负责加载底部导航栏（BottomNavigationView）。
 * 2. 绑定 Navigation 组件（NavController）。
 * 3. 根据导航图（mobile_navigation.xml）加载对应的 Fragment。
 *
 * 页面对应关系：
 * - HomeFragment.java        → 系统简介页面
 * - DashboardFragment.java   → 我的音乐播放器页面
 * - NotificationsFragment.java → 主页面
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 绑定布局文件 activity_main.xml（包含导航栏与 NavHostFragment）
        setContentView(R.layout.activity_main);

        // 找到底部导航栏控件
        BottomNavigationView navView = findViewById(R.id.nav_view);

        // 找到 NavHostFragment（在 XML 里定义的 fragment）
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);

        if (navHostFragment != null) {
            // 获取导航控制器
            NavController navController = navHostFragment.getNavController();
            // 将 BottomNavigationView 与导航控制器绑定
            NavigationUI.setupWithNavController(navView, navController);
        }
    }
}

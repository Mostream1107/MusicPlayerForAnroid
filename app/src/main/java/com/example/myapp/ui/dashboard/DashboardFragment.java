package com.example.myapp.ui.dashboard;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapp.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private ListView listView;
    private ImageButton btnPrev;
    private ImageButton btnPlay;
    private ImageButton btnNext;
    private Button btnMode;
    private Button btnSpeed;
    private Button btnRefresh;
    private Button btnSleepTimer;
    private Button btnToggleList;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvDuration;
    private ImageView ivCover;
    private boolean isListVisible = false;
    private String lastCoverPath = null;

    private MusicAdapter adapter;
    private final List<Music> musicList = new ArrayList<>();

    private MusicService musicService;
    private boolean isBound = false;

    private final String[] modes = {"顺序", "循环", "随机"};
    private int modeIndex = 0;

    private Handler handler;

    private final float[] speedList = {0.5f, 1.0f, 1.5f, 2.0f};
    private int speedIndex = 1;

    private int currentSleepMinutes = 0;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setMusicList(musicList);
            isBound = true;
            updatePlayButtonIcon();
            updateCoverFromService();
            updateModeButtonStyle();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            updateSleepTimerText();
        }
    };

    private BroadcastReceiver storageReceiver;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        listView = root.findViewById(R.id.listView);
        btnPrev = root.findViewById(R.id.btnPrev);
        btnPlay = root.findViewById(R.id.btnPlay);
        btnNext = root.findViewById(R.id.btnNext);
        btnMode = root.findViewById(R.id.btnMode);
        btnSpeed = root.findViewById(R.id.btnSpeed);
        btnRefresh = root.findViewById(R.id.btnRefresh);
        btnSleepTimer = root.findViewById(R.id.btnSleepTimer);
        btnToggleList = root.findViewById(R.id.btnToggleList);
        seekBar = root.findViewById(R.id.seekBar);
        tvCurrentTime = root.findViewById(R.id.tvCurrentTime);
        tvDuration = root.findViewById(R.id.tvDuration);
        ivCover = root.findViewById(R.id.ivCover);

        btnMode.setText(modes[modeIndex]);
        btnSpeed.setText("1.0X");
        btnRefresh.setText("刷新列表");
        updateSleepTimerText();
        updateModeButtonStyle();
        tvCurrentTime.setText("00:00");
        tvDuration.setText("00:00");
        ivCover.setImageResource(android.R.drawable.ic_menu_report_image);
        listView.setVisibility(View.GONE);
        btnToggleList.setText("展开列表");

        ActivityCompat.requestPermissions(
                requireActivity(),
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                1
        );

        initStorageReceiver();
        loadMusic();

        adapter = new MusicAdapter(requireContext(), musicList);
        listView.setAdapter(adapter);

        Intent intent = new Intent(getContext(), MusicService.class);
        requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);

        listView.setOnItemClickListener((p, v, pos, id) -> {
            if (isBound) {
                Music selected = musicList.get(pos);
                musicService.play(pos);
                updatePlayButtonIcon();
                lastCoverPath = selected.getPath();
                updateCover(lastCoverPath);
            }
        });

        btnPlay.setOnClickListener(v -> {
            if (isBound) {
                musicService.pauseOrResume();
                updatePlayButtonIcon();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (isBound) {
                musicService.next();
                updatePlayButtonIcon();
                updateCoverFromService();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (isBound) {
                musicService.prev();
                updatePlayButtonIcon();
                updateCoverFromService();
            }
        });

        btnMode.setOnClickListener(v -> switchMode());

        btnSpeed.setOnClickListener(v -> changeSpeed());

        btnRefresh.setOnClickListener(v -> scanAllMusic());

        btnSleepTimer.setOnClickListener(v -> showSleepTimerDialog());
        btnToggleList.setOnClickListener(v -> toggleListVisibility());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && isBound && musicService.getDuration() > 0) {
                    int pos = (musicService.getDuration() * progress) / 100;
                    musicService.seekTo(pos);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) { }

            @Override
            public void onStopTrackingTouch(SeekBar sb) { }
        });

        handler = new Handler(Looper.getMainLooper());
        handler.post(updateSeek);

        return root;
    }

    private void changeSpeed() {
        if (!isBound) {
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Toast.makeText(getContext(), "倍速播放需要Android 6.0+", Toast.LENGTH_SHORT).show();
            return;
        }

        speedIndex = (speedIndex + 1) % speedList.length;
        float speed = speedList[speedIndex];
        musicService.setSpeed(speed);
        btnSpeed.setText(String.format(Locale.getDefault(), "%.1fX", speed));
    }

    private final Runnable updateSeek = new Runnable() {
        @Override
        public void run() {
            if (isBound) {
                int pos = musicService.getCurrentPosition();
                int dur = musicService.getDuration();
                if (dur > 0) {
                    int progress = (int) Math.min(100, (long) pos * 100 / dur);
                    seekBar.setProgress(progress);
                    tvDuration.setText(formatTime(dur));
                } else {
                    seekBar.setProgress(0);
                    tvDuration.setText("00:00");
                }
                tvCurrentTime.setText(formatTime(pos));
                updatePlayButtonIcon();
                if (currentSleepMinutes != 0 && !musicService.isSleepTimerActive()) {
                    currentSleepMinutes = 0;
                    updateSleepTimerText();
                }
            }
            if (handler != null) {
                handler.postDelayed(this, 500);
            }
        }
    };

    private void switchMode() {
        if (!isBound || musicService == null) {
            Toast.makeText(getContext(), "播放器尚未就绪", Toast.LENGTH_SHORT).show();
            return;
        }
        modeIndex = (modeIndex + 1) % modes.length;
        if (modeIndex == 0) {
            musicService.setLoop(false);
            musicService.setRandom(false);
        } else if (modeIndex == 1) {
            musicService.setLoop(true);
            musicService.setRandom(false);
        } else {
            musicService.setLoop(false);
            musicService.setRandom(true);
        }
        updateModeButtonStyle();
    }

    private void loadMusic() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " !=0";
        Cursor cursor = requireActivity()
                .getContentResolver()
                .query(uri, null, selection, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String path = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                musicList.add(new Music(title, path));
            }
            cursor.close();
        }
    }

    @Override
    public void onDestroy() {
        if (isBound) {
            requireActivity().unbindService(connection);
        }
        if (handler != null) {
            handler.removeCallbacks(updateSeek);
        }
        if (storageReceiver != null) {
            try {
                requireContext().unregisterReceiver(storageReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    private void initStorageReceiver() {
        storageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)
                        || Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                    refreshMusicList();
                    Toast.makeText(context, "音乐列表已更新", Toast.LENGTH_SHORT).show();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addDataScheme("file");
        ContextCompat.registerReceiver(
                requireContext(),
                storageReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    public void refreshMusicList() {
        musicList.clear();
        loadMusic();
        adapter.notifyDataSetChanged();
        if (isBound) {
            musicService.setMusicList(musicList);
        }
    }

    public void scanNewMusicFile(String filePath) {
        if (getContext() == null || filePath == null) {
            return;
        }
        File file = new File(filePath);
        if (!file.exists() || !file.getName().endsWith(".mp3")) {
            Toast.makeText(getContext(), "文件不存在或不是MP3格式", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaScannerConnection.scanFile(
                    getContext(),
                    new String[]{filePath},
                    new String[]{"audio/mpeg"},
                    (path, uri) -> requireActivity().runOnUiThread(this::refreshMusicList)
            );
        } else {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(file));
            getContext().sendBroadcast(scanIntent);
            if (handler != null) {
                handler.postDelayed(this::refreshMusicList, 1000);
            }
        }
    }

    private void scanAllMusic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaScannerConnection.scanFile(
                    getContext(),
                    new String[]{Environment.getExternalStorageDirectory().getPath()},
                    null,
                    (path, uri) -> requireActivity().runOnUiThread(() -> {
                        refreshMusicList();
                        Toast.makeText(getContext(), "已扫描所有音频", Toast.LENGTH_SHORT).show();
                    })
            );
        } else {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_MOUNTED);
            scanIntent.setData(Uri.fromFile(Environment.getExternalStorageDirectory()));
            getContext().sendBroadcast(scanIntent);
            if (handler != null) {
                handler.postDelayed(() -> {
                    refreshMusicList();
                    Toast.makeText(getContext(), "已扫描所有音频", Toast.LENGTH_SHORT).show();
                }, 2000);
            }
        }
    }

    private void showSleepTimerDialog() {
        if (!isBound) {
            Toast.makeText(getContext(), "请先开始播放", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("输入分钟数，如 30");

        new AlertDialog.Builder(requireContext())
                .setTitle("睡眠定时器")
                .setMessage("自定义几分钟后暂停播放")
                .setView(input)
                .setPositiveButton("开始", (dialog, which) -> {
                    String text = input.getText() == null ? "" : input.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        Toast.makeText(getContext(), "请输入分钟数", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int minutes;
                    try {
                        minutes = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "格式不正确", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (minutes <= 0) {
                        Toast.makeText(getContext(), "分钟数需大于0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    musicService.startSleepTimer(minutes * 60 * 1000L);
                    currentSleepMinutes = minutes;
                    Toast.makeText(
                            getContext(),
                            "将在 " + minutes + " 分钟后停止播放",
                            Toast.LENGTH_SHORT
                    ).show();
                    updateSleepTimerText();
                })
                .setNeutralButton("取消定时", (dialog, which) -> {
                    musicService.cancelSleepTimer();
                    currentSleepMinutes = 0;
                    updateSleepTimerText();
                    Toast.makeText(getContext(), "已取消定时", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private String formatTime(int millis) {
        if (millis <= 0) {
            return "00:00";
        }
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void updateModeButtonStyle() {
        if (btnMode == null) {
            return;
        }
        btnMode.setText(modes[modeIndex]);
        if (modeIndex == 0) {
            btnMode.setBackgroundResource(R.drawable.bg_function_chip);
            btnMode.setTextColor(Color.parseColor("#3A2B6A"));
        } else {
            btnMode.setBackgroundResource(R.drawable.bg_function_chip_active);
            btnMode.setTextColor(Color.WHITE);
        }
    }

    private void updatePlayButtonIcon() {
        if (btnPlay == null || musicService == null || !isBound) {
            return;
        }
        int icon = musicService.isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play;
        btnPlay.setImageResource(icon);
    }

    private void updateSleepTimerText() {
        if (btnSleepTimer == null) {
            return;
        }
        if (currentSleepMinutes > 0) {
            btnSleepTimer.setText("定时: " + currentSleepMinutes + " 分钟");
        } else {
            btnSleepTimer.setText("睡眠定时");
        }
    }

    private void toggleListVisibility() {
        isListVisible = !isListVisible;
        listView.setVisibility(isListVisible ? View.VISIBLE : View.GONE);
        btnToggleList.setText(isListVisible ? "收起列表" : "展开列表");
    }

    private void updateCoverFromService() {
        if (!isBound || musicService == null) {
            return;
        }
        Music current = musicService.getCurrentMusic();
        if (current != null) {
            String path = current.getPath();
            if (TextUtils.equals(lastCoverPath, path)) {
                return;
            }
            lastCoverPath = path;
            updateCover(path);
        }
    }

    private void updateCover(String path) {
        if (path == null || ivCover == null) {
            return;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                ivCover.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.length));
            } else {
                ivCover.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        } catch (Exception e) {
            ivCover.setImageResource(android.R.drawable.ic_menu_report_image);
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) { }
        }
    }
}

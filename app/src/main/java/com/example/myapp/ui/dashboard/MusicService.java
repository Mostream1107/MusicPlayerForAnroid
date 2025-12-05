package com.example.myapp.ui.dashboard;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.io.IOException;
import java.util.List;

public class MusicService extends Service {

    private MediaPlayer player;
    private List<Music> musicList;
    private int currentIndex = 0;

    private boolean isLoop = false;
    private boolean isRandom = false;

    private float playSpeed = 1.0f;

    private final IBinder binder = new MusicBinder();
    private final Handler sleepHandler = new Handler(Looper.getMainLooper());
    private final Runnable sleepRunnable = this::stopPlaybackForSleep;
    private long sleepEndTime = 0L;

    public class MusicBinder extends Binder {
        public MusicService getService() { return MusicService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setMusicList(List<Music> list) {
        this.musicList = list;
    }

    public void play(int index) {
        if (musicList == null || musicList.isEmpty()) {
            return;
        }

        currentIndex = index;

        if (player != null) {
            player.stop();
            player.release();
        }

        player = new MediaPlayer();
        try {
            player.setDataSource(musicList.get(currentIndex).getPath());
            player.prepare();
            applySpeed();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pauseOrResume() {
        if (player == null) {
            return;
        }
        if (player.isPlaying()) {
            player.pause();
        } else {
            applySpeed();
            player.start();
        }
    }

    public void next() {
        if (musicList == null || musicList.isEmpty()) {
            return;
        }

        if (isRandom) {
            currentIndex = (int) (Math.random() * musicList.size());
        } else if (!isLoop) {
            currentIndex = (currentIndex + 1) % musicList.size();
        }
        play(currentIndex);
    }

    public void prev() {
        if (musicList == null || musicList.isEmpty()) {
            return;
        }
        if (isRandom) {
            currentIndex = (int) (Math.random() * musicList.size());
        } else if (!isLoop) {
            currentIndex = (currentIndex - 1 + musicList.size()) % musicList.size();
        }
        play(currentIndex);
    }

    public void setLoop(boolean loop) {
        this.isLoop = loop;
    }

    public void setRandom(boolean random) {
        this.isRandom = random;
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public int getCurrentPosition() {
        return player == null ? 0 : player.getCurrentPosition();
    }

    public int getDuration() {
        return player == null ? 0 : player.getDuration();
    }

    public Music getCurrentMusic() {
        if (musicList == null || musicList.isEmpty()) {
            return null;
        }
        int safeIndex = Math.max(0, Math.min(currentIndex, musicList.size() - 1));
        return musicList.get(safeIndex);
    }

    public void seekTo(int msec) {
        if (player != null) {
            player.seekTo(msec);
        }
    }

    public void setSpeed(float speed) {
        this.playSpeed = speed;
        applySpeed();
    }

    private void applySpeed() {
        if (player == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PlaybackParams params = player.getPlaybackParams();
                params.setSpeed(playSpeed);
                player.setPlaybackParams(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startSleepTimer(long millis) {
        cancelSleepTimer();
        if (millis <= 0) {
            return;
        }
        sleepEndTime = System.currentTimeMillis() + millis;
        sleepHandler.postDelayed(sleepRunnable, millis);
    }

    public void cancelSleepTimer() {
        sleepEndTime = 0L;
        sleepHandler.removeCallbacks(sleepRunnable);
    }

    public boolean isSleepTimerActive() {
        return sleepEndTime > 0;
    }

    private void stopPlaybackForSleep() {
        sleepEndTime = 0L;
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.pause();
                }
                player.seekTo(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        cancelSleepTimer();
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}

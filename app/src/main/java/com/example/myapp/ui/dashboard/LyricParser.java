package com.example.myapp.ui.dashboard;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LyricParser {

    private static final Pattern TIME_PATTERN =
            Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:\\.(\\d{1,3}))?]");
    private static final Pattern OFFSET_PATTERN =
            Pattern.compile("\\[offset:(-?\\d+)]", Pattern.CASE_INSENSITIVE);

    private LyricParser() { }

    public static List<LyricLine> parseFromAudioPath(String audioPath) {
        if (TextUtils.isEmpty(audioPath)) {
            return Collections.emptyList();
        }
        File audio = new File(audioPath);
        if (!audio.exists()) {
            return Collections.emptyList();
        }
        String baseName = stripExtension(audio.getName());
        if (TextUtils.isEmpty(baseName)) {
            return Collections.emptyList();
        }
        File lyricFile = new File(audio.getParentFile(), baseName + ".lrc");
        if (!lyricFile.exists()) {
            // try uppercase extension just in case
            lyricFile = new File(audio.getParentFile(), baseName + ".LRC");
            if (!lyricFile.exists()) {
                return Collections.emptyList();
            }
        }
        return parseLrcFile(lyricFile);
    }

    private static List<LyricLine> parseLrcFile(File file) {
        List<LyricLine> result = new ArrayList<>();
        int offsetMs = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                Matcher offsetMatcher = OFFSET_PATTERN.matcher(line);
                if (offsetMatcher.matches()) {
                    try {
                        offsetMs = Integer.parseInt(offsetMatcher.group(1));
                    } catch (NumberFormatException ignored) { }
                    continue;
                }

                Matcher timeMatcher = TIME_PATTERN.matcher(line);
                List<Integer> timeStamps = new ArrayList<>();
                while (timeMatcher.find()) {
                    int minutes = safeParseInt(timeMatcher.group(1));
                    int seconds = safeParseInt(timeMatcher.group(2));
                    String fractionStr = timeMatcher.group(3);
                    double fraction = 0d;
                    if (!TextUtils.isEmpty(fractionStr)) {
                        try {
                            fraction = Double.parseDouble("0." + fractionStr);
                        } catch (NumberFormatException ignored) {
                            fraction = 0d;
                        }
                    }
                    int ms = (int) ((minutes * 60 + seconds + fraction) * 1000);
                    ms += offsetMs;
                    timeStamps.add(Math.max(ms, 0));
                }
                if (timeStamps.isEmpty()) {
                    continue;
                }
                int lastBracket = line.lastIndexOf(']');
                String lyricText = lastBracket >= 0 && lastBracket + 1 < line.length()
                        ? line.substring(lastBracket + 1).trim()
                        : "";
                for (int stamp : timeStamps) {
                    result.add(new LyricLine(stamp, lyricText));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        Collections.sort(result);
        return result;
    }

    private static int safeParseInt(String value) {
        if (TextUtils.isEmpty(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String stripExtension(String name) {
        if (TextUtils.isEmpty(name)) {
            return name;
        }
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex <= 0) {
            return name;
        }
        return name.substring(0, dotIndex);
    }
}

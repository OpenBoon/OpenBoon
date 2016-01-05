package com.zorroa.archivist.sdk.util;

import com.google.common.base.Splitter;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by chambers on 11/6/15.
 */
public class StringUtil {

    public static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    public static String join(String[] array, String delimiter) {
        StringBuilder sb = new StringBuilder(512);
        for (String s: array) {
            sb.append(s);
            sb.append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    /**
     * A pattern for splitting up a string by all whitespace, including new lines and carriage returns.
     * Note: Intellij doesn't seem to support \R yet, but it does compile.
     */
    private static final Pattern WORD_STREAM_PATTERN = Pattern.compile("\\R|\\p{javaSpaceChar}");

    /**
     * Return an Iterable<String> of all individual words in a string.
     *
     * @param words
     * @return
     */
    public static Iterable<String> getWordStream(String words) {
        return Splitter.on(WORD_STREAM_PATTERN)
                .trimResults().omitEmptyStrings().split(words);
    }

    /**
     * Return a list of all individual words in a string.
     *
     * @param words
     * @return
     */
    public static List<String> getWordList(String words) {
        return Splitter.on(WORD_STREAM_PATTERN)
                .trimResults().omitEmptyStrings().splitToList(words);
    }


    private static final Pattern DURATION_TUPLE_PATTERN =Pattern.compile("((?<=[a-z])|(?=[a-z]))");

    /**
     * Convert a human readable duration string to milliseconds.  The string must
     * be in the format of [value][unit], for example:
     *
     *   - 1d2h = 1 eay, 2 hours
     *   - 1w3d1h = 1 week, 3 days, 1 hour
     *   - 10m1s = 10 minutes, 1 second
     *
     * Available units are:
     *
     *   - w = week
     *   - d = day
     *   - h = hour
     *   - m = minute
     *   - s = seconds
     *
     * @param duration
     * @return
     */
    public static long durationToMillis(String duration) {
        return durationToSeconds(duration) * 1000;
    }

    /**
     * Convert a human readable duration string to seconds.  The string must
     * be in the format of [value][unit], for example:
     *
     *   - 1d2h = 1 eay, 2 hours
     *   - 1w3d1h = 1 week, 3 days, 1 hour
     *   - 10m1s = 10 minutes, 1 second
     *
     * Available units are:
     *
     *   - w = week
     *   - d = day
     *   - h = hour
     *   - m = minute
     *   - s = seconds
     *
     * @param duration
     * @return
     */
    public static long durationToSeconds(String duration) {
        long result = 0;

        String[] stream = DURATION_TUPLE_PATTERN.split(duration);
        for (int i=0; i<stream.length; i=i+2) {
            int value = Integer.valueOf(stream[i]);
            char unit = stream[i+1].charAt(0);

            switch(unit) {
                case 's':
                    result+=value;
                    break;
                case 'm':
                    result+=60*value;
                    break;
                case 'h':
                    result+=3600*value;
                    break;
                case 'd':
                    result+=86400*value;
                    break;
                case 'w':
                    result+=(86400*7)*value;
                    break;
                default:
                    throw new IllegalArgumentException("Invalidate duration character: '" + unit + '"');
            }
        }

        return result;
    }
}

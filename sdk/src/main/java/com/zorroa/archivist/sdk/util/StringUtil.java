package com.zorroa.archivist.sdk.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by chambers on 11/6/15.
 */
public class StringUtil {

    public static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    /**
     * Join an array of strings using the given delimiter.
     *
     * @param delimiter
     * @param array
     * @return
     */
    public static String join(String delimiter, String ... array) {
        StringBuilder sb = new StringBuilder(512);
        for (String s: array) {
            sb.append(s);
            sb.append(delimiter);
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    /**
     * Join an array of integers using the given delimiter.
     *
     * @param delimiter
     * @param values
     * @return
     */
    public static String join(String delimiter, int ... values) {
        StringBuilder sb = new StringBuilder(512);
        for (int v: values) {
            sb.append(v);
            sb.append(delimiter);
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    public static final Pattern NUMBER_PATTERN =  Pattern.compile("^[-+]?\\d*\\.?\\d+$");

    /**
     * Return true if the string parses as a number.
     *
     * @param s
     * @return
     */
    public static boolean isNumeric(String s) {
        return NUMBER_PATTERN.matcher(s).matches();
    }

    /**
     * A pattern for splitting up a string by all whitespace, including new lines and carriage returns.
     * Note: Intellij doesn't seem to support \R yet, but it does compile.
     */
    public static final Pattern WORD_STREAM_PATTERN = Pattern.compile("\\R|\\p{javaSpaceChar}");

    /**
     * Return an Iterable<String> of all individual words in a string.
     *
     * @param words
     * @return
     */
    public static Iterable<String> getWordStream(String words) {
        if (words == null) {
            return Lists.newArrayListWithCapacity(0);
        }
        return Splitter.on(WORD_STREAM_PATTERN)
                .trimResults().omitEmptyStrings().split(words);
    }

    /**
     * Return an Iterable<String> of all individual words in a string.
     *
     * @param words
     * @return
     */
    public static List<String> getWordList(String words) {
        if (words == null) {
            return Lists.newArrayListWithCapacity(0);
        }
        return Splitter.on(WORD_STREAM_PATTERN)
                .trimResults().omitEmptyStrings().splitToList(words);
    }


    /**
     * Strip all letters from a word and return all integers
     * as a single number
     *
     * @param word
     * @return
     */
    public static Integer findInteger(String word) {
        word = word.replaceAll("[^0-9]", "");
        if (word.isEmpty()) {
            return null;
        }
        return Integer.valueOf(word);
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

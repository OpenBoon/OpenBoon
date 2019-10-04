package com.zorroa.archivist.domain;

import com.google.common.base.Splitter;

import java.util.Iterator;
import java.util.List;

/**
 * Created by chambers on 2/16/17.
 */
public class PathIterator implements Iterable<String> {

    private List<String> elements;
    private boolean prefixDelimiter = false;
    private int size;
    private String delimit;

    public PathIterator(String path, String delimit) {
        this.elements = Splitter.on(delimit).omitEmptyStrings().trimResults().splitToList(path);
        this.prefixDelimiter = path.startsWith(delimit);
        this.size = elements.size();
        this.delimit = delimit;
    }

    @Override
    public Iterator<String> iterator() {
        Iterator<String> it = new Iterator<String>() {

            private int currentIndex = 0;
            private StringBuilder sb = new StringBuilder(512);

            @Override
            public boolean hasNext() {
                return currentIndex < size;
            }

            @Override
            public String next() {
                String next;
                if (currentIndex == 0 && prefixDelimiter) {
                    next = delimit + elements.get(currentIndex++) + delimit;
                }
                else {
                    next = elements.get(currentIndex++) + delimit;
                }
                sb.append(next);
                return sb.toString();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
    }
}

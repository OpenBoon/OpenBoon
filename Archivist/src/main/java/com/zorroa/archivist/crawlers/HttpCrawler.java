package com.zorroa.archivist.crawlers;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.Allocation;
import com.zorroa.archivist.sdk.util.FileUtils;
import com.zorroa.archivist.service.ObjectFileSystem;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.*;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by chambers on 1/28/16.
 */
public class HttpCrawler extends AbstractCrawler {

    private static final Pattern TAG_IMG = Pattern.compile("<img.*? src=\"(.*?)\"");
    private static final  Pattern TAG_LINK = Pattern.compile("href=\"(.*?)\"");

    private final CloseableHttpClient httpClient;

    private final ThreadPoolExecutor crawlerThreads = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    private final ThreadPoolExecutor downloadThreads = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

    /**
     * Keep a set of the places we've already visited.
     */
    private final Set<URI> visited = Sets.newConcurrentHashSet();

    /**
     * A set of valid hostnames.
     */
    private final Set<String> validDomains = Sets.newConcurrentHashSet();

    /**
     * The maximum number of assets to download.
     */
    private final long maxAssetDownloads = 100;

    /**
     * The number of assets downloaded.
     */
    private final AtomicLong assetsDownloaded = new AtomicLong(0);

    /**
     * Create an HttpCrawler and store matching files using the given
     * object file system.
     *
     * @param fileSystem
     */
    public HttpCrawler(ObjectFileSystem fileSystem) {
        super(fileSystem);

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(20);
        cm.setMaxTotal(10);

        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    @Override
    public void start(URI uri, Consumer<File> consumer) throws IOException {
        validDomains.add(uri.getHost());

        if (targetFileFormats.isEmpty()) {
            setTargetFileFormats(Lists.newArrayList("png", "jpg", "gif"));
        }

        crawlerThreads.execute(()->read(uri, consumer));
        while(true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                return;
            }
            if (assetsDownloaded.longValue() >= maxAssetDownloads ||
                    (crawlerThreads.getQueue().isEmpty() && crawlerThreads.getActiveCount() == 0)) {
                return;
            }
        }
    }

    public void read(URI uri, Consumer<File> consumer) {
        if (!visited.add(uri)) {
            return;
        }

        Set<URI> imgLinks = Sets.newHashSetWithExpectedSize(16);

        try {
            HttpResponse response = httpClient.execute(new HttpGet(uri));
            InputStream stream = response.getEntity().getContent();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String content = reader.lines().collect(Collectors.joining(""));

                Matcher linkMatcher = TAG_LINK.matcher(content);
                while(linkMatcher.find()) {
                    URI next = normalizeUrl(uri, linkMatcher.group(1));
                    if (next != null) {
                        crawlerThreads.execute(()->read(next, consumer));
                    }
                }

                Matcher imgMatcher = TAG_IMG.matcher(content);
                while(imgMatcher.find()) {
                    URI imageLink = normalizeUrl(uri, imgMatcher.group(1));
                    if (imageLink != null) {
                        imgLinks.add(imageLink);
                    }
                }
            }

        } catch (Exception e) {
            logger.warn("Failure to read page: '{}'", uri, e);
            return;
        }

        if (assetsDownloaded.longValue() >= maxAssetDownloads) {
            return;
        }

        imgLinks.removeIf(f -> !visited.add(f));
        imgLinks.removeIf(f -> !targetFileFormats.contains(FileUtils.extension(f.toString())));
        if (!imgLinks.isEmpty()) {
            downloadThreads.execute(()->download(uri, imgLinks, consumer));
        }
    }

    public URI normalizeUrl(URI page, String link) {
        URI result = null;

        if (link.startsWith("#")) {
            return null;
        }

        if (link.startsWith("//")) {
            link = new StringBuilder(256)
                    .append("http:")
                    .append(link)
                    .toString();
        }

        try {
            if (link.startsWith("http://")) {
                result = URI.create(link);
            }
            else if (link.startsWith("/")) {
                result = URI.create(new StringBuilder(256)
                        .append("http://")
                        .append(page.getHost())
                        .append(link)
                        .toString());
            }
            else {
                return null;
            }

        } catch (RuntimeException e) {
            logger.warn("URI normalization error, page: {} , img link: '{}'", page, link, e);
            return null;
        }

        if (result != null) {
            if (visited.contains(result)) {
                return null;
            }
            if (!isValidDomain(result.getHost())) {
                return null;
            }
        }

        return result;
    }

    public boolean isValidDomain(String domain) {
        if (domain == null) {
            return false;
        }

        for (String valid: validDomains) {
            if (domain.contains(valid)) {
                return true;
            }
        }
        return false;
    }

    public void download(URI page, Set<URI> links, Consumer<File> consumer) {

        Allocation alloc = fileSystem.build(page, "assets");
        for (URI link : links) {

            String basename = FileUtils.basename(link.getPath());
            String ext = FileUtils.extension(link.getPath());

            if (alloc.exists(ext, basename)) {
                continue;
            }

            HttpGet httpget = new HttpGet(link);
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpget);
            } catch (IOException e) {
                continue;
            }

            HttpEntity entity = response.getEntity();
            try (InputStream is = entity.getContent()) {
                File dst = alloc.store(is, ext, basename);
                consumer.accept(dst);
                if (assetsDownloaded.incrementAndGet() >= maxAssetDownloads) {
                    return;
                }
            } catch (IOException e) {
                logger.warn("Failed to download file: '{}'", link, e);
            }
        }
    }
}

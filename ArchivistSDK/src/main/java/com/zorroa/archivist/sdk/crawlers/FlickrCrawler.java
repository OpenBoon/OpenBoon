package com.zorroa.archivist.sdk.crawlers;

import com.zorroa.archivist.sdk.domain.AnalyzeRequestEntry;
import com.zorroa.archivist.sdk.util.Json;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The FlickrCrawler is for ingesting images from Flickr.  Currently the only operation supported is
 * a flickr search.
 * https://www.flickr.com/services/api/flickr.photos.search.html
 *
 * All of the options on that page are supported except for the ones we are already using:
 * format, extras, page.
 *
 * So for example, to search for cat, the ingest URI would be:
 * flickr://search?text=cat
 *
 * You could limit to common photo repositories with:
 * flickr://search?text=cat&is_common=true
 *
 * Currently this ingestor o
 */
public class FlickrCrawler extends AbstractCrawler
{

    /**
     * Note: these are tied to Matt's personal yahoo account.  We'll probably have to register a business app.
     */
    private static final String API_KEY = "3fff2cec0a1829e38501e3e9dccd0ac0";
    private static final String API_SECRET = "5ee1c9a6fb559059";
    private static final String BASE_URL = "https://api.flickr.com/services/rest/?api_key=" + API_KEY +"&method=";

    private final CloseableHttpClient httpClient;

    public FlickrCrawler() {
        httpClient = HttpClients.createDefault();
    }

    @Override
    public void start(URI uri, Consumer<AnalyzeRequestEntry> consumer) throws IOException {
        String request;

        switch (uri.getHost()) {
            case "search":
                request = new StringBuilder(512)
                        .append(BASE_URL)
                        .append("flickr.photos.search&format=json&nojsoncallback=1&extras=geo,url_o,url_l,description,tags,machine_tags&page=%d&")
                        .append(uri.getQuery()).toString();
                break;
            default:
                throw new IOException("Invalid URI: " + uri);
        }

        walkSearchResult(uri, request, consumer);
    }

    public void walkSearchResult(URI ingestUri, String restRequest, Consumer<AnalyzeRequestEntry> consumer) {

        /**
         * This gets reset after the first page;
         */
        int maxPages = 100;
        int page = 1;

        while(page < maxPages) {

            try {
                String request = String.format(restRequest, page);
                HttpResponse response = httpClient.execute(new HttpGet(request));
                InputStream stream = response.getEntity().getContent();

                Map<String, Object> result = Json.Mapper.readValue(stream, Json.GENERIC_MAP);
                FlickrSearchResult sresult = Json.Mapper.convertValue(result, FlickrSearchResult.class);

                for (FlickrPhoto photo : sresult.getPhotos().getPhoto()) {
                    if (photo.url_o == null) {
                        continue;
                    }
                    consumer.accept(new AnalyzeRequestEntry(URI.create(photo.url_o))
                            .setAttr("reference:search", request)
                            .setAttr("reference:title", photo.title)
                            .setAttr("reference:tags", photo.tags)
                            .setAttr("reference:machineTags", photo.machine_tags)
                            .setAttr("reference:owner", photo.owner)
                            .setAttr("reference:source", "flickr")
                            .setAttr("reference:id", photo.id)
                            .setAttr("keywords:reference", photo.tags));
                }

                if (page == 1) {
                    maxPages = sresult.getPhotos().pages;
                }
                page++;
            } catch (Exception e) {
                logger.warn("Failed to search flickr: {}", ingestUri, e);
                break;
            }
        }
    }

    private static class FlickrSearchResult {
        public FlickrSearchResultPhotos photos;

        public FlickrSearchResultPhotos getPhotos() {
            return photos;
        }
        public FlickrSearchResult setPhotos(FlickrSearchResultPhotos photos) {
            this.photos = photos;
            return this;
        }
    }

    private static class FlickrSearchResultPhotos {
        public int page;
        public int pages;
        public int perpage;
        public int total;

        public List<FlickrPhoto> photo;

        public int getPage() {
            return page;
        }

        public FlickrSearchResultPhotos setPage(int page) {
            this.page = page;
            return this;
        }

        public int getPages() {
            return pages;
        }

        public FlickrSearchResultPhotos setPages(int pages) {
            this.pages = pages;
            return this;
        }

        public int getPerpage() {
            return perpage;
        }

        public FlickrSearchResultPhotos setPerpage(int perpage) {
            this.perpage = perpage;
            return this;
        }

        public int getTotal() {
            return total;
        }

        public FlickrSearchResultPhotos setTotal(int total) {
            this.total = total;
            return this;
        }

        public List<FlickrPhoto> getPhoto() {
            return photo;
        }

        public FlickrSearchResultPhotos setPhoto(List<FlickrPhoto> photo) {
            this.photo = photo;
            return this;
        }
    }

    private static class FlickrPhoto {
        public long id;
        public String owner;
        public String secret;
        public int server;
        public String title;
        public String tags;
        public String machine_tags;
        public String url_o;
        public String url_l;
    }
}

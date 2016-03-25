package com.zorroa.archivist.sdk.crawlers;

import com.zorroa.archivist.sdk.domain.AnalyzeRequestEntry;
import com.zorroa.shotgun.SgRequest;
import com.zorroa.shotgun.Shotgun;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by chambers on 3/23/16.
 */
public class ShotgunCrawler extends AbstractCrawler {

    /**
     * This currently only works on our http://zorroa.shotgunstudio.com
     */
    private String SCRIPT_NAME = "Archivist";
    private String SCRIPT_KEY = "9b79ae646eab8304a5a9133c7d470871a800644f9fd36aa026c996d39b78ea70";
    private Shotgun shotgun;

    public ShotgunCrawler() { }

    @Override
    public void start(URI uri, Consumer<AnalyzeRequestEntry> consumer) throws IOException {

        String server = "https://" + uri.getHost();
        shotgun = new Shotgun(server, SCRIPT_NAME, SCRIPT_KEY);

        int page = 1;

        for (;;) {

            List<Map<String,Object>> result = shotgun.find(new SgRequest("Shot")
                            .setFields("id", "image", "code", "sg_asset_type", "tag_list", "project", "sg_asset_group")
                            .setPage(page));

            if (result.isEmpty()) {
                break;
            }

            for (Map<String, Object> row: result) {
                AnalyzeRequestEntry entry = new AnalyzeRequestEntry(URI.create((String) row.get("image")))
                        .set("reference:server", server)
                        .set("reference:source", "shotgun");
                /*
                 * Add all the fields we selected.
                 */
                for (Map.Entry<String, Object> col: row.entrySet()) {
                    entry.set("shotgun:" + col.getKey(), col.getValue());
                }
                consumer.accept(entry);
            }
            page++;
        }
    }
}

/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.ingestors;

import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RetrosheetIngestor extends IngestProcessor {

    private class Team {
        String nickname;
        String league;
        String city;
        String state;
    }

    private class Site {
        String name;
        String aka;
        String city;
        String state;
        String league;
    }

    private class Game {
        Date date;
        int lengthMin;
        Team home;
        Team visitor;
        Site site;
        String dayOrNight;
        String windDir;
        int windSpeed;
        String precipitation;
        String fieldCondition;
        String sky;
    }

    private HashMap<String, Team> teamMap = new HashMap<String, Team>();
    private HashMap<Date, Game> gameMap = new HashMap<Date, Game>();
    private HashMap<String, Site> siteMap = new HashMap<String, Site>();
    private boolean parseError = false;
    private boolean restrictToGameInterval = true;

    private static final Logger logger = LoggerFactory.getLogger(CaffeIngestor.class);

    public RetrosheetIngestor() { }

    public boolean readModelFiles() {
        Map<String, String> env = System.getenv();
        String modelPath = env.get("ZORROA_OPENCV_MODEL_PATH");
        if (modelPath == null) {
            logger.error("RetrosheetIngestor requires ZORROA_OPENCV_MODEL_PATH");
            return false;
        }
        File modelFolder = new File(modelPath + "/retrosheet");
        if (!parseTeamFile(modelFolder)) {
            return false;
        }
        if (!parseSiteFile(modelFolder)) {
            return false;
        }
        if (!parseGameFiles(modelFolder)) {
            return false;
        }
        logger.info("Read Retrosheet team, site and game files.");
        return true;
    }

    public boolean parseTeamFile(File modelFolder) {
        String path = modelFolder.getPath() + "/TeamNames.csv";
        try {
            Reader in = new FileReader(path);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
            for (CSVRecord record : records) {
                Team team = new Team();
                team.nickname = record.get(5);
                team.league = record.get(2);
                team.city = record.get(9);
                team.state = record.get(10);
                String abbreviation = record.get(0);
                teamMap.put(abbreviation, team);
            }
        } catch (FileNotFoundException e) {
            logger.error("Cannot find Retrosheet team file " + path);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean parseSiteFile(File modelFolder) {
        String path = modelFolder.getPath() + "/ParkCode.csv";
        try {
            Reader in = new FileReader(path);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
            for (CSVRecord record : records) {
                Site site = new Site();
                site.name = record.get(1);
                site.aka = record.get(2);
                site.city = record.get(3);
                site.state = record.get(4);
                site.league = record.get(7);
                String abbreviation = record.get(0);
                siteMap.put(abbreviation, site);
            }
        } catch (FileNotFoundException e) {
            logger.error("Cannot find Retrosheet park code file " + path);
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean parseGameFiles(File modelFolder) {
        File[] csvFiles = modelFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.matches("[0-9]+SFGiantsGames.csv");
            }
        });
        for (int i = 0; i < csvFiles.length; ++i) {
            if (csvFiles[i].isFile()) {
                parseGameFile(csvFiles[i]);
            }
        }
        return true;
    }

    private Date roundToDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public boolean parseGameFile(File file) {
        try {
            Reader in = new FileReader(file.getPath());
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
            Game game = null;
            String starttime = null;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mma");
            SimpleDateFormat timeFormat2 = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            for (CSVRecord record : records) {
                String cmd = record.get(0);
                if (cmd.equals("id")) {
                    if (game != null && game.date != null) {
                        Date key = roundToDay(game.date);
                        gameMap.put(key, game);
                    }
                    game = new Game();
                } else if (cmd.equals("info")) {
                    String info = record.get(1);
                    String value = record.get(2);
                    if (value == null || value.equals("unknown")) {
                        continue;
                    }
                    if (info.equals("visteam")) {
                        String abbreviation = value;
                        game.visitor = teamMap.get(abbreviation);
                    } else if (info.equals("hometeam")) {
                        String abbreviation = value;
                        game.home = teamMap.get(abbreviation);
                    } else if (info.equals("date")) {       // e.g. 2014/04/08
                        starttime = value;
                        try {
                            game.date = dateFormat.parse(value);
                        } catch (ParseException e) {
                            game.date = null;
                        }
                    } else if (info.equals("starttime")) {  // e.g. 1:36PM
                        starttime = starttime + " " + value;
                        try {
                            game.date = timeFormat.parse(starttime);
                        } catch (ParseException e) {
                            try {
                                game.date = timeFormat2.parse(starttime);
                            } catch (ParseException e1) {
                                logger.warn("Invalid date " + starttime);
                            }
                        }
                    } else if (info.equals("daynight")) {
                        game.dayOrNight = value;
                    } else if (info.equals("winddir")) {
                        game.windDir = value;
                    } else if (info.equals("windspeed")) {
                        game.windSpeed = Integer.valueOf(value);
                    } else if (info.equals("precip")) {
                        game.precipitation = value;
                    } else if (info.equals("fieldcond")) {
                        game.fieldCondition = value;
                    } else if (info.equals("site")) {
                        game.site = siteMap.get(value);
                    } else if (info.equals("timeofgame")) {
                        game.lengthMin = Integer.valueOf(value);
                    } else if (info.equals("sky") && !value.equals("unknown")) {
                        game.sky = value;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public void putField(AssetBuilder asset, String key, String value) {
        if (value == null)
            return;
        asset.addKeywords(0.8, true, value);
    }

    public boolean dateWithoutTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.HOUR) == 0 && cal.get(Calendar.MINUTE) == 0 &&
                cal.get(Calendar.SECOND) == 0 && cal.get(Calendar.MILLISECOND) == 0;
    }

    public boolean dateDuringGame(Date date, Game game) {
        // Check for the case where we don't have a start time for the game
        // Assume that the year/month/day already match from map lookup
        if (dateWithoutTime(date) || dateWithoutTime(game.date))
            return true;
        if (date.before(game.date))
            return false;
        long t = game.date.getTime();
        final long ONE_MINUTE_IN_MILLIS = 60000;
        Date end = new Date(t + game.lengthMin * ONE_MINUTE_IN_MILLIS);
        if (date.after(end))
            return false;
        return true;
    }

    @Override
    public void process(AssetBuilder asset) {
        if (!parseError && teamMap.size() == 0) {
            if (!readModelFiles()) {
                parseError = true;
                return;
            }
            Object restrictArg = getArgs().get("useGameInterval");
            if (restrictArg != null) {
                restrictToGameInterval = Boolean.valueOf(restrictArg.toString());
            }
        }
        if (!asset.getSource().getType().startsWith("image")) {
            return;
        }
        Date date = asset.getSource().getDate();
        if (date == null) {
            return;
        }
        Date key = roundToDay(date);
        Game game = gameMap.get(key);
        if (game == null) {
            return;
        }
        //if (!dateDuringGame(date, game))
        //    return;
        logger.info("Found Retrosheet game: " + game.home.nickname + " against " + game.visitor.nickname + " for " + date);
        if (game.home != null) {
            putField(asset, "home", game.home.nickname);
            putField(asset, "home.city", game.home.city);
            putField(asset, "home.state", game.home.state);
            if (!game.home.league.equals("NL")) {
                putField(asset, "home", "interleague");
            }
        }
        if (game.visitor != null) {
            putField(asset, "visitor", game.visitor.nickname);
            putField(asset, "visitor.city", game.visitor.city);
            putField(asset, "visitor.state", game.visitor.state);
            if (!game.visitor.league.equals("NL")) {
                putField(asset, "visitor", "interleague");
            }
        }
        if (game.site != null) {
            putField(asset, "site", game.site.aka);
            putField(asset, "city", game.site.city);
            putField(asset, "state", game.site.state);
        }
        putField(asset, "precipitation", game.precipitation);
        putField(asset, "sky", game.sky);
    }
}

package com.zorroa.es.similarity;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.script.ScriptEngine;
import org.elasticsearch.script.SearchScript;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SimilarityPlugin extends Plugin implements ScriptPlugin {

    private static final double NO_SCORE = 0;

    @Override
    public ScriptEngine getScriptEngine(Settings settings, Collection<ScriptContext<?>> ctx) {
        return new SimilarityEngine();
    }

    private static class SimilarityEngine implements ScriptEngine {

        @Override
        public String getType() {
            return "zorroa-similarity";
        }

        @Override
        public <FactoryType> FactoryType compile(String name, String code, ScriptContext<FactoryType> context, Map<String, String> params) {
            if (context.equals(SearchScript.CONTEXT) == false) {
                throw new IllegalArgumentException(getType() + " scripts cannot be used for context [" + context.name + "]");
            }

            System.out.println("Code: " + code);
            if ("similarity".equals(code)) {
                SearchScript.Factory factory = (p, lookup) -> new SearchScript.LeafFactory() {

                    private static final double NORM = 100.0;

                    private String field;
                    private final List<String> charHashes;
                    private final List<Float> weights;
                    private int length = 0;
                    private double minScore = 0;
                    private int resolution;
                    private final boolean header;
                    private final char version;
                    private final int dataPos;
                    private final int numHashes;
                    private final double possibleScore;
                    private final double singleScore;

                    {
                        field = params.get("field");
                        minScore =  Double.valueOf(params.getOrDefault("minScore", "1")) / NORM;
                        resolution = 15;

                        List<String> _hashes = Arrays.asList(params.get("hashes").split(","));
                        List<Float> _weights = Arrays.asList(params.get("weights").split(",")).stream()
                                .map(e-> Float.valueOf(e)).collect(Collectors.toList());

                        System.out.println("Step1");
                        if (_hashes == null) {
                            throw new IllegalArgumentException(
                                    "Hashes cannot be null");
                        }

                        if (_weights == null) {
                            _weights = Collections.nCopies(_hashes.size(), 1.0f);
                        }

                        if (_hashes.size() != _weights.size()) {
                            throw new IllegalArgumentException(
                                    "HammingDistanceScript weights must align with hashes");
                        }

                        System.out.println("Step2");
                        /**
                         * Go through all the values and remove the null
                         * values and populate the charHashes and
                         * weights fields with valid values.
                         */
                        charHashes = new ArrayList();
                        weights = new ArrayList();
                        for (int i=0; i<_hashes.size(); i++) {
                            String hash = _hashes.get(i);
                            if (hash == null || hash.isEmpty()) {
                                continue;
                            }
                            charHashes.add(hash);
                            weights.add(_weights.get(i));
                        }

                        System.out.println("Step3");
                        /**
                         * If there are no valid hashes left, initialize to defaults
                         */
                        if (charHashes.isEmpty()) {
                            singleScore = possibleScore = numHashes = dataPos = version = 0;
                            header = false;
                        }
                        else {
                            /**
                             * Use the first hash to determine if there is a header.
                             */
                            String hash = charHashes.get(0);
                            header = hash.charAt(0) == '#';
                            length = hash.length();
                            numHashes = charHashes.size();

                            /**
                             * TODO: more sophisticated header parsing.
                             *
                             * There are 2 fields every has leads with:
                             * 1 char: version
                             * 2 chars: position of data (called "headerSize" here)
                             *
                             * A version 0 hash has 1 field, resolution.
                             */
                            if (header) {
                                version = hash.charAt(1);

                                // The start position of the data.
                                dataPos = Integer.parseInt(hash.substring(2, 4), 16);

                                if (version <= 0) {
                                    // Resolution is the next byte.
                                    resolution = Integer.parseInt(hash.substring(4, 6), 16);
                                }
                            } else {
                                version = 0;
                                dataPos = 0;
                            }

                            // To get the proper score, we subtract header size from the length here.
                            singleScore = resolution * (length - dataPos);
                            possibleScore = singleScore * numHashes;
                        }

                        System.out.println("Step4");
                    }

                    @Override
                    public SearchScript newInstance(LeafReaderContext ctx) throws IOException {
                        return new SearchScript(p, lookup, ctx) {

                            @Override
                            public double runAsDouble() {

                                ScriptDocValues.Strings strings;

                                if (getDoc().containsKey(field)) {
                                    System.out.println("field  " + field);
                                    strings = (ScriptDocValues.Strings) getDoc().get(field);
                                    System.out.println("strings 2  " + field);
                                }
                                else {
                                    return NO_SCORE;
                                }

                                double score = charHashesComparison(strings.getBytesValue());
                                return score >= minScore ? score : NO_SCORE;
                            }

                            public final double charHashesComparison(BytesRef fieldValue) {
                                double score = 0;
                                if (possibleScore == 0) {
                                    return NO_SCORE;
                                }

                                if (fieldValue == null || fieldValue.length == 0) {
                                    return NO_SCORE;
                                }

                                byte ver = fieldValue.bytes[1];
                                for (int i = 0; i < numHashes; ++i) {
                                    String hash = charHashes.get(i);
                                    if (hash == null) {
                                        continue;
                                    }
                                    if (header) {
                                        if (ver != hash.charAt(1)) {
                                            continue;
                                        }
                                    }
                                    else {
                                        if (fieldValue.length != hash.length()) {
                                            continue;
                                        }
                                    }
                                    score += (weights.get(i) * hammingDistance(fieldValue, hash));
                                }
                                score = normalize(score);
                                return score;
                            }

                            public final double normalize(double score) {
                                score = (score / possibleScore);
                                return score;
                            }

                            public final double hammingDistance(final BytesRef lhs, final String rhs) {
                                double score = singleScore;
                                for (int i = dataPos; i < length; i++) {
                                    score -= Math.abs(lhs.bytes[i] - rhs.charAt(i));
                                }
                                return score;
                            }

                            public int getResolution() {
                                return resolution;
                            }
                            public String getField() { return field; }
                            public int getNumHashes() {
                                return numHashes;
                            }
                        };
                    }

                    @Override
                    public boolean needs_score() {
                        return false;
                    }
                };

                return context.factoryClazz.cast(factory);
            }

            throw new IllegalArgumentException("Unknown script name " + code);
        }
    }

}

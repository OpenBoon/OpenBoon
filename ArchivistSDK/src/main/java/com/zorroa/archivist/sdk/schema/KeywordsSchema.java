package com.zorroa.archivist.sdk.schema;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by chambers on 11/24/15.
 */
public class KeywordsSchema implements Schema {

    private Set<String> level1 = Sets.newHashSet();
    private Set<String> level2 = Sets.newHashSet();
    private Set<String> level3 = Sets.newHashSet();
    private Set<String> level4 = Sets.newHashSet();
    private Set<String> level5 = Sets.newHashSet();

    public Set<String> getLevel1() {
        return level1;
    }

    public void setLevel1(Set<String> level1) {
        this.level1 = level1;
    }

    public Set<String> getLevel2() {
        return level2;
    }

    public void setLevel2(Set<String> level2) {
        this.level2 = level2;
    }

    public Set<String> getLevel3() {
        return level3;
    }

    public void setLevel3(Set<String> level3) {
        this.level3 = level3;
    }

    public Set<String> getLevel4() {
        return level4;
    }

    public void setLevel4(Set<String> level4) {
        this.level4 = level4;
    }

    public Set<String> getLevel5() {
        return level5;
    }

    public void setLevel5(Set<String> level5) {
        this.level5 = level5;
    }

    public void addKeywords(int confidence, String... keywords) {

        /**
         * TODO: if we keep this, move to reflection based.
         */
        Set<String> bucket;

        switch(confidence) {
            case 1:
                if (this.level1 == null) {
                    this.level1 = Sets.newHashSet();
                }
                bucket = this.level1;
                break;
            case 2:
                if (this.level2 == null) {
                    this.level2 = Sets.newHashSet();
                }
                bucket = this.level2;
                break;
            case 3:
                if (this.level3 == null) {
                    this.level3 = Sets.newHashSet();
                }
                bucket = this.level3;
                break;
            case 4:
                if (this.level4 == null) {
                    this.level4 = Sets.newHashSet();
                }
                bucket = this.level4;
                break;
            case 5:
                if (this.level5 == null) {
                    this.level5 = Sets.newHashSet();
                }
                bucket = this.level5;
                break;
            default:
                Preconditions.checkArgument(confidence >= 1 && confidence <= 5, "Illegal confidence value: 1-5");
                return;
        }

        for (String word: keywords) {
            bucket.add(word);
        }
    }


    @Override
    public String getNamespace() {
        return "keywords";
    }
}

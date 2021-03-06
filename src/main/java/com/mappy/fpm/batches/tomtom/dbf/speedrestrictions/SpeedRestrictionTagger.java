package com.mappy.fpm.batches.tomtom.dbf.speedrestrictions;

import com.google.common.collect.Maps;
import com.google.common.collect.TreeMultimap;
import com.mappy.fpm.batches.utils.Feature;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static com.mappy.fpm.batches.tomtom.helpers.RoadTagger.isReversed;

public class SpeedRestrictionTagger {

    private final SrDbf dbf;

    @Inject
    public SpeedRestrictionTagger(SrDbf dbf) {
        this.dbf = dbf;
    }

    public Map<String, String> tag(Feature feature) {
        TreeMultimap<String, Integer> speeds = TreeMultimap.create();
        List<SpeedRestriction> restrictions = dbf.getSpeedRestrictions(feature.getLong("ID"));
        boolean reversed = isReversed(feature);
        for (SpeedRestriction restriction : restrictions) {
            switch (restriction.getValidity()) {
                case positive:
                    speeds.put(reversed ? "maxspeed:backward" : "maxspeed:forward", restriction.getSpeed());
                    break;
                case negative:
                    speeds.put(reversed ? "maxspeed:forward" : "maxspeed:backward", restriction.getSpeed());
                    break;
                case both:
                    speeds.put("maxspeed", restriction.getSpeed());
                    break;
            }
        }
        Map<String, String> result = Maps.newHashMap();
        for (String key : speeds.keySet()) {
            result.put(key, String.valueOf(speeds.get(key).iterator().next()));
        }
        return result;
    }
}

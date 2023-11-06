package com.example.elevation.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Utils {
    private static final ArrayList<String> keysExcluded = new ArrayList<>(Arrays.asList(
            "CreationDate",
            "Creator",
            "EditDate",
            "Editor",
            "GlobalID",
            "OBJECTID"));

    private static final ArrayList<String> relevantKeys = new ArrayList<>(Arrays.asList(
            "Note",
            "Status"));

    private Utils() {} // private constructor to prevent instantiation

    public static Map<String, Object> filterAttributes (Map<String, Object> featureAttrs) {
        // If I don't create a copy of the Map, the original one will be modified
        Map<String, Object> filteredFeatureAttrs = new HashMap<>(featureAttrs);;
        filteredFeatureAttrs.entrySet().removeIf(entry -> !relevantKeys.contains(entry.getKey()));
        return filteredFeatureAttrs;
    }
}


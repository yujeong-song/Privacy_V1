package com.example.privacy_v1.utils;

import android.content.Context;

import com.example.privacy_v1.model.BrandData;
import com.example.privacy_v1.model.MakersMap;
import com.google.gson.Gson;

public class BrandRepository {
    private final Gson gson = new Gson();
    public MakersMap loadMakers(Context ctx) throws Exception {
        return gson.fromJson(AssetJson.read(ctx, "makers.json"), MakersMap.class);
    }

    public BrandData loadBrandData(Context ctx, String brandFile) throws Exception {
        return gson.fromJson(AssetJson.read(ctx, brandFile), BrandData.class);
    }

//    public MakersMap loadMakers(Context ctx) throws Exception {
//
//        String json = AssetJson.read(ctx, "makers.json");
//        return gson.fromJson(json, MakersMap.class);
//    }
//
//    public BrandData loadBrandData(Context ctx, String fileName) throws Exception {
//        String json = AssetJson.read(ctx, fileName);
//        return gson.fromJson(json, BrandData.class);
//    }
}
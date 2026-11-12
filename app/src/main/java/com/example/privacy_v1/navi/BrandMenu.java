package com.example.privacy_v1.navi;

import java.util.List;

public class BrandMenu {
    private final String id;
    private final String name;
    private final int tabLogoResId;
    private final int itemLogoResId;
    private final List<NavOption> items;

    public BrandMenu(
            String id,
            String name,
            int tabLogoResId,
            int itemLogoResId,
            List<NavOption> items
    ) {
        this.id = id;
        this.name = name;
        this.tabLogoResId = tabLogoResId;
        this.itemLogoResId = itemLogoResId;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTabLogoResId() {
        return tabLogoResId;
    }

    public int getItemLogoResId() {
        return itemLogoResId;
    }

    public List<NavOption> getItems() {
        return items;
    }

}

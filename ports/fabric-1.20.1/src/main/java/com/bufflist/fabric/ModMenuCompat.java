package com.bufflist.fabric;

import com.bufflist.BuffListSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** 装了 Mod Menu 时，模组列表里的配置按钮打开设置界面。 */
public final class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BuffListSettingsScreen::new;
    }
}

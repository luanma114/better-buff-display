package com.bufflist;

import com.bufflist.forge.ForgeEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;

@Mod(BuffList.MODID)
public class BuffList {
    public static final String MODID = "bufflist";

    public BuffList() {
        // 纯客户端：不要求服务器安装
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeEntry.init();
    }
}

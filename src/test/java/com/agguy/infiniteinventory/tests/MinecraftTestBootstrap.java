package com.agguy.infiniteinventory.tests;

import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

public final class MinecraftTestBootstrap {
    private static boolean bootstrapped;

    private MinecraftTestBootstrap() {
    }

    public static synchronized void ensureBootstrapped() {
        if (bootstrapped) {
            return;
        }
        if (LoadingModList.get() == null) {
            LoadingModList.of(
                    List.<ModFile>of(),
                    List.<ModFile>of(),
                    List.<ModInfo>of(),
                    List.<ModLoadingIssue>of(),
                    Map.of()
            );
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bootstrapped = true;
    }
}

package com.agguy.infiniteinventory.service.search;

public record DatabaseSearchEnvironmentSignature(
        int featureHash,
        boolean hasPermissions
) {
}

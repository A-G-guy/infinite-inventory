package com.agguy.build.versioning;

public abstract class RecommendModVersionTask extends AbstractVersionGovernanceTask {
    @Override
    protected boolean shouldFailOnInsufficientVersion() {
        return false;
    }
}

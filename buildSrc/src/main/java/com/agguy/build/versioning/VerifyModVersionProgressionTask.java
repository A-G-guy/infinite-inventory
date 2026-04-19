package com.agguy.build.versioning;

public abstract class VerifyModVersionProgressionTask extends AbstractVersionGovernanceTask {
    @Override
    protected boolean shouldFailOnInsufficientVersion() {
        return true;
    }
}

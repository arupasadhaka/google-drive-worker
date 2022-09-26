package com.oslash.integration.utils;

import java.util.Arrays;

public enum ResourceState {
    sync, add, remove, update, trash, untrash,  change;
    public boolean in(ResourceState... states) {
        return Arrays.stream(states).anyMatch(state -> state.equals(this));
    }
}

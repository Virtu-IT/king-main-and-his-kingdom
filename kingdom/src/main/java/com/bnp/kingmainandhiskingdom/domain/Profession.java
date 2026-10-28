package com.bnp.kingmainandhiskingdom.domain;

/** What a person can be trained to. Everybody starts without a profession. */
public enum Profession {
    SOLDIER(21),
    FARMER(16),
    NURSE(18),
    BLACKSMITH(18);

    private final int minimumAge;

    Profession(int minimumAge) {
        this.minimumAge = minimumAge;
    }

    public int getMinimumAge() {
        return minimumAge;
    }
}

package dev.cerez.calisto;

import lombok.Getter;

@Getter
public enum Quality {
    LOW(0.2f),
    MEDIUM(0.5f),
    HIGH(0.8f),
    ORIGINAL(1f);

    private final float quality;

    Quality(float quality) {
        this.quality = quality;
    }

}

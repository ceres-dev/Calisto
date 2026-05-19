package dev.cerez.calisto;

import lombok.Getter;

@Getter
public enum Resolution {
    SD(720*480),
    HD(1280*720),
    FULL_HD(1920*1080),
    QHD(2560*1440),
    UHD(3840*2160),
    UHD8(7680*4320),
    ORIGINAL(Long.MAX_VALUE);



    private final long pixels;

    Resolution(long pixels) {
        this.pixels = pixels;
    }
}
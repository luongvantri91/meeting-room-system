package com.company.meetingroom.booking;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(
        @DefaultValue("PT15M") Duration minDuration,
        @DefaultValue("PT8H") Duration maxDuration) {
}

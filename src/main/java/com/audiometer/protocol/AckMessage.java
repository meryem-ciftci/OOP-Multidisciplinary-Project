package com.audiometer.protocol;

public record AckMessage(String command) implements DeviceMessage {
}

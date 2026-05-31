package com.audiometer.protocol;

public record NakMessage(NakReason reason) implements DeviceMessage {
}

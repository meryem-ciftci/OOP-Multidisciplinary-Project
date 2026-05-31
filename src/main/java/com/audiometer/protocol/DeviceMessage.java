package com.audiometer.protocol;

public sealed interface DeviceMessage permits ReadyMessage, AckMessage, NakMessage, ResponseMessage {
}

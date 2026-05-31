# Serial Protocol Specification

## Overview

The Clinical Audiometer communicates with the DAC (Digital-to-Analog Converter) device via a serial interface. This document defines the complete protocol for command transmission and device response parsing.

## Physical Layer

- **Connection**: Serial port (RS-232 or USB-serial)
- **Baud Rate**: 9600 (standard audiometer protocol)
- **Data Bits**: 8
- **Parity**: None
- **Stop Bits**: 1
- **Flow Control**: None
- **Line Termination**: LF (newline) or CRLF

## Command Format

All commands are text-based, case-insensitive, newline-terminated.

### Command Categories

#### 1. Device Control

**PING**
- Purpose: Test device connectivity
- Response: READY
- Example: `PING\n`

**START**
- Purpose: Begin audio stimulus presentation
- Response: ACK:START
- Prerequisites: Frequency and amplitude must be set
- Example: `START\n`

**STOP**
- Purpose: Immediately stop audio stimulus
- Response: ACK:STOP
- Used after: Patient responds or timeout occurs
- Example: `STOP\n`

#### 2. Stimulus Configuration

**FREQ:<frequency>**
- Purpose: Set test frequency
- Format: `FREQ:` followed by frequency in Hz
- Valid Values: 250, 500, 1000, 2000, 4000, 8000
- Response: `ACK:FREQ:<frequency>` or `NAK:FREQ_OUT_OF_RANGE`
- Example: `FREQ:1000\n` → `ACK:FREQ:1000\n`

**AMP:<amplitude>**
- Purpose: Set DAC amplitude
- Format: `AMP:` followed by integer 0-4095
- Valid Range: 0 (maximum) to 4095 (minimum)
- Response: `ACK:AMP:<amplitude>` or `NAK:AMP_OUT_OF_RANGE`
- Example: `AMP:2048\n` → `ACK:AMP:2048\n`
- Note: This is the DAC control value, NOT clinical dB

#### 3. Combined Configuration

**FREQ:<frequency>,AMP:<amplitude>**
- Purpose: Set both frequency and amplitude in single message
- Format: Comma-separated, no spaces (or spaces ignored)
- Response: 
  - `ACK:FREQ:<frequency>,AMP:<amplitude>` if both valid
  - `NAK:FREQ_OUT_OF_RANGE` if frequency invalid
  - `NAK:AMP_OUT_OF_RANGE` if amplitude invalid
- Example: `FREQ:1000,AMP:2048\n` → `ACK:FREQ:1000,AMP:2048\n`

## Response Format

All responses are text-based, case-insensitive (normalized by parser to uppercase).

### Response Types

#### 1. Ready Status

**READY**
- Meaning: Device is initialized and ready for commands
- Sent: At device startup/initialization
- Example: `READY\n`

#### 2. Response Notification

**RESPONSE**
- Meaning: Patient responded to stimulus
- Sent: When patient presses response button during active stimulus
- Example: `RESPONSE\n`
- Timing: Should arrive while PLAYING_TONE stimulus is active

#### 3. Acknowledgment

**ACK:<command>**
- Meaning: Command successfully received and executed
- Format: `ACK:` followed by echoed command or command type
- Examples:
  - `ACK:START\n`
  - `ACK:STOP\n`
  - `ACK:FREQ:1000\n`
  - `ACK:AMP:2048\n`
  - `ACK:FREQ:1000,AMP:2048\n`

#### 4. Negative Acknowledgment

**NAK:<reason>**
- Meaning: Command rejected; reason provided
- Format: `NAK:` followed by reason code
- Valid Reasons:
  - `FREQ_OUT_OF_RANGE`: Frequency not in allowed set
  - `AMP_OUT_OF_RANGE`: Amplitude outside 0-4095
  - `UNKNOWN_CMD`: Unrecognized command
- Examples:
  - `NAK:FREQ_OUT_OF_RANGE\n` (if `FREQ:999` sent)
  - `NAK:AMP_OUT_OF_RANGE\n` (if `AMP:5000` sent)
  - `NAK:UNKNOWN_CMD\n` (if `INVALID` sent)

## Command Sequence Examples

### Example 1: Normal Test Sequence

```
[Host → Device]: PING
[Device → Host]: READY

[Host → Device]: FREQ:1000
[Device → Host]: ACK:FREQ:1000

[Host → Device]: AMP:2048
[Device → Host]: ACK:AMP:2048

[Host → Device]: START
[Device → Host]: ACK:START
[Stimulus plays for ~2 seconds...]
[Host → Device]: STOP
[Device → Host]: ACK:STOP
```

### Example 2: With Combined Command

```
[Host → Device]: FREQ:1000,AMP:2048
[Device → Host]: ACK:FREQ:1000,AMP:2048

[Host → Device]: START
[Device → Host]: ACK:START
[Stimulus plays...]
[Patient responds, presses button]
[Device → Host]: RESPONSE
[Host → Device]: STOP
[Device → Host]: ACK:STOP
```

### Example 3: Error Recovery

```
[Host → Device]: FREQ:999
[Device → Host]: NAK:FREQ_OUT_OF_RANGE

[Host → Device]: FREQ:1000      (retry with valid frequency)
[Device → Host]: ACK:FREQ:1000

[Host → Device]: AMP:5000
[Device → Host]: NAK:AMP_OUT_OF_RANGE

[Host → Device]: AMP:2048       (retry with valid amplitude)
[Device → Host]: ACK:AMP:2048
```

### Example 4: Multiple Frequencies

```
[Host → Device]: FREQ:250,AMP:2048
[Device → Host]: ACK:FREQ:250,AMP:2048

[Host → Device]: START
[Device → Host]: ACK:START
[Stimulus plays...]
[Device → Host]: RESPONSE
[Host → Device]: STOP
[Device → Host]: ACK:STOP

[Host → Device]: FREQ:500,AMP:2048    (next frequency)
[Device → Host]: ACK:FREQ:500,AMP:2048

[Host → Device]: START
[Device → Host]: ACK:START
[Stimulus plays...]
[No response from patient within 2 seconds - timeout]
```

## Protocol Invariants

### Semantic Domains

**Important Distinction:**

The protocol uses two distinct numerical domains that must not be confused:

1. **Clinical dB (Attenuation)**: 0-120
   - 0 dB = maximum volume (loudest hearing test level)
   - 120 dB = minimum volume (quietest audible)
   - Used in audiometry algorithms
   - NOT transmitted to device directly

2. **DAC Amplitude**: 0-4095
   - 0 = maximum output
   - 4095 = minimum output
   - Hardware control value
   - Transmitted via `AMP:<amplitude>` commands

The relationship between clinical dB and DAC amplitude is device-specific and determined by calibration curves.

### State Invariants

1. **Command Validity**:
   - FREQ must be one of: 250, 500, 1000, 2000, 4000, 8000
   - AMP must be integer in range [0, 4095]
   - Anything else generates NAK

2. **Device Readiness**:
   - Device must send READY before accepting commands
   - All commands (except PING) require READY first

3. **Stimulus Sequencing**:
   - START must have preceding FREQ/AMP configuration
   - STOP should follow START (stops active stimulus)
   - START while already playing should be rejected (device-dependent)

4. **Response Timing**:
   - RESPONSE arrives during stimulus presentation
   - RESPONSE should not arrive without active stimulus
   - Device should not send RESPONSE after STOP

## Parser Robustness

The parser implements defensive programming:

### Acceptable Variations

```
Case-insensitive:
  "ready" = "READY" = "Ready"
  "ack:start" = "ACK:START"

Whitespace-tolerant:
  "  READY  " → READY
  "ACK:  START  " → ACK:START
  "FREQ:  1000  " → FREQ:1000

Malformed handling:
  null input → Optional.empty()
  "" (empty string) → Optional.empty()
  "   " (blank) → Optional.empty()
  "garbage" → Optional.empty()
  "ACK:" (no command) → Optional.empty()
  "NAK:INVALID_REASON" → Optional.empty()
```

### Parser Error Behavior

The parser NEVER throws exceptions. Invalid input always returns `Optional.empty()`:

```java
Optional<DeviceMessage> result = parser.parse(input);

if (result.isPresent()) {
    DeviceMessage msg = result.get();
    // Process valid message
} else {
    // Log malformed input, ignore
}
```

## Command Builder Validation

The command builder validates before generating commands:

### Validation Rules

**Frequency Validation:**
```java
public boolean isFrequencyValid(int frequency) {
    return ClinicalConstants.ALLOWED_FREQUENCIES.contains(frequency);
}
```

Valid: 250, 500, 1000, 2000, 4000, 8000  
Invalid: Anything else (0, -1, 999, 1001, 10000, etc.)

**Amplitude Validation:**
```java
public boolean isAmplitudeValid(int amplitude) {
    return amplitude >= MIN_AMPLITUDE && amplitude <= MAX_AMPLITUDE;
}
```

Valid: 0 to 4095 (inclusive)  
Invalid: -1, 4096, Integer.MAX_VALUE, etc.

### Builder Returns

```java
// Valid input
Optional<String> cmd = builder.buildFrequencyCommand(1000);
// cmd = Optional.of("FREQ:1000")

// Invalid input
Optional<String> cmd = builder.buildFrequencyCommand(999);
// cmd = Optional.empty()

// Valid combined command
Optional<String> cmd = builder.buildCombinedCommand(1000, 2048);
// cmd = Optional.of("FREQ:1000,AMP:2048")

// Invalid combined (either parameter invalid)
Optional<String> cmd = builder.buildCombinedCommand(999, 2048);
// cmd = Optional.empty()
```

## Timeout Handling

Device communication has a 2-second timeout:

```
[Host → Device]: START
[Device → Host]: ACK:START
[Stimulus plays for ~2 seconds...]
[No RESPONSE received after 2 seconds]
↓
[Host detects timeout]
↓
[Host → Device]: STOP
[Device → Host]: ACK:STOP
↓
Treated as: NOT_HEARD response
```

## Error Recovery

### Device-Level Errors

If device rejects command:

```
[Host → Device]: FREQ:999
[Device → Host]: NAK:FREQ_OUT_OF_RANGE
↓
[Host validates before retry]
↓
[Host → Device]: FREQ:1000
[Device → Host]: ACK:FREQ:1000
```

### Protocol-Level Errors

If unparseable response received:

```
[Device → Host]: CORRUPTED_DATA
↓
[Parser returns Optional.empty()]
↓
[Host logs error, retries last command]
```

### Connection Errors

If no response received (timeout > 5 seconds):

```
[Host → Device]: PING
[Host waits 5 seconds...]
[No READY received]
↓
[Host reports: Device communication failure]
↓
[Transitions to ERROR state]
↓
[Session can be reset for retry]
```

## Performance Characteristics

- **Command Size**: 5-30 bytes (small)
- **Latency**: <100ms typical (serial at 9600 baud)
- **Response Time**: <10ms device processing
- **No Streaming**: Request/response pattern (not streaming)

## Extensibility

Protocol can be extended with:

- New frequencies (add to ALLOWED_FREQUENCIES)
- New commands (e.g., QUERY:device_status)
- New response types (e.g., DEVICE_INFO:name=AudioDev)

All extensions must maintain:
- Text-based format
- Newline termination
- Case-insensitivity
- Optional-based parsing (never throw)
- Clear separation of clinical dB from DAC amplitude

## Testing Protocol Parsing

### Test Scenarios

1. **Valid Commands**:
   - All valid frequency values
   - All valid amplitude ranges
   - Combined commands
   - Case variations
   - Whitespace variations

2. **Invalid Commands**:
   - Out-of-range frequencies
   - Out-of-range amplitudes
   - Malformed syntax
   - Unknown commands

3. **Robustness**:
   - Null input
   - Very long input
   - Special characters
   - Repeated prefixes
   - Garbage data

4. **Edge Cases**:
   - Boundary frequencies
   - Boundary amplitudes
   - Empty command parts
   - Whitespace-only input

All must parse safely without exceptions.

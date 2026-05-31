# State Machine Design

## Overview

The Clinical Audiometer implements a deterministic finite-state machine (FSM) that governs the entire test workflow. The state machine ensures that operations occur only in valid sequences and prevents invalid state transitions.

## State Definition

```java
enum TestState {
    IDLE,               // Initial state; no test active
    INITIALIZING,       // Session created; preparing test
    PLAYING_TONE,       // Actively presenting stimulus
    WAITING_RESPONSE,   // Stimulus played; awaiting patient response
    PROCESSING_RESPONSE,// Processing response; deciding next action
    THRESHOLD_FOUND,    // Threshold detected at current frequency/ear
    NEXT_FREQUENCY,     // Advancing to next frequency
    NEXT_EAR,          // Advancing to next ear
    TEST_FINISHED,      // All required testing complete
    ERROR               // Unrecoverable error occurred
}
```

## Event Definition

```java
enum ClinicalEvent {
    START_SESSION,      // Begin test from initialization
    PLAY_TONE,         // Present stimulus to patient
    WAIT_FOR_RESPONSE, // Transition to waiting after tone plays
    PROCESS_RESPONSE,  // Process patient's response
    THRESHOLD_FOUND,   // Threshold detected
    ADVANCE_FREQUENCY, // Move to next frequency
    ADVANCE_EAR,       // Move to next ear
    RESET,             // Reset to IDLE
    ERROR              // Error condition occurred
}
```

## State Transition Table

Valid transitions (current_state, event) → next_state:

| Current State | Event | Next State | Semantics |
|---------------|-------|-----------|-----------|
| IDLE | START_SESSION | INITIALIZING | Begin test |
| INITIALIZING | START_SESSION | PLAYING_TONE | Start actual testing |
| INITIALIZING | ERROR | ERROR | Error during init |
| PLAYING_TONE | WAIT_FOR_RESPONSE | WAITING_RESPONSE | After tone plays |
| WAITING_RESPONSE | PROCESS_RESPONSE | PROCESSING_RESPONSE | Begin processing |
| WAITING_RESPONSE | ERROR | ERROR | Error while waiting |
| PROCESSING_RESPONSE | PLAY_TONE | PLAYING_TONE | No threshold yet; repeat |
| PROCESSING_RESPONSE | THRESHOLD_FOUND | THRESHOLD_FOUND | Threshold detected |
| THRESHOLD_FOUND | ADVANCE_FREQUENCY | NEXT_FREQUENCY | Move to next frequency |
| THRESHOLD_FOUND | ADVANCE_EAR | NEXT_EAR | Move to next ear |
| NEXT_FREQUENCY | PLAY_TONE | PLAYING_TONE | Resume testing |
| NEXT_EAR | PLAY_TONE | PLAYING_TONE | Resume testing |
| TEST_FINISHED | RESET | IDLE | Complete; ready for new test |
| ERROR | RESET | IDLE | Error recovery |

## State Diagram

```
                         ┌──────────────┐
                         │    IDLE      │
                         │  (initial)   │
                         └────────┬─────┘
                                  │
                    START_SESSION │
                                  ▼
                         ┌──────────────────────┐
                         │   INITIALIZING       │ ◄──────────────┐
                         │                      │                │
                         └─┬──────────────────┬─┘                │
                           │                  │ ERROR            │
                  START     │                  ▼                 │
                 SESSION    │           ┌──────────┐             │
                           ▼           │  ERROR   │             │
                        ┌──────────┐    └────┬─────┘             │
                        │PLAYING   │         │ RESET             │
                        │ TONE     │         │                   │
                        └────┬─────┘         └──────────┬────────┘
                             │                          │
              WAIT_FOR_      │                          ▼
              RESPONSE       │                   Back to IDLE
                             ▼
                      ┌──────────────┐
                      │WAITING_      │ ◄──────────────────┐
                      │RESPONSE      │                    │
                      └────┬──────┬──┘                    │
                           │      │ ERROR                │
                           │      └──────────┐           │
          PROCESS_RESPONSE │                 ▼           │
                           │          ┌────────────┐     │
                           ▼          │   ERROR    │     │
                    ┌──────────────┐   └─────┬──────┘    │
                    │PROCESSING    │         │ RESET     │
                    │RESPONSE      │         │           │
                    └───┬────────┬─┘         └────┬──────┘
                        │        │                │
         THRESHOLD_FOUND│        │ PLAY_TONE     │
                        │        │                │
            ┌───────────┘        └────────┐       │
            │                             ▼       │
            │                      ┌──────────┐   │
            │                      │PLAYING   │   │
            │                      │ TONE (2) │   │
            │                      └──────────┘   │
            │                                     │
            ▼                                     │
    ┌──────────────────┐                         │
    │ THRESHOLD_FOUND  │                         │
    └─┬─────────────┬──┘                         │
      │             │                           │
      │ ADVANCE_    │ ADVANCE_EAR                │
      │ FREQUENCY   │                           │
      │ (or EAR)    │                           │
      ▼             ▼                           │
    ┌────────────┐  ┌────────────┐              │
    │ NEXT_      │  │  NEXT_EAR  │              │
    │ FREQUENCY  │  └────┬───────┘              │
    └────┬───────┘       │                      │
         │               │                      │
         │ PLAY_TONE     │ PLAY_TONE            │
         │               │                      │
         └───────┬───────┘                      │
                 │                              │
                 └──────────────────────────────┤
                                                │
                    [Repeat until all           │
                     frequencies/ears tested]  │
                                                │
                         ┌──────────────────┐   │
                         │ TEST_FINISHED    │   │
                         │ (after 12 tests) │   │
                         └────────┬─────────┘   │
                                  │ RESET       │
                                  ▼             │
                             IDLE ◄─────────────┘
```

## Transition Validation

The ClinicalStateMachine validates each transition:

```java
public boolean isValidTransition(TestState current, ClinicalEvent event) {
    return switch (current) {
        case IDLE -> event == ClinicalEvent.START_SESSION;
        case INITIALIZING -> 
            event == ClinicalEvent.START_SESSION || 
            event == ClinicalEvent.ERROR;
        case PLAYING_TONE -> 
            event == ClinicalEvent.WAIT_FOR_RESPONSE;
        case WAITING_RESPONSE -> 
            event == ClinicalEvent.PROCESS_RESPONSE || 
            event == ClinicalEvent.ERROR;
        case PROCESSING_RESPONSE -> 
            event == ClinicalEvent.PLAY_TONE || 
            event == ClinicalEvent.THRESHOLD_FOUND;
        case THRESHOLD_FOUND -> 
            event == ClinicalEvent.ADVANCE_FREQUENCY || 
            event == ClinicalEvent.ADVANCE_EAR;
        case NEXT_FREQUENCY -> 
            event == ClinicalEvent.PLAY_TONE;
        case NEXT_EAR -> 
            event == ClinicalEvent.PLAY_TONE;
        case TEST_FINISHED -> 
            event == ClinicalEvent.RESET;
        case ERROR -> 
            event == ClinicalEvent.RESET;
    };
}
```

## Typical Test Workflow (State Sequence)

```
IDLE
  ↓ START_SESSION
INITIALIZING
  ↓ START_SESSION
PLAYING_TONE (1000 Hz, LEFT ear, 40 dB)
  ↓ WAIT_FOR_RESPONSE
WAITING_RESPONSE
  ↓ PROCESS_RESPONSE (patient heard)
PROCESSING_RESPONSE
  ↓ PLAY_TONE (next level: 30 dB)
PLAYING_TONE (1000 Hz, LEFT ear, 30 dB)
  ↓ WAIT_FOR_RESPONSE
WAITING_RESPONSE
  ↓ PROCESS_RESPONSE (patient didn't hear)
PROCESSING_RESPONSE
  ↓ PLAY_TONE (next level: 35 dB)

[... repeat until threshold detected ...]

PROCESSING_RESPONSE
  ↓ THRESHOLD_FOUND (detected 20 dB at 1000 Hz)
THRESHOLD_FOUND
  ↓ ADVANCE_FREQUENCY (move to 2000 Hz)
NEXT_FREQUENCY
  ↓ PLAY_TONE (2000 Hz, LEFT ear, 40 dB)

[... repeat for 2000, 4000, 8000 Hz on LEFT ear ...]

THRESHOLD_FOUND (8000 Hz, LEFT ear)
  ↓ ADVANCE_EAR (switch to RIGHT ear)
NEXT_EAR
  ↓ PLAY_TONE (250 Hz, RIGHT ear, 40 dB)

[... repeat for all 6 frequencies on RIGHT ear ...]

THRESHOLD_FOUND (8000 Hz, RIGHT ear)
  ↓ [No more frequencies/ears]
TEST_FINISHED
  ↓ RESET
IDLE
```

## Error Handling

### Error State Entry

Errors can occur during:
- Initialization: Invalid session parameters
- Response waiting: Device communication failure
- State machine violation: Illegal transition attempt

```java
// From INITIALIZING
if (initializationFails) {
    session = session.withState(TestState.ERROR)
        .withClinicalError("Initialization failed");
}

// From WAITING_RESPONSE
if (deviceCommunicationFails) {
    session = session.withState(TestState.ERROR)
        .withClinicalError("No response from device");
}

// Invalid transition
machine.transition(TestState.IDLE, ClinicalEvent.PLAY_TONE)
    // Throws IllegalStateException
```

### Error Recovery

```
ERROR
  ↓ RESET
IDLE
  ↓ [start new test]
```

All errors eventually transition to IDLE for recovery.

## Implementation Details

### Transition Atomicity

Each transition is atomic:
```java
public TestState transition(TestState current, ClinicalEvent event) {
    // 1. Validate
    if (!isValidTransition(current, event)) {
        throw new IllegalStateException(...);
    }
    
    // 2. Calculate next state
    TestState next = switch(current) { ... };
    
    // 3. Return (never fails after validation)
    return next;
}
```

### No Side Effects

Transition logic is pure:
- Input: (current_state, event)
- Output: next_state
- No side effects
- Idempotent (same inputs → same output)

### Thread Safety

State machine is thread-safe:
- Stateless (no stored state)
- Pure functions
- Immutable return values

## State Properties

### Idempotent States

Some states have idempotent behavior:

```java
// In PLAYING_TONE, calling playTone() again is safe
if (session.state() == TestState.PLAYING_TONE) {
    return session;  // No change
}
```

### Terminal States

- `TEST_FINISHED`: Test complete (only transition: RESET → IDLE)
- `ERROR`: Error occurred (only transition: RESET → IDLE)

### Repeating Patterns

- `PLAYING_TONE` → `WAITING_RESPONSE` → `PROCESSING_RESPONSE` → `PLAYING_TONE` (or advance)
- Pattern repeats for each test stimulus

## Invariants

The state machine guarantees:

1. **Valid Sequence Only**: Invalid (state, event) pairs throw
2. **Deterministic**: Same (state, event) always produces same next_state
3. **No Deadlocks**: Every state has valid transitions leading forward or to IDLE
4. **Complete Coverage**: All states and events are documented
5. **No Silent Failures**: Invalid transitions throw immediately

## Testing State Machine

### Unit Test Coverage

```java
// Valid transitions
assert machine.isValidTransition(IDLE, START_SESSION);
assert machine.transition(IDLE, START_SESSION) == INITIALIZING;

// Invalid transitions
assert !machine.isValidTransition(IDLE, PLAY_TONE);
assertThrows(IllegalStateException.class, 
    () -> machine.transition(IDLE, PLAY_TONE));

// All states covered
for (TestState state : TestState.values()) {
    // At least one valid transition from each state
    assert !validTransitionsFrom(state).isEmpty();
}

// All events covered
for (ClinicalEvent event : ClinicalEvent.values()) {
    // Event is valid from at least one state
    assert validStatesFor(event).size() > 0;
}
```

### State Reachability

All states are reachable in normal test workflow:
```
IDLE → INITIALIZING → PLAYING_TONE → WAITING_RESPONSE 
→ PROCESSING_RESPONSE → [loop or advance] 
→ THRESHOLD_FOUND → NEXT_FREQUENCY/NEXT_EAR → PLAYING_TONE 
→ [loop] → TEST_FINISHED → IDLE
```

## Advantages of This Design

1. **Explicitness**: All valid transitions documented in code
2. **Compile-Time Safety**: Pattern matching exhaustiveness in switch
3. **Testability**: Each state transition independently testable
4. **Debuggability**: Clear state at each point (no implicit transitions)
5. **Predictability**: Deterministic behavior
6. **Maintenance**: Easy to add new states/events (extend switch statements)

## Potential Extensions

Could be extended with:
- **Entry/Exit Actions**: Code executed on entering/leaving states
- **Guarded Transitions**: Conditional logic before transition
- **Timeout Handling**: Automatic transitions on timeout
- **State History**: Previous states for rollback
- **Sub-states**: Hierarchical state machines (for complex inner logic)

Current design keeps things simple; if needed, could introduce these without breaking existing code.

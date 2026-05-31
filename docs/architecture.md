# Clinical Audiometer Architecture

## Overview

The Clinical Audiometer Engine is built using a **layered hexagonal architecture** with clear separation of concerns. Data flows through distinct layers: protocol (external interface) → session orchestration → clinical engine → domain models.

## Architectural Layers

### 1. Protocol Layer (`com.audiometer.protocol`)

**Responsibility**: Serial device communication

**Components**:
- `CommandBuilder`: Constructs valid DAC commands
  - Validates frequency (must be standard frequency)
  - Validates amplitude (0-4095 DAC range, NOT clinical dB)
  - Defensive: returns Optional.empty() on invalid input
  
- `ProtocolParser`: Parses device responses
  - Case-insensitive, whitespace-tolerant
  - Defensive: null-safe, returns Optional.empty() on malformed input
  - Never throws on bad input

- `DeviceMessage` hierarchy:
  - `ReadyMessage`: Device initialization complete
  - `ResponseMessage`: Patient responded to stimulus
  - `AckMessage`: Device acknowledged command
  - `NakMessage`: Device rejected command with reason

**Design Pattern**: Optional-based error handling; no exceptions for normal failures

**Key Invariants**:
- All message parsing is non-throwing
- Amplitude validation is **distinct** from clinical dB validation
- Frequency validation enforces standard list only

---

### 2. Session Management Layer (`com.audiometer.session`)

**Responsibility**: High-level test workflow orchestration

**Components**:
- `SessionManager`: Orchestrates complete test session
  - Initializes session with standard parameters
  - Manages state transitions via state machine
  - Records patient responses
  - Detects thresholds
  - Advances through frequencies and ears
  - Handles timeouts

**Session Lifecycle**:
1. `initialize()` → create new test session
2. `startTest()` → begin test
3. Loop: `playTone()` → `awaitResponse()` → `recordResponse()`
4. `resolveAfterProcessing()` → check threshold
5. If threshold: `advanceAfterThreshold()` → next frequency/ear
6. Repeat until `sessionCompleted() == true`

**Data Flow**:
```
TestSession (immutable state)
    ↓
SessionManager methods
    ↓
HughsonWestlakeEngine (algorithm)
    ↓
ClinicalStateMachine (state transitions)
    ↓
New TestSession (with updated state)
```

**Key Invariants**:
- SessionManager never modifies sessions; always returns new instances
- All operations are idempotent (safe to retry)
- Session state is always valid (validated before modifications)

---

### 3. Clinical Engine Layer (`com.audiometer.engine`)

**Responsibility**: Hughson-Westlake algorithm and state machine

**Components**:

#### HughsonWestlakeEngine
- `nextDb()`: Calculate next stimulus level
  - HEARD → decrease 10 dB (descending run)
  - NOT_HEARD/TIMEOUT → increase 5 dB (ascending run)
  - Clamps result to [0, 120]
  
- `detectThreshold()`: Delegates to ThresholdDetector
  
- `nextFrequencyIndex()`: Advance frequency index
  
- `nextEar()`: Alternate LEFT ↔ RIGHT
  
- `validateClinicalRules()`: Ensure session validity
  - Frequency in standard set
  - dB level in [0, 120]
  - No null fields

#### ThresholdDetector
- Analyzes response history
- Applies clinical threshold criteria:
  - ≥2 HEARD responses at same level
  - ≥3 total responses at that level
  - Returns lowest qualifying level (conservative)
- Only examines ascending-run responses

#### ClinicalStateMachine
- Deterministic state transitions
- Validates transition legality
- Throws `IllegalStateException` on invalid transitions
- Pure function: (current_state, event) → next_state

**Design Pattern**: State machine with explicit transition validation

**Key Invariants**:
- Only valid transitions allowed
- Transitions are atomic
- State is never `null`
- All events have defined handling per state

---

### 4. Domain Layer (`com.audiometer.domain`)

**Responsibility**: Immutable data models

**Components**:

#### TestSession (record)
- Central state holder
- Fields:
  - `currentEar`: LEFT or RIGHT
  - `currentFrequency`: Hz (from allowed set)
  - `currentDbLevel`: 0-120 dB
  - `currentPhase`: ASCENDING or DESCENDING
  - `state`: Current state machine state
  - `responseHistory`: Immutable list of responses
  - `thresholds`: Immutable list of detected thresholds
  - `frequencyIndex`: Index into standard frequency list
  - `sessionCompleted`: Boolean flag
- Compact constructor defensively copies lists
- Builder methods return new instances

#### ResponseRecord (record)
- `frequency`: Hz
- `dbLevel`: 0-120
- `responseType`: HEARD, NOT_HEARD, TIMEOUT
- `phase`: ASCENDING, DESCENDING
- `timestamp`: When response occurred
- `ear`: LEFT or RIGHT

#### ThresholdResult (record)
- `frequency`: Hz
- `ear`: LEFT or RIGHT
- `thresholdDb`: Detected threshold level

#### Enums
- `TestState`: 10 states in workflow
- `TestPhase`: ASCENDING or DESCENDING run
- `ResponseType`: HEARD, NOT_HEARD, TIMEOUT
- `Ear`: LEFT or RIGHT
- `ClinicalEvent`: Events triggering transitions

**Design Pattern**: Java records (immutable by default)

**Key Invariants**:
- No field is `null` (except as documented)
- Lists are never `null` (may be empty)
- All numeric fields have documented valid ranges

---

### 5. Utilities & Constants (`com.audiometer.util`)

**ClinicalConstants**:
```java
// Clinical dB range
MIN_DB = 0
MAX_DB = 120

// DAC amplitude range
MIN_AMPLITUDE = 0
MAX_AMPLITUDE = 4095

// Test frequencies (Hz)
ALLOWED_FREQUENCIES = [250, 500, 1000, 2000, 4000, 8000]

// Algorithm parameters
START_FREQUENCY = 1000
INITIAL_DB = 40
HEARD_STEP = 10
NOT_HEARD_STEP = 5

// Timeout (ms)
TIMEOUT_MILLIS = 2000
```

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│         Serial Device (Hardware)                        │
└─────────────────┬───────────────────────────────────────┘
                  │
                  │ String messages
                  ▼
        ┌──────────────────────┐
        │ ProtocolParser       │  Layer 1: Protocol
        │ - parse()            │
        └──────────┬───────────┘
                   │
                   │ DeviceMessage
                   ▼
        ┌──────────────────────┐
        │ SessionManager       │  Layer 2: Session
        │ - recordResponse()   │
        │ - playTone()         │
        └──────────┬───────────┘
                   │
                   │ TestSession (immutable)
                   ▼
        ┌──────────────────────┐
        │ HughsonWestlakeEngine│  Layer 3: Engine
        │ - nextDb()           │
        │ - detectThreshold()  │
        │ ClinicalStateMachine │
        │ - transition()       │
        └──────────┬───────────┘
                   │
                   │ Algorithm decisions
                   ▼
        ┌──────────────────────┐
        │ Domain Models        │  Layer 4: Domain
        │ - TestSession        │
        │ - ResponseRecord     │
        │ - ThresholdResult    │
        └──────────────────────┘
```

---

## Control Flow: Recording a Response

```
1. SessionManager.recordResponse(session, HEARD, timestamp)
   
   ↓
   
2. Validate: session.state() == WAITING_RESPONSE
   
   ↓
   
3. Create ResponseRecord capturing all details
   
   ↓
   
4. Call HughsonWestlakeEngine.nextDb()
   - Determine delta: HEARD → -10, NOT_HEARD → +5
   - Clamp to [0, 120]
   
   ↓
   
5. Call ClinicalStateMachine.transition(WAITING_RESPONSE, PROCESS_RESPONSE)
   - Returns PROCESSING_RESPONSE
   
   ↓
   
6. Return new TestSession with:
   - Updated responseHistory
   - Updated currentDbLevel
   - Updated state (PROCESSING_RESPONSE)
   
   ↓
   
7. Caller then calls SessionManager.resolveAfterProcessing()
   - Check ThresholdDetector criteria
   - Advance state accordingly
```

---

## Error Handling Strategy

### Protocol Layer
- Defensive parsing
- Return Optional.empty() on any parse failure
- No exceptions

### Session/Engine Layer
- Validate state before operation
- Throw IllegalStateException on invalid transition
- Use Optional for threshold detection results
- Use Optional for ClinicalError validation

### Domain Layer
- Immutability prevents invalid state
- Defensive constructor copies for lists

---

## Thread Safety

**The engine is thread-safe by design:**

1. **No shared mutable state**: SessionManager and engine don't store session state; all operations are stateless
2. **Immutable data**: TestSession and all domain objects are immutable records
3. **Functional operations**: Pure functions; result depends only on inputs

**Usage pattern**:
```java
// Safe to call from multiple threads
TestSession updated1 = manager.recordResponse(session, HEARD, time1);
TestSession updated2 = manager.recordResponse(session, NOT_HEARD, time2);

// Both operations are independent; no interference
```

---

## Extension Points

If new functionality is needed, prefer:

1. **New domain types** (as records in `com.audiometer.domain`)
2. **New engine methods** (pure functions in engine layer)
3. **New validation rules** (optional-based in protocol layer)

Avoid:
- Modifying domain records (immutable by design)
- Adding mutable state to SessionManager
- Breaking protocol parsing into different exception types
- Introducing new dependencies

---

## Testing Architecture

```
Domain Layer Tests
├── Individual record tests
├── Enum exhaustiveness
└── Immutability verification

Protocol Layer Tests
├── Parser edge cases
├── Command builder boundaries
└── Protocol robustness (fuzz)

Engine Layer Tests
├── Algorithm correctness
├── Threshold detection criteria
├── State machine transitions
└── Illegal transition rejection

Session Layer Tests
├── Workflow orchestration
├── Timeout handling
├── Repeated cycle testing
└── Session immutability

Integration Tests
└── Complete workflows
    ├── Multi-frequency tests
    ├── Multi-ear tests
    └── End-to-end sessions
```

---

## Performance Characteristics

- **Memory**: O(response_history_size) per session
- **Time**: O(1) for most operations
- **Threshold Detection**: O(n log n) where n = responses at current frequency (typically < 20)
- **No blocking**: All operations complete immediately
- **No allocation**: Reuses immutable collections (defensive copy only at construction)

---

## Design Decisions

### Why Immutability?

1. Thread-safe without synchronization
2. Audit trail (every change tracked)
3. Easier testing (no setup/teardown)
4. Functional composition
5. No aliasing bugs

### Why State Machine?

1. Enforces valid workflows
2. Prevents invalid operations
3. Documents valid transitions
4. Deterministic (testable)
5. Catches logic errors at compile time (exhaustive pattern matching)

### Why Optional?

1. No null pointers
2. Explicit about failure cases
3. Functional composition
4. Type-safe error handling

### Why Protocol Layer?

1. Isolates device communication
2. Testable in isolation
3. Defensive parsing
4. Easy to mock/stub

---

## Future Enhancements

Possible extensions while maintaining architecture:

- Multi-patient sessions (List<TestSession>)
- Audiogram export (new domain type)
- Signal averaging (new engine method)
- Masking logic (new state/event)
- Test result analysis (new domain type)
- Network protocol (new protocol layer)

All would fit within existing architecture without breaking immutability or state machine guarantees.

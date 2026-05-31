package com.audiometer.state;

import com.audiometer.domain.TestState;
import com.audiometer.engine.ClinicalEvent;

public record StateTransition(TestState from, TestState to, ClinicalEvent event) {
}

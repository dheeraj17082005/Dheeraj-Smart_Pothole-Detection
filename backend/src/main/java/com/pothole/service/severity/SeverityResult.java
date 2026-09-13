package com.pothole.service.severity;

import com.pothole.model.enums.SeverityClass;

public record SeverityResult(
        double severityScore,
        SeverityClass severityClass
) {}

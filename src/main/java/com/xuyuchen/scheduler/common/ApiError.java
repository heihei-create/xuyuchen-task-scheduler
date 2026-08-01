package com.xuyuchen.scheduler.common;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String message) {}

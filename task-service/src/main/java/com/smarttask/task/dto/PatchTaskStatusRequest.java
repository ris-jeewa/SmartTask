package com.smarttask.task.dto;

import com.smarttask.task.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record PatchTaskStatusRequest(@NotNull TaskStatus status) {}

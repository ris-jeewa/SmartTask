package com.smarttask.task.dto;

import com.smarttask.task.domain.Priority;
import com.smarttask.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateTaskRequest(
		@NotBlank @Size(max = 500) String title,
		@Size(max = 10_000) String description,
		TaskStatus status,
		@NotNull Priority priority,
		Long assigneeId,
		Instant dueDate) {}

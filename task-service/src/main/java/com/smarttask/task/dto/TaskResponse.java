package com.smarttask.task.dto;

import com.smarttask.task.domain.Priority;
import com.smarttask.task.domain.TaskStatus;

import java.time.Instant;

public record TaskResponse(
		Long id,
		String title,
		String description,
		TaskStatus status,
		Priority priority,
		Long assigneeId,
		Instant dueDate,
		Instant createdAt,
		Instant updatedAt) {}

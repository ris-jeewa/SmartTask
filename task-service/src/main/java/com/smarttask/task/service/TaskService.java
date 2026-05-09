package com.smarttask.task.service;

import com.smarttask.task.domain.Priority;
import com.smarttask.task.domain.TaskStatus;
import com.smarttask.task.dto.CreateTaskRequest;
import com.smarttask.task.dto.PatchTaskStatusRequest;
import com.smarttask.task.dto.TaskResponse;
import com.smarttask.task.dto.UpdateTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {

	Page<TaskResponse> list(
			TaskStatus status,
			Priority priority,
			Long assigneeId,
			boolean overdue,
			Pageable pageable);

	TaskResponse get(Long id);

	TaskResponse create(CreateTaskRequest request);

	TaskResponse update(Long id, UpdateTaskRequest request);

	TaskResponse patchStatus(Long id, PatchTaskStatusRequest request);

	void delete(Long id);
}

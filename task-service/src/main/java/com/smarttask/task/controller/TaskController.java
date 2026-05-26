package com.smarttask.task.controller;

import com.smarttask.task.domain.Priority;
import com.smarttask.task.domain.TaskStatus;
import com.smarttask.task.dto.CreateTaskRequest;
import com.smarttask.task.dto.PatchTaskStatusRequest;
import com.smarttask.task.dto.TaskResponse;
import com.smarttask.task.dto.UpdateTaskRequest;
import com.smarttask.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerJwt")
public class TaskController {

	private final TaskService taskService;

	@GetMapping
	@Operation(summary = "List tasks", description = "Paginated list; optional filters. Cancelled tasks omitted unless status=CANCELLED.")
	public Page<TaskResponse> listTasks(
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) Priority priority,
			@RequestParam(required = false) Long assigneeId,
			@RequestParam(defaultValue = "false") boolean overdue,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
					Pageable pageable) {
		return taskService.list(status, priority, assigneeId, overdue, pageable);
	}

	@GetMapping("/{id}")
	public TaskResponse getTask(@PathVariable Long id) {
		return taskService.get(id);
	}

	@PostMapping
	public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
		TaskResponse body = taskService.create(request);
		return ResponseEntity.created(URI.create("/api/tasks/" + body.id())).body(body);
	}

	@PutMapping("/{id}")
	public TaskResponse replaceTask(
			@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
		return taskService.update(id, request);
	}

	@PatchMapping("/{id}/status")
	public TaskResponse patchStatus(
			@PathVariable Long id, @Valid @RequestBody PatchTaskStatusRequest request) {
		return taskService.patchStatus(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTask(@PathVariable Long id) {
		taskService.delete(id);
	}
}

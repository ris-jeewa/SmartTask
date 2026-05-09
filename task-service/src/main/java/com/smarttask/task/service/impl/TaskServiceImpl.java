package com.smarttask.task.service.impl;

import com.smarttask.task.domain.Priority;
import com.smarttask.task.domain.Task;
import com.smarttask.task.domain.TaskStatus;
import com.smarttask.task.dto.CreateTaskRequest;
import com.smarttask.task.dto.PatchTaskStatusRequest;
import com.smarttask.task.dto.TaskResponse;
import com.smarttask.task.dto.UpdateTaskRequest;
import com.smarttask.task.exception.TaskNotFoundException;
import com.smarttask.task.mapper.TaskMapper;
import com.smarttask.task.repository.TaskRepository;
import com.smarttask.task.repository.TaskSpecifications;
import com.smarttask.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

	private final TaskRepository taskRepository;
	private final TaskMapper taskMapper;

	@Override
	@Transactional(readOnly = true)
	public Page<TaskResponse> list(
			TaskStatus status,
			Priority priority,
			Long assigneeId,
			boolean overdue,
			Pageable pageable) {
		Specification<Task> spec =
				Specification.where(TaskSpecifications.defaultExcludeCancelled(status))
						.and(TaskSpecifications.statusEquals(status))
						.and(TaskSpecifications.priorityEquals(priority))
						.and(TaskSpecifications.assigneeEquals(assigneeId));
		if (overdue) {
			spec = spec.and(TaskSpecifications.overdue(Instant.now()));
		}
		return taskRepository.findAll(spec, pageable).map(taskMapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public TaskResponse get(Long id) {
		return taskMapper.toResponse(taskOrThrow(id));
	}

	@Override
	@Transactional
	public TaskResponse create(CreateTaskRequest request) {
		Task task = taskMapper.toEntity(request);
		if (task.getStatus() == null) {
			task.setStatus(TaskStatus.TODO);
		}
		return taskMapper.toResponse(taskRepository.save(task));
	}

	@Override
	@Transactional
	public TaskResponse update(Long id, UpdateTaskRequest request) {
		Task task = taskOrThrow(id);
		assertWritable(task);
		taskMapper.applyUpdate(request, task);
		return taskMapper.toResponse(taskRepository.save(task));
	}

	@Override
	@Transactional
	public TaskResponse patchStatus(Long id, PatchTaskStatusRequest request) {
		Task task = taskOrThrow(id);
		assertWritable(task);
		task.setStatus(request.status());
		return taskMapper.toResponse(taskRepository.save(task));
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Task task = taskOrThrow(id);
		assertWritable(task);
		task.setStatus(TaskStatus.CANCELLED);
		taskRepository.save(task);
	}

	private Task taskOrThrow(Long id) {
		return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}

	private void assertWritable(Task task) {
		if (task.getStatus() == TaskStatus.CANCELLED) {
			throw new TaskNotFoundException(task.getId());
		}
	}
}

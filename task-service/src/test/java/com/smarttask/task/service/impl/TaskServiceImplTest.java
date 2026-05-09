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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private TaskMapper taskMapper;

	@InjectMocks
	private TaskServiceImpl taskService;

	@Test
	void create_defaultsStatusWhenNullInEntity() {
		CreateTaskRequest req =
				new CreateTaskRequest(
						"Title", "Desc", null, Priority.HIGH, 5L, Instant.parse("2030-01-01T00:00:00Z"));
		Task mapped = Task.builder().title("Title").priority(Priority.HIGH).build();
		Task saved = sampleTask(1L, TaskStatus.TODO);

		when(taskMapper.toEntity(req)).thenReturn(mapped);
		when(taskRepository.save(any(Task.class))).thenReturn(saved);
		when(taskMapper.toResponse(saved)).thenReturn(sampleResponse(saved));

		TaskResponse out = taskService.create(req);

		assertThat(out.id()).isEqualTo(1L);
		assertThat(out.status()).isEqualTo(TaskStatus.TODO);
		ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(TaskStatus.TODO);
	}

	@Test
	void get_returnsMappedTask() {
		Task task = sampleTask(2L, TaskStatus.IN_PROGRESS);
		when(taskRepository.findById(2L)).thenReturn(Optional.of(task));
		when(taskMapper.toResponse(task)).thenReturn(sampleResponse(task));

		TaskResponse r = taskService.get(2L);
		assertThat(r.title()).isEqualTo("Sample");
	}

	@Test
	void get_throwsWhenAbsent() {
		when(taskRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.get(99L)).isInstanceOf(TaskNotFoundException.class);
	}

	@Test
	void update_appliesAndSaves() {
		Task stored = sampleTask(3L, TaskStatus.TODO);
		UpdateTaskRequest body =
				new UpdateTaskRequest("N", "D", TaskStatus.IN_PROGRESS, Priority.MEDIUM, null, null);
		when(taskRepository.findById(3L)).thenReturn(Optional.of(stored));

		when(taskRepository.save(eq(stored))).thenReturn(stored);
		when(taskMapper.toResponse(stored)).thenReturn(sampleResponse(stored));

		taskService.update(3L, body);
		verify(taskMapper).applyUpdate(body, stored);
		verify(taskRepository).save(stored);
	}

	@Test
	void update_throwsWhenCancelled() {
		Task cancelled = sampleTask(4L, TaskStatus.CANCELLED);
		when(taskRepository.findById(4L)).thenReturn(Optional.of(cancelled));

		assertThatThrownBy(
						() ->
								taskService.update(
										4L,
										new UpdateTaskRequest(
												"n",
												"d",
												TaskStatus.TODO,
												Priority.LOW,
												null,
												null)))
				.isInstanceOf(TaskNotFoundException.class);
		verify(taskMapper, org.mockito.Mockito.never()).applyUpdate(any(), any());
	}

	@Test
	void patchStatus_updatesOnlyStatus() {
		Task stored = sampleTask(5L, TaskStatus.TODO);
		when(taskRepository.findById(5L)).thenReturn(Optional.of(stored));
		when(taskRepository.save(stored)).thenReturn(stored);
		when(taskMapper.toResponse(stored)).thenReturn(sampleResponse(stored));

		taskService.patchStatus(5L, new PatchTaskStatusRequest(TaskStatus.DONE));
		verify(taskRepository).save(stored);
		assertThat(stored.getStatus()).isEqualTo(TaskStatus.DONE);
	}

	@Test
	void delete_setsCancelledStatus() {
		Task stored = sampleTask(6L, TaskStatus.TODO);
		when(taskRepository.findById(6L)).thenReturn(Optional.of(stored));
		when(taskRepository.save(stored)).thenReturn(stored);

		taskService.delete(6L);
		assertThat(stored.getStatus()).isEqualTo(TaskStatus.CANCELLED);
		verify(taskRepository).save(stored);
	}

	@Test
	void delete_throwsWhenMissing() {
		when(taskRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.delete(7L)).isInstanceOf(TaskNotFoundException.class);
	}

	@Test
	void list_queriesRepositoryPage() {
		Task row = sampleTask(8L, TaskStatus.TODO);
		when(taskRepository.findAll(any(Specification.class), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));
		when(taskMapper.toResponse(row)).thenReturn(sampleResponse(row));

		var page =
				taskService.list(TaskStatus.TODO, Priority.HIGH, 1L, false, PageRequest.of(0, 20));

		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.getContent().get(0).id()).isEqualTo(8L);
		verify(taskRepository).findAll(any(Specification.class), any(Pageable.class));
	}

	private static Task sampleTask(Long id, TaskStatus status) {
		Instant now = Instant.parse("2025-05-08T12:00:00Z");
		return Task.builder()
				.id(id)
				.title("Sample")
				.description("d")
				.status(status)
				.priority(Priority.MEDIUM)
				.createdAt(now)
				.updatedAt(now)
				.build();
	}

	private static TaskResponse sampleResponse(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getPriority(),
				task.getAssigneeId(),
				task.getDueDate(),
				task.getCreatedAt(),
				task.getUpdatedAt());
	}
}

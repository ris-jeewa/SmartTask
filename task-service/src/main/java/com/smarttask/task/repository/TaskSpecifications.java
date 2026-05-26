package com.smarttask.task.repository;

import com.smarttask.task.domain.Priority;
import com.smarttask.task.domain.Task;
import com.smarttask.task.domain.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class TaskSpecifications {

	private TaskSpecifications() {}

	public static Specification<Task> statusEquals(TaskStatus status) {
		if (status == null) {
			return (root, q, cb) -> cb.conjunction();
		}
		return (root, q, cb) -> cb.equal(root.get("status"), status);
	}

	/** When no status filter is passed, hide soft-deleted (cancelled) tasks from the list. */
	public static Specification<Task> defaultExcludeCancelled(TaskStatus statusFilter) {
		if (statusFilter != null) {
			return (root, q, cb) -> cb.conjunction();
		}
		return (root, q, cb) -> cb.notEqual(root.get("status"), TaskStatus.CANCELLED);
	}

	public static Specification<Task> priorityEquals(Priority priority) {
		if (priority == null) {
			return (root, q, cb) -> cb.conjunction();
		}
		return (root, q, cb) -> cb.equal(root.get("priority"), priority);
	}

	public static Specification<Task> assigneeEquals(Long assigneeId) {
		if (assigneeId == null) {
			return (root, q, cb) -> cb.conjunction();
		}
		return (root, q, cb) -> cb.equal(root.get("assigneeId"), assigneeId);
	}

	public static Specification<Task> overdue(Instant now) {
		return (root, q, cb) ->
				cb.and(
						cb.isNotNull(root.get("dueDate")),
						cb.lessThan(root.get("dueDate"), now),
						root.get("status").in(TaskStatus.TODO, TaskStatus.IN_PROGRESS));
	}
}

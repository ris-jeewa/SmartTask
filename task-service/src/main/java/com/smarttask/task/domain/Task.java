package com.smarttask.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(length = 10_000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 24)
	private TaskStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Priority priority;

	@Column(name = "assignee_id")
	private Long assigneeId;

	@Column(name = "due_date")
	private Instant dueDate;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
		if (status == null) {
			status = TaskStatus.TODO;
		}
		if (priority == null) {
			priority = Priority.MEDIUM;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}

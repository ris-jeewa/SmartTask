package com.smarttask.task.repository;

import com.smarttask.task.domain.Task;
import com.smarttask.task.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

	List<Task> findByStatusAndAssigneeId(TaskStatus status, Long assigneeId);

	List<Task> findByAssigneeId(Long assigneeId);
}

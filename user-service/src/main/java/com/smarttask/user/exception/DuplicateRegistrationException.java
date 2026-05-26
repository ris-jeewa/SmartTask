package com.smarttask.user.exception;

public class DuplicateRegistrationException extends RuntimeException {

	public DuplicateRegistrationException(String message) {
		super(message);
	}
}

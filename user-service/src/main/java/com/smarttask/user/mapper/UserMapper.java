package com.smarttask.user.mapper;

import com.smarttask.user.domain.User;
import com.smarttask.user.dto.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
	@Mapping(target = "role", expression = "java(com.smarttask.user.domain.Role.USER)")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	User toEntity(RegisterRequest request);
}

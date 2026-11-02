package app.user.mapper;

import app.user.domain.UserAccount;
import app.user.dto.UserResponse;
import org.mapstruct.Mapper;

/**
 * Maps the {@link UserAccount} entity to the {@link UserResponse} DTO, so controllers never expose
 * the entity. The password hash is deliberately not part of {@link UserResponse}.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

  UserResponse toResponse(UserAccount userAccount);
}

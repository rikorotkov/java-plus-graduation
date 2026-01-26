package ru.practicum.service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.parameters.UserAdminSearchParam;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest user);

    void delete(Long userId);

    List<UserDto> getUsers(UserAdminSearchParam params);

    UserDto getUserById(@NotNull @Positive Long userId);

    UserShortDto getUserShortDtoById(@NotNull @Positive Long userId);
}

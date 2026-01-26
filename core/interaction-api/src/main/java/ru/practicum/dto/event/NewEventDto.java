package ru.practicum.dto.event;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data

public class NewEventDto {
    @NotNull
    @NotBlank
    @Size(min = 20, max = 2000)
    private String annotation;
    private Long category;
    @NotNull
    @NotBlank
    @Size(min = 20, max = 7000)
    private String description;
    @Future
    @NotNull
    private LocalDateTime eventDate;
    private LocationDto location;
    private Boolean paid = false;
    @PositiveOrZero
    private Integer participantLimit = 0;
    private Boolean requestModeration = true;
    @Size(min = 3, max = 120)
    private String title;
}

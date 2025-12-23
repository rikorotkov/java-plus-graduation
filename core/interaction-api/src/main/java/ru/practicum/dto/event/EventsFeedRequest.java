package ru.practicum.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.params.PublicEventSearchParam;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventsFeedRequest {
    private List<Long> followedUsersIds;
    private PublicEventSearchParam param;
}

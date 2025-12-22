package ru.practicum.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/confirmed")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds) throws FeignException;
}

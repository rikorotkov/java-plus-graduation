package ru.practicum;

import com.google.common.collect.Lists;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;

@Slf4j
@Component
public class AnalyzerClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        try {
            return Lists.newArrayList(client.getInteractionsCount(request));
        } catch (StatusRuntimeException ex) {
            log.debug("Analyzer gRPC is unavailable (getInteractionsCount): {}", ex.getMessage());
            return List.of();
        }
    }

    public List<RecommendedEventProto> getSimilarEvent(SimilarEventsRequestProto request) {
        try {
            return Lists.newArrayList(client.getSimilarEvents(request));
        } catch (StatusRuntimeException ex) {
            log.debug("Analyzer gRPC is unavailable (getSimilarEvents): {}", ex.getMessage());
            return List.of();
        }
    }

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        try {
            return Lists.newArrayList(client.getRecommendationsForUser(request));
        } catch (StatusRuntimeException ex) {
            log.debug("Analyzer gRPC is unavailable (getRecommendationsForUser): {}", ex.getMessage());
            return List.of();
        }
    }
}

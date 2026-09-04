package com.example.orderprocessing.payment.idempotency;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProcessedEventService {

    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    public boolean alreadyProcessed(UUID eventId) {
        return processedEventRepository.existsById(eventId);
    }

    public void markProcessed(UUID eventId, String eventType) {
        processedEventRepository.save(new ProcessedEvent(eventId, eventType, Instant.now()));
    }
}

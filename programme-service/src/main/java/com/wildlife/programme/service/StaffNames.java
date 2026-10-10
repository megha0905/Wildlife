package com.wildlife.programme.service;

import com.wildlife.programme.client.ResourceClient;
import com.wildlife.programme.client.StaffRefDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Looks up staff names in resource-service at read time (CONTRACTS.md 7.8). Failures become a warning. */
@Component
public class StaffNames {

    private static final Logger log = LoggerFactory.getLogger(StaffNames.class);

    private final ResourceClient resourceClient;

    public StaffNames(ResourceClient resourceClient) {
        this.resourceClient = resourceClient;
    }

    /** Result: id -> full name, plus a warning when resource-service could not be reached. */
    public record Lookup(Map<Long, String> names, String warning) {
        public String nameOf(Long id) {
            return id == null ? null : names.get(id);
        }
    }

    public Lookup lookup(Collection<Long> ids) {
        List<Long> wanted = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (wanted.isEmpty()) {
            return new Lookup(Map.of(), null);
        }
        try {
            Map<Long, String> names = new HashMap<>();
            for (StaffRefDto s : resourceClient.lookupStaff(wanted)) {
                names.put(s.id(), s.fullName());
            }
            return new Lookup(names, null);
        } catch (Exception e) {
            log.warn("resource-service unavailable for staff lookup: {}", e.getMessage());
            return new Lookup(Map.of(), "Staff names could not be loaded from resource-service.");
        }
    }
}

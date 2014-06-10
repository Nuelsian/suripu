package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class KinesisConfiguration {

    @Valid
    @NotNull
    @JsonProperty
    private String endpoint;

    public String getEndpoint() {
        return endpoint;
    }


    @Valid
    @NotNull
    @JsonProperty("streams")
    private Map<Queues, String> streams;

    public ImmutableMap<Queues, String> getStreams() {
        return ImmutableMap.copyOf(streams);
    }
}

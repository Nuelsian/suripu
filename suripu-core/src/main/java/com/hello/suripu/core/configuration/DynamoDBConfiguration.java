package com.hello.suripu.core.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class DynamoDBConfiguration extends Configuration{

    @Valid
    @NotNull
    @JsonProperty
    private String endpoint;

    public String getEndpoint(){
        return endpoint;
    }


    @Valid
    @NotNull
    @JsonProperty
    private String region;

    public String getRegion(){ return region; }


    @Valid
    @NotNull
    @JsonProperty("keyStoreTable")
    private String keyStoreTable;

    public String getKeyStoreTable(){ return keyStoreTable; }
}

package com.github.learntocode2013.stampede.request_response;

import java.io.Serializable;

public record DomainResponse(String responseBody, String requestBody, String status) implements Serializable {
}

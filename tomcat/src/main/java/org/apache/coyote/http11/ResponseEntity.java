package org.apache.coyote.http11;

import org.apache.coyote.http11.response.HttpStatus;

public class ResponseEntity {

    private HttpStatus status;
    private String body;

    public ResponseEntity(HttpStatus status, String body) {
        this.status = status;
        this.body = body;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }
}
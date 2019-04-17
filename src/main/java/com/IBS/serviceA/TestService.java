package com.IBS.serviceA;

import feign.RequestLine;

public interface TestService {

    @RequestLine("GET /greeting")
    public String greeting();


}
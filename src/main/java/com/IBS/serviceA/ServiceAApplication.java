package com.IBS.serviceA;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.IBS.serviceA.feign.FeignDecorators;
import com.IBS.serviceA.feign.Resilience4jFeign;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

@SpringBootApplication
@RestController
public class ServiceAApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceAApplication.class, args);
	}
	
	
   
    CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("serviceB");
    TestServiceFallback testServiceFallback = new TestServiceFallback();
    FeignDecorators decorators = FeignDecorators.builder()
            .withFallback(testServiceFallback, FeignException.class)            
            .build();
    
	@RequestMapping("/greeting1")
    public String greeting1()  {
		TestService myService = Resilience4jFeign.builder(decorators).target(TestService.class, "http://localhost:8081/");
        return myService.greeting();
    }
	
	
 
}

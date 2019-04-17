package com.IBS.serviceA;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.IBS.serviceA.feign.FeignDecorators;
import com.IBS.serviceA.feign.Resilience4jFeign;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.prometheus.client.CollectorRegistry;

@SpringBootApplication
@RestController
public class ServiceAApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceAApplication.class, args);
	}
	
	private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
   
    CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
    TestServiceFallback testServiceFallback = new TestServiceFallback();
    FeignDecorators decorators = FeignDecorators.builder()
            .withFallback(testServiceFallback, FeignException.class)            
            .build();
    
	@RequestMapping("/greeting1")
    public String greeting1()  {
		TestService myService = Resilience4jFeign.builder(decorators).target(TestService.class, "http://localhost:8081/");
        return myService.greeting();
    }
	
	@RequestMapping("/greeting")
    public Greeting greeting(@RequestParam(value="name", defaultValue="World") String name) {
        return new Greeting(counter.incrementAndGet(),
                            String.format(template, name));
    }
	
	@Bean
	public CollectorRegistry collectorRegistry() {
	    return CollectorRegistry.defaultRegistry;
	}
 
}

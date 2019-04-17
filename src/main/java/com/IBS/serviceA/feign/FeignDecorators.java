/*
 *
 * Copyright 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package com.IBS.serviceA.feign;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.vavr.CheckedFunction1;

/**
 * Builder to help build stacked decorators. <br>
 *
 * <pre>
 * {
 *     &#64;code
 *     CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("backendName");
 *     RateLimiter rateLimiter = RateLimiter.ofDefaults("backendName");
 *     FeignDecorators decorators = FeignDecorators.builder()
 *             .withCircuitBreaker(circuitBreaker)
 *             .withRateLimiter(rateLimiter)
 *             .build();
 *     MyService myService = Resilience4jFeign.builder(decorators).target(MyService.class, "http://localhost:8080/");
 * }
 * </pre>
 *
 * The order in which decorators are applied correspond to the order in which they are declared. For
 * example, calling {@link FeignDecorators.Builder#withFallback(Object)} before
 * {@link FeignDecorators.Builder#withCircuitBreaker(CircuitBreaker)} would mean that the fallback
 * is called when the HTTP request fails, but would no longer be reachable if the CircuitBreaker
 * were open. However, reversing the order would mean that the fallback is called both when the HTTP
 * request fails and when the CircuitBreaker is open. <br>
 * So be wary of this when designing your "resilience" strategy.
 */
public class FeignDecorators implements FeignDecorator {

    private final List<FeignDecorator> decorators;

    private FeignDecorators(List<FeignDecorator> decorators) {
        this.decorators = decorators;
    }

    @Override
    public CheckedFunction1<Object[], Object> decorate(CheckedFunction1<Object[], Object> fn,
            Method method, MethodHandler methodHandler, Target<?> target) {
        CheckedFunction1<Object[], Object> decoratedFn = fn;
        for (final FeignDecorator decorator : decorators) {
            decoratedFn = decorator.decorate(decoratedFn, method, methodHandler, target);
        }
        return decoratedFn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<FeignDecorator> decorators = new ArrayList<>();

        /**
         * Adds a {@link CircuitBreaker} to the decorator chain.
         *
         * @param circuitBreaker a fully configured {@link CircuitBreaker}.
         * @return the builder
         */
        public Builder withCircuitBreaker(CircuitBreaker circuitBreaker) {
            decorators.add((fn, m, mh, t) -> CircuitBreaker.decorateCheckedFunction(circuitBreaker, fn));
            return this;
        }

        /**
         * Adds a {@link RateLimiter} to the decorator chain.
         *
         * @param rateLimiter a fully configured {@link RateLimiter}.
         * @return the builder
         */
        public Builder withRateLimiter(RateLimiter rateLimiter) {
            decorators.add((fn, m, mh, t) -> RateLimiter.decorateCheckedFunction(rateLimiter, fn));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the feign interface, i.e. the interface specified when calling
         *        {@link Resilience4jFeign.Builder#target(Class, String)}.
         * @return the builder
         */
        public Builder withFallback(Object fallback) {
            decorators.add(new FallbackDecorator<Object>(fallback));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the feign interface, i.e. the interface specified when calling
         *        {@link Resilience4jFeign.Builder#target(Class, String)}.
         * @param filter only {@link Exception}s matching the specified {@link Exception} will
         *        trigger the fallback.
         * @return the builder
         */
        public Builder withFallback(Object fallback, Class<? extends Exception> filter) {
            decorators.add(new FallbackDecorator<Object>(fallback, filter));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the feign interface, i.e. the interface specified when calling
         *        {@link Resilience4jFeign.Builder#target(Class, String)}.
         * @param filter the filter must return <code>true</code> for the fallback to be called.
         * @return the builder
         */
        public Builder withFallback(Object fallback, Predicate<Exception> filter) {
            decorators.add(new FallbackDecorator<Object>(fallback, filter));
            return this;
        }

        /**
         * Builds the decorator chain. This can then be used to setup an instance of
         * {@link Resilience4jFeign}.
         *
         * @return the decorators.
         */
        public FeignDecorators build() {
            return new FeignDecorators(decorators);
        }

    }

}
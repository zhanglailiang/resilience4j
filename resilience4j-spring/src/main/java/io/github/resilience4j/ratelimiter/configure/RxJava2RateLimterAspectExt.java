/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.ratelimiter.configure;

import static io.github.resilience4j.utils.AspectUtil.newHashSet;

import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerAspect;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.operator.RateLimiterOperator;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;

/**
 * the Rx RateLimiter logic support for the spring AOP
 * conditional on the presence of Rx classes on the spring class loader
 */
public class RxJava2RateLimterAspectExt implements RateLimiterAspectExt {

	private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);
	private final Set<Class> rxSupportedTypes = newHashSet(ObservableSource.class, SingleSource.class, CompletableSource.class, MaybeSource.class, Flowable.class);

	/**
	 * @param returnType the AOP method return type class
	 * @return boolean if the method has Rx java 2 rerun type
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean canHandleReturnType(Class returnType) {
		return rxSupportedTypes.stream().anyMatch(classType -> classType.isAssignableFrom(returnType));
	}

	/**
	 * @param proceedingJoinPoint Spring AOP proceedingJoinPoint
	 * @param rateLimiter         the configured rateLimiter
	 * @param methodName          the method name
	 * @return the result object
	 * @throws Throwable exception in case of faulty flow
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object handle(ProceedingJoinPoint proceedingJoinPoint, RateLimiter rateLimiter, String methodName) throws Throwable {
		RateLimiterOperator<?> rateLimiterOperator = RateLimiterOperator.of(rateLimiter);
		Object returnValue = proceedingJoinPoint.proceed();
		return executeRxJava2Aspect(rateLimiterOperator, returnValue);
	}

	private Object executeRxJava2Aspect(RateLimiterOperator rateLimiterOperator, Object returnValue) {
		if (returnValue instanceof ObservableSource) {
			Observable<?> observable = (Observable) returnValue;
			return observable.lift(rateLimiterOperator);
		} else if (returnValue instanceof SingleSource) {
			Single<?> single = (Single) returnValue;
			return single.lift(rateLimiterOperator);
		} else if (returnValue instanceof CompletableSource) {
			Completable completable = (Completable) returnValue;
			return completable.lift(rateLimiterOperator);
		} else if (returnValue instanceof MaybeSource) {
			Maybe<?> maybe = (Maybe) returnValue;
			return maybe.lift(rateLimiterOperator);
		} else if (returnValue instanceof Flowable) {
			Flowable<?> flowable = (Flowable) returnValue;
			return flowable.lift(rateLimiterOperator);
		} else {
			logger.error("Unsupported type for Rate limiter RxJava2 {}", returnValue.getClass().getTypeName());
			throw new IllegalArgumentException("Not Supported type for the Rate limiter in RxJava2 :" + returnValue.getClass().getName());
		}
	}
}

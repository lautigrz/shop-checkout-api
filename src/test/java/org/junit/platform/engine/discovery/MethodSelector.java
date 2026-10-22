/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.discovery;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.apiguardian.api.API.Status.STABLE;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ClassUtils;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ToStringBuilder;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.DiscoverySelectorIdentifier;

@API(status = STABLE, since = "1.0")
public final class MethodSelector implements DiscoverySelector {

	private final @Nullable ClassLoader classLoader;
	private final String className;
	private final String methodName;
	private final String parameterTypeNames;

	private volatile @Nullable Class<?> javaClass;

	private volatile @Nullable Method javaMethod;

	private volatile Class<?> @Nullable [] parameterTypes;

	MethodSelector(@Nullable ClassLoader classLoader, String className, String methodName, String parameterTypeNames) {
		this.classLoader = classLoader;
		this.className = className;
		this.methodName = methodName;
		this.parameterTypeNames = parameterTypeNames;
	}

	MethodSelector(Class<?> javaClass, String methodName, String parameterTypeNames) {
		this(javaClass.getClassLoader(), javaClass.getName(), methodName, parameterTypeNames);
		this.javaClass = javaClass;
	}

	MethodSelector(@Nullable ClassLoader classLoader, String className, String methodName, Class<?>... parameterTypes) {
		this(classLoader, className, methodName, ClassUtils.nullSafeToString(Class::getTypeName, parameterTypes));
		this.parameterTypes = parameterTypes.clone();
	}

	MethodSelector(Class<?> javaClass, String methodName, Class<?>... parameterTypes) {
		this(javaClass.getClassLoader(), javaClass.getName(), methodName,
			ClassUtils.nullSafeToString(Class::getTypeName, parameterTypes));
		this.javaClass = javaClass;
		this.parameterTypes = parameterTypes.clone();
	}

	MethodSelector(Class<?> javaClass, Method method) {
		this(javaClass, method, method.getParameterTypes());
	}

	private MethodSelector(Class<?> javaClass, Method method, Class<?>... parameterTypes) {
		this(javaClass.getClassLoader(), javaClass.getName(), method.getName(),
			ClassUtils.nullSafeToString(Class::getTypeName, parameterTypes));
		this.javaClass = javaClass;
		this.javaMethod = method;
		this.parameterTypes = parameterTypes;
	}

	@API(status = MAINTAINED, since = "1.13.3")
	public @Nullable ClassLoader getClassLoader() {
		return this.classLoader;
	}

	public String getClassName() {
		return this.className;
	}

	public String getMethodName() {
		return this.methodName;
	}

	@API(status = STABLE, since = "1.10")
	public String getParameterTypeNames() {
		return this.parameterTypeNames;
	}

	/**
	 * Backwards compatibility for IntelliJ IDEA JUnit test runner reflection call.
	 * Restores the method removed in JUnit 6.
	 */
	public String getMethodParameterTypes() {
		return this.parameterTypeNames;
	}

	public Class<?> getJavaClass() {
		return lazyLoadJavaClass();
	}

	public Method getJavaMethod() {
		return lazyLoadJavaMethod();
	}

	@API(status = MAINTAINED, since = "1.13.3")
	public Class<?>[] getParameterTypes() {
		return lazyLoadParameterTypes().clone();
	}

	private Class<?> lazyLoadJavaClass() {
		Class<?> value = this.javaClass;
		if (value == null) {
			Try<Class<?>> tryToLoadClass = this.classLoader == null
				? ReflectionSupport.tryToLoadClass(this.className)
				: ReflectionSupport.tryToLoadClass(this.className, this.classLoader);
			value = tryToLoadClass.getNonNullOrThrow(cause ->
				new PreconditionViolationException("Could not load class with name: " + this.className, cause));
			this.javaClass = value;
		}
		return value;
	}

	private Method lazyLoadJavaMethod() {
		var value = this.javaMethod;
		if (value == null) {
			Class<?> javaClass = lazyLoadJavaClass();
			var parameterTypes = lazyLoadParameterTypes();
			if (parameterTypes.length > 0) {
				value = ReflectionSupport.findMethod(javaClass, this.methodName, parameterTypes).orElseThrow(
					() -> new PreconditionViolationException(
						"Could not find method with name [%s] and parameter types [%s] in class [%s].".formatted(
							this.methodName, this.parameterTypeNames, javaClass.getName())));
			}
			else {
				value = ReflectionSupport.findMethod(javaClass, this.methodName).orElseThrow(
					() -> new PreconditionViolationException(
						"Could not find method with name [%s] in class [%s].".formatted(this.methodName,
							javaClass.getName())));
			}
			this.javaMethod = value;
		}
		return value;
	}

	private Class<?>[] lazyLoadParameterTypes() {
		var value = this.parameterTypes;
		if (value == null) {
			value = ReflectionUtils.resolveParameterTypes(lazyLoadJavaClass(), this.methodName,
				this.parameterTypeNames);
			this.parameterTypes = value;
		}
		return value;
	}

	@API(status = STABLE, since = "1.3")
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MethodSelector that = (MethodSelector) o;
		return Objects.equals(this.className, that.className)
				&& Objects.equals(this.methodName, that.methodName)
				&& Objects.equals(this.parameterTypeNames, that.parameterTypeNames);
	}

	@API(status = STABLE, since = "1.3")
	@Override
	public int hashCode() {
		return Objects.hash(this.className, this.methodName, this.parameterTypeNames);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("className", getClassName())
				.append("methodName", getMethodName())
				.append("parameterTypes", getParameterTypeNames())
				.append("classLoader", getClassLoader())
				.toString();
	}

	@Override
	public Optional<DiscoverySelectorIdentifier> toIdentifier() {
		String fullyQualifiedMethodName = ReflectionUtils.getFullyQualifiedMethodName(this.className, this.methodName,
			this.parameterTypeNames);
		return Optional.of(DiscoverySelectorIdentifier.create(IdentifierParser.PREFIX, fullyQualifiedMethodName));
	}

	@API(status = INTERNAL, since = "1.11")
	public static class IdentifierParser implements DiscoverySelectorIdentifierParser {

		private static final String PREFIX = "method";

		public IdentifierParser() {
		}

		@Override
		public String getPrefix() {
			return PREFIX;
		}

		@Override
		public Optional<MethodSelector> parse(DiscoverySelectorIdentifier identifier, Context context) {
			return Optional.of(DiscoverySelectors.selectMethod(identifier.getValue()));
		}

	}

}

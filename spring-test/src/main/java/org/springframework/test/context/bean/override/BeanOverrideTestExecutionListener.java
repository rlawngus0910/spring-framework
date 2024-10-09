/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.bean.override;

import java.lang.reflect.Field;
import java.util.List;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * {@code TestExecutionListener} that enables {@link BeanOverride @BeanOverride}
 * support in tests, by injecting overridden beans in appropriate fields of the
 * test instance.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
public class BeanOverrideTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Executes almost last ({@code LOWEST_PRECEDENCE - 50}).
	 */
	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE - 50;
	}

	/**
	 * Inject each {@link BeanOverride @BeanOverride} field in the
	 * {@linkplain Object test instance} of the supplied {@linkplain TestContext
	 * test context} with a corresponding bean override instance.
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		injectFields(testContext, false);
	}

	/**
	 * Re-inject each {@link BeanOverride @BeanOverride} field in the
	 * {@linkplain Object test instance} of the supplied {@linkplain TestContext
	 * test context} with a corresponding bean override instance.
	 * <p>This method does nothing if the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} attribute is not present in the
	 * {@code TestContext} with a value of {@link Boolean#TRUE}.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		Object reinjectDependenciesAttribute = testContext.getAttribute(
				DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE);
		if (Boolean.TRUE.equals(reinjectDependenciesAttribute)) {
			injectFields(testContext, true);
		}
	}

	/**
	 * Inject each {@link BeanOverride @BeanOverride} field in the test instance with
	 * a corresponding bean override instance.
	 * <p>If the {@code reinjectFields} flag is {@code true} (which indicates that
	 * a fresh instance is required), the field is nulled out before injecting
	 * the overridden bean instance.
	 */
	private static void injectFields(TestContext testContext, boolean reinjectFields) {
		List<OverrideMetadata> overrideMetadataList = OverrideMetadata.forTestClass(testContext.getTestClass());
		if (!overrideMetadataList.isEmpty()) {
			Object testInstance = testContext.getTestInstance();
			BeanOverrideRegistrar registrar = testContext.getApplicationContext()
					.getBean(BeanOverrideContextCustomizer.REGISTRAR_BEAN_NAME, BeanOverrideRegistrar.class);

			for (OverrideMetadata overrideMetadata : overrideMetadataList) {
				if (reinjectFields) {
					Field field = overrideMetadata.getField();
					ReflectionUtils.makeAccessible(field);
					ReflectionUtils.setField(field, testInstance, null);
				}
				registrar.inject(testInstance, overrideMetadata);
			}
		}
	}

}

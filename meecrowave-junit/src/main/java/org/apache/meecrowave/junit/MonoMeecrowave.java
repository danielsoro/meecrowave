/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.meecrowave.junit;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.testing.Injector;
import org.apache.meecrowave.testing.MonoBase;
import org.junit.rules.MethodRule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import javax.enterprise.context.spi.CreationalContext;
import java.util.List;

// a MeecrowaveRule starting a single container, very awesome for forkCount=1, reuseForks=true
public class MonoMeecrowave {
    private static final MonoBase BASE = new MonoBase();
    private static final AutoCloseable NOOP_CLOSEABLE = () -> {
    };

    public static class Runner extends BlockJUnit4ClassRunner {
        public Runner(final Class<?> klass) throws InitializationError {
            super(klass);
        }

        @Override
        protected List<MethodRule> rules(final Object test) {
            final List<MethodRule> rules = super.rules(test);
            rules.add((base, method, target) -> new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    BASE.startIfNeeded();
                    configInjection(test.getClass(), test);
                    final CreationalContext<?> creationalContext = Injector.inject(test);
                    try {
                        base.evaluate();
                    } finally {
                        creationalContext.release();
                    }
                }

                private void configInjection(final Class<?> aClass, final Object test) {
                    Injector.injectConfig(BASE.getConfiguration(), test);
                    final Class<?> parent = aClass.getSuperclass();
                    if (parent != null && parent != Object.class) {
                        configInjection(parent, true);
                    }
                }
            });
            return rules;
        }
    }

    public static class Rule extends MeecrowaveRuleBase<Rule> {
        @Override
        public Meecrowave.Builder getConfiguration() {
            return BASE.getConfiguration();
        }

        @Override
        protected AutoCloseable onStart() {
            BASE.startIfNeeded();
            return NOOP_CLOSEABLE;
        }
    }
}

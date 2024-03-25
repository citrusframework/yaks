/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.groovy.dsl;

import org.citrusframework.Citrus;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.citrusframework.yaks.groovy.dsl.beans.BeansConfiguration;

/**
 * @author Christoph Deppisch
 */
public class CitrusConfiguration {

    private final Citrus citrus;

    public CitrusConfiguration(Citrus citrus) {
        this.citrus = citrus;
    }

    public void beans(@DelegatesTo(BeansConfiguration.class) Closure<?> callable) {
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.setDelegate(new BeansConfiguration(citrus));
        callable.call();
    }

    public void queues(@DelegatesTo(QueueConfiguration.class) Closure<?> callable) {
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.setDelegate(new QueueConfiguration(citrus));
        callable.call();
    }

    public void endpoints(@DelegatesTo(EndpointsConfiguration.class) Closure<?> callable) {
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.setDelegate(new EndpointsConfiguration(citrus));
        callable.call();
    }
}

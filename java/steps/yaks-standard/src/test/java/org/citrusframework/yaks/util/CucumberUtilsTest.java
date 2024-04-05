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

package org.citrusframework.yaks.util;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class CucumberUtilsTest {

    @Test
    public void extractFeatureFileName() {
        Assert.assertEquals("", CucumberUtils.extractFeatureFileName((URI) null));
        Assert.assertEquals("foo.feature", CucumberUtils.extractFeatureFileName(URI.create("foo.feature")));
        Assert.assertEquals("foo.feature", CucumberUtils.extractFeatureFileName(URI.create("/foo.feature")));
        Assert.assertEquals("foo.feature", CucumberUtils.extractFeatureFileName(URI.create("classpath:org/citrusframework/yaks/foo/foo.feature")));
    }

    @Test
    public void extractFeatureFile() {
        Assert.assertEquals("", CucumberUtils.extractFeatureFile((URI) null));
        Assert.assertEquals("foo.feature", CucumberUtils.extractFeatureFile(URI.create("foo.feature")));
        Assert.assertEquals("/foo.feature", CucumberUtils.extractFeatureFile(URI.create("/foo.feature")));
        Assert.assertEquals("classpath:org/citrusframework/yaks/foo/foo.feature", CucumberUtils.extractFeatureFile(URI.create("classpath:org/citrusframework/yaks/foo/foo.feature")));
    }

    @Test
    public void extractFeaturePackage() {
        Assert.assertEquals("", CucumberUtils.extractFeaturePackage((URI) null));
        Assert.assertEquals("", CucumberUtils.extractFeaturePackage(URI.create("foo.feature")));
        Assert.assertEquals("", CucumberUtils.extractFeaturePackage(URI.create("/foo.feature")));
        Assert.assertEquals("org.citrusframework.yaks.foo", CucumberUtils.extractFeaturePackage(URI.create("classpath:org/citrusframework/yaks/foo/foo.feature")));
    }
}

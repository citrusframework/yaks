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

package org.citrusframework.yaks.camelk;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.citrusframework.spi.Resources;
import org.citrusframework.util.FileUtils;
import org.citrusframework.yaks.camelk.model.Integration;
import org.citrusframework.yaks.camelk.model.IntegrationSpec;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.StringUtils;

public class IntegrationBuilderTest {

	@Test
	public void shouldSanitizeIntegrationNames() {
		Integration i = new Integration.Builder()
				.name("SomeCamelCaseSource.java")
				.source("from(\"timer:x\").log('${body}')")
				.build();

		Assert.assertEquals(i.getMetadata().getName(), "some-camel-case-source");
		Assert.assertEquals(i.getSpec().getSources().get(0).getName(), "SomeCamelCaseSource.java");

		i = new Integration.Builder()
				.name("ThisIsATest%&/$_!.java")
				.source("from(\"timer:x\").log('${body}')")
				.build();

		Assert.assertEquals(i.getMetadata().getName(), "this-is-atest");
	}

	@Test
	public void shouldSupportExplicitIntegrationNames() {
		Integration i = new Integration.Builder()
				.name("some-integration")
				.source("SomeCamelCaseSource.java", "from(\"timer:x\").log('${body}')")
				.build();

		Assert.assertEquals(i.getMetadata().getName(), "some-integration");
		Assert.assertEquals(i.getSpec().getSources().get(0).getName(), "SomeCamelCaseSource.java");

		i = new Integration.Builder()
				.name("ThisIsATest%&/$_!.java")
				.source("from(\"timer:x\").log('${body}')")
				.build();

		Assert.assertEquals(i.getMetadata().getName(), "this-is-atest");
	}

	@Test
	public void buildComplexIntegrationTest() throws IOException {
		Map<String, IntegrationSpec.TraitConfig> traits = new HashMap<>();
		IntegrationSpec.TraitConfig quarkus = new IntegrationSpec.TraitConfig("enabled", true);
		quarkus.add("native", "true");
		traits.put("quarkus", quarkus);
		traits.put("route", new IntegrationSpec.TraitConfig("enabled", true));

		IntegrationSpec.TraitConfig builder = new IntegrationSpec.TraitConfig("properties", Arrays.asList("quarkus.foo=bar", "quarkus.verbose=true"));
		builder.add("verbose", true);
		traits.put("builder", builder);

		List<String> dependencies = Arrays.asList("mvn:fake.dependency:id:version-1", "camel:jackson");
		Integration i = new Integration.Builder()
				.name("bar.groovy")
				.source("from(\"timer:x\").log('${body}')")
				.traits(traits)
				.dependencies(dependencies)
				.build();

		final String json = KubernetesSupport.json().writeValueAsString(i);
		Assert.assertEquals(StringUtils.trimAllWhitespace(
				FileUtils.readToString(Resources.create("integration.json", IntegrationBuilderTest.class))), json);
	}

	@Test
	public void buildOpenApiIntegrationTest() throws IOException {
		Integration i = new Integration.Builder()
				.name("openapi.groovy")
				.source("from(\"timer:x\").log('${body}')")
				.openApi("openapi.yaml", FileUtils.readToString(FileUtils.getFileResource("classpath:openapi.yaml")))
				.build();

		final String json = StringUtils.trimAllWhitespace(KubernetesSupport.json().writeValueAsString(i));
		Assert.assertEquals(StringUtils.trimAllWhitespace(
				FileUtils.readToString(Resources.create("integration-api.json", IntegrationBuilderTest.class))), json);
	}
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.camelk;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.util.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.citrusframework.yaks.camelk.model.Kamelet;
import org.citrusframework.yaks.camelk.model.KameletSpec;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

public class KameletBuilderTest {

	@Test
	public void buildComplexKamelet() throws IOException {
		Map<String, KameletSpec.DataTypesSpec> dataTypes = new HashMap<>();
		dataTypes.put("out", new KameletSpec.DataTypesSpec("text",
				new KameletSpec.DataTypeSpec("camel", "binary"),
				new KameletSpec.DataTypeSpec("camel", "text")));

		KameletSpec.Definition definition = new KameletSpec.Definition();
		definition.setTitle("Timer Source");
		definition.getProperties().put("period", new KameletSpec.Definition.PropertyConfig("Period", "integer", 1000, null));
		definition.getProperties().put("message", new KameletSpec.Definition.PropertyConfig("Message", "string", null, "hello world"));
		definition.getRequired().add("message");

		Kamelet kamelet = new Kamelet.Builder()
				.name("time-source")
				.addLabel(KameletSettings.KAMELET_TYPE_LABEL, "source")
				.definition(definition)
				.dataTypes(dataTypes)
				.dependencies(Collections.singletonList("mvn:fake.dependency:id:version-1"))
				.template("from:\n" +
						"  uri: timer:tick\n" +
						"  parameters:\n" +
						"    period: \"#property:period\"\n" +
						"  steps:\n" +
						"  - set-body:\n" +
						"      constant: \"{{message}}\"\n" +
						"  - to: \"kamelet:sink\"")
				.build();

		final String json = KubernetesSupport.json().writeValueAsString(kamelet);
		Assert.assertEquals(StringUtils.trimAllWhitespace(
				FileUtils.readToString(new ClassPathResource("kamelet.json", KameletBuilderTest.class))),
				StringUtils.trimAllWhitespace(json));
	}

	@Test
	public void shouldDeserializeKamelet() throws IOException {
		Kamelet deserialized = new ObjectMapper().readValue(
				FileUtils.readToString(new ClassPathResource("timer-source.kamelet.json")), Kamelet.class);

		Assert.assertNull(deserialized.getSpec().getFlow());
		Assert.assertNotNull(deserialized.getSpec().getTemplate());
		Assert.assertEquals(1L, deserialized.getSpec().getTemplate().size());
	}
}

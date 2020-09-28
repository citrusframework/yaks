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

package org.citrusframework.yaks.camelk.model;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IntegrationBuilderTest {

	public static final String TEST_INTEGRATION = "{\"apiVersion\":\"camel.apache.org/v1\"," +
			"\"kind\":\"Integration\"," +
			"\"metadata\":{\"name\":\"bar\"}," +
			"\"spec\":{\"sources\":[{\"content\":\"from(\\\"timer:x\\\").log('${body}')\",\"name\":\"bar.groovy\"}]," +
			"\"dependencies\":[\"mvn:fake.dependency:id:version-1\"]," +
			"\"traits\":{\"route\":{\"configuration\":{\"enabled\":\"true\"}},\"quarkus\":{\"configuration\":{\"native\":\"true\",\"enabled\":\"true\"}}}" +
			"}}";


	@Test
	public void buildComplexIntegrationTest() throws JsonProcessingException {
		Map<String, Integration.TraitConfig> traits = new HashMap<>();
		Integration.TraitConfig quarkus = new Integration.TraitConfig("enabled", "true");
		quarkus.add("native", "true");
		traits.put("quarkus", quarkus);
		traits.put("route", new Integration.TraitConfig("enabled", "true"));
		Integration i = new Integration.Builder()
				.name("bar.groovy")
				.source("from(\"timer:x\").log('${body}')")
				.traits(traits)
				.dependencies(Collections.singletonList("mvn:fake.dependency:id:version-1"))
				.build();

		final ObjectMapper obj = new ObjectMapper();
		final String json = obj.writeValueAsString(i);

		Assert.assertEquals(TEST_INTEGRATION, json);

	}
}

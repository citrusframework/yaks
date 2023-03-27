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

import com.consol.citrus.util.FileUtils;
import org.citrusframework.yaks.camelk.model.Binding;
import org.citrusframework.yaks.camelk.model.BindingSpec;
import org.citrusframework.yaks.kafka.KafkaSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

public class BindingBuilderTest {

	@Test
	public void buildComplexBinding() throws IOException {
		BindingSpec.Endpoint.ObjectReference sourceRef = new BindingSpec.Endpoint.ObjectReference();
		sourceRef.setName("timer-source");
		sourceRef.setKind("Kamelet");
		sourceRef.setNamespace(CamelKSettings.getNamespace());

		BindingSpec.Endpoint source = new BindingSpec.Endpoint(sourceRef);
		source.getProperties().put("message", "Hello World");
		source.getDataTypes().put("out", new BindingSpec.Endpoint.DataTypeRef("camel", "string"));

		BindingSpec.Endpoint.ObjectReference sinkRef = new BindingSpec.Endpoint.ObjectReference();
		sinkRef.setName("hello-topic");
		sinkRef.setKind("KafkaTopic");
		sinkRef.setNamespace(KafkaSettings.getNamespace());

		BindingSpec.Endpoint sink = new BindingSpec.Endpoint(sinkRef);

		Binding binding = new Binding.Builder()
				.name("time-source-kafka")
				.source(source)
				.sink(sink)
				.build();

		final String json = KubernetesSupport.json().writeValueAsString(binding);
		Assert.assertEquals(StringUtils.trimAllWhitespace(
				FileUtils.readToString(new ClassPathResource("kamelet-binding.json", BindingBuilderTest.class))),
				StringUtils.trimAllWhitespace(json));
	}
}

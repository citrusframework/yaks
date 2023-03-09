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
import org.citrusframework.yaks.camelk.model.KameletBinding;
import org.citrusframework.yaks.camelk.model.KameletBindingSpec;
import org.citrusframework.yaks.kafka.KafkaSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

public class KameletBindingBuilderTest {

	@Test
	public void buildComplexKameletBinding() throws IOException {
		KameletBindingSpec.Endpoint.ObjectReference sourceRef = new KameletBindingSpec.Endpoint.ObjectReference();
		sourceRef.setName("timer-source");
		sourceRef.setKind("Kamelet");
		sourceRef.setNamespace(CamelKSettings.getNamespace());

		KameletBindingSpec.Endpoint source = new KameletBindingSpec.Endpoint(sourceRef);
		source.getProperties().put("message", "Hello World");
		source.getDataTypes().put("out", new KameletBindingSpec.Endpoint.DataTypeRef("camel", "string"));

		KameletBindingSpec.Endpoint.ObjectReference sinkRef = new KameletBindingSpec.Endpoint.ObjectReference();
		sinkRef.setName("hello-topic");
		sinkRef.setKind("KafkaTopic");
		sinkRef.setNamespace(KafkaSettings.getNamespace());

		KameletBindingSpec.Endpoint sink = new KameletBindingSpec.Endpoint(sinkRef);

		KameletBinding binding = new KameletBinding.Builder()
				.name("time-source-kafka")
				.source(source)
				.sink(sink)
				.build();

		final String json = KubernetesSupport.json().writeValueAsString(binding);
		Assert.assertEquals(StringUtils.trimAllWhitespace(
				FileUtils.readToString(new ClassPathResource("kamelet-binding.json", KameletBindingBuilderTest.class))),
				StringUtils.trimAllWhitespace(json));
	}
}

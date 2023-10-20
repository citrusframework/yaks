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

import org.citrusframework.spi.Resources;
import org.citrusframework.util.FileUtils;
import org.citrusframework.yaks.camelk.model.Pipe;
import org.citrusframework.yaks.camelk.model.PipeSpec;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBinding;
import org.citrusframework.yaks.kafka.KafkaSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.StringUtils;

public class PipeBuilderTest {

	@Test
	public void buildComplexBinding() throws IOException {
		PipeSpec.Endpoint.ObjectReference sourceRef = new PipeSpec.Endpoint.ObjectReference();
		sourceRef.setName("timer-source");
		sourceRef.setKind("Kamelet");
		sourceRef.setNamespace(CamelKSettings.getNamespace());

		PipeSpec.Endpoint source = new PipeSpec.Endpoint(sourceRef);
		source.getProperties().put("message", "Hello World");
		source.getDataTypes().put("out", new PipeSpec.Endpoint.DataTypeRef("camel", "string"));

		PipeSpec.Endpoint.ObjectReference sinkRef = new PipeSpec.Endpoint.ObjectReference();
		sinkRef.setName("hello-topic");
		sinkRef.setKind("KafkaTopic");
		sinkRef.setNamespace(KafkaSettings.getNamespace());

		PipeSpec.Endpoint sink = new PipeSpec.Endpoint(sinkRef);

		KameletBinding pipe = new KameletBinding.Builder()
				.name("time-source-kafka")
				.source(source)
				.sink(sink)
				.build();

		final String json = KubernetesSupport.json().writeValueAsString(pipe);
		Assert.assertEquals(StringUtils.trimAllWhitespace(
				FileUtils.readToString(Resources.create("kamelet-binding.json", PipeBuilderTest.class))),
				StringUtils.trimAllWhitespace(json));
	}

	@Test
	public void buildComplexPipe() throws IOException {
		PipeSpec.Endpoint.ObjectReference sourceRef = new PipeSpec.Endpoint.ObjectReference();
		sourceRef.setName("timer-source");
		sourceRef.setKind("Kamelet");
		sourceRef.setNamespace(CamelKSettings.getNamespace());

		PipeSpec.Endpoint source = new PipeSpec.Endpoint(sourceRef);
		source.getProperties().put("message", "Hello World");
		source.getDataTypes().put("out", new PipeSpec.Endpoint.DataTypeRef("camel", "string"));

		PipeSpec.Endpoint.ObjectReference sinkRef = new PipeSpec.Endpoint.ObjectReference();
		sinkRef.setName("hello-topic");
		sinkRef.setKind("KafkaTopic");
		sinkRef.setNamespace(KafkaSettings.getNamespace());

		PipeSpec.Endpoint sink = new PipeSpec.Endpoint(sinkRef);

		Pipe pipe = new Pipe.Builder()
				.name("time-source-kafka")
				.source(source)
				.sink(sink)
				.build();

		final String json = KubernetesSupport.json().writeValueAsString(pipe);
		Assert.assertEquals(StringUtils.trimAllWhitespace(
				FileUtils.readToString(Resources.create("pipe.json", PipeBuilderTest.class))),
				StringUtils.trimAllWhitespace(json));
	}
}

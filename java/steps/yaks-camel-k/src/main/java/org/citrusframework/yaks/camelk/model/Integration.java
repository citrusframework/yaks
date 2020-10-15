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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResource;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec"})
@JsonDeserialize(
		using = JsonDeserializer.None.class
)
public class Integration extends CustomResource {

	@JsonProperty("spec")
	private IntegrationSpec spec = new IntegrationSpec();

	@Override
	public String getApiVersion() {
		return CamelKSupport.CAMELK_CRD_GROUP + "/" + CamelKSettings.getApiVersion();
	}

	public IntegrationSpec getSpec() {
		return spec;
	}

	public void setSpec(IntegrationSpec spec) {
		this.spec = spec;
	}

	/**
	 * Fluent builder
	 */
	public static class Builder {
		private Map<String, IntegrationSpec.TraitConfig> traits;
		private List<String> dependencies;
		private String source;
		private String name;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder source(String source) {
			this.source = source;
			return this;
		}

		public Builder dependencies(List<String> dependencies) {
			this.dependencies = Collections.unmodifiableList(dependencies);
			return this;
		}

		public Builder traits(Map<String, IntegrationSpec.TraitConfig> traits) {
			this.traits = Collections.unmodifiableMap(traits);
			return this;
		}

		public Integration build() {
			Integration i = new Integration();
			i.getMetadata().setName(name.substring(0, name.indexOf(".")));
			i.getSpec().setSources(Collections.singletonList(new IntegrationSpec.Source(name, source)));
			i.getSpec().setDependencies(dependencies);
			i.getSpec().setTraits(traits);
			return i;
		}
	}
}

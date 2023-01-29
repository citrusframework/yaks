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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Group(CamelKSupport.CAMELK_CRD_GROUP)
@Version(CamelKSettings.API_VERSION_DEFAULT)
public class Integration extends CustomResource<IntegrationSpec, IntegrationStatus> implements Namespaced {

	public Integration() {
		super();
		this.spec = new IntegrationSpec();
		this.status = null;
	}

	@Override
	public String getApiVersion() {
		return CamelKSupport.CAMELK_CRD_GROUP + "/" + CamelKSettings.getApiVersion();
	}

	/**
	 * Fluent builder
	 */
	public static class Builder {
		private final Map<String, IntegrationSpec.TraitConfig> traits = new LinkedHashMap<>();
		private final List<IntegrationSpec.Resource> resources = new ArrayList<>();
		private final List<String> dependencies = new ArrayList<>();
		private final List<IntegrationSpec.Configuration> configuration = new ArrayList<>();
		private String source;
		private String fileName;
		private String name;

		public Builder name(String name) {
			this.name = name;

			if (fileName == null) {
				this.fileName = name;
			}

			return this;
		}

		public Builder source(String source) {
			this.source = source;
			return this;
		}

		public Builder source(String fileName, String source) {
			this.source = source;
			this.fileName = fileName;
			return this;
		}

		public Builder openApi(String fileName, String content) {
			this.resources.add(new IntegrationSpec.Resource("openapi", null, fileName, content));
			return this;
		}

		public Builder dependencies(List<String> dependencies) {
			this.dependencies.addAll(dependencies);
			return this;
		}

		public Builder traits(Map<String, IntegrationSpec.TraitConfig> traits) {
			this.traits.putAll(traits);
			return this;
		}

		public Builder trait(String name, IntegrationSpec.TraitConfig config) {
			this.traits.put(name ,config);
			return this;
		}

		public Builder configuration(List<IntegrationSpec.Configuration> configuration) {
			this.configuration.addAll(configuration);
			return this;
		}

		public Integration build() {
			Integration i = new Integration();
			i.getMetadata().setName(sanitizeIntegrationName(name));
			i.getSpec().setSources(Collections.singletonList(new IntegrationSpec.Source(fileName, source)));

			if (!dependencies.isEmpty()) {
				i.getSpec().setDependencies(dependencies);
			}

			if (!traits.isEmpty()) {
				i.getSpec().setTraits(traits);
			}

			if (!configuration.isEmpty()) {
				i.getSpec().setConfiguration(configuration);
			}

			if (!resources.isEmpty()) {
				i.getSpec().setResources(resources);
			}

			return i;
		}

		private String sanitizeIntegrationName(String name) {
			String sanitized;

			if (name.contains(".")) {
				sanitized = name.substring(0, name.indexOf("."));
			} else {
				sanitized = name;
			}

			sanitized = sanitized.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
			return sanitized.replaceAll("[^a-z0-9-]", "");
		}
	}
}

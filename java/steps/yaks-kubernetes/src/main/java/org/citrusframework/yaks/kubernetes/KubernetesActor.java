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

package org.citrusframework.yaks.kubernetes;

import org.citrusframework.TestActor;
import org.citrusframework.yaks.YaksSettings;

/**
 * Test actor disabled when running a local test where no Kubernetes and Openshift is involved.
 */
public class KubernetesActor extends TestActor {

    public KubernetesActor() {
        setName("k8s");
    }

    @Override
    public boolean isDisabled() {
        return YaksSettings.isLocal();
    }
}

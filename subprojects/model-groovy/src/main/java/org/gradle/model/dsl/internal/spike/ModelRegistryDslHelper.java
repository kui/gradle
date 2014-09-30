/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.dsl.internal.spike;

import groovy.lang.Closure;
import org.gradle.api.Transformer;
import org.gradle.model.internal.core.ModelPath;

import java.util.List;
import java.util.Map;

public class ModelRegistryDslHelper {

    private final ModelRegistry registry;

    public ModelRegistryDslHelper(ModelRegistry registry) {
        this.registry = registry;
    }

    void addCreator(String path, final Closure creatorClosure, List<String> inputPaths) {
        Transformer<Object, Map<String, Object>> closureBackedCreator = new Transformer<Object, Map<String, Object>>() {
            public Object transform(Map<String, Object> inputs) {
                return creatorClosure.call(inputs);
            }
        };
        registry.create(ModelPath.path(path), new ModelCreator(inputPaths, closureBackedCreator));
    }
}
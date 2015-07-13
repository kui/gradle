/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.jvm
import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class CustomJarBinarySpecSubtypeIntegrationTest extends AbstractIntegrationSpec {
    def "managed subclass of JarBinarySpec can be instantiated"() {
        buildFile << """
plugins {
    id 'jvm-component'
}

@Managed
interface CustomJarBinarySpec extends JarBinarySpec {
    String getValue()
    void setValue(String value)
}

class CustomJarBinarySpecRules extends RuleSource {
    @Model
    void customJarBinary(CustomJarBinarySpec binarySpec) {
        binarySpec.baseName = "base"
    }

    @Validate
    void validateCustomJarBinarySpec(BinaryContainer binaries, CustomJarBinarySpec binarySpec) {
        println "Validating"
        assert binarySpec.value == "12"
        assert binarySpec.baseName == "base"
        assert binarySpec.name == "customJarBinary"
    }
}

apply plugin: CustomJarBinarySpecRules

model {
    customJarBinary {
        value = "12"
    }
}
"""
        expect:
        succeeds "components"
    }
}

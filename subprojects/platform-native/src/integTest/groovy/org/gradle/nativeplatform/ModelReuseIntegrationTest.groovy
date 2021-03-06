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

package org.gradle.nativeplatform

import org.gradle.integtests.fixtures.daemon.DaemonIntegrationSpec
import org.gradle.model.internal.persist.ReusingModelRegistryStore
import org.gradle.nativeplatform.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativeplatform.fixtures.RequiresInstalledToolChain
import org.gradle.nativeplatform.fixtures.SingleToolChainTestRunner
import org.junit.runner.RunWith

// Requires daemon because reuse right now doesn't handle the build actually changing
@RequiresInstalledToolChain
@RunWith(SingleToolChainTestRunner.class)
class ModelReuseIntegrationTest extends DaemonIntegrationSpec {

    def setup() {
        def toolChain = AbstractInstalledToolChainIntegrationSpec.toolChain
        def initScript = file("init.gradle") << """
allprojects { p ->
    apply plugin: ${toolChain.pluginClass}

    model {
          toolChains {
            ${toolChain.buildScriptConfig}
          }
    }
}
"""
        executer.beforeExecute {
            usingInitScript(initScript)
            withArgument("-D$ReusingModelRegistryStore.TOGGLE=true")
        }
    }

    def "can enable reuse with the component model"() {
        when:
        buildScript """
            plugins {
              id "c"
            }

            model {
                components {
                  main(NativeExecutableSpec)
                }
            }
        """

        file("src/main/c/lib.c") << """
            int main() {
              return 0;
            }
        """

        then:
        succeeds "build"
        executedAndNotSkipped ":compileMainExecutableMainC"

        when:
        file("src/main/c/lib.c").text = """
            int main() {
              return 10;
            }
        """

        then:
        succeeds "build"
        executedAndNotSkipped ":compileMainExecutableMainC"
    }
}

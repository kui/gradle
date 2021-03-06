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

package org.gradle.model.internal.report.unbound

import org.gradle.util.TextUtil
import spock.lang.Specification

class UnboundRulesReporterTest extends Specification {

    def output = new StringWriter()
    def reporter = new UnboundRulesReporter(new PrintWriter(output), "> ")

    def "reports on unbound rules"() {
        when:
        reporter.reportOn([
                UnboundRule.descriptor("r1")
                        .mutableInput(UnboundRuleInput.type(String).path("parent.p1"))
                        .mutableInput(UnboundRuleInput.type(String).scope("some.scope"))
                        .mutableInput(UnboundRuleInput.type(Integer).bound().path("parent.p3"))
                        .immutableInput(UnboundRuleInput.type(Number).path("parent.p4").suggestions("parent.p31", "parent.p32"))
                        .immutableInput(UnboundRuleInput.type(Number))
                        .immutableInput(UnboundRuleInput.type(Number).bound().path("parent.p6")).build()
        ])

        then:
        output.toString() == TextUtil.toPlatformLineSeparators("""> r1
>   Subject:
>      | Found:false | Path:parent.p1 | Type:java.lang.String|
>      | Found:false | Path:<unspecified> | Type:java.lang.String | Scope:'some.scope'|
>      | Found:true | Path:parent.p3 | Type:java.lang.Integer|
>   Inputs:
>      | Found:false | Path:parent.p4 | Type:java.lang.Number | Suggestions:parent.p31, parent.p32|
>      | Found:false | Path:<unspecified> | Type:java.lang.Number|
>      | Found:true | Path:parent.p6 | Type:java.lang.Number|""")
    }
}

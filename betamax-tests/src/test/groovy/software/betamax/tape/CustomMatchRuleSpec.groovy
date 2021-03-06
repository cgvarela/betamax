/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.betamax.tape

import com.google.common.base.Optional
import com.google.common.io.Files
import org.junit.ClassRule
import software.betamax.ComposedMatchRule
import software.betamax.Configuration
import software.betamax.MatchRule
import software.betamax.junit.RecorderRule
import software.betamax.message.Request
import software.betamax.util.message.BasicRequest
import spock.lang.*

import static com.google.common.net.HttpHeaders.ACCEPT
import static software.betamax.MatchRules.method
import static software.betamax.MatchRules.uri

@Issue("https://github.com/robfletcher/betamax/issues/43")
@Unroll
class CustomMatchRuleSpec extends Specification {

    @Shared @AutoCleanup("deleteDir") def tapeRoot = Files.createTempDir()
    @Shared def configuration = Configuration.builder().tapeRoot(tapeRoot).build()
    @Shared @ClassRule RecorderRule recorder = new RecorderRule(configuration)

    @Shared def url = "http://freeside.co/betamax"
    @Shared def acceptHeaderRule = new MatchRule() {
        @Override
        boolean isMatch(Request a, Request b) {
            a.getHeader(ACCEPT) == b.getHeader(ACCEPT)
        }
    }

    void setupSpec() {
        new File(tapeRoot, "custom_match_rule_spec.yaml").withWriter { writer ->
            writer << """\
!tape
name: custom match rule spec
interactions:
- recorded: 2013-10-31T07:44:59.023Z
  request:
    method: GET
    uri: $url
    headers:
      Accept: text/plain
      X-Whatever: some random value
  response:
    status: 200
    headers:
      Content-Length: '12'
      Content-Type: text/plain
      Date: Tue, 01 Oct 2013 21:53:58 GMT
    body: Hello World!
"""
        }
    }

    def cleanup() {
        recorder.stop()
    }

    void "#verb request with Accept header #acceptHeader #description using #rules"() {
        given:
        recorder.start("custom match rule spec", Optional.absent(), Optional.of(ComposedMatchRule.of(rules)))

        and:
        def request = new BasicRequest(verb, url)
        request.addHeader(ACCEPT, acceptHeader)

        expect:
        recorder.tape.seek(request) == shouldMatch

        where:
        verb   | acceptHeader       | rules                           | shouldMatch
        "GET"  | "text/plain"       | [method, uri]                   | true
        "POST" | "text/plain"       | [method, uri]                   | false
        "GET"  | "text/plain"       | [acceptHeaderRule]              | true
        "GET"  | "application/json" | [acceptHeaderRule]              | false
        "GET"  | "text/plain"       | [method, uri, acceptHeaderRule] | true
        "POST" | "text/plain"       | [method, uri, acceptHeaderRule] | false
        "GET"  | "application/json" | [method, uri, acceptHeaderRule] | false

        description = shouldMatch ? "matches" : "does not match"
    }

}

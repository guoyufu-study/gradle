/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.initialization

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.executer.ExecutionFailure
import org.gradle.util.GradleVersion

class InternalGradleFailuresIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        buildScript """
            task hello() {
                doLast {
                    println "Hello Gradle!"
                }
            }
        """
    }

    def "Error message due to unwritable project's Gradle cache directory is not scary"() {
        given:
        def localGradleCache = file('.gradle')
        localGradleCache.touch()

        when:
        fails 'hello'

        then:
        assertHasStartupFailure(failure, "Failed to create directory '${localGradleCache}${File.separator}checksums'")
    }

    def "Error message due to unwritable user home directory is not scary"() {
        given:
        requireOwnGradleUserHomeDir()
        requireGradleDistribution()

        executer.gradleUserHomeDir.touch()

        when:
        fails 'hello'

        then:
        assertHasStartupFailure(failure, "Cannot create parent directory '${executer.gradleUserHomeDir}${File.separator}caches' when creating directory '${executer.gradleUserHomeDir}${File.separator}caches${File.separator}${GradleVersion.current().version}${File.separator}generated-gradle-jars'")
    }

    def "Error message due to unwritable Gradle daemon directory is not scary"() {
        given:
        requireGradleDistribution()
        executer.requireIsolatedDaemons()
        executer.requireDaemon()

        def daemonDir = executer.daemonBaseDir
        daemonDir.touch()

        when:
        fails 'hello'

        then:
        assertHasStartupFailure(failure, "Failed to create directory '${daemonDir}${File.separator}${GradleVersion.current().version}'")
    }

    def "Error message due to unwritable native directory is not scary"() {
        given:
        requireOwnGradleUserHomeDir()
        requireGradleDistribution()

        def nativeDir = testDirectory.file("native-dir")
        nativeDir.touch()

        executer.withEnvironmentVars(GRADLE_OPTS: "-Dorg.gradle.native.dir=\"${nativeDir}\"")

        when:
        fails 'hello'

        then:
        assertHasStartupFailure(failure, "Could not initialize native services.")
        failure.assertHasErrorOutput("Caused by: net.rubygrapefruit.platform.NativeException: Failed to load native library")
    }

    private static void assertHasStartupFailure(ExecutionFailure failure, String cause) {
        failure.assertHasFailures(1)
        failure.assertHasDescription("Gradle could not start your build.")
        failure.assertHasCause(cause)
    }
}

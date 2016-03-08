/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.build.gradle.integration.instant;

import static com.android.build.gradle.integration.common.truth.AbstractAndroidSubject.ClassFileScope.INSTANT_RUN;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatDex;

import com.android.annotations.NonNull;
import com.android.build.gradle.integration.common.category.DeviceTests;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.fixture.Logcat;
import com.android.build.gradle.integration.common.runner.FilterableParameterized;
import com.android.build.gradle.integration.common.utils.TestFileUtils;
import com.android.build.gradle.internal.incremental.ColdswapMode;
import com.android.build.gradle.internal.incremental.InstantRunVerifierStatus;
import com.android.builder.model.InstantRun;
import com.android.ddmlib.IDevice;
import com.android.tools.fd.client.InstantRunArtifact;
import com.android.tools.fd.client.InstantRunClient;
import com.google.common.collect.Iterables;

import org.gradle.api.JavaVersion;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests support for Dagger and Instant Run.
 */
@RunWith(FilterableParameterized.class)
public class DaggerTest {

    private static final ColdswapMode COLDSWAP_MODE = ColdswapMode.MULTIDEX;
    private static final String ORIGINAL_MESSAGE = "from module";
    private static final String HOTSWAP_MESSAGE = "hotswap";
    private static final String APP_MODULE_DESC = "Lcom/android/tests/AppModule;";
    private static final String GET_MESSAGE = "getMessage";

    @Rule
    public Logcat logcat = Logcat.create();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{"daggerOne"}, {"daggerTwo"}});
    }

    @Rule
    public GradleTestProject project;

    private File mAppModule;

    private final String testProject;

    public DaggerTest(String testProject) {
        this.testProject = testProject;

        project = GradleTestProject.builder()
                .fromTestProject(testProject)
                .create();
    }

    @Before
    public void setUp() throws IOException {
        Assume.assumeFalse("Disabled until instant run supports Jack", GradleTestProject.USE_JACK);
        if (this.testProject.equals("daggerTwo")) {
            Assume.assumeTrue(
                    "Dagger 2 only works on java 7+",
                    JavaVersion.current().isJava7Compatible());
        }
        mAppModule = project.file("src/main/java/com/android/tests/AppModule.java");
    }

    @Test
    public void normalBuild() throws Exception {
        project.execute("assembleDebug");
    }

    @Test
    public void coldSwap() throws Exception {
        ColdSwapTester.testMultiDex(project, new ColdSwapTester.Steps() {
            @Override
            public void checkApk(File apk) throws Exception {
                assertThatApk(apk).hasClass(APP_MODULE_DESC, INSTANT_RUN);
            }

            @Override
            public void makeChange() throws Exception {
                TestFileUtils.addMethod(
                        mAppModule,
                        "public String getMessage() { return \"coldswap\"; }");
                TestFileUtils.searchAndReplace(mAppModule, "\"from module\"", "getMessage()");
            }

            @Override
            public void checkVerifierStatus(InstantRunVerifierStatus status) throws Exception {
                assertThat(status).isEqualTo(InstantRunVerifierStatus.METHOD_ADDED);
            }

            @Override
            public void checkArtifacts(List<InstantRunArtifact> artifacts) throws Exception {
                InstantRunArtifact artifact = Iterables.getOnlyElement(artifacts);
                assertThatDex(artifact.file)
                        .hasClass(APP_MODULE_DESC)
                        .that().hasMethod(GET_MESSAGE);
            }
        });
    }

    @Test
    public void hotSwap() throws Exception {
        InstantRun instantRunModel = InstantRunTestUtils.doInitialBuild(project, 23, COLDSWAP_MODE);

        makeHotSwapChange();

        project.execute(InstantRunTestUtils.getInstantRunArgs(23, COLDSWAP_MODE),
                instantRunModel.getIncrementalAssembleTaskName());

        InstantRunArtifact artifact =
                InstantRunTestUtils.getCompiledHotSwapCompatibleChange(instantRunModel);

        assertThatDex(artifact.file).hasClass("Lcom/android/tests/AppModule$override;");
    }

    @Test
    @Category(DeviceTests.class)
    public void hotSwap_device() throws Exception {
        HotSwapTester.run(
                project,
                "com.android.tests",
                "MainActivity",
                this.testProject,
                logcat,
                new HotSwapTester.Steps() {
                    @Override
                    public void verifyOriginalCode(
                            @NonNull InstantRunClient client,
                            @NonNull Logcat logcat,
                            @NonNull IDevice device) throws Exception {
                        assertThat(logcat).containsMessageWithText(ORIGINAL_MESSAGE);
                        assertThat(logcat).doesNotContainMessageWithText(HOTSWAP_MESSAGE);
                    }

                    @Override
                    public void makeChange() throws Exception {
                        makeHotSwapChange();
                    }

                    @Override
                    public void verifyNewCode(@NonNull InstantRunClient client,
                            @NonNull Logcat logcat,
                            @NonNull IDevice device) throws Exception {
                        // Should not have restarted activity
                        assertThat(logcat).doesNotContainMessageWithText(ORIGINAL_MESSAGE);
                        assertThat(logcat).doesNotContainMessageWithText(HOTSWAP_MESSAGE);

                        client.restartActivity(device);
                        Thread.sleep(500); // TODO: blocking logcat assertions with timeouts.

                        assertThat(logcat).doesNotContainMessageWithText(ORIGINAL_MESSAGE);
                        assertThat(logcat).containsMessageWithText(HOTSWAP_MESSAGE);
                    }
                });
    }

    private void makeHotSwapChange() throws Exception {
        TestFileUtils.searchAndReplace(mAppModule, "from module", "hotswap");
    }
}

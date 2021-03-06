/**
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.datacollector.el;

import com.google.common.collect.ImmutableMap;
import com.streamsets.datacollector.config.PipelineConfiguration;
import com.streamsets.datacollector.config.StageConfiguration;
import com.streamsets.datacollector.store.PipelineInfo;
import com.streamsets.pipeline.api.Config;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class TestPipelineEL {

  @Test
  public void testUndefinedPipelineNameAndVersion() {
    PipelineConfiguration pipelineConfiguration = new PipelineConfiguration(5, 5, UUID.randomUUID(), "label", "", Collections.<Config>emptyList(), Collections.<String, Object>emptyMap(), Collections.<StageConfiguration>emptyList(), null, null);
    PipelineEL.setConstantsInContext(pipelineConfiguration);
    Assert.assertEquals("UNDEFINED", PipelineEL.name());
    Assert.assertEquals("UNDEFINED", PipelineEL.version());
  }

  @Test
  public void testPipelineNameAndVersion() {
    Map<String, Object> metadata = ImmutableMap.<String, Object>of(PipelineEL.PIPELINE_VERSION_VAR, "3");
    PipelineConfiguration pipelineConfiguration = new PipelineConfiguration(5, 5, UUID.randomUUID(), "label", "", Collections.<Config>emptyList(), Collections.<String, Object>emptyMap(), Collections.<StageConfiguration>emptyList(), null, null);
    pipelineConfiguration.setMetadata(metadata);
    pipelineConfiguration.setPipelineInfo(new PipelineInfo("hello" , "label", "", new Date(), new Date(), "", "", "", UUID.randomUUID(), false, metadata));
    PipelineEL.setConstantsInContext(pipelineConfiguration);
    Assert.assertEquals("hello", PipelineEL.name());
    Assert.assertEquals("3", PipelineEL.version());
  }
}

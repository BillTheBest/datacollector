/**
 * Copyright 2017 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.origin.mongodb;

import com.mongodb.CursorType;
import com.mongodb.MongoClient;
import com.mongodb.MongoQueryException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.base.BaseSource;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.lib.util.JsonUtil;
import com.streamsets.pipeline.stage.common.DefaultErrorRecordHandler;
import com.streamsets.pipeline.stage.common.ErrorRecordHandler;
import com.streamsets.pipeline.stage.common.mongodb.Errors;
import com.streamsets.pipeline.stage.common.mongodb.Groups;
import com.streamsets.pipeline.stage.common.mongodb.MongoDBConfig;
import org.apache.commons.io.IOUtils;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMongoDBSource extends BaseSource {
  private static final Logger LOG = LoggerFactory.getLogger(MongoDBSource.class);

  protected final MongoSourceConfigBean configBean;

  protected ErrorRecordHandler errorRecordHandler;
  protected MongoCollection<Document> mongoCollection;
  protected MongoCursor<Document> cursor;

  private MongoClient mongoClient;

  public AbstractMongoDBSource(MongoSourceConfigBean configBean) {
    this.configBean = configBean;
    this.cursor = null;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected List<ConfigIssue> init() {
    List<ConfigIssue> issues = super.init();
    errorRecordHandler = new DefaultErrorRecordHandler(getContext());

    if (!issues.isEmpty()) {
      return issues;
    }

    configBean.mongoConfig.init(getContext(), issues, configBean.readPreference.getReadPreference(), null);

    if (issues.isEmpty()) {
      // since no issue was found in validation, the followings must not be null at this point.
      Utils.checkNotNull(configBean.mongoConfig.getMongoDatabase(), "MongoDatabase");
      mongoClient = Utils.checkNotNull(configBean.mongoConfig.getMongoClient(), "MongoClient");
      mongoCollection = Utils.checkNotNull(configBean.mongoConfig.getMongoCollection(), "MongoCollection");
      checkCursor(issues);
    }

    return issues;
  }

  @Override
  public void destroy() {
    IOUtils.closeQuietly(cursor);
    IOUtils.closeQuietly(mongoClient);
    cursor = null;
    mongoClient = null;
    super.destroy();
  }

  private void checkCursor(List<ConfigIssue> issues) {
    if (configBean.isCapped) {
      try {
        mongoCollection.find().cursorType(CursorType.TailableAwait).batchSize(1).limit(1).iterator().close();
      } catch (MongoQueryException e) {
        LOG.error("Error during Mongo Query in checkCursor: {}", e);
        issues.add(getContext().createConfigIssue(
            Groups.MONGODB.name(),
            MongoDBConfig.MONGO_CONFIG_PREFIX + "collection",
            Errors.MONGODB_04,
            configBean.mongoConfig.database,
            e.toString()
        ));
      }
    } else {
      try {
        mongoCollection.find().cursorType(CursorType.NonTailable).batchSize(1).limit(1).iterator().close();
      } catch (MongoQueryException e) {
        LOG.error("Error during Mongo Query in checkCursor: {}", e);
        issues.add(getContext().createConfigIssue(
            Groups.MONGODB.name(),
            MongoDBConfig.MONGO_CONFIG_PREFIX + "collection",
            Errors.MONGODB_06,
            configBean.mongoConfig.database,
            e.toString()
        ));
      }
    }
  }
}

/*
 * Copyright (C) 2014-2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 */

package gobblin.data.management.copy;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.google.common.collect.Lists;

import gobblin.dataset.DatasetsFinder;


public class TestCopyablePartitionableDatasedFinder implements DatasetsFinder<CopyableDataset> {

  public TestCopyablePartitionableDatasedFinder(FileSystem fs, Properties props) {
  }

  @Override public List<CopyableDataset> findDatasets() throws IOException {
    return Lists.<CopyableDataset>newArrayList(new TestCopyablePartitionableDataset());
  }

  @Override public Path commonDatasetRoot() {
    return new Path("/test");
  }
}

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

package gobblin.data.management.retention.version;

import java.util.Set;

import org.apache.hadoop.fs.Path;


/**
 * @deprecated
 * Dataset version extends {@link gobblin.data.management.version.StringDatasetVersion} and implements
 * {@link gobblin.data.management.retention.version.DatasetVersion}.
 */
@Deprecated
public class StringDatasetVersion extends gobblin.data.management.version.StringDatasetVersion implements
    DatasetVersion {

  public StringDatasetVersion(String version, Path path) {
    super(version, path);
  }

  public StringDatasetVersion(gobblin.data.management.version.StringDatasetVersion datasetVersion) {
    this(datasetVersion.getVersion(), datasetVersion.getPath());
  }

  @Override
  public Set<Path> getPathsToDelete() {
    return this.getPaths();
  }
}

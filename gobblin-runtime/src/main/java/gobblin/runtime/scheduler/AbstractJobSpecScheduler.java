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
package gobblin.runtime.scheduler;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import gobblin.runtime.api.JobSpec;
import gobblin.runtime.api.JobSpecSchedule;
import gobblin.runtime.api.JobSpecScheduler;
import gobblin.runtime.api.JobSpecSchedulerListener;

/**
 * A base implementation of {@link JobSpecScheduler} that keeps track
 */
public abstract class AbstractJobSpecScheduler implements JobSpecScheduler {
  protected final Map<URI, JobSpecSchedule> _schedules = new HashMap<>();
  private final Logger _log;
  private final JobSpecSchedulerListeners _callbacksDispatcher;

  public AbstractJobSpecScheduler(Optional<Logger> log) {
    _log = log.isPresent() ? log.get() : LoggerFactory.getLogger(getClass());
    _callbacksDispatcher = new JobSpecSchedulerListeners(_log);
  }

  /** {@inheritDoc} */
  @Override public void registerJobSpecSchedulerListener(JobSpecSchedulerListener listener) {
    _callbacksDispatcher.registerJobSpecSchedulerListener(listener);
  }

  /** {@inheritDoc} */
  @Override public void registerWeakJobSpecSchedulerListener(JobSpecSchedulerListener listener) {
    _callbacksDispatcher.registerWeakJobSpecSchedulerListener(listener);
  }

  /** {@inheritDoc} */
  @Override public void unregisterJobSpecSchedulerListener(JobSpecSchedulerListener listener) {
    _callbacksDispatcher.unregisterJobSpecSchedulerListener(listener);
  }

  /** {@inheritDoc} */
  @Override public List<JobSpecSchedulerListener> getJobSpecSchedulerListeners() {
    return _callbacksDispatcher.getJobSpecSchedulerListeners();
  }

  /** {@inheritDoc} */
  @Override public JobSpecSchedule scheduleJob(JobSpec jobSpec, Runnable jobRunnable) {
    _log.info("Scheduling JobSpec " + jobSpec);
    final URI jobSpecURI = jobSpec.getUri();

    JobSpecSchedule newSchedule = null;
    Runnable runnableWithTriggerCallback = new TriggerRunnable(jobSpec, jobRunnable);
    synchronized (this) {
      JobSpecSchedule existingSchedule = _schedules.get(jobSpecURI);
      if (null != existingSchedule) {
        if (existingSchedule.getJobSpec().equals(jobSpec)) {
          _log.warn("Ignoring already scheduled job: " + jobSpec);
          return existingSchedule;
        }

        // a new job spec -- unschedule first so that we schedule the new version
        unscheduleJob(jobSpecURI);
      }

      newSchedule = doScheduleJob(jobSpec, runnableWithTriggerCallback);
      _schedules.put(jobSpecURI, newSchedule);
    }
    _callbacksDispatcher.onJobScheduled(newSchedule);

    return newSchedule;
  }

  /** {@inheritDoc} */
  @Override public JobSpecSchedule scheduleOnce(JobSpec jobSpec, Runnable jobRunnable) {
    _log.info("Scheduling once JobSpec " + jobSpec);
    Runnable runOnceRunnable = new RunOnceRunnable(jobSpec.getUri(), jobRunnable);
    return scheduleJob(jobSpec, runOnceRunnable);
  }

  /** {@inheritDoc} */
  @Override public void unscheduleJob(URI jobSpecURI) {
    JobSpecSchedule existingSchedule = null;
    synchronized (this) {
      existingSchedule = _schedules.get(jobSpecURI);
      if (null != existingSchedule) {
        _log.info("Unscheduling " + existingSchedule);
        this._schedules.remove(jobSpecURI);
        doUnschedule(existingSchedule);
      }
    }
    if (null != existingSchedule) {
      _callbacksDispatcher.onJobUnscheduled(existingSchedule);
    }
  }

  /** Actual implementation of scheduling */
  protected abstract JobSpecSchedule doScheduleJob(JobSpec jobSpec, Runnable jobRunnable);

  /** Implementations should override this method */
  protected abstract void doUnschedule(JobSpecSchedule existingSchedule);

  /** {@inheritDoc} */
  @Override public Map<URI, JobSpecSchedule> getSchedules() {
    return Collections.unmodifiableMap(_schedules);
  }

  public Logger getLog() {
    return _log;
  }

  /**
   * A helper class for run-once jobs. It will run the runnable associated with a schedule and
   * remove the schedule automatically.
   *  */
  public class RunOnceRunnable implements Runnable {
    private final URI _jobSpecURI;
    private final Runnable _scheduleRunnable;

    public RunOnceRunnable(URI jobSpecURI, Runnable innerRunnable) {
      Preconditions.checkNotNull(jobSpecURI);
      Preconditions.checkNotNull(innerRunnable);

      _jobSpecURI = jobSpecURI;
      _scheduleRunnable = innerRunnable;
    }

    @Override public void run() {
      try {
        _scheduleRunnable.run();
      }
      finally {
        unscheduleJob(_jobSpecURI);
      }
    }
  }
  protected class TriggerRunnable implements Runnable {
    private final JobSpec _jobSpec;
    private final Runnable _jobRunnable;

    public TriggerRunnable(JobSpec jobSpec, Runnable jobRunnable) {
      _jobSpec = jobSpec;
      _jobRunnable = jobRunnable;
    }

    @Override public void run() {
      _callbacksDispatcher.onJobTriggered(_jobSpec);
      _jobRunnable.run();
    }
  }

}
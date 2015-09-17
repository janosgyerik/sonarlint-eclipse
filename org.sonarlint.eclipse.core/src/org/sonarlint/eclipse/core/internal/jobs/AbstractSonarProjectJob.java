/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.core.internal.jobs;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;

public abstract class AbstractSonarProjectJob extends Job {

  private final SonarLintProject sonarProject;

  private static final ISchedulingRule SONAR_ANALYSIS_RULE = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();

  public AbstractSonarProjectJob(String title, SonarLintProject project) {
    super(title);
    this.sonarProject = project;
    setPriority(Job.DECORATE);
    // Prevent concurrent SQ analysis
    setRule(SONAR_ANALYSIS_RULE);
  }

  @Override
  protected final IStatus run(final IProgressMonitor monitor) {
    SonarRunnerFacade facadeToUse = SonarLintCorePlugin.getDefault().getRunner();
    return run(facadeToUse, monitor);
  }

  protected SonarLintProject getSonarProject() {
    return sonarProject;
  }

  protected abstract IStatus run(SonarRunnerFacade runner, final IProgressMonitor monitor);

}

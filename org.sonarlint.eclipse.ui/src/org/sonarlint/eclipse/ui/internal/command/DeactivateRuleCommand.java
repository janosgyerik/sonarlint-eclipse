/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal.command;

import java.util.function.Predicate;
import org.eclipse.core.resources.IMarker;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public class DeactivateRuleCommand extends AbstractIssueCommand {

  @Override
  protected void execute(IMarker selectedMarker) {
    RuleKey ruleKey = MarkerUtils.getRuleKey(selectedMarker);
    if (ruleKey == null) {
      return;
    }
    
    PreferencesUtils.excludeRule(ruleKey);
    Predicate<ISonarLintFile> filter = f -> !f.getProject().isBound();
    JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.EDITOR_CHANGE, filter);
  }
}

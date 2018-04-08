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
package org.sonarlint.eclipse.core.internal.resources;

import java.util.Objects;

import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public class RuleExclusionItem {
  private final RuleKey ruleKey;
  private final String ruleName;

  public RuleExclusionItem(RuleKey ruleKey, String ruleName) {
    this.ruleKey = ruleKey;
    this.ruleName = ruleName;
  }

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public String ruleName() {
    return ruleName;
  }

  @Override
  public int hashCode() {
	// note: intentionally ignoring ruleName
    return ruleKey.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof RuleExclusionItem)) {
      return false;
    }

    RuleExclusionItem o = (RuleExclusionItem) other;
	// note: intentionally ignoring ruleName
    return Objects.equals(ruleKey, o.ruleKey);
  }
}

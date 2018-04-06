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
package org.sonarlint.eclipse.core.internal.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public class PreferencesUtils {

  public static final String PREF_MARKER_SEVERITY = "markerSeverity"; //$NON-NLS-1$
  public static final int PREF_MARKER_SEVERITY_DEFAULT = IMarker.SEVERITY_INFO;
  public static final String PREF_EXTRA_ARGS = "extraArgs"; //$NON-NLS-1$
  public static final String PREF_FILE_EXCLUSIONS = "fileExclusions"; //$NON-NLS-1$
  public static final String PREF_RULE_EXCLUSIONS = "ruleExclusions"; //$NON-NLS-1$
  public static final String PREF_DEFAULT = ""; //$NON-NLS-1$
  public static final String PREF_TEST_FILE_REGEXPS = "testFileRegexps"; //$NON-NLS-1$
  public static final String PREF_TEST_FILE_REGEXPS_DEFAULT = "**/*Test.*,**/test/**/*"; //$NON-NLS-1$

  private PreferencesUtils() {
    // Utility class
  }

  public static String getTestFileRegexps() {
    return Platform.getPreferencesService().getString(SonarLintCorePlugin.UI_PLUGIN_ID, PREF_TEST_FILE_REGEXPS, PREF_TEST_FILE_REGEXPS_DEFAULT, null);
  }

  public static int getMarkerSeverity() {
    return Platform.getPreferencesService().getInt(SonarLintCorePlugin.UI_PLUGIN_ID, PREF_MARKER_SEVERITY, PREF_MARKER_SEVERITY_DEFAULT, null);
  }

  public static List<SonarLintProperty> getExtraPropertiesForLocalAnalysis(ISonarLintProject project) {
    List<SonarLintProperty> props = new ArrayList<>();
    // First add all global properties
    String globalExtraArgs = getPreferenceString(PREF_EXTRA_ARGS);
    props.addAll(deserializeExtraProperties(globalExtraArgs));

    // Then add project properties
    SonarLintProjectConfiguration sonarProject = SonarLintProjectConfiguration.read(project.getScopeContext());
    if (sonarProject.getExtraProperties() != null) {
      props.addAll(sonarProject.getExtraProperties());
    }

    return props;
  }

  public static List<SonarLintProperty> deserializeExtraProperties(@Nullable String property) {
    List<SonarLintProperty> props = new ArrayList<>();
    // First add all global properties
    String[] keyValuePairs = StringUtils.split(property, "\r\n");
    for (String keyValuePair : keyValuePairs) {
      String[] keyValue = StringUtils.split(keyValuePair, "=");
      props.add(new SonarLintProperty(keyValue[0], keyValue[1]));
    }

    return props;
  }

  public static String serializeFileExclusions(List<ExclusionItem> exclusions) {
    return exclusions.stream()
      .map(ExclusionItem::toStringWithType)
      .collect(Collectors.joining("\r\n"));
  }

  public static String serializeExtraProperties(List<SonarLintProperty> properties) {
    List<String> keyValuePairs = new ArrayList<>(properties.size());
    for (SonarLintProperty prop : properties) {
      keyValuePairs.add(prop.getName() + "=" + prop.getValue());
    }
    return StringUtils.joinSkipNull(keyValuePairs, "\r\n");
  }

  public static List<ExclusionItem> deserializeFileExclusions(@Nullable String property) {
    String[] values = StringUtils.split(property, "\r\n");
    return Arrays.stream(values)
      .map(ExclusionItem::parse)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  public static List<ExclusionItem> getGlobalExclusions() {
    // add globally-configured exclusions
    String props = getPreferenceString(PreferencesUtils.PREF_FILE_EXCLUSIONS);
    return deserializeFileExclusions(props);
  }

  private static String getPreferenceString(String key) {
    return Platform.getPreferencesService().getString(SonarLintCorePlugin.UI_PLUGIN_ID, key, PREF_DEFAULT, null);
  }

  private static void setPreferenceString(String key, String value) {
    Preferences preferences = ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID);
    preferences.put(key, value);
    try {
      preferences.flush();
    } catch (BackingStoreException e) {
      SonarLintLogger.get().error("Could not save preference: " + key + " = " + value, e);
    }
  }

  public static String serializeRuleKey(RuleKey ruleKey) {
    return ruleKey.repository() + ":" + ruleKey.rule();
  }

  public static RuleKey deserializeRuleKey(String serialized) {
    int indexOfSeparator = serialized.indexOf(':');
    String repository = serialized.substring(0, indexOfSeparator);
    String key = serialized.substring(indexOfSeparator + 1);
    return new RuleKey(repository, key);
  }

  public static String serializeRuleExclusions(Collection<RuleKey> exclusions) {
    return exclusions.stream()
      .map(PreferencesUtils::serializeRuleKey)
      .collect(Collectors.joining(";"));
  }

  public static Set<RuleKey> deserializeRuleExclusions(@Nullable String property) {
    String[] values = StringUtils.split(property, ";");
    return Arrays.stream(values)
      .map(PreferencesUtils::deserializeRuleKey)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  public static void excludeRule(RuleKey ruleKey) {
    Set<RuleKey> excludedRules = deserializeRuleExclusions(getPreferenceString(PREF_RULE_EXCLUSIONS));
    excludedRules.add(ruleKey);
    setPreferenceString(PREF_RULE_EXCLUSIONS, serializeRuleExclusions(excludedRules));
  }

  public static Collection<RuleKey> getExcludedRules() {
    return deserializeRuleExclusions(getPreferenceString(PREF_RULE_EXCLUSIONS));
  }
}

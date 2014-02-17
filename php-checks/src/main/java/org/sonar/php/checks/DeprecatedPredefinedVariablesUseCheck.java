/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010 Codehaus Sonar Plugins
 * dev@sonar.codehaus.org
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
package org.sonar.php.checks;

import com.google.common.collect.ImmutableMap;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.php.api.PHPPunctuator;
import org.sonar.php.api.PHPTokenType;
import org.sonar.php.parser.PHPGrammar;

@Rule(
  key = "S1600",
  priority = Priority.MAJOR)
@BelongsToProfile(title = CheckList.SONAR_WAY_PROFILE, priority = Priority.MAJOR)
public class DeprecatedPredefinedVariablesUseCheck extends SquidCheck<Grammar> {

  private static ImmutableMap<String, String> predefinedVariables = ImmutableMap.<String, String>builder()
    .put("$HTTP_SERVER_VARS", "$_SERVER")
    .put("$HTTP_GET_VARS", "$_GET")
    .put("$HTTP_POST_VARS", "$_POST")
    .put("$HTTP_POST_FILES", "$_FILES")
    .put("$HTTP_SESSION_VARS", "$_SESSION")
    .put("$HTTP_ENV_VARS", "$_ENV")
    .put("$HTTP_COOKIE_VARS", "$_COOKIE").build();

  @Override
  public void init() {
    subscribeTo(PHPGrammar.COMPOUND_VARIABLE);
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (isSimpleVariable(astNode)) {
      String varName = astNode.getFirstChild().getTokenOriginalValue();

      if (predefinedVariables.containsKey(varName)) {
        getContext().createLineViolation(this, "Replace this use of the deprecated \"{0}\" variable with \"{1}\".", astNode, varName, predefinedVariables.get(varName));
      }
    }
  }

  private static boolean isSimpleVariable(AstNode compoundNode) {
    AstNode compoundChild = compoundNode.getFirstChild();

    return compoundChild.is(PHPTokenType.VAR_IDENTIFIER)
      && compoundNode.getParent().getPreviousAstNode().isNot(PHPGrammar.SIMPLE_INDIRECT_REFERENCE)
      && compoundChild.getPreviousAstNode().isNot(PHPPunctuator.ARROW, PHPPunctuator.DOUBLECOLON, PHPPunctuator.DOLAR_LCURLY);
  }

}
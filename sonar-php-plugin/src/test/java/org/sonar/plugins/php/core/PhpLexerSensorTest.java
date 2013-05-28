/*
 * Sonar PHP Plugin
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
package org.sonar.plugins.php.core;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PhpLexerSensorTest {

  private final PhpParserConfiguration conf = PhpParserConfiguration.builder()
      .setCharset(Charsets.UTF_8)
      .build();

  @Test
  public void computeLinesMetricsOnRealFile() throws URISyntaxException {
    PhpLexerSensor sensor = new PhpLexerSensor(mock(FileLinesContextFactory.class));
    final Set<Integer> linesOfCode = Sets.newHashSet();
    final Set<Integer> linesOfComments = Sets.newHashSet();
    sensor.computePerLineMetrics(new File(this.getClass().getResource("/Math2.php").toURI()), Charsets.UTF_8, 212, mock(FileLinesContext.class), linesOfCode, linesOfComments);

    assertThat(linesOfCode).contains(1);
    assertThat(linesOfComments).excludes(1);

    assertThat(linesOfCode).excludes(2);
    assertThat(linesOfComments).contains(2);

    assertThat(linesOfCode).excludes(3);
    assertThat(linesOfComments).contains(3);

    // Last line of comment
    assertThat(linesOfCode).excludes(43);
    assertThat(linesOfComments).contains(43);

    // Blank line
    assertThat(linesOfCode).excludes(44);
    assertThat(linesOfComments).excludes(44);

  }

}

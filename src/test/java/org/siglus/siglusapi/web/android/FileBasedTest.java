/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.siglus.siglusapi.web.android;

import static java.util.Collections.emptyList;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class FileBasedTest {

  @SneakyThrows
  protected String readFromFile(String fileName) {
    String name = this.getClass().getName();
    String folder = name.replace("org.siglus.siglusapi.", "").replaceAll("\\.", "/");
    ClassLoader classLoader = this.getClass().getClassLoader();
    List<String> allLines = Optional.ofNullable(classLoader.getResource(folder + "/" + fileName))
        .map(this::toUri)
        .map(Paths::get)
        .map(this::readAllLines)
        .orElse(emptyList());
    return String.join("\n", allLines);
  }

  @SneakyThrows
  private URI toUri(URL url) {
    return url.toURI();
  }

  @SneakyThrows
  private List<String> readAllLines(Path path) {
    return Files.readAllLines(path);
  }

}

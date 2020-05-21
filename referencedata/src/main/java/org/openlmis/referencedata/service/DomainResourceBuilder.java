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

package org.openlmis.referencedata.service;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.openlmis.referencedata.domain.BaseEntity.BaseImporter;
import org.openlmis.referencedata.domain.Identifiable;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;

interface DomainResourceBuilder<I, O> {

  O build(I input);

  default <R> R findResource(Function<UUID, R> finder, BaseImporter importer, String errorMessage) {
    if (null == importer || null == importer.getId()) {
      throw new ValidationMessageException(errorMessage);
    }

    R resource = finder.apply(importer.getId());

    if (null == resource) {
      throw new ValidationMessageException(errorMessage);
    }

    return resource;
  }

  default <R extends Identifiable> List<R> findResources(Function<Set<UUID>, Iterable<R>> finder,
      Set<UUID> ids, String errorMessage) {
    Iterable<R> iterable = finder.apply(ids);

    Map<UUID, R> resources = StreamSupport
        .stream(iterable.spliterator(), false)
        .collect(Collectors.toMap(Identifiable::getId, Function.identity()));

    Set<UUID> missing = ids
        .stream()
        .map(id -> new ImmutablePair<>(id, resources.get(id)))
        .filter(pair -> null == pair.getRight())
        .map(ImmutablePair::getLeft)
        .collect(Collectors.toSet());

    if (!missing.isEmpty()) {
      throw new ValidationMessageException(new Message(errorMessage));
    }

    return Lists.newArrayList(resources.values());
  }

}

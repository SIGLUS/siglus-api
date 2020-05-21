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

package org.openlmis.referencedata.repository.custom.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.repository.custom.ProgramRepositoryCustom;

public class ProgramRepositoryImpl implements ProgramRepositoryCustom {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve all Programs with programName similar to name parameter.
   * Method is ignoring case for program name.
   * To find all wanted Programs we use criteria query and like operator.
   *
   * @param name Part of wanted program name.
   * @return List of Programs with wanted name.
   */
  public List<Program> findProgramsByName(String name) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Program> query = builder.createQuery(Program.class);
    Root<Program> root = query.from(Program.class);
    Predicate predicate = builder.conjunction();

    if (name != null) {
      predicate = builder.and(
          predicate,
          builder.like(
                  builder.upper(root.get("name")), "%" + name.toUpperCase() + "%"));
    }
    query.where(predicate);
    return entityManager.createQuery(query).getResultList();
  }
}

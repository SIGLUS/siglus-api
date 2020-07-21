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

package org.siglus.siglusapi.domain;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IdClass(RequisitionExtensionPK.class)
@Table(name = "requisition_extension", schema = "siglusintegration")
public class RequisitionExtension implements Serializable {

  @Id
  @GeneratedValue(generator = "uuid-gen")
  @GenericGenerator(name = "uuid-gen",
      strategy = "org.siglus.common.util.ConditionalUuidGenerator")
  @Type(type = "pg-uuid")
  private UUID id;

  private UUID requisitionId;

  private String requisitionNumberPrefix;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence-gen")
  @SequenceGenerator(
      name = "sequence-gen",
      sequenceName = "requisition_extension_requisitionnumber_seq",
      schema = "siglusintegration",
      allocationSize = 1
  )
  private Integer requisitionNumber;
}

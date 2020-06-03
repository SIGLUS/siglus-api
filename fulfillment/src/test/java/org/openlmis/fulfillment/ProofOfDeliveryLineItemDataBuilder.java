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

package org.openlmis.fulfillment;

import java.util.UUID;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.openlmis.fulfillment.domain.ProofOfDeliveryLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.domain.naming.VvmStatus;

@SuppressWarnings("PMD.TooManyMethods")
public class ProofOfDeliveryLineItemDataBuilder {
  private UUID id = UUID.randomUUID();
  private VersionEntityReference orderable = new VersionEntityReference(UUID.randomUUID(), 1L);
  private UUID lotId = UUID.randomUUID();
  private Integer quantityAccepted = RandomUtils.nextInt(1, 10);
  private boolean useVvm = true;
  private VvmStatus vvmStatus = VvmStatus.STAGE_1;
  private Integer quantityRejected = RandomUtils.nextInt(1, 5);
  private UUID rejectionReasonId = UUID.randomUUID();
  private String notes = RandomStringUtils.randomAlphanumeric(25);

  /**
   * Builds new instance of {@link ProofOfDeliveryLineItem} without id.
   */
  public ProofOfDeliveryLineItem buildAsNew() {
    return new ProofOfDeliveryLineItem(
        orderable, lotId, quantityAccepted, vvmStatus,
        quantityRejected, rejectionReasonId, notes
    );
  }

  public ProofOfDeliveryLineItemDataBuilder withOrderable(UUID orderableId, Long versionNumber) {
    this.orderable = new VersionEntityReference(orderableId, versionNumber);
    return this;
  }

  public ProofOfDeliveryLineItemDataBuilder withIncorrectQuantityAccepted() {
    quantityAccepted =  RandomUtils.nextInt(1, 10) * -1;
    return this;
  }

  public ProofOfDeliveryLineItemDataBuilder withIncorrectQuantityRejected() {
    quantityRejected =  RandomUtils.nextInt(1, 5) * -1;
    return this;
  }

  public ProofOfDeliveryLineItemDataBuilder withoutVvmStatus() {
    vvmStatus = null;
    return this;
  }

  public ProofOfDeliveryLineItemDataBuilder withVvmStatus(VvmStatus vvmStatus) {
    this.vvmStatus = vvmStatus;
    return this;
  }

  public ProofOfDeliveryLineItemDataBuilder withoutReason() {
    rejectionReasonId = null;
    return this;
  }

  public ProofOfDeliveryLineItemDataBuilder withoutLotId() {
    this.lotId = null;
    return this;
  }

  /**
   * Sets quantity accepted as null value. If useVVM flag is set, the vvmStatus will have a null
   * value.
   */
  public ProofOfDeliveryLineItemDataBuilder withoutQuantityAccepted() {
    this.quantityAccepted = null;

    if (useVvm) {
      this.vvmStatus = null;
    }

    return this;
  }

  /**
   * Sets quantity rejected as null value. The rejection reason id will have a null value as well.
   */
  public ProofOfDeliveryLineItemDataBuilder withoutQuantityRejected() {
    this.quantityRejected = null;
    this.rejectionReasonId = null;
    return this;
  }

  /**
   * Builds new instance of {@link ProofOfDeliveryLineItem}.
   */
  public ProofOfDeliveryLineItem build() {
    ProofOfDeliveryLineItem lineItem = buildAsNew();
    lineItem.setId(id);

    return lineItem;
  }

}

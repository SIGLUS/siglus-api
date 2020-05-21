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

package org.openlmis.requisition.service.referencedata;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.ProgramDtoDataBuilder;

public class ApproveProductsAggregatorTest {
  private static final int PRODUCT_COUNT = 25;

  private ProgramDto program;
  private Set<ApprovedProductReference> references = Sets.newHashSet();
  private Set<ApprovedProductReference> fullSupplyReferences = Sets.newHashSet();
  private Set<ApprovedProductReference> nonFullSupplyReferences = Sets.newHashSet();
  private List<ApprovedProductDto> approvedProducts = Lists.newArrayList();

  @Before
  public void setUp() {
    program = new ProgramDtoDataBuilder().buildAsDto();

    for (int i = 0; i < PRODUCT_COUNT; ++i) {
      UUID productId = UUID.randomUUID();
      UUID orderableId = UUID.randomUUID();
      references.add(new ApprovedProductReference(productId, 1L, orderableId, 1L));

      if (i % 3 == 0) {
        fullSupplyReferences.add(new ApprovedProductReference(productId, 1L, orderableId, 1L));
      }
    }

    nonFullSupplyReferences.addAll(references);
    nonFullSupplyReferences.removeAll(fullSupplyReferences);

    for (ApprovedProductReference reference : references) {
      approvedProducts.add(new ApprovedProductDtoDataBuilder()
          .withId(reference.getFacilityTypeApprovedProduct().getId())
          .withVersionNumber(reference.getFacilityTypeApprovedProduct().getVersionNumber())
          .withOrderable(new OrderableDtoDataBuilder()
              .withId(reference.getOrderable().getId())
              .withVersionNumber(reference.getOrderable().getVersionNumber())
              .withProgramOrderable(program.getId(),
                  fullSupplyReferences.contains(reference))
              .buildAsDto())
          .withProgram(program)
          .buildAsDto()
      );
    }
  }

  @Test
  public void shouldCreateNewInstance() {
    ApproveProductsAggregator products = new ApproveProductsAggregator(
        approvedProducts, program.getId()
    );

    compareCollections(products.getApprovedProductReferences(), references);
    compareCollections(
        products.getFullSupplyOrderableIds(),
        fullSupplyReferences
            .stream()
            .map(ApprovedProductReference::getOrderable)
            .map(VersionEntityReference::getId)
            .collect(Collectors.toSet()));
    compareCollections(products.getNonFullSupplyApprovedProductReferences(),
        nonFullSupplyReferences);

    Collection<ApprovedProductDto> fullSupplyProducts = approvedProducts
        .stream()
        .filter(item -> requireNonNull(
            item.getOrderable().getProgramOrderable(program.getId())).getFullSupply())
        .collect(Collectors.toList());

    compareCollections(products.getFullSupplyProducts(), fullSupplyProducts);
  }

  private <T> void compareCollections(Collection<T> actual, Collection<T> expected) {
    assertThat(actual, hasSize(expected.size()));
    assertThat(actual, containsInAnyOrder(expected.toArray()));
  }
}

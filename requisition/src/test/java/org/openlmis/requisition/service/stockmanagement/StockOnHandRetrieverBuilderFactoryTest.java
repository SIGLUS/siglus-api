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

package org.openlmis.requisition.service.stockmanagement;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.RequisitionTemplateDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;

@RunWith(MockitoJUnitRunner.class)
public class StockOnHandRetrieverBuilderFactoryTest {

  @Mock
  private StockCardSummariesStockManagementService stockCardSummariesStockManagementService;

  @InjectMocks
  private StockOnHandRetrieverBuilderFactory factory;

  @Test
  public void shouldCreateBuilderForRegularTemplate() {
    RequisitionTemplate template = new RequisitionTemplateDataBuilder().withAllColumns().build();

    StockOnHandRetrieverBuilder instance = factory
        .getInstance(template, RequisitionLineItem.STOCK_ON_HAND);
    assertThat(instance, instanceOf(EmptyStockOnHandRetrieverBuilder.class));
    assertThat(
        instance.getStockCardSummariesService(),
        is(stockCardSummariesStockManagementService)
    );
  }

  @Test
  public void shouldCreateBuilderForStockBasedTemplate() {
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withPopulateStockOnHandFromStockCards().withAllColumns().build();

    StockOnHandRetrieverBuilder instance = factory
        .getInstance(template, RequisitionLineItem.STOCK_ON_HAND);
    assertThat(instance, instanceOf(StandardStockOnHandRetrieverBuilder.class));
    assertThat(
        instance.getStockCardSummariesService(),
        is(stockCardSummariesStockManagementService)
    );
  }

  @Test
  public void shouldCreateBuilderForStockBasedTemplateIfColumnNotExist() {
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withPopulateStockOnHandFromStockCards().withAllColumnsExceptStockOnHand().build();

    StockOnHandRetrieverBuilder instance = factory
        .getInstance(template, RequisitionLineItem.STOCK_ON_HAND);
    assertThat(instance, instanceOf(EmptyStockOnHandRetrieverBuilder.class));
    assertThat(
        instance.getStockCardSummariesService(),
        is(stockCardSummariesStockManagementService)
    );
  }

  @Test
  public void shouldCreateBuilderForStockBasedTemplateIfColumnIsNotDisplayed() {
    RequisitionTemplate template = new RequisitionTemplateDataBuilder()
        .withPopulateStockOnHandFromStockCards().withStockOnHandColumnHiden().build();

    StockOnHandRetrieverBuilder instance = factory
        .getInstance(template, RequisitionLineItem.STOCK_ON_HAND);
    assertThat(instance, instanceOf(EmptyStockOnHandRetrieverBuilder.class));
    assertThat(
        instance.getStockCardSummariesService(),
        is(stockCardSummariesStockManagementService)
    );
  }
}

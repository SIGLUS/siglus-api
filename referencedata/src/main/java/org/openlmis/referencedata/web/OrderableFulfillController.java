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

package org.openlmis.referencedata.web;

import static org.openlmis.referencedata.web.OrderableFulfillController.RESOURCE_PATH;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.referencedata.domain.CommodityType;
import org.openlmis.referencedata.domain.FacilityTypeApprovedProduct;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.TradeItem;
import org.openlmis.referencedata.repository.CommodityTypeRepository;
import org.openlmis.referencedata.repository.FacilityTypeApprovedProductRepository;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.repository.TradeItemRepository;
import org.openlmis.referencedata.util.EntityCollection;
import org.openlmis.referencedata.util.Pagination;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping(RESOURCE_PATH)
public class OrderableFulfillController extends BaseController {

  private static final XLogger XLOGGER = XLoggerFactory
          .getXLogger(OrderableFulfillController.class);

  public static final String RESOURCE_PATH = API_PATH + "/orderableFulfills";

  @Autowired
  private OrderableRepository orderableRepository;

  @Autowired
  private OrderableFulfillFactory orderableFulfillFactory;

  @Autowired
  private TradeItemRepository tradeItemRepository;

  @Autowired
  private CommodityTypeRepository commodityTypeRepository;

  @Autowired
  private FacilityTypeApprovedProductRepository ftapRepository;

  /**
   * Gets orderable fulfills.
   */
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public Map<UUID, OrderableFulfill> getOrderableFulfills(
      @RequestParam MultiValueMap<String, Object> requestParams) {
    Profiler profiler = new Profiler("GET_ORDERABLE_FULFILLS");
    profiler.setLogger(XLOGGER);

    profiler.start("VALIDATE_PARAMS");
    OrderableFulfillSearchParams searchParams = new OrderableFulfillSearchParams(requestParams);

    Set<UUID> ids = getOrderableIds(searchParams, profiler);

    profiler.start("FIND_ALL_TRADE_ITEMS_AND_COMMODITY_TYPES");
    EntityCollection<TradeItem> tradeItems = new EntityCollection<>(tradeItemRepository.findAll());
    EntityCollection<CommodityType> commodityTypes
        = new EntityCollection<>(commodityTypeRepository.findAll());

    profiler.start("GET_ORDERABLES");
    List<Orderable> orderables = getOrderables(ids);

    profiler.start("CONVERT_TO_ORDERABLE_FULFILLS");
    Map<UUID, OrderableFulfill> map = Maps.newHashMap();
    orderables.forEach(orderable -> addEntry(map, orderable, tradeItems, commodityTypes));

    profiler.stop().log();
    return map;
  }

  private List<Orderable> getOrderables(Set<UUID> ids) {
    PageRequest noPaginationRequest = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER,
        Pagination.NO_PAGINATION);
    Page<Orderable> pageWithAllOrderables = ids.isEmpty()
        ? orderableRepository.findAllLatest(noPaginationRequest)
        : orderableRepository.findAllLatestByIds(ids, noPaginationRequest);
    return pageWithAllOrderables.getContent();
  }

  private void addEntry(Map<UUID, OrderableFulfill> map, Orderable orderable,
                        EntityCollection<TradeItem> tradeItems,
                        EntityCollection<CommodityType> commodityTypes) {
    Optional
        .ofNullable(orderableFulfillFactory.createFor(orderable, tradeItems, commodityTypes))
        .ifPresent(resource -> map.put(orderable.getId(), resource));
  }

  private Set<UUID> getOrderableIds(OrderableFulfillSearchParams queryMap, Profiler profiler) {
    if (queryMap.isSearchByFacilityIdAndProgramId()) {
      profiler.start("GET_ORDERABLES_IDS_BY_FACILITY_AND_PROGRAM");
      return ftapRepository
          .searchProducts(queryMap.getFacilityId(), queryMap.getProgramId(), null, null, true, null)
          .getContent()
          .stream()
          .map(FacilityTypeApprovedProduct::getOrderableId)
          .collect(Collectors.toSet());
    }
    profiler.start("GET_ORDERABLES_IDS");
    return queryMap.getIds();
  }

}

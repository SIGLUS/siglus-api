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

package org.openlmis.fulfillment.web;

import static org.openlmis.fulfillment.domain.OrderStatus.TRANSFER_FAILED;
import static org.openlmis.fulfillment.i18n.MessageKeys.ORDER_RETRY_INVALID_STATUS;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openlmis.fulfillment.domain.CreationDetails;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.Template;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.service.FileTemplateService;
import org.openlmis.fulfillment.service.JasperReportsViewService;
import org.openlmis.fulfillment.service.OrderCsvHelper;
import org.openlmis.fulfillment.service.OrderSearchParams;
import org.openlmis.fulfillment.service.OrderService;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.service.ResultDto;
import org.openlmis.fulfillment.service.ShipmentService;
import org.openlmis.fulfillment.service.TemplateService;
import org.openlmis.fulfillment.service.referencedata.UserDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.util.BasicOrderDto;
import org.openlmis.fulfillment.web.util.BasicOrderDtoBuilder;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.fulfillment.web.util.OrderDtoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsMultiFormatView;

@Controller
@Transactional
public class OrderController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);
  private static final String DISPOSITION_BASE = "attachment; filename=";

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private OrderService orderService;

  @Autowired
  private OrderCsvHelper csvHelper;

  @Autowired
  private FileTemplateService fileTemplateService;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private JasperReportsViewService jasperReportsViewService;

  @Autowired
  private TemplateService templateService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private ShipmentService shipmentService;

  @Autowired
  private OrderDtoBuilder orderDtoBuilder;

  @Autowired
  private BasicOrderDtoBuilder basicOrderDtoBuilder;

  @Value("${groupingSeparator}")
  private String groupingSeparator;

  @Value("${groupingSize}")
  private String groupingSize;

  @Value("${dateTimeFormat}")
  private String dateTimeFormat;

  /**
   * Allows creating new orders.
   * If the id is specified, it will be ignored.
   *
   * @param orderDto A order bound to the request body
   * @return the newly created order
   */
  @RequestMapping(value = "/orders", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public OrderDto createOrder(@RequestBody OrderDto orderDto, OAuth2Authentication authentication) {
    Order order = createSingleOrder(orderDto, authentication);
    return orderDtoBuilder.build(order);
  }

  /**
   * Allows creating multiple new orders at once in a single transaction.
   * If the id is specified for any of the orders, it will be ignored.
   *
   * @param orders A list of orders to be created
   * @return a list of newly created orders
   */
  @RequestMapping(value = "/orders/batch", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public Iterable<BasicOrderDto> batchCreateOrders(@RequestBody List<OrderDto> orders,
      OAuth2Authentication authentication) {
    List<Order> newOrders = orders
        .stream()
        .map(order -> createSingleOrder(order, authentication))
        .collect(Collectors.toList());
    return basicOrderDtoBuilder.build(newOrders);
  }

  /**
   * Search through orders with given parameters.
   *
   * @param params   order search params
   * @param pageable pagination parameters
   * @return OrderDtos.
   */
  @GetMapping("/orders")
  @ResponseBody
  public Page<BasicOrderDto> searchOrders(OrderSearchParams params, Pageable pageable) {
    Profiler profiler = new Profiler("SEARCH_ORDERS");
    profiler.setLogger(LOGGER);

    profiler.start("SEARCH_ORDERS_IN_SERVICE");
    Page<Order> orders = orderService.searchOrders(params, pageable);

    profiler.start("TO_DTO");
    List<BasicOrderDto> dtos = basicOrderDtoBuilder.build(orders.getContent());
    Page<BasicOrderDto> dtoPage = new PageImpl<>(
        dtos,
        pageable, orders.getTotalElements());

    profiler.stop().log();
    return dtoPage;
  }

  /**
   * Get chosen order.
   *
   * @param orderId UUID of order whose we want to get
   * @param expand a set of field names to expand
   * @return OrderDto.
   */
  @RequestMapping(value = "/orders/{id}", method = RequestMethod.GET)
  @ResponseBody
  public OrderDto getOrder(@PathVariable("id") UUID orderId,
                           @RequestParam(required = false) Set<String> expand) {
    Order order = orderRepository.findOne(orderId);
    if (order == null) {
      throw new OrderNotFoundException(orderId);
    } else {
      permissionService.canViewOrder(order);
      OrderDto orderDto = orderDtoBuilder.build(order);
      expandDto(orderDto, expand);
      return orderDto;
    }
  }

  /**
   * Retrieves the distinct UUIDs of the available requesting facilities.
   */
  @RequestMapping(value = "/orders/requestingFacilities", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UUID> getRequestingFacilities(
      @RequestParam(name = "supplyingFacilityId", required = false)
          List<UUID> supplyingFacilityIds) {
    return orderRepository.getRequestingFacilities(supplyingFacilityIds);
  }

  /**
   * Returns csv or pdf of defined object in response.
   *
   * @param orderId UUID of order to print
   * @param format  String describing return format (pdf or csv)
   */
  @RequestMapping(value = "/orders/{id}/print", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ModelAndView printOrder(HttpServletRequest request,
                                 @PathVariable("id") UUID orderId,
                                 @RequestParam("format") String format) throws IOException {

    Order order = orderRepository.findOne(orderId);
    if (order == null) {
      throw new OrderNotFoundException(orderId);
    }
    permissionService.canViewOrder(order);

    String filePath = "jasperTemplates/ordersJasperTemplate.jrxml";
    ClassLoader classLoader = getClass().getClassLoader();

    Template template = new Template();
    template.setName("ordersJasperTemplate");

    try (InputStream fis = classLoader.getResourceAsStream(filePath)) {
      templateService.createTemplateParameters(template, fis);
    }

    Map<String, Object> params = new HashMap<>();
    params.put("format", format);
    DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
    decimalFormatSymbols.setGroupingSeparator(groupingSeparator.charAt(0));
    DecimalFormat decimalFormat = new DecimalFormat("", decimalFormatSymbols);
    decimalFormat.setGroupingSize(Integer.parseInt(groupingSize));
    params.put("decimalFormat", decimalFormat);
    params.put("dateTimeFormat", dateTimeFormat);

    JasperReportsMultiFormatView jasperView = jasperReportsViewService
        .getJasperReportsView(template, request);

    return jasperReportsViewService.getOrderJasperReportView(jasperView, params, order);
  }


  /**
   * Exporting order to csv.
   *
   * @param orderId  UUID of order to print
   * @param type     export type
   * @param response HttpServletResponse object
   */
  @RequestMapping(value = "/orders/{id}/export", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public void export(@PathVariable("id") UUID orderId,
                  @RequestParam(value = "type", required = false, defaultValue = "csv") String type,
                     HttpServletResponse response) throws IOException {
    if (!"csv".equals(type)) {
      String msg = "Export type: " + type + " not allowed";
      LOGGER.warn(msg);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
      return;
    }

    Order order = orderRepository.findOne(orderId);

    if (order == null) {
      String msg = "Order does not exist.";
      LOGGER.warn(msg);
      response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
      return;
    }

    permissionService.canViewOrder(order);

    FileTemplate fileTemplate = fileTemplateService.getOrderFileTemplate();

    if (fileTemplate == null) {
      String msg = "Could not export Order, because Order Template File not found";
      LOGGER.warn(msg);
      response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
      return;
    }

    response.setContentType("text/csv");
    response.addHeader(HttpHeaders.CONTENT_DISPOSITION,
        DISPOSITION_BASE + fileTemplate.getFilePrefix() + order.getOrderCode() + ".csv");

    try {
      csvHelper.writeCsvFile(order, fileTemplate, response.getWriter());
    } catch (IOException ex) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Error occurred while exporting order to csv.");
      LOGGER.error("Error occurred while exporting order to csv", ex);
    }
  }

  /**
   * Manually retry for transferring order file via FTP after updating or checking the FTP
   * transfer properties.
   *
   * @param id UUID of order
   */
  @RequestMapping(value = "/orders/{id}/retry", method = RequestMethod.GET)
  @ResponseBody
  public ResultDto<Boolean> retryOrderTransfer(@PathVariable("id") UUID id) {
    Order order = orderRepository.findOne(id);

    if (null == order) {
      throw new OrderNotFoundException(id);
    }

    permissionService.canTransferOrder(order);

    if (TRANSFER_FAILED != order.getStatus()) {
      throw new ValidationException(ORDER_RETRY_INVALID_STATUS, TRANSFER_FAILED.toString());
    }

    orderService.save(order);
    return new ResultDto<>(TRANSFER_FAILED != order.getStatus());
  }

  private Order createSingleOrder(OrderDto orderDto,
                                  OAuth2Authentication authentication) {
    orderDto.setId(null);

    UserDto currentUser = authenticationHelper.getCurrentUser();
    UUID userId = currentUser == null ? orderDto.getLastUpdater().getId() : currentUser.getId();

    if (!authentication.isClientOnly()) {
      LOGGER.debug("Checking rights to create order");
      permissionService.canEditOrder(orderDto);
    }

    LOGGER.debug("Creating new order");
    Order order = orderService.createOrder(orderDto, userId);

    if (order.isExternal()) {
      List<ShipmentLineItem> items = order
          .getOrderLineItems()
          .stream()
          .map(line -> new ShipmentLineItem(line.getOrderable(), line.getOrderedQuantity()))
          .collect(Collectors.toList());

      Shipment shipment = new Shipment(
          order, new CreationDetails(order.getCreatedById(), order.getCreatedDate()),
          null, items, ImmutableMap.of("external", "true"));

      shipmentService.save(shipment);
    }

    return order;
  }
}

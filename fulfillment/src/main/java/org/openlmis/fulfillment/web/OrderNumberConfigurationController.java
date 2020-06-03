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

import java.util.Iterator;
import javax.validation.Valid;
import org.openlmis.fulfillment.domain.OrderNumberConfiguration;
import org.openlmis.fulfillment.repository.OrderNumberConfigurationRepository;
import org.openlmis.fulfillment.service.PermissionService;
import org.openlmis.fulfillment.web.util.OrderNumberConfigurationDto;
import org.openlmis.fulfillment.web.validator.OrderNumberConfigurationValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Transactional
public class OrderNumberConfigurationController extends BaseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileTemplateController.class);


  @Autowired
  private OrderNumberConfigurationRepository orderNumberConfigurationRepository;

  @Autowired
  private OrderNumberConfigurationValidator validator;

  @Autowired
  private PermissionService permissionService;

  @InitBinder
  protected void initBinder(final WebDataBinder binder) {
    binder.addValidators(validator);
  }

  /**
   * Saves given OrderNumberConfiguration to database.
   *
   * @param orderNumberConfigurationDto object to save.
   * @return Response entity with Http status code.
   */
  @RequestMapping(value = "/orderNumberConfigurations", method = RequestMethod.POST)
  public ResponseEntity<Object> saveOrderNumberConfigurations(
      @RequestBody @Valid OrderNumberConfigurationDto orderNumberConfigurationDto,
      BindingResult bindingResult) {

    LOGGER.debug("Checking right to update order number configuration");
    permissionService.canManageSystemSettings();

    if (bindingResult.hasErrors()) {
      return new ResponseEntity<>(getErrors(bindingResult), HttpStatus.BAD_REQUEST);
    }

    OrderNumberConfiguration orderNumberConfiguration = OrderNumberConfiguration
        .newInstance(orderNumberConfigurationDto);

    Iterator<OrderNumberConfiguration> it = orderNumberConfigurationRepository.findAll().iterator();

    if (it.hasNext()) {
      orderNumberConfiguration.setId(it.next().getId());
    }

    OrderNumberConfiguration savedOrderNumberConfiguration =
        orderNumberConfigurationRepository.save(orderNumberConfiguration);

    OrderNumberConfigurationDto orderNumberConfigurationDto1 = OrderNumberConfigurationDto
        .newInstance(savedOrderNumberConfiguration);

    return new ResponseEntity<>(orderNumberConfigurationDto1, HttpStatus.OK);
  }

  /**
   * Get orderNumberConfiguration.
   *
   * @return OrderNumberConfiguration.
   */
  @RequestMapping(value = "/orderNumberConfigurations", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<OrderNumberConfigurationDto> getOrderFileTemplate() {

    LOGGER.debug("Checking right to view order number configuration");
    permissionService.canManageSystemSettings();

    Iterator<OrderNumberConfiguration> it = orderNumberConfigurationRepository.findAll().iterator();

    if (!it.hasNext()) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    OrderNumberConfigurationDto orderNumberConfigurationDto = new OrderNumberConfigurationDto();
    it.next().export(orderNumberConfigurationDto);

    return new ResponseEntity<>(orderNumberConfigurationDto, HttpStatus.OK);
  }
}

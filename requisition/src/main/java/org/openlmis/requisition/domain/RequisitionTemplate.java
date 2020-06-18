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

package org.openlmis.requisition.domain;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_CANNOT_ASSIGN_TEMPLATE_TO_SEVERAL_PROGRAMS;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMNS_MAP_IS_NULL;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMNS_MAP_TAGS_DUPLICATED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMNS_TAG_NOT_SET;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_COLUMN_NOT_IN_TEMPLATE;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_OPTION_NOT_AVAILABLE_FOR_THIS_COLUMN;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_SOURCE_NOT_AVAILABLE_FOR_THIS_COLUMN;

import com.google.common.collect.Sets;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.BatchSize;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.utils.Message;
import org.siglus.common.domain.RequisitionTemplateExtension;

@SuppressWarnings("PMD.TooManyMethods")
@Entity
@Table(name = "requisition_templates", schema = "requisition")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"programId", "facilityTypeIds"})
public class RequisitionTemplate extends BaseTimestampedEntity {
  public static final String SOURCE = "Source ";
  public static final String OPTION = "Option ";
  public static final String WARNING_SUFFIX = " is not available for this column.";

  public static final List<String> ORDER_RELATED_COLUMNS = Collections.unmodifiableList(
      asList(
          "requestedQuantity",
          "requestedQuantityExplanation",
          "approvedQuantity",
          "packsToShip",
          "calculatedOrderQuantity",
          "pricePerPack",
          "totalCost",
          "additionalQuantityRequired"
      )
  );

  @Getter
  private Integer numberOfPeriodsToAverage;

  @Getter
  private boolean populateStockOnHandFromStockCards;
  
  @Getter
  private String name;

  @Column(nullable = false)
  @Getter
  private boolean archived = false;

  @ElementCollection(fetch = FetchType.LAZY)
  @MapKeyColumn(name = "key")
  @Column(name = "value")
  @CollectionTable(name = "columns_maps", schema = "requisition",
      joinColumns = @JoinColumn(name = "requisitionTemplateId"))
  @BatchSize(size = STANDARD_BATCH_SIZE)
  private Map<String, RequisitionTemplateColumn> columnsMap = new HashMap<>();

  @OneToMany(
      cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.REMOVE},
      orphanRemoval = true,
      mappedBy = "template")
  @DiffIgnore
  @BatchSize(size = STANDARD_BATCH_SIZE)
  private Set<RequisitionTemplateAssignment> templateAssignments = new HashSet<>();

  @Transient
  @Getter
  private UUID programId;

  @Transient
  @Getter
  private Set<UUID> facilityTypeIds = Sets.newHashSet();

  // [SIGLUS change start]
  // [change reason]: for support usage report which attributes
  @Transient
  @Getter
  @Setter
  private RequisitionTemplateExtension templateExtension;
  // [SIGLUS change end]

  RequisitionTemplate(UUID id) {
    this(id, null, false, null, null, null);
  }

  /**
   * Allows creating requisition template with predefined columns.
   *
   * @param columns Columns to appear in requisition template.
   */
  public RequisitionTemplate(Map<String, RequisitionTemplateColumn> columns) {
    this(null, null, false, null, columns, null);
  }

  /**
   * Allows creating requisition template with the given properties.
   */
  public RequisitionTemplate(UUID id, Integer numberOfPeriodsToAverage,
                             boolean populateStockOnHandFromStockCards,
                             String name,
                             Map<String, RequisitionTemplateColumn> columnsMap,
                             Set<RequisitionTemplateAssignment> templateAssignments) {
    setId(id);

    this.numberOfPeriodsToAverage = numberOfPeriodsToAverage;
    this.populateStockOnHandFromStockCards = populateStockOnHandFromStockCards;
    this.name = name;

    addColumns(columnsMap);
    addAssignments(templateAssignments);
  }

  /**
   * Copy constructor.
   * 
   * @param source source requisition template to copy from
   */
  public RequisitionTemplate(RequisitionTemplate source) {
    this.id = source.id;
    this.setCreatedDate(source.getCreatedDate());
    this.setModifiedDate(source.getModifiedDate());
    this.numberOfPeriodsToAverage = source.numberOfPeriodsToAverage;
    this.populateStockOnHandFromStockCards = source.populateStockOnHandFromStockCards;
    this.name = source.name;
    this.archived = source.archived;
    this.programId = source.programId;

    this.columnsMap = new HashMap<>();
    source.columnsMap.forEach((key, value) -> this.columnsMap.put(key, value.copy()));
    this.templateAssignments = new HashSet<>();
    this.templateAssignments.addAll(source.templateAssignments);
    this.facilityTypeIds = new HashSet<>();
    this.facilityTypeIds.addAll(source.facilityTypeIds);
  }

  /**
   * Returns current columns view.
   */
  public Map<String, RequisitionTemplateColumn> viewColumns() {
    Map<String, RequisitionTemplateColumn> map = new HashMap<>();
    columnsMap.forEach((key, value) -> map.put(key, value.copy()));

    return Collections.unmodifiableMap(map);
  }

  /**
   * Add new assignment. Currently template can be assign to single program and several facility
   * types.
   */
  public void addAssignment(UUID programId, UUID facilityTypeId) {
    setProgramId(programId);
    addFacilityTypeId(facilityTypeId);
    templateAssignments.add(new RequisitionTemplateAssignment(programId, facilityTypeId, this));
  }

  public boolean isColumnFromPreviousRequisition(String name) {
    RequisitionTemplateColumn column = findColumn(name);
    return SourceType.PREVIOUS_REQUISITION.equals(column.getSource());
  }


  /**
   * Checks if column with given name is displayed.
   *
   * @param name name of requisition column.
   * @return return true if column is displayed
   */
  public boolean isColumnDisplayed(String name) {
    RequisitionTemplateColumn column = findColumn(name);

    return column.getIsDisplayed();
  }

  /**
   * Checks if column with given name is calculated.
   *
   * @param name name of requisition column.
   * @return return true if column is calculated
   */
  public boolean isColumnCalculated(String name) {
    RequisitionTemplateColumn column = findColumn(name);

    return SourceType.CALCULATED.equals(column.getSource());
  }

  /**
   * Checks if column with given name is stock based.
   *
   * @param name name of requisition column.
   * @return return true if column is stock based
   */
  public boolean isColumnStockBased(String name) {
    RequisitionTemplateColumn column = findColumn(name);

    return SourceType.STOCK_CARDS.equals(column.getSource());
  }

  /**
   * Checks if column with given name is input by user.
   *
   * @param name name of requisition column.
   * @return return true if column is calculated
   */
  public boolean isColumnUserInput(String name) {
    RequisitionTemplateColumn column = findColumn(name);

    return SourceType.USER_INPUT.equals(column.getSource());
  }

  /**
   * Allows changing the display order of columns.
   *
   * @param key             Key to column which needs a new display order.
   * @param newDisplayOrder Number specifying new display order of extracted column.
   */
  public void changeColumnDisplayOrder(String key, int newDisplayOrder) {
    RequisitionTemplateColumn column = columnsMap.get(key);
    Integer oldDisplayOrder = column.getDisplayOrder();
    if (oldDisplayOrder == null) {
      moveDownAllColumnsBelowIndex(newDisplayOrder);
    } else {
      if (newDisplayOrder > oldDisplayOrder) {
        moveUpAllColumnsBetweenIndexes(newDisplayOrder, oldDisplayOrder);
      } else {
        moveDownAllColumnsBetweenIndexes(newDisplayOrder, oldDisplayOrder);
      }
    }
    if (column.getColumnDefinition().getCanChangeOrder()) {
      column.setDisplayOrder(newDisplayOrder);
    }
  }

  /**
   * Changes display flag of given column.
   * @param key     Key to column which needs a new display property.
   * @param display Should column be displayed.
   */
  public void changeColumnDisplay(String key, boolean display) {
    RequisitionTemplateColumn column = columnsMap.get(key);
    if (isNotTrue(column.getColumnDefinition().getIsDisplayRequired())) {
      if (display && "productCode".equals(key)) {
        column.setDisplayOrder(1);
      }
      column.setIsDisplayed(display);
    }
  }

  /**
   * Changes label of given column.
   * @param key  Key to column which needs a new name.
   * @param name New name for label.
   */
  public void changeColumnLabel(String key, String name) {
    RequisitionTemplateColumn column = columnsMap.get(key);
    column.setLabel(name);
  }

  /**
   * Validate source of column and change it if it's available.
   *
   * @param key    Key to column which needs a new source.
   * @param source New source for column.
   */
  public void changeColumnSource(String key, SourceType source) {

    RequisitionTemplateColumn column = findColumn(key);

    if (column.getColumnDefinition().getSources() == null) {
      throw new ValidationMessageException(new Message(ERROR_SOURCE_NOT_AVAILABLE_FOR_THIS_COLUMN,
          source.toString(), key));
    }

    if (!column.getColumnDefinition().getSources().contains(source)) {
      throw new ValidationMessageException(new Message(ERROR_SOURCE_NOT_AVAILABLE_FOR_THIS_COLUMN,
          source.toString(), key));
    }
    column.setSource(source);
  }

  /**
   * Validate option of column and change it if it's available.
   *
   * @param key    Key to column which needs a new option.
   * @param option New option for column.
   */
  public void changeColumnOption(String key, AvailableRequisitionColumnOption option) {

    RequisitionTemplateColumn column = findColumn(key);

    if (column.getColumnDefinition().getOptions() == null) {
      throw new ValidationMessageException(new Message(ERROR_OPTION_NOT_AVAILABLE_FOR_THIS_COLUMN,
          option.getOptionName(), key));
    }

    if (!column.getColumnDefinition().getOptions().contains(option)) {
      throw new ValidationMessageException(new Message(ERROR_OPTION_NOT_AVAILABLE_FOR_THIS_COLUMN,
          option.getOptionName(), key));
    }
    column.setOption(option);
  }

  /**
   * Copy values of attributes into new or updated RequisitionTemplate.
   *
   * @param requisitionTemplate RequisitionTemplate with new values.
   */
  public void updateFrom(RequisitionTemplate requisitionTemplate) {
    this.numberOfPeriodsToAverage = requisitionTemplate.numberOfPeriodsToAverage;
    this.populateStockOnHandFromStockCards = requisitionTemplate.populateStockOnHandFromStockCards;
    this.name = requisitionTemplate.name;

    addColumns(requisitionTemplate.columnsMap);
    addAssignments(requisitionTemplate.templateAssignments);
  }

  public boolean hasColumnsDefined() {
    return columnsMap != null && !columnsMap.isEmpty();
  }

  /**
   * Checks if column with given name is defined in the template.
   *
   * @param columnName name of requisition column.
   * @return return true if column is defined in the template.
   */
  public boolean isColumnInTemplate(String columnName) {
    return getRequisitionTemplateColumn(columnName) != null;
  }


  /**
   * Checks if column with given name is defined in the template and displayed.
   *
   * @param columnName name of requisition column.
   * @return return true if column is defined in the template and displayed.
   */
  public boolean isColumnInTemplateAndDisplayed(String columnName) {
    return isColumnInTemplate(columnName) && isColumnDisplayed(columnName);
  }

  /**
   * Finds a column by column name or throws exception.
   *
   * @param name name of requisition column.
   * @return {c@link RequisitionTemplateColumn} if found column with the given name.
   */
  public RequisitionTemplateColumn findColumn(String name) {
    RequisitionTemplateColumn column = getRequisitionTemplateColumn(name);
    if (column == null) {
      throw new ValidationMessageException(new Message(ERROR_COLUMN_NOT_IN_TEMPLATE, name));
    }
    return column;
  }

  /**
   * Archives the given template.
   */
  public void archive() {
    // we don't need assignments for archived templates. If there is any requisition in
    // the system that uses the given template we retrieve it by ID field not by
    // assignment
    this.templateAssignments.clear();
    archived = true;
  }

  /**
   * Hides all order-related columns in the template.
   */
  public void hideOrderRelatedColumns() {
    for (Map.Entry<String, RequisitionTemplateColumn> entry : columnsMap.entrySet()) {
      if (ORDER_RELATED_COLUMNS.contains(entry.getKey())) {
        entry.getValue().setIsDisplayed(false);
      }
    }
  }

  /**
   * Create a new instance of requisiton template based on data
   * from {@link Importer}.
   *
   * @param importer instance of {@link Importer}
   * @return new instance od template.
   */
  public static RequisitionTemplate newInstance(Importer importer,
      List<String> tagRequiredColumns) {

    Map<String, RequisitionTemplateColumn> columns = new HashMap<>();
    importer
        .getColumns()
        .forEach((key, column) -> columns.put(key, RequisitionTemplateColumn.newInstance(column)));

    if (importer.isPopulateStockOnHandFromStockCards()) {
      validateAllTagsDistinct(columns.values().stream().map(RequisitionTemplateColumn::getTag)
          .collect(Collectors.toList()));
      validateRequiredTagsSet(columns, tagRequiredColumns);
    }

    RequisitionTemplate template = new RequisitionTemplate(
        importer.getId(), importer.getNumberOfPeriodsToAverage(),
        importer.isPopulateStockOnHandFromStockCards(), importer.getName(),
        columns, new HashSet<>()
    );
    template.setCreatedDate(importer.getCreatedDate());
    template.setModifiedDate(importer.getModifiedDate());

    if (importer.getFacilityTypeIds().isEmpty()) {
      template.addAssignment(importer.getProgramId(), null);
    } else {
      for (UUID facilityTypeId : importer.getFacilityTypeIds()) {
        template.addAssignment(importer.getProgramId(), facilityTypeId);
      }
    }

    return template;
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setCreatedDate(getCreatedDate());
    exporter.setModifiedDate(getModifiedDate());
    exporter.setPopulateStockOnHandFromStockCards(populateStockOnHandFromStockCards);
    exporter.setNumberOfPeriodsToAverage(numberOfPeriodsToAverage);
    exporter.setName(name);
    exporter.setProgramId(programId);
    exporter.setFacilityTypeIds(facilityTypeIds);
  }

  @PostLoad
  private void postLoad() {
    programId = null;
    facilityTypeIds = new HashSet<>();

    for (RequisitionTemplateAssignment assignment : templateAssignments) {
      setProgramId(assignment.getProgramId());
      addFacilityTypeId(assignment.getFacilityTypeId());
    }
  }

  private synchronized void setProgramId(UUID programId) {
    if (null == this.programId) {
      this.programId = programId;
    }

    if (!Objects.equals(this.programId, programId)) {
      throw new ValidationMessageException(ERROR_CANNOT_ASSIGN_TEMPLATE_TO_SEVERAL_PROGRAMS);
    }
  }

  private synchronized void addFacilityTypeId(UUID facilityTypeId) {
    Optional.ofNullable(facilityTypeId).ifPresent(id -> facilityTypeIds.add(id));
  }

  private void addAssignments(Set<RequisitionTemplateAssignment> templateAssignments) {
    Set<RequisitionTemplateAssignment> safe = Optional
        .ofNullable(templateAssignments)
        .orElse(Collections.emptySet())
        .stream()
        .map(elem ->
            new RequisitionTemplateAssignment(elem.getProgramId(), elem.getFacilityTypeId(), this))
        .collect(Collectors.toSet());

    this.templateAssignments.addAll(safe);
    this.templateAssignments.retainAll(safe);
    postLoad();
  }

  private void addColumns(Map<String, RequisitionTemplateColumn> columnsMap) {
    columnsMap.forEach(this.columnsMap::put);
  }

  private void moveDownAllColumnsBelowIndex(int beginIndex) {
    for (RequisitionTemplateColumn column : columnsMap.values()) {
      if (column.getDisplayOrder() >= beginIndex) {
        column.setDisplayOrder(column.getDisplayOrder() + 1);
      }
    }
  }

  private void moveUpAllColumnsBetweenIndexes(int beginIndex, int endIndex) {
    for (RequisitionTemplateColumn column : columnsMap.values()) {
      if (column.getDisplayOrder() <= beginIndex && column.getDisplayOrder() > endIndex) {
        column.setDisplayOrder(column.getDisplayOrder() - 1);
      }
    }
  }

  private void moveDownAllColumnsBetweenIndexes(int beginIndex, int endIndex) {
    for (RequisitionTemplateColumn column : columnsMap.values()) {
      if (column.getDisplayOrder() >= beginIndex && column.getDisplayOrder() < endIndex) {
        column.setDisplayOrder(column.getDisplayOrder() + 1);
      }
    }
  }

  private RequisitionTemplateColumn getRequisitionTemplateColumn(String name) {
    if (columnsMap == null) {
      throw new ValidationMessageException(new Message(ERROR_COLUMNS_MAP_IS_NULL));
    }
    return columnsMap.get(name);
  }

  private static void validateAllTagsDistinct(List<String> tags) {
    Set<String> distinctTags = new HashSet<>();
    for (String tag : tags) {
      if (StringUtils.isNotBlank(tag) && !distinctTags.add(tag)) {
        throw new ValidationMessageException(ERROR_COLUMNS_MAP_TAGS_DUPLICATED);
      }
    }
  }

  private static void validateRequiredTagsSet(Map<String, RequisitionTemplateColumn> columns,
      List<String> tagRequiredColumns) {

    for (String columnName : tagRequiredColumns) {
      RequisitionTemplateColumn column = columns.get(columnName);
      if (column.getIsDisplayed() && StringUtils.isBlank(column.getTag())) {
        throw new ValidationMessageException(ERROR_COLUMNS_TAG_NOT_SET, column.getLabel());
      }
    }
  }

  public interface Importer {
    UUID getId();

    ZonedDateTime getCreatedDate();

    ZonedDateTime getModifiedDate();

    boolean isPopulateStockOnHandFromStockCards();

    String getName();

    Integer getNumberOfPeriodsToAverage();

    Map<String, ? extends RequisitionTemplateColumn.Importer> getColumns();

    UUID getProgramId();

    Set<UUID> getFacilityTypeIds();
  }

  public interface Exporter {
    void setId(UUID id);

    void setCreatedDate(ZonedDateTime createdDate);

    void setModifiedDate(ZonedDateTime modifiedDate);

    void setPopulateStockOnHandFromStockCards(boolean populateStockOnHandFromStockCards);

    void setNumberOfPeriodsToAverage(Integer numberOfPeriodsToAverage);

    void setName(String name);

    void setProgramId(UUID programId);

    void setFacilityTypeIds(Set<UUID> facilityTypeIds);
  }
}

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

package org.siglus.siglusapi;

import static net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock.InterceptMode.PROXY_SCHEDULER;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import lombok.SneakyThrows;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.changelog.ChangeProcessor;
import org.javers.core.commit.Commit;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.json.JsonConverter;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.property.Property;
import org.javers.core.metamodel.type.JaversType;
import org.javers.repository.jql.GlobalIdDTO;
import org.javers.repository.jql.JqlQuery;
import org.javers.shadow.Shadow;
import org.openlmis.referencedata.validate.ProcessingPeriodValidator;
import org.openlmis.referencedata.validate.RequisitionGroupValidator;
import org.openlmis.referencedata.web.csv.processor.FormatCommodityType;
import org.openlmis.referencedata.web.csv.processor.FormatProcessingPeriod;
import org.openlmis.referencedata.web.csv.processor.ParseCommodityType;
import org.openlmis.referencedata.web.csv.processor.ParseProcessingPeriod;
import org.siglus.siglusapi.config.CustomBeanNameGenerator;
import org.siglus.siglusapi.i18n.ExposedMessageSourceImpl;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.validator.SiglusMessageInterpolator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.repository.config.RepositoryBeanNameGenerator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
@ComponentScan(basePackages = {"org.siglus", "org.openlmis"}, nameGenerator = CustomBeanNameGenerator.class)
@EntityScan(basePackages = {"org.siglus", "org.openlmis"})
@EnableJpaRepositories(basePackages = {"org.siglus", "org.openlmis"})
@PropertySource("classpath:application.properties")
@EnableAspectJAutoProxy
@EnableScheduling
@EnableRetry
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
// Don't change the interceptMode, since it's not compatible with current version of spring-aop
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M", interceptMode = PROXY_SCHEDULER)
public class Application {

  private static final String[] MESSAGE_SOURCE_BASE_NAME = new String[]{
      "classpath:/messages/messages",
      "classpath:/messages/messages_pt",
      "classpath:/messages/stockmanagement_messages_en",
      "classpath:/messages/requisition_messages_en",
      "classpath:/messages/fulfillment_messages_en",
      "classpath:/messages/referencedata_messages_en"
  };

  private static final String UTF_8 = "UTF-8";

  @Value("${defaultLocale}")
  private Locale locale;

  @Value("${time.zoneId}")
  private String timeZoneId;

  @Value("${redis.url}")
  private String redisUrl;

  @Value("${redis.port}")
  private int redisPort;

  @Value("${redis.password}")
  private String redisPassword;

  @Value("${referencedata.csv.separator}")
  private String separator;

  public static void main(String[] args) {
    CustomRepositoryBeanNameGenerator.install();
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public LocaleResolver localeResolver() {
    CookieLocaleResolver lr = new CookieLocaleResolver();
    lr.setCookieName("lang");
    lr.setDefaultLocale(locale);
    return lr;
  }

  @Bean
  public ExposedMessageSourceImpl messageSource() {
    ExposedMessageSourceImpl messageSource = new ExposedMessageSourceImpl();
    messageSource.setBasenames(MESSAGE_SOURCE_BASE_NAME);
    messageSource.setDefaultEncoding(UTF_8);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }

  @Bean
  @Primary
  public org.openlmis.stockmanagement.i18n.ExposedMessageSourceImpl stockmanagementMessageSource() {
    org.openlmis.stockmanagement.i18n.ExposedMessageSourceImpl messageSource
        = new org.openlmis.stockmanagement.i18n.ExposedMessageSourceImpl();
    messageSource.setBasenames(MESSAGE_SOURCE_BASE_NAME);
    messageSource.setDefaultEncoding(UTF_8);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }

  @Bean
  @Primary
  public org.openlmis.requisition.i18n.ExposedMessageSourceImpl requisitionMessageSource() {
    org.openlmis.requisition.i18n.ExposedMessageSourceImpl messageSource =
        new org.openlmis.requisition.i18n.ExposedMessageSourceImpl();
    messageSource.setBasenames(MESSAGE_SOURCE_BASE_NAME);
    messageSource.setDefaultEncoding(UTF_8);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }

  @Bean
  @Primary
  public org.openlmis.fulfillment.i18n.ExposedMessageSourceImpl fulfillmentMessageSource() {
    org.openlmis.fulfillment.i18n.ExposedMessageSourceImpl messageSource =
        new org.openlmis.fulfillment.i18n.ExposedMessageSourceImpl();
    messageSource.setBasenames(MESSAGE_SOURCE_BASE_NAME);
    messageSource.setDefaultEncoding(UTF_8);
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }

  @Bean
  public Clock clock() {
    return Clock.system(ZoneId.of(timeZoneId));
  }

  @Bean
  public Javers javersProvider() {
    // return Dummy Javers, not write any data to DB
    return new Javers() {
      @Override
      public Commit commit(String author, Object currentVersion) {
        return null;
      }

      @Override
      public Commit commit(String author, Object currentVersion, Map<String, String> commitProperties) {
        return null;
      }

      @Override
      public Commit commitShallowDelete(String author, Object deleted) {
        return null;
      }

      @Override
      public Commit commitShallowDelete(String author, Object deleted, Map<String, String> commitProperties) {
        return null;
      }

      @Override
      public Commit commitShallowDeleteById(String author, GlobalIdDTO globalId) {
        return null;
      }

      @Override
      public Commit commitShallowDeleteById(String author, GlobalIdDTO globalId, Map<String, String> commitProperties) {
        return null;
      }

      @Override
      public Diff compare(Object oldVersion, Object currentVersion) {
        return null;
      }

      @Override
      public <T> Diff compareCollections(Collection<T> oldVersion, Collection<T> currentVersion, Class<T> itemClass) {
        return null;
      }

      @Override
      public Diff initial(Object newDomainObject) {
        return null;
      }

      @Override
      public <T> List<Shadow<T>> findShadows(JqlQuery query) {
        return null;
      }

      @Override
      public <T> Stream<Shadow<T>> findShadowsAndStream(JqlQuery query) {
        return null;
      }

      @Override
      public Changes findChanges(JqlQuery query) {
        return null;
      }

      @Override
      public List<CdoSnapshot> findSnapshots(JqlQuery query) {
        return null;
      }

      @Override
      public Optional<CdoSnapshot> getLatestSnapshot(Object localId, Class entity) {
        return Optional.empty();
      }

      @Override
      public Optional<CdoSnapshot> getHistoricalSnapshot(Object localId, Class entity, LocalDateTime effectiveDate) {
        return Optional.empty();
      }

      @Override
      public JsonConverter getJsonConverter() {
        return null;
      }

      @Override
      public <T> T processChangeList(List<Change> changes, ChangeProcessor<T> changeProcessor) {
        return null;
      }

      @Override
      public <T extends JaversType> T getTypeMapping(Type userType) {
        return null;
      }

      @Override
      public Property getProperty(PropertyChange propertyChange) {
        return null;
      }
    };
  }

  @Bean
  JedisConnectionFactory connectionFactory() {
    JedisConnectionFactory factory = new JedisConnectionFactory();
    factory.setHostName(redisUrl);
    factory.setPort(redisPort);
    factory.setPassword(redisPassword);
    factory.setUsePool(true);
    return factory;
  }

  @Bean
  ExecutorService executorService() {
    return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
  }

  @Bean
  RedisTemplate<Object, Object> redisTemplate(JedisConnectionFactory factory) {
    RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(factory);
    return redisTemplate;
  }

  @Bean
  public CamelContext camelContext() {
    return new DefaultCamelContext();
  }

  @Bean
  public ProducerTemplate camelTemplate() {
    return camelContext().createProducerTemplate();
  }

  @Bean
  public AuditorAware<UUID> auditorAware(SiglusAuthenticationHelper authenticationHelper) {
    return () -> authenticationHelper.getCurrentUserId().orElse(null);
  }

  @Bean(name = "validator")
  public LocalValidatorFactoryBean getValidator() {
    LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean() {
      @Override
      public ExecutableValidator forExecutables() {
        return unwrap(Validator.class).forExecutables();
      }
    };
    bean.setValidationMessageSource(validationMessageSource());
    ResourceBundleMessageInterpolator nativeMessageInterpolator =
        new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(validationMessageSource()));
    bean.setMessageInterpolator(new SiglusMessageInterpolator(nativeMessageInterpolator));
    return bean;
  }

  @Bean
  public RestTemplate remoteRestTemplate() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(60000);
    requestFactory.setReadTimeout(60000);
    return new RestTemplate(requestFactory);
  }

  // diff from messageSource() is in this method, the useCodeAsDefaultMessage is disabled
  // so the expression will be explained in the EL container instead of direct return code
  // https://stackoverflow.com/questions/38714521/hibernate-expression-language-does-not-work
  private MessageSource validationMessageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasenames(MESSAGE_SOURCE_BASE_NAME);
    messageSource.setDefaultEncoding(UTF_8);
    return messageSource;
  }

  @Bean
  public ProcessingPeriodValidator beforeCreatePeriodValidator() {
    return new ProcessingPeriodValidator();
  }

  @Bean
  public ProcessingPeriodValidator beforeSavePeriodValidator() {
    return new ProcessingPeriodValidator();
  }

  @Bean
  @Qualifier("requisitionGroupValidator")
  public RequisitionGroupValidator requisitionGroupValidator(RequisitionGroupValidator requisitionGroupValidator) {
    return requisitionGroupValidator;
  }

  @PostConstruct
  public void setCsvSeparator() {
    ParseCommodityType.SEPARATOR = separator;
    FormatCommodityType.SEPARATOR = separator;
    ParseProcessingPeriod.SEPARATOR = separator;
    FormatProcessingPeriod.SEPARATOR = separator;
  }

  static class CustomRepositoryBeanNameGenerator extends AnnotationBeanNameGenerator {

    @SneakyThrows
    static void install() {
      Field field = RepositoryBeanNameGenerator.class.getDeclaredField("DELEGATE");
      field.setAccessible(true);
      FieldUtils.removeFinalModifier(field);
      field.set(null, new CustomRepositoryBeanNameGenerator());
    }

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
      return definition.getBeanClassName();
    }
  }

}

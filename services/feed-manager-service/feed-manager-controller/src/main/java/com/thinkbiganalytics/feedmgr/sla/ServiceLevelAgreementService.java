package com.thinkbiganalytics.feedmgr.sla;

/*-
 * #%L
 * thinkbig-feed-manager-controller
 * %%
 * Copyright (C) 2017 ThinkBig Analytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.thinkbiganalytics.app.ServicesApplicationStartupListener;
import com.thinkbiganalytics.feedmgr.rest.model.FeedMetadata;
import com.thinkbiganalytics.feedmgr.service.feed.FeedManagerFeedService;
import com.thinkbiganalytics.metadata.api.MetadataAccess;
import com.thinkbiganalytics.metadata.api.feed.Feed;
import com.thinkbiganalytics.metadata.api.feed.FeedNotFoundExcepton;
import com.thinkbiganalytics.metadata.api.feed.FeedProvider;
import com.thinkbiganalytics.metadata.api.feed.security.FeedAccessControl;
import com.thinkbiganalytics.metadata.api.sla.FeedServiceLevelAgreement;
import com.thinkbiganalytics.metadata.api.sla.FeedServiceLevelAgreementProvider;
import com.thinkbiganalytics.metadata.modeshape.JcrMetadataAccess;
import com.thinkbiganalytics.metadata.rest.model.sla.Obligation;
import com.thinkbiganalytics.metadata.rest.model.sla.ServiceLevelAgreement;
import com.thinkbiganalytics.metadata.sla.api.ObligationGroup;
import com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreementActionConfiguration;
import com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreementActionValidation;
import com.thinkbiganalytics.metadata.sla.spi.ObligationGroupBuilder;
import com.thinkbiganalytics.metadata.sla.spi.ServiceLevelAgreementBuilder;
import com.thinkbiganalytics.metadata.sla.spi.ServiceLevelAgreementProvider;
import com.thinkbiganalytics.metadata.sla.spi.ServiceLevelAgreementScheduler;
import com.thinkbiganalytics.policy.PolicyPropertyTypes;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Service for interacting with SLA's
 */
public class ServiceLevelAgreementService implements ServicesApplicationStartupListener {

    @Inject
    ServiceLevelAgreementProvider slaProvider;
    @Inject
    FeedServiceLevelAgreementProvider feedSlaProvider;
    @Inject
    JcrMetadataAccess metadataAccess;
    @Inject
    ServiceLevelAgreementScheduler serviceLevelAgreementScheduler;
    @Inject
    private FeedManagerFeedService feedManagerFeedService;
    @Inject
    private FeedProvider feedProvider;
    @Inject
    private ServiceLevelAgreementModelTransform serviceLevelAgreementTransform;


    private List<ServiceLevelAgreementRule> serviceLevelAgreementRules;

    @Override
    public void onStartup(DateTime startTime) {
        discoverServiceLevelAgreementRules();
    }

    private List<ServiceLevelAgreementRule> discoverServiceLevelAgreementRules() {
        List<ServiceLevelAgreementRule> rules = ServiceLevelAgreementMetricTransformer.instance().discoverSlaMetrics();
        serviceLevelAgreementRules = rules;
        return serviceLevelAgreementRules;
    }

    public List<ServiceLevelAgreementRule> discoverSlaMetrics() {
        List<ServiceLevelAgreementRule> rules = serviceLevelAgreementRules;
        if (rules == null) {
            rules = discoverServiceLevelAgreementRules();
        }

        feedManagerFeedService
            .applyFeedSelectOptions(
                ServiceLevelAgreementMetricTransformer.instance().findPropertiesForRulesetMatchingRenderTypes(rules, new String[]{PolicyPropertyTypes.PROPERTY_TYPE.feedChips.name(),
                                                                                                                                  PolicyPropertyTypes.PROPERTY_TYPE.feedSelect.name(),
                                                                                                                                  PolicyPropertyTypes.PROPERTY_TYPE.currentFeed.name()}));

        return rules;
    }

    public List<com.thinkbiganalytics.metadata.rest.model.sla.FeedServiceLevelAgreement> getServiceLevelAgreements() {
        return metadataAccess.read(() -> {

            List<FeedServiceLevelAgreement> agreements = feedSlaProvider.findAllAgreements();
            if (agreements != null) {
                return serviceLevelAgreementTransform.transformFeedServiceLevelAgreements(agreements);
            }
            return null;

        });
    }

    public void enableServiceLevelAgreementSchedule(Feed.ID feedId) {
        metadataAccess.commit(() -> {
            List<FeedServiceLevelAgreement> agreements = feedSlaProvider.findFeedServiceLevelAgreements(feedId);
            if (agreements != null) {
                for (com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement sla : agreements) {
                    serviceLevelAgreementScheduler.enableServiceLevelAgreement(sla);
                }
            }
        }, MetadataAccess.SERVICE);
    }

    public void unscheduleServiceLevelAgreement(Feed.ID feedId) {
        metadataAccess.read(() -> {
            List<FeedServiceLevelAgreement> agreements = feedSlaProvider.findFeedServiceLevelAgreements(feedId);
            if (agreements != null) {
                for (com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement sla : agreements) {
                    serviceLevelAgreementScheduler.unscheduleServiceLevelAgreement(sla.getId());
                }
            }
        });
    }


    public void disableServiceLevelAgreementSchedule(Feed.ID feedId) {
        metadataAccess.commit(() -> {
            List<FeedServiceLevelAgreement> agreements = feedSlaProvider.findFeedServiceLevelAgreements(feedId);
            if (agreements != null) {
                for (com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement sla : agreements) {
                    serviceLevelAgreementScheduler.disableServiceLevelAgreement(sla);
                }
            }
        }, MetadataAccess.SERVICE);
    }


    public List<com.thinkbiganalytics.metadata.rest.model.sla.FeedServiceLevelAgreement> getFeedServiceLevelAgreements(String feedId) {
        return metadataAccess.read(() -> {

            Feed.ID id = feedProvider.resolveFeed(feedId);
            List<FeedServiceLevelAgreement> agreements = feedSlaProvider.findFeedServiceLevelAgreements(id);
            if (agreements != null) {
                return serviceLevelAgreementTransform.transformFeedServiceLevelAgreements(agreements);
            }
            return null;

        });
    }

    /**
     * get a SLA and convert it to the editable SLA form object
     */
    public ServiceLevelAgreementGroup getServiceLevelAgreementAsFormObject(String slaId) {
        return metadataAccess.read(() -> {

            FeedServiceLevelAgreement agreement = feedSlaProvider.findAgreement(slaProvider.resolve(slaId));
            if (agreement != null) {
                com.thinkbiganalytics.metadata.rest.model.sla.FeedServiceLevelAgreement modelSla = serviceLevelAgreementTransform.toModel(agreement, true);
                ServiceLevelAgreementMetricTransformerHelper transformer = new ServiceLevelAgreementMetricTransformerHelper();
                ServiceLevelAgreementGroup serviceLevelAgreementGroup = transformer.toServiceLevelAgreementGroup(modelSla);
                feedManagerFeedService
                    .applyFeedSelectOptions(
                        ServiceLevelAgreementMetricTransformer.instance()
                            .findPropertiesForRulesetMatchingRenderTypes(serviceLevelAgreementGroup.getRules(), new String[]{PolicyPropertyTypes.PROPERTY_TYPE.feedChips.name(),
                                                                                                                             PolicyPropertyTypes.PROPERTY_TYPE.feedSelect.name(),
                                                                                                                             PolicyPropertyTypes.PROPERTY_TYPE.currentFeed.name()}));
                serviceLevelAgreementGroup.setCanEdit(modelSla.isCanEdit());
                return serviceLevelAgreementGroup;
            }
            return null;

        });
    }

    public boolean removeAndUnscheduleAgreement(String id) {
        return metadataAccess.commit(() -> {
            com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement.ID slaId = slaProvider.resolve(id);
            slaProvider.removeAgreement(slaId);
            serviceLevelAgreementScheduler.unscheduleServiceLevelAgreement(slaId);
            return true;
        });

    }

    public boolean removeAllAgreements() {
        return metadataAccess.commit(() -> {
            List<com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement> agreements = slaProvider.getAgreements();
            if (agreements != null) {
                for (com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement agreement : agreements) {
                    slaProvider.removeAgreement(agreement.getId());
                }
            }
            return true;
        });

    }

    public List<ServiceLevelAgreementActionUiConfigurationItem> discoverActionConfigurations() {

        return ServiceLevelAgreementActionConfigTransformer.instance().discoverActionConfigurations();
    }


    public List<ServiceLevelAgreementActionValidation> validateAction(String actionConfigurationClassName) {
        return ServiceLevelAgreementActionConfigTransformer.instance().validateAction(actionConfigurationClassName);
    }


    public ServiceLevelAgreement saveAndScheduleSla(ServiceLevelAgreementGroup serviceLevelAgreement) {
        return saveAndScheduleSla(serviceLevelAgreement, null);
    }


    /**
     * In order to Save an SLA if it is related to a Feed(s) the user needs to have EDIT_DETAILS permission on the Feed(s)
     *
     * @param serviceLevelAgreement the sla to save
     * @param feed                  an option Feed to relate to this SLA.  If this is not present the related feeds are also embedded in the SLA policies.  The Feed is a pointer access to the current
     *                              feed the user is editing if they are creating an SLA from the Feed Details page. If creating an SLA from the main SLA page the feed property will not be populated.
     */
    private ServiceLevelAgreement saveAndScheduleSla(ServiceLevelAgreementGroup serviceLevelAgreement, FeedMetadata feed) {
        return metadataAccess.commit(() -> {

            if (serviceLevelAgreement != null) {
                ServiceLevelAgreementMetricTransformerHelper transformer = new ServiceLevelAgreementMetricTransformerHelper();

                //all referencing Feeds
                List<String> systemCategoryAndFeedNames = transformer.getCategoryFeedNames(serviceLevelAgreement);
                Set<Feed> slaFeeds = new HashSet<Feed>();
                Set<Feed.ID> slaFeedIds = new HashSet<Feed.ID>();
                for (String categoryAndFeed : systemCategoryAndFeedNames) {
                    //fetch and update the reference to the sla
                    String categoryName = StringUtils.trim(StringUtils.substringBefore(categoryAndFeed, "."));
                    String feedName = StringUtils.trim(StringUtils.substringAfterLast(categoryAndFeed, "."));
                    Feed feedEntity = feedProvider.findBySystemName(categoryName, feedName);
                    if (feedEntity != null) {
                        feedManagerFeedService.checkFeedPermission(feedEntity.getId().toString(), FeedAccessControl.EDIT_DETAILS);
                        slaFeeds.add(feedEntity);
                        slaFeedIds.add(feedEntity.getId());
                    }
                }

                if (feed != null) {
                    feedManagerFeedService.checkFeedPermission(feed.getId(), FeedAccessControl.EDIT_DETAILS);
                }

                if (feed != null) {
                    transformer.applyFeedNameToCurrentFeedProperties(serviceLevelAgreement, feed.getCategory().getSystemName(), feed.getSystemFeedName());
                }
                ServiceLevelAgreement sla = transformer.getServiceLevelAgreement(serviceLevelAgreement);

                ServiceLevelAgreementBuilder slaBuilder = null;
                com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement.ID existingId = null;
                if (StringUtils.isNotBlank(sla.getId())) {
                    existingId = slaProvider.resolve(sla.getId());
                }
                if (existingId != null) {
                    slaBuilder = slaProvider.builder(existingId);
                } else {
                    slaBuilder = slaProvider.builder();
                }

                slaBuilder.name(sla.getName()).description(sla.getDescription());
                for (com.thinkbiganalytics.metadata.rest.model.sla.ObligationGroup group : sla.getGroups()) {
                    ObligationGroupBuilder groupBuilder = slaBuilder.obligationGroupBuilder(ObligationGroup.Condition.valueOf(group.getCondition()));
                    for (Obligation o : group.getObligations()) {
                        groupBuilder.obligationBuilder().metric(o.getMetrics()).description(o.getDescription()).build();
                    }
                    groupBuilder.build();
                }
                com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement savedSla = slaBuilder.build();

                List<ServiceLevelAgreementActionConfiguration> actions = transformer.getActionConfigurations(serviceLevelAgreement);

                // now assign the sla checks
                slaProvider.slaCheckBuilder(savedSla.getId()).removeSlaChecks().actionConfigurations(actions).build();

                if (feed != null) {
                    Feed.ID feedId = feedProvider.resolveFeed(feed.getFeedId());
                    if (!slaFeedIds.contains(feedId)) {
                        Feed feedEntity = feedProvider.getFeed(feedId);
                        slaFeeds.add(feedEntity);
                    }
                }
                //relate them
                feedSlaProvider.relateFeeds(savedSla, slaFeeds);
                com.thinkbiganalytics.metadata.rest.model.sla.FeedServiceLevelAgreement restModel = serviceLevelAgreementTransform.toModel(savedSla, slaFeeds, true);
                //schedule it
                serviceLevelAgreementScheduler.scheduleServiceLevelAgreement(savedSla);
                return restModel;

            }
            return null;


        });


    }

    public ServiceLevelAgreement saveAndScheduleFeedSla(ServiceLevelAgreementGroup serviceLevelAgreement, String feedId) {

        return metadataAccess.commit(() -> {
            FeedMetadata feed = null;
            if (StringUtils.isNotBlank(feedId)) {
                feed = feedManagerFeedService.getFeedById(feedId);
            }
            if (feed != null) {

                ServiceLevelAgreement sla = saveAndScheduleSla(serviceLevelAgreement, feed);

                return sla;
            } else {
                //TODO LOG ERROR CANNOT GET FEED
                throw new FeedNotFoundExcepton("Unable to create SLA for Feed " + feedId, feedProvider.resolveFeed(feedId));
            }
        });

    }


}

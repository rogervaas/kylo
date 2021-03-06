/**
 *
 */
package com.thinkbiganalytics.metadata.modeshape.security.role;

/*-
 * #%L
 * kylo-metadata-modeshape
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

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.thinkbiganalytics.metadata.api.MetadataAccess;
import com.thinkbiganalytics.metadata.api.PostMetadataConfigAction;
import com.thinkbiganalytics.metadata.api.category.security.CategoryAccessControl;
import com.thinkbiganalytics.metadata.api.feed.security.FeedAccessControl;
import com.thinkbiganalytics.security.action.AllowedActions;
import com.thinkbiganalytics.security.action.config.ActionsModuleBuilder;
import com.thinkbiganalytics.security.role.SecurityRoleProvider;

/**
 *
 */
@Configuration
public class JcrSecurityRoleProviderTestConfig {

    @Inject
    private ActionsModuleBuilder builder;
    
    @Inject
    private MetadataAccess metadataAccess;

    @Bean
    public PostMetadataConfigAction configAuthorization() {
        return () -> metadataAccess.commit(() -> {
            //@formatter:off

            return builder
                            .module(AllowedActions.FEED)
                                .action(FeedAccessControl.ACCESS_FEED)
                                .action(FeedAccessControl.EDIT_SUMMARY)
                                .action(FeedAccessControl.ACCESS_DETAILS)
                                .action(FeedAccessControl.EDIT_DETAILS)
                                .action(FeedAccessControl.DELETE)
                                .action(FeedAccessControl.ENABLE_DISABLE)
                                .action(FeedAccessControl.EXPORT)
    //                            .action(FeedAccessControl.SCHEDULE_FEED)
                                .action(FeedAccessControl.ACCESS_OPS)
                                .action(FeedAccessControl.CHANGE_PERMS)
                                .add()
                            .module(AllowedActions.CATEGORY)
                                .action(CategoryAccessControl.ACCESS_CATEGORY)
                                .action(CategoryAccessControl.EDIT_SUMMARY)
                                .action(CategoryAccessControl.ACCESS_DETAILS)
                                .action(CategoryAccessControl.EDIT_DETAILS)
                                .action(CategoryAccessControl.DELETE)
                                .action(CategoryAccessControl.EXPORT)
                                .action(CategoryAccessControl.CREATE_FEED)
                                .action(CategoryAccessControl.CHANGE_PERMS)
                                .add()
                            .build();

            //@formatter:on
        }, MetadataAccess.SERVICE);
    }
    

    @Bean
    @Primary
    public SecurityRoleProvider roleProvider() {
        return new JcrSecurityRoleProvider();
    }

}

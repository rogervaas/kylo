package com.thinkbiganalytics.feedmgr.rest.controller;

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

import com.thinkbiganalytics.es.ElasticSearch;
import com.thinkbiganalytics.es.SearchResult;
import com.thinkbiganalytics.rest.model.RestResponseStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@Api(tags = "Feed Manager - Search", produces = "application/json")
@Path("/v1/feedmgr/search")
@Component
@SwaggerDefinition(tags = @Tag(name = "Feed Manager - Search", description = "global search"))
public class ElasticSearchRestController {

    @Autowired
    ElasticSearch elasticSearch;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation("Queries a search engine.")
    @ApiResponses({
                      @ApiResponse(code = 200, message = "The search results.", response = SearchResult.class),
                      @ApiResponse(code = 500, message = "The search engine is unavailable.", response = RestResponseStatus.class)
                  })
    public Response search(@QueryParam("q") String query, @QueryParam("rows") @DefaultValue("20") Integer rows, @QueryParam("start") @DefaultValue("0") Integer start) {
        SearchResult result = elasticSearch.search(query, rows, start);
        return Response.ok(result).build();
    }
}

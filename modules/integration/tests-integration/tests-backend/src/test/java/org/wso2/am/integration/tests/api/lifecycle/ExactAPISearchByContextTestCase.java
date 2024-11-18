/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.api.lifecycle;

import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.other.APISearchAPIByTagTestCase;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

public class ExactAPISearchByContextTestCase extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(APISearchAPIByTagTestCase.class);
    private String apiId1, apiId2, apiId3, apiId4;
    private String applicationId;
    private final String API_VERSION = "1.0.0";
    private String providerName;

    @Factory(dataProvider = "userModeDataProvider")
    public ExactAPISearchByContextTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[] { TestUserMode.TENANT_ADMIN },
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init(userMode);

        userManagementClient = new UserManagementClient(
                keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        providerName = user.getUserName();

        // Create the specified APIs
        apiId1 = createAndPublishAPI("API1", "/Exact", API_VERSION);
        apiId2 = createAndPublishAPI("API2", "/PrefixExactSuffix", API_VERSION);
        apiId3 = createAndPublishAPI("API3", "PreContext/Exact/PostContext", API_VERSION);
        apiId4 = createAndPublishAPI("Exact", "/DifferentContext", API_VERSION);

    }

    private String createAndPublishAPI(String name, String context, String version) throws Exception {
        APIRequest apiRequest = new APIRequest(name, context, new URL(backEndServerUrl.getWebAppURLHttp() + "/jaxrs_basic/services/customers/customerservice/"));
        apiRequest.setVersion(version);
        apiRequest.setDescription("Test API for exact search");
        apiRequest.setProvider(providerName);

        return  createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
    }

    @Test(groups = {"wso2.am"}, description = "Test search with context:Exact")
    public void testSearchWithContextExact() throws Exception {
        String query = "context:Exact";
        validateGetAPIsResults(query, 3, "API1", "API2", "API3");
        validateSearchAPIsResults(query, 3, "API1", "API2", "API3");
    }

    @Test(groups = {"wso2.am"}, description = "Test exact search with context:\"Exact\"",
            dependsOnMethods = "testSearchWithContextExact")
    public void testExactSearchWithQuotedContext() throws Exception {
        String query = "context:\"Exact\"";
        validateGetAPIsResults(query, 2, "API1", "API3");
        validateSearchAPIsResults(query, 2, "API1", "API3");
    }

    @Test(groups = {"wso2.am"}, description = "Test exact search with context:\"Exact/1.0.0\"",
            dependsOnMethods = "testExactSearchWithQuotedContext")
    public void testExactSearchWithContextAndVersion() throws Exception {
        String query;
        if (TestUserMode.TENANT_ADMIN.name().equals(userMode.name())) {
            query = "context:\"t/wso2.com/Exact/1.0.0\"";
        } else if (TestUserMode.SUPER_TENANT_ADMIN.name().equals(userMode.name())) {
            query = "context:\"Exact/1.0.0\"";
        } else {
            throw new IllegalStateException("Unexpected user mode: " + userMode.name());
        }
        validateGetAPIsResults(query, 1, "API1");
        validateSearchAPIsResults(query, 1, "API1");
    }


    private void validateSearchAPIsResults(String query, int expectedCount, String... expectedApiNames) throws Exception {
        int retries = 20;
        boolean resultVerifiedForPublisher = false;
        boolean resultVerifiedForStore = false;

        for (int i = 0; i <= retries; i++) {
            org.wso2.am.integration.clients.publisher.api.v1.dto.SearchResultListDTO
                    searchResultInPublisher = restAPIPublisher.searchAPIs(query);
            org.wso2.am.integration.clients.store.api.v1.dto.SearchResultListDTO
                    searchResultInAPIStore = restAPIStore.searchAPIs(query);

            // Validate results from Publisher
            if (!resultVerifiedForPublisher && searchResultInPublisher.getCount() == expectedCount) {
                ArrayList<String> foundApiNamesPublisher = new ArrayList<>();
                searchResultInPublisher.getList().forEach(apiObj -> {
                    String searchResultAPIName = ((LinkedTreeMap) apiObj).get("name").toString();
                    foundApiNamesPublisher.add(searchResultAPIName);
                });

                for (String apiName : expectedApiNames) {
                    assertEquals(foundApiNamesPublisher.contains(apiName), true,
                                 "Expected API " + apiName + " not found in Publisher search results.");
                }
                resultVerifiedForPublisher = true;
            }

            // Validate results from Store
            if (!resultVerifiedForStore && searchResultInAPIStore.getCount() == expectedCount) {
                ArrayList<String> foundApiNamesStore = new ArrayList<>();
                searchResultInAPIStore.getList().forEach(apiObj -> {
                    String searchResultAPIName = ((LinkedTreeMap) apiObj).get("name").toString();
                    foundApiNamesStore.add(searchResultAPIName);
                });

                for (String apiName : expectedApiNames) {
                    assertEquals(foundApiNamesStore.contains(apiName), true,
                                 "Expected API " + apiName + " not found in Store search results.");
                }
                resultVerifiedForStore = true;
            }

            // Break the loop if both validations are complete
            if (resultVerifiedForPublisher && resultVerifiedForStore) {
                break;
            } else {
                log.warn("API search returned unexpected count. Retrying...");
                Thread.sleep(3000);
            }
        }

        if (!resultVerifiedForPublisher) {
            Assert.fail("API search with query: '" + query + "' failed to return expected results in Publisher.");
        }
        if (!resultVerifiedForStore) {
            Assert.fail("API search with query: '" + query + "' failed to return expected results in Store.");
        }
    }


    private void validateGetAPIsResults(String query, int expectedCount, String... expectedApiNames) throws Exception {
        int retries = 20;
        boolean resultVerifiedForPublisher = false;
        boolean resultVerifiedForStore = false;

        for (int i = 0; i <= retries; i++) {
            org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO
                    searchResultInPublisher = restAPIPublisher.getAPIs(query);
            org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO
                    searchResultInAPIStore = restAPIStore.getAPIs(query);

            // Validate results from Publisher
            if (!resultVerifiedForPublisher && searchResultInPublisher.getCount() == expectedCount) {
                ArrayList<String> foundApiNamesPublisher = new ArrayList<>();
                searchResultInPublisher.getList().forEach(apiObj -> {
                    String searchResultAPIName = apiObj.getName();
                    foundApiNamesPublisher.add(searchResultAPIName);
                });

                for (String apiName : expectedApiNames) {
                    assertEquals(foundApiNamesPublisher.contains(apiName), true, "Expected API " + apiName + " not found in Publisher search results.");
                }
                resultVerifiedForPublisher = true;
            }

            // Validate results from Store
            if (!resultVerifiedForStore && searchResultInAPIStore.getCount() == expectedCount) {
                ArrayList<String> foundApiNamesStore = new ArrayList<>();
                searchResultInAPIStore.getList().forEach(apiObj -> {
                    String searchResultAPIName = apiObj.getName();
                    foundApiNamesStore.add(searchResultAPIName);
                });

                for (String apiName : expectedApiNames) {
                    assertEquals(foundApiNamesStore.contains(apiName), true, "Expected API " + apiName + " not found in Store search results.");
                }
                resultVerifiedForStore = true;
            }

            if (resultVerifiedForPublisher && resultVerifiedForStore) {
                break;
            } else {
                log.warn("API search returned unexpected count. Retrying...");
                Thread.sleep(3000);
            }
        }

        if (!resultVerifiedForPublisher) {
            Assert.fail("API search with query: '" + query + "' failed to return expected results in Publisher.");
        }
        if (!resultVerifiedForStore) {
            Assert.fail("API search with query: '" + query + "' failed to return expected results in Store.");
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws Exception {
        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
        restAPIPublisher.deleteAPI(apiId3);
        restAPIPublisher.deleteAPI(apiId4);
        super.cleanUp();
    }
}

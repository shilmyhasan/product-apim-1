/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.tests.token;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TokenRenewWithoutRevokingExisting extends APIMIntegrationBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private String apiId;
    private String appId;
    private String gatewayUrl;
    private String consumerKey;
    private String consumerSecret;

    @Factory(dataProvider = "userModeDataProvider")
    public TokenRenewWithoutRevokingExisting(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "tokenTest"
                        + File.separator + "tokenRenewal" + File.separator + "deployment.toml"));
        serverConfigurationManager.restartGracefully();

        String APIName = "TokenTestAPI1";
        String APIContext = "tokenTestAPI1";
        String tags = "youtube, token, media";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        String description = "This is test API create by API manager integration test to check a config";
        String APIVersion = "2.0.0";

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url), new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(user.getUserName());
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        gatewayUrl = getAPIInvocationURLHttp("tokenTestAPI1/2.0.0/customers/123");
    }

    @Test(groups = {"wso2.am"}, description = "Renew Token without revoking the existing token")
    public void testTokenRenewWithoutRevokingTestCase() throws Exception {

        // Create application
        ApplicationDTO applicationDTO = restAPIStore.addApplication("TokenTestAPI-Application1",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        appId = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, appId,
                APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals("Gold"));

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);


        ApplicationKeyDTO productionApplicationKeyDTO = restAPIStore.generateKeys(appId,
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = productionApplicationKeyDTO.getToken().getAccessToken();
        consumerKey = productionApplicationKeyDTO.getConsumerKey();
        consumerSecret = productionApplicationKeyDTO.getConsumerSecret();
        //Obtain user access token
        Thread.sleep(2000);
        String requestBody = "grant_type=password&username=" + user.getUserName() + "&password=" +
                user.getPassword() + "&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        HttpResponse httpAccessTokenGenerationResponse = restAPIStore.generateUserAccessKey(consumerKey,
                consumerSecret, requestBody, tokenEndpointURL);
        JSONObject accessTokenGenerationResponse = new JSONObject(httpAccessTokenGenerationResponse.getData());
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");
        Map<String, String> requestHeaders = new HashMap<String, String>();
        //Check User Access Token
        requestHeaders.put("Authorization", "Bearer " + userAccessToken);
        requestHeaders.put("accept", "text/xml");


        //Obtain user access token
        Thread.sleep(1000);
        HttpResponse httpAccessTokenGenerationResponse1 = restAPIStore.generateUserAccessKey(consumerKey,
                consumerSecret, requestBody, tokenEndpointURL);
        JSONObject accessTokenGenerationResponse1 = new JSONObject(httpAccessTokenGenerationResponse1.getData());
        String userAccessToken1 = accessTokenGenerationResponse1.getString("access_token");
        assertNotNull(userAccessToken1, "User Access Token not found");

        Thread.sleep(2000);
        HttpResponse youTubeResponse = HttpRequestUtil.doGet(gatewayUrl, requestHeaders);
        assertEquals(youTubeResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Response code mismatched");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(appId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }
}

/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.am.integration.test.impl;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIEndpointURLsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APITiersDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIURLsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationTokenDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import static org.wso2.am.integration.clients.store.api.v1.dto.WorkflowResponseDTO.WorkflowStatusEnum.APPROVED;

public class ApiTestHelper {
    private static String SWAGGER_FOLDER = "swagger";

    private RestAPIPublisherImpl restAPIPublisher;
    private RestAPIStoreImpl restAPIStore;
    private final String resourceLocation;

    public ApiTestHelper(RestAPIPublisherImpl restAPIPublisher, RestAPIStoreImpl restAPIStore, String resourceLocation) {
        this.restAPIPublisher = restAPIPublisher;
        this.restAPIStore = restAPIStore;
        this.resourceLocation = resourceLocation;
    }

    public APIDTO createCustomerInfoApi(String backendUrl) throws ApiException {
        String swaggerPath = resourceLocation + File.separator + SWAGGER_FOLDER +
                File.separator + "customer-info-api.yaml";

        File definition = new File(swaggerPath);

        JSONObject endpoints = new JSONObject();
        endpoints.put("url", backendUrl);

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "CustomerInfo");
        apiProperties.put("context", "/customer-info");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", "admin");
        apiProperties.put("endpointConfig", endpointConfig);

        return restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
    }

    public APIDTO createLeasingApi(String backendUrl) throws ApiException {
        String swaggerPath = resourceLocation + File.separator + SWAGGER_FOLDER +
                File.separator + "leasing-api.yaml";

        File definition = new File(swaggerPath);

        JSONObject endpoints = new JSONObject();
        endpoints.put("url", backendUrl);

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "Leasing");
        apiProperties.put("context", "/leasing");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", "admin");
        apiProperties.put("endpointConfig", endpointConfig);

        return restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
    }

    public ApplicationDTO verifySubscription(org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO,
                                             String applicationName, String subscriptionPolicy)
            throws org.wso2.am.integration.clients.store.api.ApiException {
        ApplicationDTO applicationDTO = restAPIStore.addApplication(applicationName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "http://localhost", "");

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiDTO.getId(),
                applicationDTO.getApplicationId(), subscriptionPolicy);

        Assert.assertEquals(subscriptionDTO.getApiId(), apiDTO.getId());
        Assert.assertEquals(subscriptionDTO.getApplicationId(), applicationDTO.getApplicationId());
        verifySubscriptionApiInfo(subscriptionDTO.getApiInfo(), apiDTO);
        verifySubscriptionAppInfo(subscriptionDTO.getApplicationInfo(), applicationDTO);
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
        Assert.assertEquals(subscriptionDTO.getThrottlingPolicy(), subscriptionPolicy);
        Assert.assertEquals(subscriptionDTO.getType(), SubscriptionDTO.TypeEnum.API_PRODUCT);

        return restAPIStore.getApplicationById(applicationDTO.getApplicationId());
    }

    public String verifyKeyGeneration(ApplicationDTO applicationDTO,
                                      ApplicationKeyGenerateRequestDTO.KeyTypeEnum keyType,
                                      ArrayList<String> scopes, List<String> grantTypes)
            throws org.wso2.am.integration.clients.store.api.ApiException {
        final String validityTime = "3600";
        final String callbackUrl = "http://localhost";

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(),
                validityTime, callbackUrl, keyType, scopes, grantTypes);

        Assert.assertEquals(applicationKeyDTO.getCallbackUrl(), callbackUrl);
        Assert.assertEquals(applicationKeyDTO.getKeyType().getValue(), keyType.getValue());
        Assert.assertEquals(new HashSet<>(applicationKeyDTO.getSupportedGrantTypes()), new HashSet<>(grantTypes));
        Assert.assertEquals(applicationKeyDTO.getKeyState(), APPROVED.getValue());

        ApplicationTokenDTO token = applicationKeyDTO.getToken();

        List<String> expectedScopes = Arrays.asList("am_application_scope", "default");
        expectedScopes.addAll(scopes);
        Assert.assertEquals(new HashSet<>(token.getTokenScopes()), new HashSet<>(expectedScopes));

        Assert.assertEquals(token.getValidityTime().longValue(), Long.parseLong(validityTime));
        return token.getAccessToken();
    }

    public void verifyInvocation(org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO,
                                 String productionAccessToken, String sandboxAccessToken) throws IOException {
        List<APIEndpointURLsDTO> endpointURLs = apiDTO.getEndpointURLs();
        Assert.assertFalse(endpointURLs.isEmpty());

        for (APIEndpointURLsDTO endpointURL : endpointURLs) {
            String environmentType = endpointURL.getEnvironmentType();
            switch (environmentType) {
                case "hybrid":
                    sendRequest(endpointURL.getUrLs(), apiDTO, productionAccessToken);
                    sendRequest(endpointURL.getUrLs(), apiDTO, sandboxAccessToken);
                    break;
                case "production":
                    sendRequest(endpointURL.getUrLs(), apiDTO, productionAccessToken);
                    break;
                case "sandbox":
                    sendRequest(endpointURL.getUrLs(), apiDTO, sandboxAccessToken);
                    break;
                default:
                    Assert.assertTrue(Arrays.stream(new String[]{"hybrid", "production", "sandbox"}).
                            parallel().anyMatch(environmentType::contains));
                    break;
            }
        }
    }
    private HttpUriRequest constructRequest(String targetUrl, String httpMethod) throws IOException {
        // Detect and replace resource path parameters with fixed id
        targetUrl = targetUrl.replaceAll("\\{\\w+}", "123");

        if ("GET".equals(httpMethod)) {
            return new HttpGet(targetUrl);
        } else if ("POST".equals(httpMethod)) {
            HttpPost request = new HttpPost(targetUrl);
            request.setEntity(new StringEntity("<test/>"));
            return request;
        } else if ("PUT".equals(httpMethod)) {
            HttpPut request = new HttpPut(targetUrl);
            request.setEntity(new StringEntity("<test/>"));
            return request;
        } else if ("DELETE".equals(httpMethod)) {
            return new HttpDelete(targetUrl);
        } else if ("HEAD".equals(httpMethod)) {
            return new HttpHead(targetUrl);
        } else {
            HttpPatch request = new HttpPatch(targetUrl);
            request.setEntity(new StringEntity("<test/>"));
            return request;
        }
    }

    private void sendRequest(APIURLsDTO apiurLsDTO, org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO,
                             String accessToken) throws IOException {
        List<org.wso2.am.integration.clients.store.api.v1.dto.APIOperationsDTO> operations = apiDTO.getOperations();

        for (org.wso2.am.integration.clients.store.api.v1.dto.APIOperationsDTO operation : operations) {
            HttpUriRequest request = constructRequest(apiurLsDTO.getHttp() + operation.getTarget(),
                    operation.getVerb());
            // add request headers
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            //request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN);

            try (CloseableHttpClient httpClient = HttpClients.custom().
                    setHostnameVerifier(new AllowAllHostnameVerifier()).build();
                 CloseableHttpResponse response = httpClient.execute(request)) {
                Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
            }
        }
    }


    private void verifySubscriptionApiInfo(APIInfoDTO apiInfo,
                                           org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO) {
        Assert.assertEquals(apiInfo.getId(), apiDTO.getId());
        Assert.assertEquals(apiInfo.getProvider(), apiDTO.getProvider());
        Assert.assertEquals(apiInfo.getName(), apiDTO.getName());
        verifySubscriptionApiInfoPolicies(apiInfo.getThrottlingPolicies(), apiDTO.getTiers());
        Assert.assertEquals(apiInfo.getLifeCycleStatus(), apiDTO.getLifeCycleStatus());
        Assert.assertEquals(apiInfo.getDescription(), apiDTO.getDescription());
        Assert.assertEquals(apiInfo.getContext(), apiDTO.getContext());
    }

    private void verifySubscriptionApiInfoPolicies(List<String> policies, List<APITiersDTO> tiers) {
        Assert.assertEquals(policies.size(), tiers.size());

        tiers.sort(Comparator.comparing(APITiersDTO::getTierName));
        policies.sort(Comparator.naturalOrder());

        for (int i = 0; i < policies.size(); ++i) {
            APITiersDTO apiTiersDTO = tiers.get(i);
            Assert.assertEquals(policies.get(i), apiTiersDTO.getTierName());
        }
    }


    private void verifySubscriptionAppInfo(ApplicationInfoDTO applicationInfo, ApplicationDTO applicationDTO) {
        Assert.assertEquals(applicationInfo.getApplicationId(), applicationDTO.getApplicationId());
        Assert.assertEquals(applicationInfo.getDescription(), applicationDTO.getDescription());
        Assert.assertEquals(applicationInfo.getName(), applicationDTO.getName());
        Assert.assertEquals(new HashSet<>(applicationInfo.getGroups()), new HashSet<>(applicationDTO.getGroups()));
        Assert.assertEquals(applicationInfo.getOwner(), applicationDTO.getOwner());
        Assert.assertEquals(applicationInfo.getStatus(), applicationDTO.getStatus());
        Assert.assertEquals(applicationInfo.getThrottlingPolicy(), applicationDTO.getThrottlingPolicy());
        Assert.assertEquals(applicationInfo.getSubscriptionCount(), new Integer(applicationDTO.getSubscriptionCount() + 1));
    }
}

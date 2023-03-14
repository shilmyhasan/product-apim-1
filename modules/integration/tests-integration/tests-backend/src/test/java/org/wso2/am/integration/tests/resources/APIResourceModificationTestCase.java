/*
 *Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.resources;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


public class APIResourceModificationTestCase extends APIMIntegrationBaseTest {
    private String APIName = "APIResourceTestAPI";
    private String APIVersion = "1.0.0";
    private String providerName = "";
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public APIResourceModificationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[] { TestUserMode.SUPER_TENANT_USER_STORE_USER },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
                new Object[] { TestUserMode.TENANT_EMAIL_USER },
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        providerName = user.getUserName();
    }

    @Test(groups = {"wso2.am"}, description = "add scope to resource test case")
    public void testSetScopeToResourceTestCase() throws Exception {

        String APIContext = "testResAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        //Add and publish an API
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiRequest.setProvider(providerName);
        HttpResponse httpResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = httpResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction());
        HttpResponse publishedApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(Response.Status.OK.getStatusCode(), publishedApiResponse.getResponseCode(), APIName +
                " is not visible in publisher");
        String oldSwagger = restAPIPublisher.getSwaggerByID(apiId);
        // resource are modified by using swagger doc. create the swagger doc with modified information.
        String modifiedResource = "{\n" +
                "    \"swagger\": \"2.0\",\n" +
                "    \"paths\": {\n" +
                "        \"/*\": {\n" +
                "            \"get\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"x-auth-type\": \"None\",\n" +
                "                \"x-throttling-tier\": \"Unlimited\"\n" +
                "            },\n" +
                "            \"post\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"parameters\": [\n" +
                "                    {\n" +
                "                        \"name\": \"Payload\",\n" +
                "                        \"description\": \"Request Body\",\n" +
                "                        \"required\": false,\n" +
                "                        \"in\": \"body\",\n" +
                "                        \"schema\": {\n" +
                "                            \"type\": \"object\",\n" +
                "                            \"properties\": {\n" +
                "                                \"payload\": {\n" +
                "                                    \"type\": \"string\"\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"x-auth-type\": \"None\",\n" +
                "                \"x-throttling-tier\": \"Unlimited\"\n" +
                "            },\n" +
                "            \"put\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"parameters\": [\n" +
                "                    {\n" +
                "                        \"name\": \"Payload\",\n" +
                "                        \"description\": \"Request Body\",\n" +
                "                        \"required\": false,\n" +
                "                        \"in\": \"body\",\n" +
                "                        \"schema\": {\n" +
                "                            \"type\": \"object\",\n" +
                "                            \"properties\": {\n" +
                "                                \"payload\": {\n" +
                "                                    \"type\": \"string\"\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"x-auth-type\": \"None\",\n" +
                "                \"x-throttling-tier\": \"Unlimited\"\n" +
                "            },\n" +
                "            \"delete\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"x-auth-type\": \"None\",\n" +
                "                \"x-throttling-tier\": \"Unlimited\"\n" +
                "            },\n" +
                "            \"patch\": {\n" +
                "                \"responses\": {\n" +
                "                    \"200\": {\n" +
                "                        \"description\": \"\"\n" +
                "                    }\n" +
                "                },\n" +
                "                \"parameters\": [\n" +
                "                    {\n" +
                "                        \"name\": \"Payload\",\n" +
                "                        \"description\": \"Request Body\",\n" +
                "                        \"required\": false,\n" +
                "                        \"in\": \"body\",\n" +
                "                        \"schema\": {\n" +
                "                            \"type\": \"object\",\n" +
                "                            \"properties\": {\n" +
                "                                \"payload\": {\n" +
                "                                    \"type\": \"string\"\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"x-auth-type\": \"None\",\n" +
                "                \"x-throttling-tier\": \"Unlimited\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"info\": {\n" +
                "        \"title\": \"APIResourceTestAPI\",\n" +
                "        \"version\": \"1.0.0\"\n" +
                "    }\n" +
                "}";
        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, modifiedResource);
        assertNotNull(swaggerResponse);
        String updatedSwagger = restAPIPublisher.getSwaggerByID(apiId);
        assertNotEquals(updatedSwagger, oldSwagger, "Modifying resources failed for API");
    }

    @Test(groups = {"wso2.am"}, description = "Update an API with AWS resource details",
            dependsOnMethods = "testSetScopeToResourceTestCase")
    public void testUpdateAPIWithAWSResourceDetailsTestCase() throws Exception {

        // This test case was written to cover resources with AWS resource details and
        // handling swagger API definitions with large doubles.
        String oldSwagger = restAPIPublisher.getSwaggerByID(apiId);
        // resource are modified by using swagger doc. create the swagger doc with modified information.
        String modifiedResource = "{\n"
                + "   \"openapi\":\"3.0.1\",\n"
                + "   \"info\":{\n"
                + "      \"title\":\"testAWSARN\",\n"
                + "      \"version\":\"1.0.0\"\n"
                + "   },\n"
                + "   \"servers\":[\n"
                + "      {\n"
                + "         \"url\":\"/\"\n"
                + "      }\n"
                + "   ],\n"
                + "   \"security\":[\n"
                + "      {\n"
                + "         \"default\":[\n"
                + "            \n"
                + "         ]\n"
                + "      }\n"
                + "   ],\n"
                + "   \"paths\":{\n"
                + "      \"/test\":{\n"
                + "         \"get\":{\n"
                + "            \"parameters\":[\n"
                + "               {\n"
                + "                  \"name\":\"testParameter\",\n"
                + "                  \"in\":\"query\",\n"
                + "                  \"required\":true,\n"
                + "                  \"style\":\"form\",\n"
                + "                  \"explode\":true,\n"
                + "                  \"schema\":{\n"
                + "                     \"maximum\":9223372036854776000,\n"
                + "                     \"minimum\":1,\n"
                + "                     \"type\":\"number\",\n"
                + "                     \"description\":\"Test large maximum values\",\n"
                + "                     \"format\":\"double\",\n"
                + "                     \"example\":12000\n"
                + "                  }\n"
                + "               }\n"
                + "            ],\n"
                + "            \"responses\":{\n"
                + "               \"200\":{\n"
                + "                  \"description\":\"ok\"\n"
                + "               }\n"
                + "            },\n"
                + "            \"security\":[\n"
                + "               {\n"
                + "                  \"default\":[\n"
                + "                     \n"
                + "                  ]\n"
                + "               }\n"
                + "            ],\n"
                + "            \"x-auth-type\":\"Application & Application User\",\n"
                + "            \"x-amzn-resource-name\":\"testARN\",\n"
                + "            \"x-amzn-resource-timeout\":230000\n"
                + "         }\n"
                + "      }\n"
                + "   },\n"
                + "   \"components\":{\n"
                + "      \"securitySchemes\":{\n"
                + "         \"default\":{\n"
                + "            \"type\":\"oauth2\",\n"
                + "            \"flows\":{\n"
                + "               \"implicit\":{\n"
                + "                  \"authorizationUrl\":\"https://test.com\",\n"
                + "                  \"scopes\":{\n"
                + "                     \n"
                + "                  }\n"
                + "               }\n"
                + "            }\n"
                + "         }\n"
                + "      }\n"
                + "   }\n"
                + "}";
        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, modifiedResource);
        assertNotNull(swaggerResponse);
        String updatedSwagger = restAPIPublisher.getSwaggerByID(apiId);
        assertNotEquals(updatedSwagger, oldSwagger,
                "Modifying resources with AWS Resource details failed for API");
        boolean updatedSwaggerContainsARName = updatedSwagger.contains("x-amzn-resource-name") &&
                updatedSwagger.contains("testARN");
        boolean updatedSwaggerContainsARTimeout = updatedSwagger.contains("x-amzn-resource-timeout") &&
                updatedSwagger.contains("230000");
        assertTrue(updatedSwaggerContainsARName, "Updated Swagger does not contain AWS resource name");
        assertTrue(updatedSwaggerContainsARTimeout, "Updated Swagger does not contain AWS resource timeout");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}

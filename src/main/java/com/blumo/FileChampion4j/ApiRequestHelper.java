import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class ApiRequestHelper {
    private final Map<String, Object> reqHeaders;
    private final String endpoint;
    private final String method;
    private final Map<String, Object> body;
    private final int[] passCodes;
    private final String passBodyContains;
    private final String failBodyContains;
    private CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    private String response;
    private byte[] fileBytes;
    private String fileName;
    private String fileChecksum;

    public ApiRequestHelper(String endpoint, String method, Map<String, Object> reqHeaders, Map<String, Object> body,
                        int[] passCodes, String passBodyContains, String failBodyContains, CredsLoadHelper credsValues) {
        this.endpoint = endpoint;
        this.method = method;
        this.reqHeaders = reqHeaders;
        this.body = body;
        this.passCodes = passCodes;
        this.passBodyContains = passBodyContains;
        this.failBodyContains = failBodyContains;
    }

    private String buildRequest(String fName, byte[] fileContent, String fChecksum) {
        this.fileName = fName;
        this.fileBytes = fileContent;
        this.fileChecksum = fChecksum;

        switch (method) {
            case "GET":
                return doGetRequest();
            case "POST":
                return doPostRequest();
            case "PUT":
                return doPutRequest();
            default:
                return "Unsupported HTTP method: "  + method;
        }
    }

    private String doGetRequest() {
        // Build and execute GET request
        HttpGet httpGet = new HttpGet(endpoint);
        URI uri = new URIBuilder(httpGet.getURI()).build();
        for (Map.Entry<String, Object> entry : reqHeaders.entrySet()) {
            // add request headers
            httpGet.addHeader(entry.getKey(), entry.getValue().toString());
        }
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            // add GET request parameters
            uri = new URIBuilder(uri).addParameter(entry.getKey(), entry.getValue().toString()).build();
        }
        httpGet.setURI(uri);

        try {
            response = httpClient.execute(httpGet, responseHandler);
        } catch (Exception e) {
            response = "Error: " + e.getMessage();
        } finally {
            if (response == null) {
                response = "Error: No response";
            }
            httpGet.releaseConnection();
            httpClient.close();
        }
        return response;
    }

    private String doPostRequest() {
        // Build and execute POST request
        HttpPost httpPost = new HttpPost(endpoint);

        URI uri = new URIBuilder(httpPost.getURI()).build();
        for (Map.Entry<String, Object> entry : reqHeaders.entrySet()) {
            // add request headers
            httpPost.addHeader(entry.getKey(), entry.getValue().toString());
        }
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            // add GET request parameters
            if (entry.getKey().equals("file")) {
                // add file to request
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.addPart("file", new ByteArrayBody((byte[]) entry.getValue(), ContentType.DEFAULT_BINARY, "file"));
                httpPost.setEntity(builder.build());
            } else {
                // add other parameters to request
                uri = new URIBuilder(uri).addParameter(entry.getKey(), entry.getValue().toString()).build();
            }
            
        }
        httpPost.setURI(uri);

        try {
            response = httpClient.execute(httpPost, responseHandler);
        } catch (Exception e) {
            response = "Error: " + e.getMessage();
        } finally {
            if (response == null) {
                response = "Error: No response";
            }
            httpPost.releaseConnection();
            httpClient.close();
        }
    }



    private String doPutRequest() {
        // Build and execute PUT request
    }




    /**
     * Response handler for HTTP requests
     */
    private ResponseHandler < String > responseHandler = httpResponse -> {
        int status = httpResponse.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = httpResponse.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    };

    public String upload(String fileName, byte[] fileContent) throws IOException {
        // Build request
        HttpEntity entity = buildRequestEntity(body, fileName, fileContent);
        if (method.equals("POST")) {
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apikey + ":" + apiSecret);
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost);
        } else if (method.equals("PUT")) {
            HttpPut httpPut = new HttpPut(endpoint);
            httpPut.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apikey + ":" + apiSecret);
            httpPut.setEntity(entity);
            response = httpClient.execute(httpPut);
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        // Check response
        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity());
        if (contains(passCodes, statusCode) && responseBody.contains(passBodyContains)) {
            return "PASS";
        } else if (responseBody.contains(failBodyContains)) {
            return "FAIL";
        } else {
            throw new RuntimeException("Unexpected response: " + responseBody);
        }
    }

        private HttpEntity buildRequestEntity(Map<String, Object> bodyConfig, String fileName, byte[] fileContent) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        for (String key : bodyConfig.keySet()) {
            Object value = bodyConfig.get(key);
            if (value instanceof Map) {
                builder.addPart(key, buildRequestEntity((Map<String, Object>) value, fileName, fileContent));
            } else if (value instanceof String) {
                builder.addPart(key, new StringBody((String) value, ContentType.TEXT_PLAIN));
            } else {
                throw new IllegalArgumentException("Unsupported body value: " + value);
            }
        }
        builder.addPart("file", new ByteArrayBody(fileContent, ContentType.APPLICATION_OCTET_STREAM, fileName));
        builder.addPart("document", new ByteArrayBody(EntityUtils.toByteArray(entity), ContentType.APPLICATION_OCTET_STREAM, fileName)); // add document
        return builder.build();
    }

    private boolean contains(int[] arr, int key) {
        for (int i : arr) {
            if (i == key) {
                return true;
            }
        }
        return false;
    }
}


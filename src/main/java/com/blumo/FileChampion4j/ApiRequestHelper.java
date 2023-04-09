import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;

public class ApiRequestHelper {
    private final String apikey;
    private final String apiSecret;
    private final String endpoint;
    private final String method;
    private final Map<String, Object> body;
    private final int[] passCodes;
    private final String passBodyContains;
    private final String failBodyContains;
    public ApiRequestHelper(String apikey, String apiSecret, String endpoint, String method, Map<String, Object> body,
                        int[] passCodes, String passBodyContains, String failBodyContains) {
        this.apikey = apikey;
        this.apiSecret = apiSecret;
        this.endpoint = endpoint;
        this.method = method;
        this.body = body;
        this.passCodes = passCodes;
        this.passBodyContains = passBodyContains;
        this.failBodyContains = failBodyContains;
    }
    public String upload(String fileName, byte[] fileContent) throws IOException {
        // Build request
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpEntity entity = buildRequestEntity(body, fileName, fileContent);
        HttpResponse response = null;
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


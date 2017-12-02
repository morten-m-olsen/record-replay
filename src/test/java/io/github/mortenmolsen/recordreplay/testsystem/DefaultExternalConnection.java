package io.github.mortenmolsen.recordreplay.testsystem;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * @author Morten Meiling Olsen
 */
public class DefaultExternalConnection implements ExternalConnection {

    private RestTemplate restTemplate = new RestTemplate();

    @Override
    public Post getTestPost() throws Exception {
        ResponseEntity<Post> result = restTemplate.exchange(new RequestEntity(new HttpHeaders(), HttpMethod.GET,
                new URI("https://jsonplaceholder.typicode.com/posts/1")), Post.class);
        return result.getBody();
    }
}

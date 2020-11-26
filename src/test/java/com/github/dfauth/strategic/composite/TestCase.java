package com.github.dfauth.strategic.composite;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

import static com.github.dfauth.strategic.composite.RESTUtils.mockRestEndpoint;
import static com.github.dfauth.strategic.composite.StrategicComposite.createCompositeOfType;
import static com.github.dfauth.strategic.composite.TimedResult.timed;
import static org.testng.Assert.*;

public class TestCase {

    private static final Logger logger = LoggerFactory.getLogger(TestCase.class);

    private static final String KID = "ffa5009f-f815-4d87-a36b-e4c29e5829b5";

    private static final byte[] RESPONSE = String.format("{\n" +
            "    \"keys\": [\n" +
            "            {\n" +
            "            \"n\": \"xe_69ro6qOFosdY2gA1theO3RwJFbd0zW025aDEGbJpwknFhaCsOQDBmjA8ZNuI5WQ\",\n" +
            "            \"e\": \"AQAB\",\n" +
            "            \"alg\": \"RS256\",\n" +
            "            \"use\": \"sig\",\n" +
            "            \"kid\": \"%s\",\n" +
            "            \"kty\": \"RSA\"\n" +
            "            }\n" +
            "           ]\n" +
            "}",KID).getBytes();


    @Test
    public void testJwkProvider() {
        try {
            JwkProvider provider = new JwkProviderBuilder("https://localhost:8080").build();
            AtomicReference<JwkProvider> composite = new AtomicReference<>();
            int cnt = mockRestEndpoint("/.well-known/jwks.json", RESPONSE, url -> {
                JwkProvider mock = new JwkProviderBuilder(url).build();
                composite.set(createCompositeOfType(JwkProvider.class).compose(provider, mock));
                TimedResult<Jwk> result = timed(() -> {
                    Jwk mockResult = composite.get().get(KID);
                    assertNotNull(mockResult);
                    return mockResult;
                });
                assertTrue(result.result().isSuccess());
                logger.info("first run took "+ result.elapsed()+" msec");
                assertTrue(result.elapsed()>1000); // first call times out
                result = timed(() -> {
                    Jwk mockResult = composite.get().get(KID);
                    assertNotNull(mockResult);
                    return mockResult;
                });
                logger.info("second run took "+result.elapsed()+" msec");
                assertTrue(result.elapsed()<10); // subsequent calls are cached
            });
            assertEquals(cnt, 1); // only one call

            // server is now down, but we continue to use the cache
            TimedResult<Jwk> result = timed(() -> {
                Jwk mockResult = composite.get().get(KID);
                assertNotNull(mockResult);
                return mockResult;
            });
            logger.info("third run took "+result.elapsed()+" msec");
            assertTrue(result.elapsed()<10); // subsequent calls are cached

        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}

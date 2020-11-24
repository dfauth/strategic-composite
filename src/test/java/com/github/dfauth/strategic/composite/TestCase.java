package com.github.dfauth.strategic.composite;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.Callable;

import static com.github.dfauth.strategic.composite.StrategicComposite.createCompositeOfType;
import static com.github.dfauth.trycatch.TryCatch.tryCatch;
import static org.testng.Assert.assertNotNull;

public class TestCase {

    private static final Logger logger = LoggerFactory.getLogger(TestCase.class);

    @Test
    public void testJwkProvider() {
        try {
            JwkProvider provider = new JwkProviderBuilder("http://localhost:8080").build();
            JwkProvider mock = new MockJwkProvider();
            JwkProvider composed = createCompositeOfType(JwkProvider.class).compose(provider, mock);
            logger.info("first run took "+timed(() -> {
                Jwk mockResult = composed.get("blah");
                assertNotNull(mockResult);
                return mockResult;
            }).elapsed()+" msec");
            logger.info("second run took "+timed(() -> {
                Jwk mockResult = composed.get("blah");
                assertNotNull(mockResult);
                return mockResult;
            }).elapsed()+" msec");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private TimedResult<Void> timed(Runnable r) {
        return timed(() -> {
            r.run();
            return null;
        });
    }

    interface TimedResult<T> {
        long elapsed();
        T result();
    }

    private <T> TimedResult<T> timed(Callable<T> c) {
        return tryCatch(() -> {
            long now = System.currentTimeMillis();
            T result = c.call();
            long elapsed = System.currentTimeMillis() - now;
            return new TimedResult<T>() {
                @Override
                public long elapsed() {
                    return elapsed;
                }

                @Override
                public T result() {
                    return result;
                }
            };
        });
    }

    public static class MockJwkProvider implements JwkProvider {
        @Override
        public Jwk get(String keyId) throws JwkException {
            return new Jwk("kid", "kty", "alg", "use", Collections.emptyList(), "x5u", Collections.emptyList(), "x5t", Collections.emptyMap());
        }
    }
}

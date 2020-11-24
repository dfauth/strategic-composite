package com.github.dfauth.strategic.composite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StrategicComposite<T> {

    private static final Logger logger = LoggerFactory.getLogger(StrategicComposite.class);

    private final Strategy strategy;
    private final List<T> clients;
    private int current = 0;

    public static <T> StrategicComposer<T> createCompositeOfType(Class<T> classOfT) {
        return new StrategicComposer<T>(classOfT);
    }

    public StrategicComposite(List<T> clients) {
        this(new StickyStrategy(clients.size()), clients);
    }

    public StrategicComposite(Strategy strategy, List<T> clients) {
        this.strategy = strategy;
        this.clients = clients;
    }

    private T current() {
        return current(0);
    }

    private T current(int i) {
        return clients.get(i%clients.size());
    }

    public T getComposite(Class<T> cls) {
        return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{cls}, (proxy, method, args) -> getComposite(current, method, args));
    }

    private T getComposite(final int i, Method method, Object[] args) {
        if(i>= clients.size()) {
            throw new RuntimeException("Oops, no available implementation");
        }
        return (T) findMethod(current(i), method).map(m -> {
            try {
                T result = (T) m.invoke(current(i), args);
                current = strategy.success(current);
                return result;
            } catch (IllegalAccessException e) {
                logger.info(e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                logger.info(e.getMessage(), e);
                current = strategy.failure(e.getCause(), i);
                return getComposite(strategy.get(current), method, args);
            }
        }).orElseThrow(() ->
                new RuntimeException("No method found: "+method.getName()));
    }

    private Optional<Method> findMethod(T current, Method method) {
        return Stream.of(current.getClass().getMethods()).filter(m ->
                m.getName().equals(method.getName()) &&
                        m.getReturnType().equals(method.getReturnType()) &&
                        Arrays.equals(m.getParameterTypes(), method.getParameterTypes())
        ).findFirst();
    }

    public interface Strategy {

        int failure(Throwable t, int i);

        int get(int current);

        int success(int current);
    }

    public static class StickyStrategy implements Strategy {

        private final int size;

        public StickyStrategy(int size) {
            this.size = size;
        }

        @Override
        public int failure(Throwable t, int i) {
            return i+1%size;
        }

        @Override
        public int get(int i) {
            return i%size;
        }

        @Override
        public int success(int i) {
            return i;
        }
    }

    public static class StrategicComposer<T> {

        private final Class<T> classOfT;
        private Optional<Strategy> optStrategy = Optional.empty();

        public StrategicComposer(Class<T> classOfT) {
            this.classOfT = classOfT;
        }

        public StrategicComposer withStrategy(Strategy strategy) {
            this.optStrategy = Optional.ofNullable(strategy);
            return this;
        }

        public T compose(T... arrayOfT) {
            return optStrategy
                    .map(s -> new StrategicComposite<T>(s, Stream.of(arrayOfT).collect(Collectors.toList())).getComposite(classOfT))
                    .orElse(new StrategicComposite<T>(Stream.of(arrayOfT).collect(Collectors.toList())).getComposite(classOfT));
        }
    }
}

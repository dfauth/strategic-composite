package com.github.dfauth.strategic.composite;

import com.github.dfauth.trycatch.ExceptionalRunnable;
import com.github.dfauth.trycatch.Try;

import java.util.concurrent.Callable;

import static com.github.dfauth.strategic.composite.TimedResult.timed;

interface TimedResult<T> {
    long elapsed();
    Try<T> result();

    static TimedResult<Void> timed(ExceptionalRunnable r) {
        return timed((Callable<Void>) r);
    }

    static <T> TimedResult<T> timed(Callable<T> c) {
        long now = System.currentTimeMillis();
        Try<T> _try = Try.tryWith(c);
        long elapsed = System.currentTimeMillis() - now;
        return new TimedResult<T>() {
            @Override
            public long elapsed() {
                return elapsed;
            }

            @Override
            public Try<T> result() {
                return _try;
            }
        };
    }
}

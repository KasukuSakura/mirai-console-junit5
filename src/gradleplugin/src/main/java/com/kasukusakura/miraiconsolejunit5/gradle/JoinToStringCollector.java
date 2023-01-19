package com.kasukusakura.miraiconsolejunit5.gradle;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class JoinToStringCollector<T> implements Collector<T, Object, StringBuilder> {
    private final Object prefix;
    private final Object suffix;
    private final Object split;

    private static class StringJoiner {
        boolean applied;
        StringBuilder builder = new StringBuilder();
    }

    public JoinToStringCollector(Object prefix, Object suffix, Object split) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.split = split;
    }

    public JoinToStringCollector(Object split) {
        this(null, null, split);
    }

    public JoinToStringCollector() {
        this(null);
    }

    @Override
    public Supplier<Object> supplier() {
        return StringJoiner::new;
    }

    @Override
    public BiConsumer<Object, T> accumulator() {
        if (split == null) {
            return (sb, value) -> ((StringJoiner) sb).builder.append(value);
        }

        return (sb, value) -> {
            StringJoiner sj = (StringJoiner) sb;
            if (sj.applied) {
                sj.builder.append(split);
            }
            sj.builder.append(value);
            sj.applied = true;
        };
    }

    @Override
    public BinaryOperator<Object> combiner() {
        return (s1, s2) -> {
            StringJoiner sj1 = (StringJoiner) s1, sj2 = (StringJoiner) s2;

            if (split == null) {
                sj1.builder.append(sj2.builder);
                return s1;
            }

            if (sj1.applied) {
                sj1.builder.append(split);
            }
            sj1.builder.append(sj2.builder);
            sj1.applied = true;

            return s1;
        };
    }

    @Override
    public Function<Object, StringBuilder> finisher() {
        return (sv) -> {
            StringJoiner sj = (StringJoiner) sv;
            if (prefix != null) {
                sj.builder.insert(0, prefix);
            }
            if (suffix != null) {
                sj.builder.append(suffix);
            }
            return sj.builder;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.singleton(Characteristics.CONCURRENT);
    }
}

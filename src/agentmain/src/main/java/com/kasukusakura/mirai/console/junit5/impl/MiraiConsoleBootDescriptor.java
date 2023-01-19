package com.kasukusakura.mirai.console.junit5.impl;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

class MiraiConsoleBootDescriptor extends AbstractTestDescriptor {

    private final Type type;

    protected MiraiConsoleBootDescriptor(UniqueId uniqueId, String displayName, Type type) {
        super(uniqueId, displayName);
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }
}

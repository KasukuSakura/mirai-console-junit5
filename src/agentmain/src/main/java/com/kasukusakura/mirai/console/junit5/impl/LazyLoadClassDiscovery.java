package com.kasukusakura.mirai.console.junit5.impl;

import org.junit.platform.engine.DiscoverySelector;

class LazyLoadClassDiscovery implements DiscoverySelector {
    final String classname;

    LazyLoadClassDiscovery(String classname) {
        this.classname = classname;
    }
}

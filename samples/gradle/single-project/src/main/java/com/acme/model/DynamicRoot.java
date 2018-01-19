package com.acme.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

public class DynamicRoot {

    private int invisiblePrivateProperty;
    protected int invisibleProtectedProperty;
    int invisiblePackageScopedProperty;
    @Getter
    int invisibleGetterOnlyProperty;

    private void setAnotherInvisiblePrivateProperty(int anotherInvisiblePrivateProperty) {
        // does not matter
    }

    protected void setAnotherInvisibleProtectedProperty(int anotherInvisibleProtectedProperty) {
        // does not matter
    }

    void setAnotherInvisiblePackageScopedProperty(int anotherInvisiblePackageScopedProperty) {
        // does not matter
    }

    public int publicProperty;

    @Getter
    @Setter
    private int visiblePrimitive;

    @Getter
    @Setter
    private Map<String, Integer> visiblePrimitiveMap;

    @Getter
    @Setter
    private Set<Integer> visiblePrimitiveSet;

    @Getter
    @Setter
    private Map<String, DynamicChild> visibleChildMap;

    @Getter
    @Setter
    private Set<DynamicChild> visibleChildSet;

    public void setPropertyViaSetter(int propertyViaSetter) {
        // does not matter
    }

}
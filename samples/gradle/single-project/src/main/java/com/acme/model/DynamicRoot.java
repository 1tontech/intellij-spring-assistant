package com.acme.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Dynamic root documentation, Intentionally written so long that this should not fit into the suggestions. After previous dot, this section should not be visible in the documentation section
 */
public class DynamicRoot {

  public int invisiblePublicProperty;
  protected int invisibleProtectedProperty;
  int invisiblePackageScopedProperty;
  @Getter
  int invisibleGetterOnlyProperty;
  private int invisiblePrivateProperty;

  /**
   * Boolean documentation
   */
  @Getter
  @Setter
  private boolean boolProp;
  /**
   * Byte documentation
   */
  @Getter
  @Setter
  private byte byteProp;
  /**
   * Short documentation
   */
  @Getter
  @Setter
  private short shortProp;
  /**
   * Int documentation
   */
  @Getter
  @Setter
  private int intProp;
  /**
   * Long documentation
   */
  @Getter
  @Setter
  private long longProp;
  /**
   * Float documentation
   */
  @Getter
  @Setter
  private float floatProp;
  /**
   * Double documentation
   */
  @Getter
  @Setter
  private double doubleProp;
  /**
   * Big decimal documentation
   */
  @Getter
  @Setter
  private BigDecimal bigDecimalProp;
  /**
   * Char documentation
   */
  @Getter
  @Setter
  private char charProp;
  /**
   * String documentation
   */
  @Getter
  @Setter
  private String stringProp;
  /**
   * Enum documentation
   */
  @Getter
  @Setter
  private DynamicEnum enumProp;
  /**
   * Primitive key -> value map documentation
   */
  @Getter
  @Setter
  private Map<String, Integer> primitiveKeyToPrimitiveValueMap;
  /**
   * Enum key -> primitive value documentation
   */
  @Getter
  @Setter
  private Map<DynamicEnum, Integer> enumKeyToPrimitiveValueMap;
  /**
   * Primitive collection documentation
   */
  @Getter
  @Setter
  private Collection<Integer> primitiveCollection;
  /**
   * Enum collection documentation
   */
  @Getter
  @Setter
  private Collection<DynamicEnum> enumCollection;
  /**
   * Dynamic child documentation
   */
  @Getter
  @Setter
  private DynamicChild dynamicChild;
  /**
   * Primitive key -> dynamic child documentation
   */
  @Getter
  @Setter
  private Map<String, DynamicChild> primitiveKeyToDynamicChildValueMap;
  /**
   * enum key -> dynamic child documentation
   */
  @Getter
  @Setter
  private Map<DynamicEnum, DynamicChild> enumKeyToDynamicChildValueMap;
  /**
   * dynamic child collection documentation
   */
  @Getter
  @Setter
  private Collection<DynamicChild> childCollection;

  private void setAnotherInvisiblePrivateProperty(int anotherInvisiblePrivateProperty) {
    // does not matter
  }

  protected void setAnotherInvisibleProtectedProperty(int anotherInvisibleProtectedProperty) {
    // does not matter
  }

  void setAnotherInvisiblePackageScopedProperty(int anotherInvisiblePackageScopedProperty) {
    // does not matter
  }

  public void setPropertyViaSetter(int propertyViaSetter) {
    // does not matter
  }

}

// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: Common.proto
package com.zq.live.proto.Common;

import com.squareup.wire.EnumAdapter;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.WireEnum;
import java.lang.Override;

/**
 * EClubMemberRoleType 成员角色类型
 */
public enum EClubMemberRoleType implements WireEnum {
  /**
   * 未知
   */
  ECMRT_Invalid(0),

  /**
   * 族长
   */
  ECMRT_Founder(1),

  /**
   * 副族长
   */
  ECMRT_CoFounder(2),

  /**
   * 主持人
   */
  ECMRT_Hostman(3),

  /**
   * 普通成员
   */
  ECMRT_Common(4);

  public static final ProtoAdapter<EClubMemberRoleType> ADAPTER = new ProtoAdapter_EClubMemberRoleType();

  private final int value;

  EClubMemberRoleType(int value) {
    this.value = value;
  }

  /**
   * Return the constant for {@code value} or null.
   */
  public static EClubMemberRoleType fromValue(int value) {
    switch (value) {
      case 0: return ECMRT_Invalid;
      case 1: return ECMRT_Founder;
      case 2: return ECMRT_CoFounder;
      case 3: return ECMRT_Hostman;
      case 4: return ECMRT_Common;
      default: return null;
    }
  }

  @Override
  public int getValue() {
    return value;
  }

  public static final class Builder {
    public EClubMemberRoleType build() {
      return ECMRT_Invalid;
    }
  }

  private static final class ProtoAdapter_EClubMemberRoleType extends EnumAdapter<EClubMemberRoleType> {
    ProtoAdapter_EClubMemberRoleType() {
      super(EClubMemberRoleType.class);
    }

    @Override
    protected EClubMemberRoleType fromValue(int value) {
      return EClubMemberRoleType.fromValue(value);
    }
  }
}
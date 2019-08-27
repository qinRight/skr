// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: race_room.proto
package com.zq.live.proto.RaceRoom;

import com.squareup.wire.EnumAdapter;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.WireEnum;
import java.lang.Override;

public enum ERaceRoundOverReason implements WireEnum {
  /**
   * 未知
   */
  ERROR_UNKNOWN(0),

  /**
   * 没人抢唱
   */
  ERROR_NO_ONE_SING(1),

  /**
   * 人数不够
   */
  ERROR_NOT_ENOUTH_PLAYER(2),

  /**
   * 正常结束
   */
  ERROR_NORMAL_OVER(3);

  public static final ProtoAdapter<ERaceRoundOverReason> ADAPTER = new ProtoAdapter_ERaceRoundOverReason();

  private final int value;

  ERaceRoundOverReason(int value) {
    this.value = value;
  }

  /**
   * Return the constant for {@code value} or null.
   */
  public static ERaceRoundOverReason fromValue(int value) {
    switch (value) {
      case 0: return ERROR_UNKNOWN;
      case 1: return ERROR_NO_ONE_SING;
      case 2: return ERROR_NOT_ENOUTH_PLAYER;
      case 3: return ERROR_NORMAL_OVER;
      default: return null;
    }
  }

  @Override
  public int getValue() {
    return value;
  }

  public static final class Builder {
    public ERaceRoundOverReason build() {
      return ERROR_UNKNOWN;
    }
  }

  private static final class ProtoAdapter_ERaceRoundOverReason extends EnumAdapter<ERaceRoundOverReason> {
    ProtoAdapter_ERaceRoundOverReason() {
      super(ERaceRoundOverReason.class);
    }

    @Override
    protected ERaceRoundOverReason fromValue(int value) {
      return ERaceRoundOverReason.fromValue(value);
    }
  }
}
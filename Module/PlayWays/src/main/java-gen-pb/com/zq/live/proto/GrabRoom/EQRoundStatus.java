// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: Room.proto
package com.zq.live.proto.GrabRoom;

import com.squareup.wire.EnumAdapter;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.WireEnum;
import java.lang.Override;

public enum EQRoundStatus implements WireEnum {
  /**
   * 轮次状态位置
   */
  QRS_UNKNOWN(0),

  /**
   * 轮次未开始
   */
  QRS_UNBEGIN(1),

  /**
   * 轮次进入导唱阶段
   */
  QRS_INTRO(2),

  /**
   * 轮次进入演唱阶段
   */
  QRS_SING(3),

  /**
   * 轮次已结束
   */
  QRS_END(4),

  /**
   * 合唱演唱阶段
   */
  QRS_CHO_SING(5),

  /**
   * spk第一位用户演唱
   */
  QRS_SPK_FIRST_PEER_SING(6),

  /**
   * spk第二位用户演唱
   */
  QRS_SPK_SECOND_PEER_SING(7),

  /**
   * 连麦小游戏进行中
   */
  QRS_MIN_GAME_PLAY(8);

  public static final ProtoAdapter<EQRoundStatus> ADAPTER = new ProtoAdapter_EQRoundStatus();

  private final int value;

  EQRoundStatus(int value) {
    this.value = value;
  }

  /**
   * Return the constant for {@code value} or null.
   */
  public static EQRoundStatus fromValue(int value) {
    switch (value) {
      case 0: return QRS_UNKNOWN;
      case 1: return QRS_UNBEGIN;
      case 2: return QRS_INTRO;
      case 3: return QRS_SING;
      case 4: return QRS_END;
      case 5: return QRS_CHO_SING;
      case 6: return QRS_SPK_FIRST_PEER_SING;
      case 7: return QRS_SPK_SECOND_PEER_SING;
      case 8: return QRS_MIN_GAME_PLAY;
      default: return null;
    }
  }

  @Override
  public int getValue() {
    return value;
  }

  public static final class Builder {
    public EQRoundStatus build() {
      return QRS_UNKNOWN;
    }
  }

  private static final class ProtoAdapter_EQRoundStatus extends EnumAdapter<EQRoundStatus> {
    ProtoAdapter_EQRoundStatus() {
      super(EQRoundStatus.class);
    }

    @Override
    protected EQRoundStatus fromValue(int value) {
      return EQRoundStatus.fromValue(value);
    }
  }
}
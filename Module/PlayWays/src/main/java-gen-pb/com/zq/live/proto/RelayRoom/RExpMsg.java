// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: relay_room.proto
package com.zq.live.proto.RelayRoom;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class RExpMsg extends Message<RExpMsg, RExpMsg.Builder> {
  public static final ProtoAdapter<RExpMsg> ADAPTER = new ProtoAdapter_RExpMsg();

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_TOTALEXP = 0;

  public static final Integer DEFAULT_BEFOREEXP = 0;

  public static final Integer DEFAULT_AFTEREXP = 0;

  public static final Integer DEFAULT_INCREXP = 0;

  public static final Integer DEFAULT_BEFORESTAR = 0;

  public static final Integer DEFAULT_AFTERSTAR = 0;

  public static final Integer DEFAULT_INCRSTAR = 0;

  public static final Integer DEFAULT_ROUNDSEQ = 0;

  /**
   * 总经验值
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer totalExp;

  /**
   * 之前经验值
   */
  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer beforeExp;

  /**
   * 之后经验值
   */
  @WireField(
      tag = 3,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer afterExp;

  /**
   * 增加经验值
   */
  @WireField(
      tag = 4,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer incrExp;

  /**
   * 之前星数
   */
  @WireField(
      tag = 5,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer beforeStar;

  /**
   * 之后星数
   */
  @WireField(
      tag = 6,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer afterStar;

  /**
   * 增加星数
   */
  @WireField(
      tag = 7,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer incrStar;

  /**
   * 轮次序号
   */
  @WireField(
      tag = 8,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer roundSeq;

  public RExpMsg(Integer totalExp, Integer beforeExp, Integer afterExp, Integer incrExp,
      Integer beforeStar, Integer afterStar, Integer incrStar, Integer roundSeq) {
    this(totalExp, beforeExp, afterExp, incrExp, beforeStar, afterStar, incrStar, roundSeq, ByteString.EMPTY);
  }

  public RExpMsg(Integer totalExp, Integer beforeExp, Integer afterExp, Integer incrExp,
      Integer beforeStar, Integer afterStar, Integer incrStar, Integer roundSeq,
      ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.totalExp = totalExp;
    this.beforeExp = beforeExp;
    this.afterExp = afterExp;
    this.incrExp = incrExp;
    this.beforeStar = beforeStar;
    this.afterStar = afterStar;
    this.incrStar = incrStar;
    this.roundSeq = roundSeq;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.totalExp = totalExp;
    builder.beforeExp = beforeExp;
    builder.afterExp = afterExp;
    builder.incrExp = incrExp;
    builder.beforeStar = beforeStar;
    builder.afterStar = afterStar;
    builder.incrStar = incrStar;
    builder.roundSeq = roundSeq;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof RExpMsg)) return false;
    RExpMsg o = (RExpMsg) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(totalExp, o.totalExp)
        && Internal.equals(beforeExp, o.beforeExp)
        && Internal.equals(afterExp, o.afterExp)
        && Internal.equals(incrExp, o.incrExp)
        && Internal.equals(beforeStar, o.beforeStar)
        && Internal.equals(afterStar, o.afterStar)
        && Internal.equals(incrStar, o.incrStar)
        && Internal.equals(roundSeq, o.roundSeq);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (totalExp != null ? totalExp.hashCode() : 0);
      result = result * 37 + (beforeExp != null ? beforeExp.hashCode() : 0);
      result = result * 37 + (afterExp != null ? afterExp.hashCode() : 0);
      result = result * 37 + (incrExp != null ? incrExp.hashCode() : 0);
      result = result * 37 + (beforeStar != null ? beforeStar.hashCode() : 0);
      result = result * 37 + (afterStar != null ? afterStar.hashCode() : 0);
      result = result * 37 + (incrStar != null ? incrStar.hashCode() : 0);
      result = result * 37 + (roundSeq != null ? roundSeq.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (totalExp != null) builder.append(", totalExp=").append(totalExp);
    if (beforeExp != null) builder.append(", beforeExp=").append(beforeExp);
    if (afterExp != null) builder.append(", afterExp=").append(afterExp);
    if (incrExp != null) builder.append(", incrExp=").append(incrExp);
    if (beforeStar != null) builder.append(", beforeStar=").append(beforeStar);
    if (afterStar != null) builder.append(", afterStar=").append(afterStar);
    if (incrStar != null) builder.append(", incrStar=").append(incrStar);
    if (roundSeq != null) builder.append(", roundSeq=").append(roundSeq);
    return builder.replace(0, 2, "RExpMsg{").append('}').toString();
  }

  public byte[] toByteArray() {
    return RExpMsg.ADAPTER.encode(this);
  }

  public static final RExpMsg parseFrom(byte[] data) throws IOException {
    RExpMsg c = null;
       c = RExpMsg.ADAPTER.decode(data);
    return c;
  }

  /**
   * 总经验值
   */
  public Integer getTotalExp() {
    if(totalExp==null){
        return DEFAULT_TOTALEXP;
    }
    return totalExp;
  }

  /**
   * 之前经验值
   */
  public Integer getBeforeExp() {
    if(beforeExp==null){
        return DEFAULT_BEFOREEXP;
    }
    return beforeExp;
  }

  /**
   * 之后经验值
   */
  public Integer getAfterExp() {
    if(afterExp==null){
        return DEFAULT_AFTEREXP;
    }
    return afterExp;
  }

  /**
   * 增加经验值
   */
  public Integer getIncrExp() {
    if(incrExp==null){
        return DEFAULT_INCREXP;
    }
    return incrExp;
  }

  /**
   * 之前星数
   */
  public Integer getBeforeStar() {
    if(beforeStar==null){
        return DEFAULT_BEFORESTAR;
    }
    return beforeStar;
  }

  /**
   * 之后星数
   */
  public Integer getAfterStar() {
    if(afterStar==null){
        return DEFAULT_AFTERSTAR;
    }
    return afterStar;
  }

  /**
   * 增加星数
   */
  public Integer getIncrStar() {
    if(incrStar==null){
        return DEFAULT_INCRSTAR;
    }
    return incrStar;
  }

  /**
   * 轮次序号
   */
  public Integer getRoundSeq() {
    if(roundSeq==null){
        return DEFAULT_ROUNDSEQ;
    }
    return roundSeq;
  }

  /**
   * 总经验值
   */
  public boolean hasTotalExp() {
    return totalExp!=null;
  }

  /**
   * 之前经验值
   */
  public boolean hasBeforeExp() {
    return beforeExp!=null;
  }

  /**
   * 之后经验值
   */
  public boolean hasAfterExp() {
    return afterExp!=null;
  }

  /**
   * 增加经验值
   */
  public boolean hasIncrExp() {
    return incrExp!=null;
  }

  /**
   * 之前星数
   */
  public boolean hasBeforeStar() {
    return beforeStar!=null;
  }

  /**
   * 之后星数
   */
  public boolean hasAfterStar() {
    return afterStar!=null;
  }

  /**
   * 增加星数
   */
  public boolean hasIncrStar() {
    return incrStar!=null;
  }

  /**
   * 轮次序号
   */
  public boolean hasRoundSeq() {
    return roundSeq!=null;
  }

  public static final class Builder extends Message.Builder<RExpMsg, Builder> {
    private Integer totalExp;

    private Integer beforeExp;

    private Integer afterExp;

    private Integer incrExp;

    private Integer beforeStar;

    private Integer afterStar;

    private Integer incrStar;

    private Integer roundSeq;

    public Builder() {
    }

    /**
     * 总经验值
     */
    public Builder setTotalExp(Integer totalExp) {
      this.totalExp = totalExp;
      return this;
    }

    /**
     * 之前经验值
     */
    public Builder setBeforeExp(Integer beforeExp) {
      this.beforeExp = beforeExp;
      return this;
    }

    /**
     * 之后经验值
     */
    public Builder setAfterExp(Integer afterExp) {
      this.afterExp = afterExp;
      return this;
    }

    /**
     * 增加经验值
     */
    public Builder setIncrExp(Integer incrExp) {
      this.incrExp = incrExp;
      return this;
    }

    /**
     * 之前星数
     */
    public Builder setBeforeStar(Integer beforeStar) {
      this.beforeStar = beforeStar;
      return this;
    }

    /**
     * 之后星数
     */
    public Builder setAfterStar(Integer afterStar) {
      this.afterStar = afterStar;
      return this;
    }

    /**
     * 增加星数
     */
    public Builder setIncrStar(Integer incrStar) {
      this.incrStar = incrStar;
      return this;
    }

    /**
     * 轮次序号
     */
    public Builder setRoundSeq(Integer roundSeq) {
      this.roundSeq = roundSeq;
      return this;
    }

    @Override
    public RExpMsg build() {
      return new RExpMsg(totalExp, beforeExp, afterExp, incrExp, beforeStar, afterStar, incrStar, roundSeq, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_RExpMsg extends ProtoAdapter<RExpMsg> {
    public ProtoAdapter_RExpMsg() {
      super(FieldEncoding.LENGTH_DELIMITED, RExpMsg.class);
    }

    @Override
    public int encodedSize(RExpMsg value) {
      return ProtoAdapter.UINT32.encodedSizeWithTag(1, value.totalExp)
          + ProtoAdapter.UINT32.encodedSizeWithTag(2, value.beforeExp)
          + ProtoAdapter.UINT32.encodedSizeWithTag(3, value.afterExp)
          + ProtoAdapter.UINT32.encodedSizeWithTag(4, value.incrExp)
          + ProtoAdapter.UINT32.encodedSizeWithTag(5, value.beforeStar)
          + ProtoAdapter.UINT32.encodedSizeWithTag(6, value.afterStar)
          + ProtoAdapter.UINT32.encodedSizeWithTag(7, value.incrStar)
          + ProtoAdapter.UINT32.encodedSizeWithTag(8, value.roundSeq)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, RExpMsg value) throws IOException {
      ProtoAdapter.UINT32.encodeWithTag(writer, 1, value.totalExp);
      ProtoAdapter.UINT32.encodeWithTag(writer, 2, value.beforeExp);
      ProtoAdapter.UINT32.encodeWithTag(writer, 3, value.afterExp);
      ProtoAdapter.UINT32.encodeWithTag(writer, 4, value.incrExp);
      ProtoAdapter.UINT32.encodeWithTag(writer, 5, value.beforeStar);
      ProtoAdapter.UINT32.encodeWithTag(writer, 6, value.afterStar);
      ProtoAdapter.UINT32.encodeWithTag(writer, 7, value.incrStar);
      ProtoAdapter.UINT32.encodeWithTag(writer, 8, value.roundSeq);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public RExpMsg decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.setTotalExp(ProtoAdapter.UINT32.decode(reader)); break;
          case 2: builder.setBeforeExp(ProtoAdapter.UINT32.decode(reader)); break;
          case 3: builder.setAfterExp(ProtoAdapter.UINT32.decode(reader)); break;
          case 4: builder.setIncrExp(ProtoAdapter.UINT32.decode(reader)); break;
          case 5: builder.setBeforeStar(ProtoAdapter.UINT32.decode(reader)); break;
          case 6: builder.setAfterStar(ProtoAdapter.UINT32.decode(reader)); break;
          case 7: builder.setIncrStar(ProtoAdapter.UINT32.decode(reader)); break;
          case 8: builder.setRoundSeq(ProtoAdapter.UINT32.decode(reader)); break;
          default: {
            FieldEncoding fieldEncoding = reader.peekFieldEncoding();
            Object value = fieldEncoding.rawProtoAdapter().decode(reader);
            builder.addUnknownField(tag, fieldEncoding, value);
          }
        }
      }
      reader.endMessage(token);
      return builder.build();
    }

    @Override
    public RExpMsg redact(RExpMsg value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
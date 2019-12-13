// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: party_room.proto
package com.zq.live.proto.PartyRoom;

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

public final class PUpdatePopularityMsg extends Message<PUpdatePopularityMsg, PUpdatePopularityMsg.Builder> {
  public static final ProtoAdapter<PUpdatePopularityMsg> ADAPTER = new ProtoAdapter_PUpdatePopularityMsg();

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_USERID = 0;

  public static final Integer DEFAULT_POPULARITY = 0;

  /**
   * 用户id
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer userID;

  /**
   * 最新的人气值
   */
  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer popularity;

  public PUpdatePopularityMsg(Integer userID, Integer popularity) {
    this(userID, popularity, ByteString.EMPTY);
  }

  public PUpdatePopularityMsg(Integer userID, Integer popularity, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.userID = userID;
    this.popularity = popularity;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.userID = userID;
    builder.popularity = popularity;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof PUpdatePopularityMsg)) return false;
    PUpdatePopularityMsg o = (PUpdatePopularityMsg) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(userID, o.userID)
        && Internal.equals(popularity, o.popularity);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (userID != null ? userID.hashCode() : 0);
      result = result * 37 + (popularity != null ? popularity.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (userID != null) builder.append(", userID=").append(userID);
    if (popularity != null) builder.append(", popularity=").append(popularity);
    return builder.replace(0, 2, "PUpdatePopularityMsg{").append('}').toString();
  }

  public byte[] toByteArray() {
    return PUpdatePopularityMsg.ADAPTER.encode(this);
  }

  public static final PUpdatePopularityMsg parseFrom(byte[] data) throws IOException {
    PUpdatePopularityMsg c = null;
       c = PUpdatePopularityMsg.ADAPTER.decode(data);
    return c;
  }

  /**
   * 用户id
   */
  public Integer getUserID() {
    if(userID==null){
        return DEFAULT_USERID;
    }
    return userID;
  }

  /**
   * 最新的人气值
   */
  public Integer getPopularity() {
    if(popularity==null){
        return DEFAULT_POPULARITY;
    }
    return popularity;
  }

  /**
   * 用户id
   */
  public boolean hasUserID() {
    return userID!=null;
  }

  /**
   * 最新的人气值
   */
  public boolean hasPopularity() {
    return popularity!=null;
  }

  public static final class Builder extends Message.Builder<PUpdatePopularityMsg, Builder> {
    private Integer userID;

    private Integer popularity;

    public Builder() {
    }

    /**
     * 用户id
     */
    public Builder setUserID(Integer userID) {
      this.userID = userID;
      return this;
    }

    /**
     * 最新的人气值
     */
    public Builder setPopularity(Integer popularity) {
      this.popularity = popularity;
      return this;
    }

    @Override
    public PUpdatePopularityMsg build() {
      return new PUpdatePopularityMsg(userID, popularity, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_PUpdatePopularityMsg extends ProtoAdapter<PUpdatePopularityMsg> {
    public ProtoAdapter_PUpdatePopularityMsg() {
      super(FieldEncoding.LENGTH_DELIMITED, PUpdatePopularityMsg.class);
    }

    @Override
    public int encodedSize(PUpdatePopularityMsg value) {
      return ProtoAdapter.UINT32.encodedSizeWithTag(1, value.userID)
          + ProtoAdapter.UINT32.encodedSizeWithTag(2, value.popularity)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, PUpdatePopularityMsg value) throws IOException {
      ProtoAdapter.UINT32.encodeWithTag(writer, 1, value.userID);
      ProtoAdapter.UINT32.encodeWithTag(writer, 2, value.popularity);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public PUpdatePopularityMsg decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.setUserID(ProtoAdapter.UINT32.decode(reader)); break;
          case 2: builder.setPopularity(ProtoAdapter.UINT32.decode(reader)); break;
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
    public PUpdatePopularityMsg redact(PUpdatePopularityMsg value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
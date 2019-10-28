// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: Room.proto
package com.zq.live.proto.GrabRoom;

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

public final class QChangeMusicTag extends Message<QChangeMusicTag, QChangeMusicTag.Builder> {
  public static final ProtoAdapter<QChangeMusicTag> ADAPTER = new ProtoAdapter_QChangeMusicTag();

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_TAGID = 0;

  public static final String DEFAULT_TAGNAME = "";

  /**
   * 分类标识
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer tagID;

  /**
   * 分类名称
   */
  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  private final String tagName;

  public QChangeMusicTag(Integer tagID, String tagName) {
    this(tagID, tagName, ByteString.EMPTY);
  }

  public QChangeMusicTag(Integer tagID, String tagName, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.tagID = tagID;
    this.tagName = tagName;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.tagID = tagID;
    builder.tagName = tagName;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof QChangeMusicTag)) return false;
    QChangeMusicTag o = (QChangeMusicTag) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(tagID, o.tagID)
        && Internal.equals(tagName, o.tagName);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (tagID != null ? tagID.hashCode() : 0);
      result = result * 37 + (tagName != null ? tagName.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (tagID != null) builder.append(", tagID=").append(tagID);
    if (tagName != null) builder.append(", tagName=").append(tagName);
    return builder.replace(0, 2, "QChangeMusicTag{").append('}').toString();
  }

  public byte[] toByteArray() {
    return QChangeMusicTag.ADAPTER.encode(this);
  }

  public static final QChangeMusicTag parseFrom(byte[] data) throws IOException {
    QChangeMusicTag c = null;
       c = QChangeMusicTag.ADAPTER.decode(data);
    return c;
  }

  /**
   * 分类标识
   */
  public Integer getTagID() {
    if(tagID==null){
        return DEFAULT_TAGID;
    }
    return tagID;
  }

  /**
   * 分类名称
   */
  public String getTagName() {
    if(tagName==null){
        return DEFAULT_TAGNAME;
    }
    return tagName;
  }

  /**
   * 分类标识
   */
  public boolean hasTagID() {
    return tagID!=null;
  }

  /**
   * 分类名称
   */
  public boolean hasTagName() {
    return tagName!=null;
  }

  public static final class Builder extends Message.Builder<QChangeMusicTag, Builder> {
    private Integer tagID;

    private String tagName;

    public Builder() {
    }

    /**
     * 分类标识
     */
    public Builder setTagID(Integer tagID) {
      this.tagID = tagID;
      return this;
    }

    /**
     * 分类名称
     */
    public Builder setTagName(String tagName) {
      this.tagName = tagName;
      return this;
    }

    @Override
    public QChangeMusicTag build() {
      return new QChangeMusicTag(tagID, tagName, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_QChangeMusicTag extends ProtoAdapter<QChangeMusicTag> {
    public ProtoAdapter_QChangeMusicTag() {
      super(FieldEncoding.LENGTH_DELIMITED, QChangeMusicTag.class);
    }

    @Override
    public int encodedSize(QChangeMusicTag value) {
      return ProtoAdapter.UINT32.encodedSizeWithTag(1, value.tagID)
          + ProtoAdapter.STRING.encodedSizeWithTag(2, value.tagName)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, QChangeMusicTag value) throws IOException {
      ProtoAdapter.UINT32.encodeWithTag(writer, 1, value.tagID);
      ProtoAdapter.STRING.encodeWithTag(writer, 2, value.tagName);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public QChangeMusicTag decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.setTagID(ProtoAdapter.UINT32.decode(reader)); break;
          case 2: builder.setTagName(ProtoAdapter.STRING.decode(reader)); break;
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
    public QChangeMusicTag redact(QChangeMusicTag value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
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
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

/**
 * 某个时刻的分值状态
 */
public final class ScoreState extends Message<ScoreState, ScoreState.Builder> {
  public static final ProtoAdapter<ScoreState> ADAPTER = new ProtoAdapter_ScoreState();

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_USERID = 0;

  public static final Integer DEFAULT_SEQ = 0;

  public static final Long DEFAULT_MAINRANKING = 0L;

  public static final Long DEFAULT_SUBRANKING = 0L;

  public static final Long DEFAULT_CURRSTAR = 0L;

  public static final Long DEFAULT_MAXSTAR = 0L;

  public static final Long DEFAULT_PROTECTBATTLEINDEX = 0L;

  public static final Long DEFAULT_CURRBATTLEINDEX = 0L;

  public static final Long DEFAULT_MAXBATTLEINDEX = 0L;

  public static final Long DEFAULT_TOTALSCORE = 0L;

  public static final Long DEFAULT_CURREXP = 0L;

  public static final Long DEFAULT_MAXEXP = 0L;

  public static final String DEFAULT_RANKINGDESC = "";

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#UINT32"
  )
  private final Integer userID;

  /**
   * 分值状态的时间顺序, 数字越大越晚
   */
  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  private final Integer seq;

  /**
   * 主段位数值
   */
  @WireField(
      tag = 3,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long mainRanking;

  /**
   * 子段位数值
   */
  @WireField(
      tag = 4,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long subRanking;

  /**
   * 子段位当前星星数
   */
  @WireField(
      tag = 5,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long currStar;

  /**
   * 子段位星星数上限
   */
  @WireField(
      tag = 6,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long maxStar;

  /**
   * 掉段保护所需战力分值
   */
  @WireField(
      tag = 7,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long protectBattleIndex;

  /**
   * 当前战力分值
   */
  @WireField(
      tag = 8,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long currBattleIndex;

  /**
   * 战力分值上限
   */
  @WireField(
      tag = 9,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long maxBattleIndex;

  /**
   * 用在段位排行榜中的总分值
   */
  @WireField(
      tag = 10,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long totalScore;

  /**
   * 子段位当前经验值
   */
  @WireField(
      tag = 11,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long currExp;

  /**
   * 子段位经验值上限
   */
  @WireField(
      tag = 12,
      adapter = "com.squareup.wire.ProtoAdapter#INT64"
  )
  private final Long maxExp;

  /**
   * 白银歌者2段
   */
  @WireField(
      tag = 13,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  private final String rankingDesc;

  public ScoreState(Integer userID, Integer seq, Long mainRanking, Long subRanking, Long currStar,
      Long maxStar, Long protectBattleIndex, Long currBattleIndex, Long maxBattleIndex,
      Long totalScore, Long currExp, Long maxExp, String rankingDesc) {
    this(userID, seq, mainRanking, subRanking, currStar, maxStar, protectBattleIndex, currBattleIndex, maxBattleIndex, totalScore, currExp, maxExp, rankingDesc, ByteString.EMPTY);
  }

  public ScoreState(Integer userID, Integer seq, Long mainRanking, Long subRanking, Long currStar,
      Long maxStar, Long protectBattleIndex, Long currBattleIndex, Long maxBattleIndex,
      Long totalScore, Long currExp, Long maxExp, String rankingDesc, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.userID = userID;
    this.seq = seq;
    this.mainRanking = mainRanking;
    this.subRanking = subRanking;
    this.currStar = currStar;
    this.maxStar = maxStar;
    this.protectBattleIndex = protectBattleIndex;
    this.currBattleIndex = currBattleIndex;
    this.maxBattleIndex = maxBattleIndex;
    this.totalScore = totalScore;
    this.currExp = currExp;
    this.maxExp = maxExp;
    this.rankingDesc = rankingDesc;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.userID = userID;
    builder.seq = seq;
    builder.mainRanking = mainRanking;
    builder.subRanking = subRanking;
    builder.currStar = currStar;
    builder.maxStar = maxStar;
    builder.protectBattleIndex = protectBattleIndex;
    builder.currBattleIndex = currBattleIndex;
    builder.maxBattleIndex = maxBattleIndex;
    builder.totalScore = totalScore;
    builder.currExp = currExp;
    builder.maxExp = maxExp;
    builder.rankingDesc = rankingDesc;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ScoreState)) return false;
    ScoreState o = (ScoreState) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(userID, o.userID)
        && Internal.equals(seq, o.seq)
        && Internal.equals(mainRanking, o.mainRanking)
        && Internal.equals(subRanking, o.subRanking)
        && Internal.equals(currStar, o.currStar)
        && Internal.equals(maxStar, o.maxStar)
        && Internal.equals(protectBattleIndex, o.protectBattleIndex)
        && Internal.equals(currBattleIndex, o.currBattleIndex)
        && Internal.equals(maxBattleIndex, o.maxBattleIndex)
        && Internal.equals(totalScore, o.totalScore)
        && Internal.equals(currExp, o.currExp)
        && Internal.equals(maxExp, o.maxExp)
        && Internal.equals(rankingDesc, o.rankingDesc);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (userID != null ? userID.hashCode() : 0);
      result = result * 37 + (seq != null ? seq.hashCode() : 0);
      result = result * 37 + (mainRanking != null ? mainRanking.hashCode() : 0);
      result = result * 37 + (subRanking != null ? subRanking.hashCode() : 0);
      result = result * 37 + (currStar != null ? currStar.hashCode() : 0);
      result = result * 37 + (maxStar != null ? maxStar.hashCode() : 0);
      result = result * 37 + (protectBattleIndex != null ? protectBattleIndex.hashCode() : 0);
      result = result * 37 + (currBattleIndex != null ? currBattleIndex.hashCode() : 0);
      result = result * 37 + (maxBattleIndex != null ? maxBattleIndex.hashCode() : 0);
      result = result * 37 + (totalScore != null ? totalScore.hashCode() : 0);
      result = result * 37 + (currExp != null ? currExp.hashCode() : 0);
      result = result * 37 + (maxExp != null ? maxExp.hashCode() : 0);
      result = result * 37 + (rankingDesc != null ? rankingDesc.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (userID != null) builder.append(", userID=").append(userID);
    if (seq != null) builder.append(", seq=").append(seq);
    if (mainRanking != null) builder.append(", mainRanking=").append(mainRanking);
    if (subRanking != null) builder.append(", subRanking=").append(subRanking);
    if (currStar != null) builder.append(", currStar=").append(currStar);
    if (maxStar != null) builder.append(", maxStar=").append(maxStar);
    if (protectBattleIndex != null) builder.append(", protectBattleIndex=").append(protectBattleIndex);
    if (currBattleIndex != null) builder.append(", currBattleIndex=").append(currBattleIndex);
    if (maxBattleIndex != null) builder.append(", maxBattleIndex=").append(maxBattleIndex);
    if (totalScore != null) builder.append(", totalScore=").append(totalScore);
    if (currExp != null) builder.append(", currExp=").append(currExp);
    if (maxExp != null) builder.append(", maxExp=").append(maxExp);
    if (rankingDesc != null) builder.append(", rankingDesc=").append(rankingDesc);
    return builder.replace(0, 2, "ScoreState{").append('}').toString();
  }

  public byte[] toByteArray() {
    return ScoreState.ADAPTER.encode(this);
  }

  public static final ScoreState parseFrom(byte[] data) throws IOException {
    ScoreState c = null;
       c = ScoreState.ADAPTER.decode(data);
    return c;
  }

  public Integer getUserID() {
    if(userID==null){
        return DEFAULT_USERID;
    }
    return userID;
  }

  /**
   * 分值状态的时间顺序, 数字越大越晚
   */
  public Integer getSeq() {
    if(seq==null){
        return DEFAULT_SEQ;
    }
    return seq;
  }

  /**
   * 主段位数值
   */
  public Long getMainRanking() {
    if(mainRanking==null){
        return DEFAULT_MAINRANKING;
    }
    return mainRanking;
  }

  /**
   * 子段位数值
   */
  public Long getSubRanking() {
    if(subRanking==null){
        return DEFAULT_SUBRANKING;
    }
    return subRanking;
  }

  /**
   * 子段位当前星星数
   */
  public Long getCurrStar() {
    if(currStar==null){
        return DEFAULT_CURRSTAR;
    }
    return currStar;
  }

  /**
   * 子段位星星数上限
   */
  public Long getMaxStar() {
    if(maxStar==null){
        return DEFAULT_MAXSTAR;
    }
    return maxStar;
  }

  /**
   * 掉段保护所需战力分值
   */
  public Long getProtectBattleIndex() {
    if(protectBattleIndex==null){
        return DEFAULT_PROTECTBATTLEINDEX;
    }
    return protectBattleIndex;
  }

  /**
   * 当前战力分值
   */
  public Long getCurrBattleIndex() {
    if(currBattleIndex==null){
        return DEFAULT_CURRBATTLEINDEX;
    }
    return currBattleIndex;
  }

  /**
   * 战力分值上限
   */
  public Long getMaxBattleIndex() {
    if(maxBattleIndex==null){
        return DEFAULT_MAXBATTLEINDEX;
    }
    return maxBattleIndex;
  }

  /**
   * 用在段位排行榜中的总分值
   */
  public Long getTotalScore() {
    if(totalScore==null){
        return DEFAULT_TOTALSCORE;
    }
    return totalScore;
  }

  /**
   * 子段位当前经验值
   */
  public Long getCurrExp() {
    if(currExp==null){
        return DEFAULT_CURREXP;
    }
    return currExp;
  }

  /**
   * 子段位经验值上限
   */
  public Long getMaxExp() {
    if(maxExp==null){
        return DEFAULT_MAXEXP;
    }
    return maxExp;
  }

  /**
   * 白银歌者2段
   */
  public String getRankingDesc() {
    if(rankingDesc==null){
        return DEFAULT_RANKINGDESC;
    }
    return rankingDesc;
  }

  public boolean hasUserID() {
    return userID!=null;
  }

  /**
   * 分值状态的时间顺序, 数字越大越晚
   */
  public boolean hasSeq() {
    return seq!=null;
  }

  /**
   * 主段位数值
   */
  public boolean hasMainRanking() {
    return mainRanking!=null;
  }

  /**
   * 子段位数值
   */
  public boolean hasSubRanking() {
    return subRanking!=null;
  }

  /**
   * 子段位当前星星数
   */
  public boolean hasCurrStar() {
    return currStar!=null;
  }

  /**
   * 子段位星星数上限
   */
  public boolean hasMaxStar() {
    return maxStar!=null;
  }

  /**
   * 掉段保护所需战力分值
   */
  public boolean hasProtectBattleIndex() {
    return protectBattleIndex!=null;
  }

  /**
   * 当前战力分值
   */
  public boolean hasCurrBattleIndex() {
    return currBattleIndex!=null;
  }

  /**
   * 战力分值上限
   */
  public boolean hasMaxBattleIndex() {
    return maxBattleIndex!=null;
  }

  /**
   * 用在段位排行榜中的总分值
   */
  public boolean hasTotalScore() {
    return totalScore!=null;
  }

  /**
   * 子段位当前经验值
   */
  public boolean hasCurrExp() {
    return currExp!=null;
  }

  /**
   * 子段位经验值上限
   */
  public boolean hasMaxExp() {
    return maxExp!=null;
  }

  /**
   * 白银歌者2段
   */
  public boolean hasRankingDesc() {
    return rankingDesc!=null;
  }

  public static final class Builder extends Message.Builder<ScoreState, Builder> {
    private Integer userID;

    private Integer seq;

    private Long mainRanking;

    private Long subRanking;

    private Long currStar;

    private Long maxStar;

    private Long protectBattleIndex;

    private Long currBattleIndex;

    private Long maxBattleIndex;

    private Long totalScore;

    private Long currExp;

    private Long maxExp;

    private String rankingDesc;

    public Builder() {
    }

    public Builder setUserID(Integer userID) {
      this.userID = userID;
      return this;
    }

    /**
     * 分值状态的时间顺序, 数字越大越晚
     */
    public Builder setSeq(Integer seq) {
      this.seq = seq;
      return this;
    }

    /**
     * 主段位数值
     */
    public Builder setMainRanking(Long mainRanking) {
      this.mainRanking = mainRanking;
      return this;
    }

    /**
     * 子段位数值
     */
    public Builder setSubRanking(Long subRanking) {
      this.subRanking = subRanking;
      return this;
    }

    /**
     * 子段位当前星星数
     */
    public Builder setCurrStar(Long currStar) {
      this.currStar = currStar;
      return this;
    }

    /**
     * 子段位星星数上限
     */
    public Builder setMaxStar(Long maxStar) {
      this.maxStar = maxStar;
      return this;
    }

    /**
     * 掉段保护所需战力分值
     */
    public Builder setProtectBattleIndex(Long protectBattleIndex) {
      this.protectBattleIndex = protectBattleIndex;
      return this;
    }

    /**
     * 当前战力分值
     */
    public Builder setCurrBattleIndex(Long currBattleIndex) {
      this.currBattleIndex = currBattleIndex;
      return this;
    }

    /**
     * 战力分值上限
     */
    public Builder setMaxBattleIndex(Long maxBattleIndex) {
      this.maxBattleIndex = maxBattleIndex;
      return this;
    }

    /**
     * 用在段位排行榜中的总分值
     */
    public Builder setTotalScore(Long totalScore) {
      this.totalScore = totalScore;
      return this;
    }

    /**
     * 子段位当前经验值
     */
    public Builder setCurrExp(Long currExp) {
      this.currExp = currExp;
      return this;
    }

    /**
     * 子段位经验值上限
     */
    public Builder setMaxExp(Long maxExp) {
      this.maxExp = maxExp;
      return this;
    }

    /**
     * 白银歌者2段
     */
    public Builder setRankingDesc(String rankingDesc) {
      this.rankingDesc = rankingDesc;
      return this;
    }

    @Override
    public ScoreState build() {
      return new ScoreState(userID, seq, mainRanking, subRanking, currStar, maxStar, protectBattleIndex, currBattleIndex, maxBattleIndex, totalScore, currExp, maxExp, rankingDesc, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_ScoreState extends ProtoAdapter<ScoreState> {
    public ProtoAdapter_ScoreState() {
      super(FieldEncoding.LENGTH_DELIMITED, ScoreState.class);
    }

    @Override
    public int encodedSize(ScoreState value) {
      return ProtoAdapter.UINT32.encodedSizeWithTag(1, value.userID)
          + ProtoAdapter.INT32.encodedSizeWithTag(2, value.seq)
          + ProtoAdapter.INT64.encodedSizeWithTag(3, value.mainRanking)
          + ProtoAdapter.INT64.encodedSizeWithTag(4, value.subRanking)
          + ProtoAdapter.INT64.encodedSizeWithTag(5, value.currStar)
          + ProtoAdapter.INT64.encodedSizeWithTag(6, value.maxStar)
          + ProtoAdapter.INT64.encodedSizeWithTag(7, value.protectBattleIndex)
          + ProtoAdapter.INT64.encodedSizeWithTag(8, value.currBattleIndex)
          + ProtoAdapter.INT64.encodedSizeWithTag(9, value.maxBattleIndex)
          + ProtoAdapter.INT64.encodedSizeWithTag(10, value.totalScore)
          + ProtoAdapter.INT64.encodedSizeWithTag(11, value.currExp)
          + ProtoAdapter.INT64.encodedSizeWithTag(12, value.maxExp)
          + ProtoAdapter.STRING.encodedSizeWithTag(13, value.rankingDesc)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, ScoreState value) throws IOException {
      ProtoAdapter.UINT32.encodeWithTag(writer, 1, value.userID);
      ProtoAdapter.INT32.encodeWithTag(writer, 2, value.seq);
      ProtoAdapter.INT64.encodeWithTag(writer, 3, value.mainRanking);
      ProtoAdapter.INT64.encodeWithTag(writer, 4, value.subRanking);
      ProtoAdapter.INT64.encodeWithTag(writer, 5, value.currStar);
      ProtoAdapter.INT64.encodeWithTag(writer, 6, value.maxStar);
      ProtoAdapter.INT64.encodeWithTag(writer, 7, value.protectBattleIndex);
      ProtoAdapter.INT64.encodeWithTag(writer, 8, value.currBattleIndex);
      ProtoAdapter.INT64.encodeWithTag(writer, 9, value.maxBattleIndex);
      ProtoAdapter.INT64.encodeWithTag(writer, 10, value.totalScore);
      ProtoAdapter.INT64.encodeWithTag(writer, 11, value.currExp);
      ProtoAdapter.INT64.encodeWithTag(writer, 12, value.maxExp);
      ProtoAdapter.STRING.encodeWithTag(writer, 13, value.rankingDesc);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public ScoreState decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.setUserID(ProtoAdapter.UINT32.decode(reader)); break;
          case 2: builder.setSeq(ProtoAdapter.INT32.decode(reader)); break;
          case 3: builder.setMainRanking(ProtoAdapter.INT64.decode(reader)); break;
          case 4: builder.setSubRanking(ProtoAdapter.INT64.decode(reader)); break;
          case 5: builder.setCurrStar(ProtoAdapter.INT64.decode(reader)); break;
          case 6: builder.setMaxStar(ProtoAdapter.INT64.decode(reader)); break;
          case 7: builder.setProtectBattleIndex(ProtoAdapter.INT64.decode(reader)); break;
          case 8: builder.setCurrBattleIndex(ProtoAdapter.INT64.decode(reader)); break;
          case 9: builder.setMaxBattleIndex(ProtoAdapter.INT64.decode(reader)); break;
          case 10: builder.setTotalScore(ProtoAdapter.INT64.decode(reader)); break;
          case 11: builder.setCurrExp(ProtoAdapter.INT64.decode(reader)); break;
          case 12: builder.setMaxExp(ProtoAdapter.INT64.decode(reader)); break;
          case 13: builder.setRankingDesc(ProtoAdapter.STRING.decode(reader)); break;
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
    public ScoreState redact(ScoreState value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
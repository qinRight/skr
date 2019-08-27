package com.module.playways.race.room.model

import com.zq.live.proto.RaceRoom.RaceRoundInfo
import com.zq.live.proto.RaceRoom.SubRoundInfo

class RaceSubRoundInfo {
    var beginMs = 0
    var endMs = 0
    var choiceID = 0
    var overReason = 0
    var subRoundSeq = 0
    var userID = 0
}

internal fun parseFromSubRoundInfoPB(pb: SubRoundInfo):RaceSubRoundInfo {
    val model = RaceSubRoundInfo()
    model.userID = pb.userID
    model.subRoundSeq = pb.subRoundSeq
    model.choiceID = pb.choiceID
    model.beginMs = pb.beginMs
    model.endMs = pb.endMs
    model.overReason = pb.overReason.value
    return model
}
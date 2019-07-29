package com.module.feeds.make

import com.common.utils.U
import java.io.Serializable
import com.module.feeds.watch.model.FeedSongModel

class FeedsMakeModel(var songModel: FeedSongModel):Serializable{

    var uploadFeedsId: String? = null // 上传后生成的feedsid
    var uploadSongName: String? = null // 上传后歌曲名称
    var uploadSongDesc: String? = null // 上传后歌曲描述

    val composeSavePath: String = U.getAppInfoUtils().getFilePathInSubDir("feeds","feeds_compose.aac")
    var bgmDownloadProgress: Float = 0f

    var recordDuration: Long = 0 // 录音时间
    var recording: Boolean = false
    var beginRecordTs: Long = Long.MAX_VALUE

    val recordSavePath: String = U.getAppInfoUtils().getFilePathInSubDir("feeds","feeds_make.aac")

    var recordingClick: Boolean = false
    var withBgm  = false
}
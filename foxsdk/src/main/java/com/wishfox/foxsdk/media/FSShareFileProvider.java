package com.wishfox.foxsdk.media;

import androidx.core.content.FileProvider;

/** 独立 Provider，避免与宿主游戏的 FileProvider 声明发生合并冲突。 */
public final class FSShareFileProvider extends FileProvider { }

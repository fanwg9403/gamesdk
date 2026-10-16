package com.wishfox.foxsdk.media;

/** 面向用户的提示；不展示原始异常、路径或平台内部组件名。 */
public final class FSMediaFeedback {
    private FSMediaFeedback() { }

    public static String message(String code, String appName) {
        String app = appName == null ? "目标应用" : appName;
        switch (code) {
            case "APP_NOT_INSTALLED": return "请先安装" + app + "后再使用分享功能";
            case "APP_DISABLED": return app + "已停用，请启用后重试";
            case "TARGET_DISABLED": return app + "的外部图片分享入口已关闭，请保存图片后在应用内手动发布";
            case "TARGET_UNSUPPORTED": return app + "当前未提供可用的图片分享入口，请保存图片后在应用内手动发布";
            case "TARGET_ACCESS_DENIED": return app + "不允许外部应用打开此分享页面，请保存图片后在应用内手动发布";
            case "DOWNLOAD_FAILED":
            case "DOWNLOAD_TIMEOUT": return "图片下载失败，请检查网络或稍后重试";
            case "SANDBOX_WRITE_FAILED":
            case "GALLERY_WRITE_FAILED":
            case "CACHE_FAILED": return "图片保存失败，请检查手机剩余空间后重试";
            case "IMAGE_NOT_FOUND": return "图片已失效，请重新加载图片后再分享";
            case "INVALID_IMAGE_URL": return "图片地址已失效，请刷新页面后重试";
            case "FILE_TOO_LARGE": return "图片过大，请更换图片后重试";
            case "IMAGE_VALIDATION_FAILED":
            case "UNSUPPORTED_IMAGE_TYPE": return "图片格式不支持或内容已损坏，请重新加载后重试";
            case "BUSY":
            case "DUPLICATE_REQUEST": return "图片正在处理，请勿重复点击；完成后可重新发起";
            case "USER_CANCELLED":
            case "CANCELLED": return "已取消操作";
            case "HOST_NOT_RESUMED": return "操作已暂停，请返回游戏页面后重试";
            case "UNSUPPORTED_NO_PERMISSION": return "当前系统无法直接保存到相册，请使用系统文件保存功能";
            case "INVALID_ARGUMENT": return "操作信息不完整，请刷新页面后重试";
            case "IMAGE_URI_FAILED": return "图片暂时无法分享，请稍后重试";
            case "PREPARE_TIMEOUT": return "图片处理超时，请稍后重试";
            case "SHARE_CACHE_FULL": return "分享图片缓存已满，请稍后重试或保存图片后手动分享";
            default: return "操作未完成，请稍后重试；也可保存图片后手动分享";
        }
    }
}

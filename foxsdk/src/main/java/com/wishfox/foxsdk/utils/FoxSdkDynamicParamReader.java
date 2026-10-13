package com.wishfox.foxsdk.utils;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * 读取母包/分包 APK Signing Block 中的 WishFox 动态参数。
 *
 * <p>这里不依赖 Walle，避免把服务端 APK 改包工具带进 SDK AAR。读取逻辑遵循
 * APK Signing Block 的 little-endian 格式，只识别固定的 WishFox 自定义 ID，返回
 * 写入时的原始 UTF-8 字符串，不尝试解析 JSON。</p>
 */
public final class FoxSdkDynamicParamReader {

    private static final String TAG = "FoxSdk[DynamicParam]";

    /** 冻结的 WishFox 自定义 APK Signing Block ID（ASCII 形式的 W X F D）。 */
    public static final int WISHFOX_DYNAMIC_JSON_BLOCK_ID = 0x57584644;

    private static final long APK_SIG_BLOCK_MAGIC_LO = 0x20676953204b5041L;
    private static final long APK_SIG_BLOCK_MAGIC_HI = 0x3234206b636f6c42L;
    private static final int EOCD_MIN_SIZE = 22;
    private static final int MAX_EOCD_COMMENT_SIZE = 0xFFFF;
    private static final int APK_SIG_BLOCK_FOOTER_SIZE = 24;
    private static final int MAX_SIGNING_BLOCK_SIZE = 64 * 1024 * 1024;

    private FoxSdkDynamicParamReader() {
    }

    /** 从当前安装包读取动态参数；任何格式或 IO 异常均按未携带参数处理。 */
    public static String read(Context context) {
        if (context == null || context.getApplicationInfo() == null) {
            return null;
        }
        String sourceDir = context.getApplicationInfo().sourceDir;
        return sourceDir == null ? null : read(new File(sourceDir));
    }

    /** 从指定 APK 读取动态参数；返回值保持写入时的精确字符串。 */
    public static String read(File apkFile) {
        if (apkFile == null || !apkFile.isFile() || !apkFile.canRead()) {
            FoxSdkLogger.w(TAG, "读取 APK Signing Block 失败：APK 文件不存在或不可读");
            return null;
        }

        try (RandomAccessFile file = new RandomAccessFile(apkFile, "r")) {
            long centralDirectoryOffset = findCentralDirectoryOffset(file);
            if (centralDirectoryOffset < APK_SIG_BLOCK_FOOTER_SIZE) {
                FoxSdkLogger.w(TAG, "读取 APK Signing Block 失败：未找到 Central Directory");
                return null;
            }

            long footerOffset = centralDirectoryOffset - APK_SIG_BLOCK_FOOTER_SIZE;
            file.seek(footerOffset);
            long sizeInFooter = readLongLE(file);
            long magicLo = readLongLE(file);
            long magicHi = readLongLE(file);
            if (magicLo != APK_SIG_BLOCK_MAGIC_LO || magicHi != APK_SIG_BLOCK_MAGIC_HI) {
                FoxSdkLogger.d(TAG, "APK 未携带有效 APK Signing Block，动态参数不存在");
                return null;
            }

            long totalSize = sizeInFooter + 8L;
            if (sizeInFooter < APK_SIG_BLOCK_FOOTER_SIZE || totalSize > MAX_SIGNING_BLOCK_SIZE
                    || totalSize > centralDirectoryOffset) {
                FoxSdkLogger.w(TAG, "读取 APK Signing Block 失败：Signing Block 大小非法");
                return null;
            }

            long blockOffset = centralDirectoryOffset - totalSize;
            file.seek(blockOffset);
            long sizeInHeader = readLongLE(file);
            if (sizeInHeader != sizeInFooter) {
                FoxSdkLogger.w(TAG, "读取 APK Signing Block 失败：头尾大小不一致");
                return null;
            }

            int blockSize = safeInt(totalSize);
            byte[] block = new byte[blockSize];
            file.seek(blockOffset);
            file.readFully(block);
            String value = findValue(block);
            if (value == null) {
                FoxSdkLogger.d(TAG, "APK Signing Block 存在，但未找到动态参数 ID=0x"
                        + Integer.toHexString(WISHFOX_DYNAMIC_JSON_BLOCK_ID));
            } else {
                FoxSdkLogger.d(TAG, "已找到动态参数 ID=0x"
                        + Integer.toHexString(WISHFOX_DYNAMIC_JSON_BLOCK_ID)
                        + ", payloadBytes=" + value.getBytes(StandardCharsets.UTF_8).length);
            }
            return value;
        } catch (IOException | RuntimeException e) {
            FoxSdkLogger.e(TAG, "读取 APK Signing Block 异常", e);
            return null;
        }
    }

    private static String findValue(byte[] block) {
        ByteBuffer buffer = ByteBuffer.wrap(block).order(ByteOrder.LITTLE_ENDIAN);
        long sizeInHeader = buffer.getLong();
        long pairEnd = block.length - APK_SIG_BLOCK_FOOTER_SIZE;
        long offset = 8L;

        while (offset <= pairEnd - 8L) {
            if (offset > Integer.MAX_VALUE) {
                return null;
            }
            buffer.position((int) offset);
            long pairSize = buffer.getLong();
            long nextOffset = offset + 8L + pairSize;
            if (pairSize < 4L || nextOffset < offset || nextOffset > pairEnd) {
                return null;
            }

            int id = buffer.getInt();
            int valueLength = safeInt(pairSize - 4L);
            if (id == WISHFOX_DYNAMIC_JSON_BLOCK_ID) {
                byte[] value = new byte[valueLength];
                buffer.get(value);
                return new String(value, StandardCharsets.UTF_8);
            }
            offset = nextOffset;
        }
        return null;
    }

    private static long findCentralDirectoryOffset(RandomAccessFile file) throws IOException {
        long fileLength = file.length();
        long searchStart = Math.max(0L, fileLength - EOCD_MIN_SIZE - MAX_EOCD_COMMENT_SIZE);
        long searchEnd = fileLength - EOCD_MIN_SIZE;
        byte[] signature = new byte[4];
        for (long offset = searchEnd; offset >= searchStart; offset--) {
            file.seek(offset);
            file.readFully(signature);
            if ((signature[0] & 0xFF) == 0x50 && (signature[1] & 0xFF) == 0x4B
                    && (signature[2] & 0xFF) == 0x05 && (signature[3] & 0xFF) == 0x06) {
                file.seek(offset + 16L);
                long centralDirectoryOffset = readUnsignedIntLE(file);
                long commentLengthOffset = offset + 20L;
                file.seek(commentLengthOffset);
                int commentLength = readUnsignedShortLE(file);
                if (offset + EOCD_MIN_SIZE + commentLength <= fileLength) {
                    return centralDirectoryOffset;
                }
            }
        }
        return -1L;
    }

    private static long readLongLE(RandomAccessFile file) throws IOException {
        byte[] bytes = new byte[8];
        file.readFully(bytes);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private static long readUnsignedIntLE(RandomAccessFile file) throws IOException {
        byte[] bytes = new byte[4];
        file.readFully(bytes);
        return ((long) bytes[0] & 0xFFL)
                | (((long) bytes[1] & 0xFFL) << 8)
                | (((long) bytes[2] & 0xFFL) << 16)
                | (((long) bytes[3] & 0xFFL) << 24);
    }

    private static int readUnsignedShortLE(RandomAccessFile file) throws IOException {
        int low = file.readUnsignedByte();
        int high = file.readUnsignedByte();
        return low | (high << 8);
    }

    private static int safeInt(long value) {
        if (value < 0L || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("APK Signing Block value is too large");
        }
        return (int) value;
    }
}

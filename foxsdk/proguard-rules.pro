# WishFox SDK library-build rules. NOT exported to host apps.
# The release build currently has minifyEnabled false; leave that choice intact.
# If library-side shrinking is enabled later, apply the same reflection rules
# before publishing, otherwise host keep rules cannot restore removed symbols.
-include consumer-rules.pro

# A library build cannot see calls made by future consumers. Keep its complete
# public/protected ABI here, including nested callbacks and method descriptors.
# This broad ABI rule belongs ONLY in the library build, not consumer rules.
-keep,includedescriptorclasses public class com.wishfox.foxsdk.** {
    public *;
    protected *;
}

-keepattributes RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes SourceFile,LineNumberTable

# No blanket suppression of missing classes or global log stripping.
# Resolve dependency/ABI problems explicitly; do not ship -ignorewarnings.

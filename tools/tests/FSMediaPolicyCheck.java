import com.wishfox.foxsdk.media.FSMediaPolicy;
import java.util.Arrays;
import java.util.List;

public final class FSMediaPolicyCheck {
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
    public static void main(String[] args) {
        List<String> origins = Arrays.asList("https://media.example.com", "https://media.example.com:8443");
        check(FSMediaPolicy.allowed("https://media.example.com/a.mp4?signature=abc", origins), "signed URL");
        check(FSMediaPolicy.allowed("https://MEDIA.example.com:443/a.jpg", origins), "normalized host/default port");
        check(FSMediaPolicy.allowed("https://media.example.com:8443/a", origins), "explicit trusted port");
        String[] invalid = {"http://media.example.com/a", "https://media.example.com.evil.test/a",
            "https://media.example.com@evil.test/a", "https://user@media.example.com/a",
            "https://media.example.com:444/a", "//media.example.com/a", "/a", "file:///a",
            "data:image/png;base64,a", "https://media.example.com/a#fragment", "https://media.example.com:0/a",
            "https://media.example.com/a\nInjected: x", "https://media.example.com\\@evil.test/a"};
        for (String value : invalid) check(!FSMediaPolicy.allowed(value, origins), "reject " + value);
        char[] huge = new char[4097]; Arrays.fill(huge, 'a');
        check(!FSMediaPolicy.allowed(new String(huge), origins), "length limit");
        check(Arrays.equals(FSMediaPolicy.fit(1920,1080,1080,1920), new int[]{608,1080}), "portrait video landscape window");
        check(Arrays.equals(FSMediaPolicy.fit(1080,1920,1920,1080), new int[]{1080,608}), "landscape video portrait window");
        check(Arrays.equals(FSMediaPolicy.fit(1920,1080,1280,720), new int[]{1920,1080}), "matching landscape");
        check(Arrays.equals(FSMediaPolicy.fit(1080,1920,720,1280), new int[]{1080,1920}), "matching portrait");
        check(Arrays.equals(FSMediaPolicy.fit(0,0,1920,1080), new int[]{0,0}), "unmeasured view");
        check(Arrays.equals(FSMediaPolicy.fit(1000,1000,10000,100), new int[]{1000,10}), "extreme aspect");
        System.out.println("FSMediaPolicyCheck: 23 checks passed");
    }
}

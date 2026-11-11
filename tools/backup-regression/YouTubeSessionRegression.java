import com.lormusic.app.data.YouTubeSessionCookiesKt;
public class YouTubeSessionRegression {
    static int count;
    static void check(boolean value, String label) { if(!value)throw new AssertionError(label); count++; System.out.println("PASS "+label); }
    public static void main(String[] args) {
        String merged=YouTubeSessionCookiesKt.mergeYouTubeSessionCookies("SAPISID=music; A=x=y", "SAPISID=other; B=www", "B=root; C=z");
        check(merged.equals("SAPISID=music; A=x=y; B=www; C=z"),"music cookie jar takes precedence and values preserve equals");
        check(YouTubeSessionCookiesKt.mergeYouTubeSessionCookies(null,"", " bad ; empty= ; SID=ok").equals("SID=ok"),"empty jars and invalid cookie entries ignored");
        check(YouTubeSessionCookiesKt.isYouTubeMusicPage("https://music.youtube.com/"),"exact Music origin accepted");
        check(YouTubeSessionCookiesKt.isYouTubeMusicPage("HTTPS://MUSIC.YOUTUBE.COM:443/library"),"origin case and explicit TLS port accepted");
        check(!YouTubeSessionCookiesKt.isYouTubeMusicPage("https://music.youtube.com.evil.example/"),"lookalike origin rejected");
        check(!YouTubeSessionCookiesKt.isYouTubeMusicPage("https://music.youtube.com@evil.example/"),"userinfo origin spoof rejected");
        check(!YouTubeSessionCookiesKt.isYouTubeMusicPage("http://music.youtube.com/"),"unencrypted origin rejected");
        check(!YouTubeSessionCookiesKt.isYouTubeMusicPage("https://music.youtube.com:8443/"),"nonstandard port rejected");
        check(!YouTubeSessionCookiesKt.isYouTubeMusicPage(null),"missing page rejected");
        check(!YouTubeSessionCookiesKt.isYouTubeMusicPage("https://accounts.google.com/?next=https://music.youtube.com"),"Google page is not prematurely authenticated as Music");
        System.out.println("PASSED "+count+" session regression checks");
    }
}

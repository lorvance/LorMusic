import android.content.Context;
import android.content.SharedPreferences;
import com.lormusic.app.data.PreferencesRepository;
import java.lang.reflect.Proxy;
import java.util.*;
import org.json.*;

/** Runs the actual compiled repository with an in-memory SharedPreferences implementation. */
public class BackupRegression {
    static int passed;
    static final Map<String,Object> values = new LinkedHashMap<>();
    static SharedPreferences prefs;
    static boolean failNextCommit;
    static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        passed++; System.out.println("PASS " + label);
    }
    static SharedPreferences memory() {
        return (SharedPreferences) Proxy.newProxyInstance(BackupRegression.class.getClassLoader(), new Class[]{SharedPreferences.class}, (p,m,a) -> {
            switch(m.getName()) {
                case "getAll": return new LinkedHashMap<>(values);
                case "contains": return values.containsKey(a[0]);
                case "edit": {
                    Map<String,Object> changes = new LinkedHashMap<>();
                    return Proxy.newProxyInstance(BackupRegression.class.getClassLoader(), new Class[]{SharedPreferences.Editor.class}, (ep,em,ea) -> {
                        String name=em.getName();
                        if (name.startsWith("put")) { changes.put((String)ea[0],ea[1]); return ep; }
                        if (name.equals("remove")) { changes.put((String)ea[0],null); return ep; }
                        if (name.equals("commit") || name.equals("apply")) {
                            changes.forEach((k,v) -> {if(v==null)values.remove(k);else values.put(k,v);});
                            if (name.equals("commit") && failNextCommit) { failNextCommit=false; return false; }
                            return name.equals("commit") ? true : null;
                        }
                        throw new UnsupportedOperationException(name);
                    });
                }
                default:
                    if (m.getName().startsWith("get")) return values.getOrDefault((String)a[0],a[1]);
                    throw new UnsupportedOperationException(m.getName());
            }
        });
    }
    static PreferencesRepository repository() {
        return new PreferencesRepository(new Context() {
            public SharedPreferences getSharedPreferences(String name, int mode) { return prefs; }
        });
    }
    public static void main(String[] args) {
        prefs=memory();
        values.put("total_listening_ms", 123);
        values.put("equalizer_preset", 2L);
        values.put("playback_speed", 1);
        PreferencesRepository r=repository();
        check(r.getTotalListeningTimeMs()==123 && values.get("total_listening_ms") instanceof Long,"existing Int listening time migrates to Long");
        check(r.getEqualizerPreset()==2 && values.get("equalizer_preset") instanceof Integer,"legacy Long equalizer migrates to Int");
        check(r.getPlaybackSpeed()==1f && values.get("playback_speed") instanceof Float,"legacy integer speed migrates to Float");
        check(r.importBackupJson("{\"app\":\"LorMusic\",\"version\":\"1.6.4\",\"preferences\":{\"total_listening_ms\":7,\"playback_speed\":1,\"favorite_song_ids\":[\"one\"]}}"),"legacy wrapper import");
        check(values.get("total_listening_ms") instanceof Long && r.getTotalListeningTimeMs()==7,"JSON small integer retains schema Long");
        check(r.importBackupJson("{\"total_listening_ms\":9223372036854775807}"),"Long.MAX_VALUE import");
        check(r.getTotalListeningTimeMs()==Long.MAX_VALUE,"64 bit value retained exactly");
        r.addListeningTimeMs(1000);
        check(r.getTotalListeningTimeMs()==Long.MAX_VALUE,"listening accumulator does not overflow");
        values.put("yt_music_cookie_raw","secret_cookie"); values.put("sc_auth_token_raw","secret_token");
        r.saveBetaExpandingSearch(true);
        r.savePlaybackSpeed(1.25f);
        String exported=r.exportBackupJson();
        JSONObject backup=new JSONObject(exported);
        check(backup.getInt("formatVersion")==2 && backup.getString("appVersion").equals("1.7.2.1") && backup.getInt("versionCode")==58 && backup.has("createdAt"),"automatic build metadata and separate format version");
        check(!exported.contains("secret_cookie") && !exported.contains("secret_token") && !exported.contains("auth_token"),"credentials excluded from export");
        check(r.importBackupJson(exported) && r.getPlaybackSpeed()==1.25f && r.getBetaExpandingSearch(),"v2 round trip with float and beta setting");
        Map<String,Object> before=new LinkedHashMap<>(values);
        check(!r.importBackupJson("{\"total_listening_ms\":100,\"enable_backdrop_blur\":\"wrong\"}") && before.equals(values),"wrong type rejects entire import without partial writes");
        check(!r.importBackupJson("{") && before.equals(values),"invalid JSON preserves settings");
        check(!r.importBackupJson("{\"formatVersion\":3,\"settings\":{\"total_listening_ms\":0}}") && before.equals(values),"future format rejected");
        check(!r.importBackupJson("{\"formatVersion\":2,\"app\":\"LorMusic\"}"),"missing settings rejected");
        check(!r.importBackupJson("{\"arbitrary_file\":true}"),"unrelated JSON rejected");
        check(!r.importBackupJson("{\"total_listening_ms\":1.5}"),"fractional listening time rejected");
        check(!r.importBackupJson("{\"total_listening_ms\":9223372036854775808}"),"Long overflow rejected");
        check(!r.importBackupJson("{\"favorite_song_ids\":[3]}"),"non string set element rejected");
        check(r.importBackupJson("{\"total_listening_ms\":9,\"unknown_future\":{\"nested\":1},\"sc_auth_token_raw\":\"replace\"}"),"unknown and secret fields skipped safely");
        check(values.get("sc_auth_token_raw").equals("secret_token"),"import cannot overwrite live credentials");
        check(repository().getTotalListeningTimeMs()==9,"repository restart after restore");
        check(!r.importBackupJson("{\"total_listening_ms\":-1}"),"negative listening time rejected");
        check(!r.importBackupJson("{\"playback_speed\":0}"),"invalid playback speed rejected");
        check(!r.importBackupJson("{\"song_play_counts_json\":\"broken\"}"),"embedded JSON validated before commit");
        Map<String,Object> old = new LinkedHashMap<>(values);
        failNextCommit=true;
        check(!r.importBackupJson("{\"total_listening_ms\":555,\"beta_expanding_search\":false}") && old.equals(values),"failed disk commit rolls back memory settings");
        values.put("enable_backdrop_blur", "corrupt");
        values.put("equalizer_bass_boost", Long.MAX_VALUE);
        r=repository();
        check(r.getEnableBlur() && r.getEqualizerBass()==0 && r.getTotalListeningTimeMs()==9,"corrupt individual types repaired without clearing other settings");
        System.out.println("PASSED " + passed + " regression checks");
    }
}

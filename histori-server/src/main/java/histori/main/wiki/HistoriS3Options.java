package histori.main.wiki;

import cloudos.service.asset.S3AssetStorageService;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.util.HashMap;
import java.util.Map;

public class HistoriS3Options extends BaseMainOptions {

    public static final String USAGE_S3_ACCESS_KEY = "S3 Access Key";
    public static final String OPT_S3_ACCESS_KEY = "-A";
    public static final String LONGOPT_S3_ACCESS_KEY= "--s3-access-key";
    @Option(name=OPT_S3_ACCESS_KEY, aliases=LONGOPT_S3_ACCESS_KEY, usage=USAGE_S3_ACCESS_KEY, required=true)
    @Getter @Setter private String s3accessKey;

    public static final String USAGE_S3_SECRET_KEY = "Environment variable holding S3 Secret Key";
    public static final String OPT_S3_SECRET_KEY = "-S";
    public static final String LONGOPT_S3_SECRET_KEY= "--s3-secret-key-env-var";
    @Option(name=OPT_S3_SECRET_KEY, aliases=LONGOPT_S3_SECRET_KEY, usage=USAGE_S3_SECRET_KEY)
    @Getter @Setter private String s3secretKeyEnvVar = "S3_SECRET_KEY";

    public String getS3secretKey() { return System.getProperty(getS3secretKeyEnvVar()); }

    public static final String USAGE_S3_BUCKET = "S3 bucket";
    public static final String OPT_S3_BUCKET = "-B";
    public static final String LONGOPT_S3_BUCKET= "--s3-bucket";
    @Option(name=OPT_S3_BUCKET, aliases=LONGOPT_S3_BUCKET, usage=USAGE_S3_BUCKET, required=true)
    @Getter @Setter private String s3bucket;

    public static final String USAGE_S3_PREFIX = "S3 prefix";
    public static final String OPT_S3_PREFIX = "-P";
    public static final String LONGOPT_S3_PREFIX= "--s3-prefix";
    @Option(name=OPT_S3_PREFIX, aliases=LONGOPT_S3_PREFIX, usage=USAGE_S3_PREFIX)
    @Getter @Setter private String s3prefix = "wikipedia-archive-";

    public Map<String, String> getS3config() {
        final Map<String, String> config = new HashMap<>();
        config.put(S3AssetStorageService.PROP_ACCESS_KEY, getS3accessKey());
        config.put(S3AssetStorageService.PROP_SECRET_KEY, getS3secretKey());
        config.put(S3AssetStorageService.PROP_BUCKET, getS3bucket());
        config.put(S3AssetStorageService.PROP_PREFIX, getS3prefix());
        return config;
    }

}

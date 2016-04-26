package histori.main;

import histori.model.Nexus;
import histori.model.support.NexusRequest;
import histori.resources.BulkNexusResource;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.wizard.client.ApiClientBase;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static histori.ApiConstants.*;
import static org.cobbzilla.util.http.HttpUtil.upload;
import static org.cobbzilla.util.io.Decompressors.isDecompressible;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.urlEncode;

public class NexusImportMain extends HistoriApiMain<NexusImportOptions> {

    public static void main (String[] args) { main(NexusImportMain.class, args); }

    @Override protected void run() throws Exception {

        final NexusImportOptions options = getOptions();
        final File source = options.getFile();

        if (source.isDirectory()) {
            final Iterator iter = FileUtils.iterateFiles(source, new String[]{"json"}, true);
            while (iter.hasNext()) {
                final File f = (File) iter.next();
                try {
                    importNexus(f);
                } catch (Exception e) {
                    err("Error importing: " + abs(f) + ": " + e);
                }
            }

        } else if (isDecompressible(source)) {
            final ApiClientBase api = getApiClient();
            out(bulkImport(source, api.getBaseUri(), getApiHeaderTokenName(), api.getToken()));

        } else {
            importNexus(source);
        }
    }

    private void importNexus(File jsonFile) throws Exception {

        final ApiClientBase api = getApiClient();
        final NexusRequest request = fromJson(jsonFile, NexusRequest.class);
        final String path = NEXUS_ENDPOINT + "/" + urlEncode(request.getName());

        request.getTags().addTag("automated_entry_please_verify", "meta");
        request.setAuthoritative(true); // imports are always authoritative

        final Nexus nexus = fromJson(api.post(path, toJson(request)).json, Nexus.class);

        // scrub nexus of short tag names
        nexus.scrubShortTagNames();

        out("imported: "+request.getName()+" with "+nexus.getTags().getTagCount()+"/"+request.getTags().getTagCount()+" tags (version "+nexus.getVersion()+")");
    }

    public static String bulkImport(File tarball, String apiBase, String tokenName, String tokenValue) throws Exception {

        final Map<String, String> headers = new HashMap<>();
        headers.put(tokenName, tokenValue);
        headers.put(BulkNexusResource.BULK_TAG_PREFIX+"automated_entry_please_verify", "meta");
        return upload(apiBase+BULK_ENDPOINT+EP_FILE, tarball, headers).toString();
    }
}

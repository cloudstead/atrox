package histori.main;

import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.support.NexusRequest;
import histori.model.tag_schema.TagSchemaFieldType;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.wizard.client.ApiClientBase;

import java.io.File;
import java.util.Iterator;

import static histori.ApiConstants.EP_TAGS;
import static histori.ApiConstants.NEXUS_ENDPOINT;
import static histori.resources.NexusTagsResource.ENCODE_PREFIX;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.HttpStatusCodes.NOT_FOUND;
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
            Iterator iter = FileUtils.iterateFiles(source, new String[] {"json"}, true);
            while (iter.hasNext()) {
                final File f = (File) iter.next();
                try {
                    importNexus(f);
                } catch (Exception e) {
                    err("Error importing: "+abs(f)+": "+e);
                }
            }
        } else {
            importNexus(source);
        }
    }

    private void importNexus(File jsonFile) throws Exception {

        final ApiClientBase api = getApiClient();
        final NexusRequest request = fromJson(jsonFile, NexusRequest.class);
        final String path = NEXUS_ENDPOINT + "/" + urlEncode(request.getName());

        if (api.doGet(path).status == NOT_FOUND) {
            if (!request.hasNexusType()) request.setNexusType(request.getFirstEventType());
            request.addTag("automated_entry_please_verify", "meta");

            final Nexus created = fromJson(api.put(path, toJson(request)).json, Nexus.class);
            for (NexusTag tag : request.getTags()) {
                if (empty(tag.getTagName())) {
                    out("Empty tag: "+tag);
                    continue;

                } else if (!tag.getTagType().equals(TagSchemaFieldType.result.name()) && tag.getTagName().length() > 100) {
                    err("Suspiciously long tag name (title="+request.getName()+"), skipping: "+tag);
                    continue;

                } else if (tag.getTagName().length() < 2) {
                    err("Suspiciously short tag name (title="+request.getName()+"), skipping: "+tag);
                    continue;
                }
                String encoded = urlEncode(ENCODE_PREFIX + urlEncode(tag.getTagName()));
                api.put(path + EP_TAGS + "/" + encoded, toJson(tag));
            }
            out("imported: "+request.getName()+" (with "+request.getTagCount()+" tags)");
        } else {
            out("already imported: "+request.getName()+" (with "+request.getTagCount()+" tags)");
        }
    }
}

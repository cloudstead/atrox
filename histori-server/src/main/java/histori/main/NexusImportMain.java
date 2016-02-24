package histori.main;

import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.support.NexusRequest;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.wizard.client.ApiClientBase;

import java.io.File;
import java.util.Iterator;

import static histori.ApiConstants.EP_TAGS;
import static histori.ApiConstants.NEXUS_ENDPOINT;
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
            final Nexus created = fromJson(api.put(path, toJson(request)).json, Nexus.class);
            if (request.hasTags()) {
                for (NexusTag tag : request.getTags()) {
                    tag.setNexus(created.getUuid());
                    api.put(path + "/" + EP_TAGS + "/" + urlEncode(tag.getTagName()), toJson(tag));
                }
            }
        }
        out("imported: "+request.getName()+" (with "+request.getTagCount()+" tags)");
    }
}

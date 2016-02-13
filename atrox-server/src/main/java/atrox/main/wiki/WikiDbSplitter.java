package atrox.main.wiki;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;

import java.io.*;
import java.util.zip.GZIPOutputStream;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.FileUtil.abs;

public class WikiDbSplitter implements Closeable {

    @Getter @Setter File outputDir;
    @Getter @Setter private String outputPrefix;
    @Getter @Setter private int splitSize;

    private int allCount = 0;
    private int count = 0;
    private int fileCount = 0;
    private File tempFile;
    private File indexTempFile;

    private final JsonFactory jfactory;
    private JsonGenerator jGenerator;
    private JsonGenerator jindexGenerator;
    private OutputStream out;
    private OutputStream indexOut;

    public WikiDbSplitter(File outputDir, String outputPrefix, int splitSize, int fileNumber) {
        this.outputDir = outputDir;
        this.outputPrefix = outputPrefix;
        this.splitSize = splitSize;
        this.fileCount = fileNumber;

        jfactory = new JsonFactory();
        newOutFile();
    }

    public void newOutFile() {
        try {
            tempFile = File.createTempFile("WikiDbSplitter-", ".tmp.json");
            out = new GZIPOutputStream(new FileOutputStream(tempFile));
            jGenerator = jfactory.createGenerator(out, JsonEncoding.UTF8);
            jGenerator.setCodec(JsonUtil.FULL_MAPPER);
            jGenerator.writeStartArray();

            indexTempFile = File.createTempFile("WikiDbSplitter-index-", ".tmp.json");
            indexOut = new FileOutputStream(indexTempFile);
            jindexGenerator = jfactory.createGenerator(indexOut, JsonEncoding.UTF8);
            jindexGenerator.setCodec(JsonUtil.FULL_MAPPER);
            jindexGenerator.writeStartArray();

        } catch (Exception e) {
            die("Error: "+e, e);
        }
    }

    @Override public void close () throws IOException { finalizeOutFile(); }

    public void writeArticle(WikiArticle article) throws IOException {
        try {
            jGenerator.writeObject(article);
            jindexGenerator.writeString(article.getTitle());
        } catch (Exception e) {
            die("writeArticle("+article+"): "+e, e);
        }
        allCount++; count++;
        if (count >= splitSize) {
            finalizeOutFile();
            newOutFile();
        } else if (count % 1000 == 0) {
            progressSummary();
        }
    }

    private void progressSummary() { System.out.println("wrote "+count+" articles, total="+allCount); }

    public void finalizeOutFile() throws IOException {
        jGenerator.writeEndArray();
        jGenerator.close();
        out.close();

        jindexGenerator.writeEndArray();
        jindexGenerator.close();
        indexOut.close();

        String path = abs(outputDir) + "/" + outputPrefix + "_" + fileCount;
        final File outfile = new File(path + ".json.gz");
        final File indexFile = new File(path + "_index.json");
        FileUtil.mkdirOrDie(outfile.getParentFile());
        if (!tempFile.renameTo(outfile)) die("Error renaming "+abs(tempFile)+" -> " +abs(outfile));
        if (!indexTempFile.renameTo(indexFile)) die("Error renaming "+abs(indexTempFile)+" -> " +abs(indexFile));

        System.out.println(abs(outfile));
        progressSummary();
        count = 0;
        fileCount++;
    }

}

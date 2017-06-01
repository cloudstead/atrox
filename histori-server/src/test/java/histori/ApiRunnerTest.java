package histori;

import lombok.Getter;
import org.cobbzilla.wizard.client.script.ApiRunner;

public class ApiRunnerTest extends ApiClientTestBase {

    @Getter(lazy=true) private final ApiRunner apiRunner = initApiRunner();
    private ApiRunner initApiRunner() { return new ApiRunner(getApi(), apiDocsRunnerListener); }

}

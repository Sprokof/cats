package com.endava.cats.args;

import com.endava.cats.dsl.CatsDSLWords;
import com.endava.cats.util.CatsUtil;
import com.google.common.collect.Maps;
import io.github.ludovicianul.prettylogger.PrettyLogger;
import io.github.ludovicianul.prettylogger.PrettyLoggerFactory;
import lombok.Getter;
import lombok.Setter;
import picocli.CommandLine;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class FilesArguments {
    private static final String ALL = "all";
    private final PrettyLogger log = PrettyLoggerFactory.getLogger(this.getClass());
    private final Map<String, Map<String, String>> headers = new HashMap<>();
    private final Map<String, Map<String, String>> queryParams = new HashMap<>();
    private final Map<String, Map<String, String>> refData = new HashMap<>();
    private final CatsUtil catsUtil;

    private Map<String, Map<String, Object>> customFuzzerDetails = new HashMap<>();
    private Map<String, Map<String, Object>> securityFuzzerDetails = new HashMap<>();

    @CommandLine.Option(names = {"--urlParams"},
            description = "A comma separated list of @|bold name:value|@ pairs of parameters to be replaced inside the URLs", split = ",")
    @Getter
    private List<String> params;

    @CommandLine.Option(names = {"--headers"},
            description = "Specifies custom headers that will be passed along with request. This can be used to pass oauth or JWT tokens for authentication purposed for example")
    @Getter
    @Setter
    private File headersFile;

    @CommandLine.Option(names = {"-H"},
            description = "Specifies the headers that will be passed along with the request. When supplied it will be applied to ALL paths. For per-path control use the `--headers` arg that requires a file.")
    @Setter
    @Getter
    Map<String, String> headersMap;

    @CommandLine.Option(names = {"--queryParams"},
            description = "Specifies additional query parameters that will be passed along with request. This can be used to pass non-documented query params")
    @Getter
    @Setter
    private File queryFile;

    @CommandLine.Option(names = {"--refData"},
            description = "Specifies the file with fields that must have a fixed value in order for requests to succeed. " +
                    "If this is supplied when @|bold FunctionalFuzzer|@ is also enabled, the @|bold FunctionalFuzzer|@ will consider it a @|bold refData|@ template and try to replace any variables")
    @Getter
    @Setter
    private File refDataFile;

    @CommandLine.Option(names = {"--functionalFuzzerFile"},
            description = "Specifies the file used by the @|bold FunctionalFuzzer|@ that will be used to create user-supplied payloads and tests")
    @Getter
    @Setter
    private File customFuzzerFile;

    @CommandLine.Option(names = {"--securityFuzzerFile"},
            description = "Specifies the file used by the @|bold SecurityFuzzer|@ that will be used to inject special strings in order to exploit possible vulnerabilities")
    @Getter
    @Setter
    private File securityFuzzerFile;

    @CommandLine.Option(names = {"--createRefData"},
            description = "This is only applicable when enabling the @|bold FunctionalFuzzer |@. It will instruct the @|bold FunctionalFuzzer|@ to create a @|bold,underline --refData|@ file " +
                    "with all the paths defined in the @|bold,underline customFuzzerFile|@ and the corresponding @|bold output|@ variables")
    @Getter
    @Setter
    private boolean createRefData;

    public FilesArguments(CatsUtil cu) {
        this.catsUtil = cu;
    }

    /**
     * Loads all supplied files for --securityFuzzerFile, --customFuzzerFile, --refData, --urlParams and --headers.
     *
     * @throws IOException if something happens during file processing
     */
    public void loadConfig() throws IOException {
        loadSecurityFuzzerFile();
        loadCustomFuzzerFile();
        loadRefData();
        loadURLParams();
        loadHeaders();
        loadQueryParams();
    }

    public void loadSecurityFuzzerFile() throws IOException {
        if (securityFuzzerFile == null) {
            log.debug("No security custom Fuzzer file. SecurityFuzzer will be skipped!");
        } else {
            log.info("Security Fuzzer file {}", securityFuzzerFile.getAbsolutePath());
            securityFuzzerDetails = catsUtil.parseYaml(securityFuzzerFile.getAbsolutePath());
        }
    }

    public void loadCustomFuzzerFile() throws IOException {
        if (customFuzzerFile == null) {
            log.debug("No custom Fuzzer file. FunctionalFuzzer will be skipped!");
        } else {
            log.info("Custom Fuzzer file supplied {}", customFuzzerFile.getAbsolutePath());
            customFuzzerDetails = catsUtil.parseYaml(customFuzzerFile.getAbsolutePath());
        }
    }

    public void loadRefData() throws IOException {
        if (refDataFile == null) {
            log.debug("No reference data file was supplied! Payloads supplied by Fuzzers will remain unchanged!");
        } else {
            log.info("Loading reference data from: {}", refDataFile.getAbsolutePath());
            refData.putAll(catsUtil.loadYamlFileToMap(refDataFile.getAbsolutePath()));
            log.debug("Reference data file loaded successfully: {}", refData);
        }
    }

    public void loadQueryParams() throws IOException {
        if (queryFile == null) {
            log.debug("No queryParams file was supplied! No additional query parameters will be added!");
        } else {
            log.info("Query params file supplied {}", queryFile.getAbsolutePath());
            queryParams.putAll(catsUtil.loadYamlFileToMap(queryFile.getAbsolutePath()));
            log.debug("Query params file loaded successfully: {}", queryParams);
        }
    }

    public void loadURLParams() {
        if (params == null) {
            log.debug("No URL parameters supplied!");
        } else {
            log.info("URL parameters: {}", params);
        }
    }

    public void loadHeaders() throws IOException {
        if (headersFile == null) {
            log.debug("No headers file was supplied! No additional header will be added!");
        } else {
            log.info("Headers file supplied {}", headersFile.getAbsolutePath());
            headers.putAll(catsUtil.loadYamlFileToMap(headersFile.getAbsolutePath()));
            log.debug("Headers file loaded successfully: {}", headers);
        }
        if (headersMap != null) {
            headers.merge(ALL, headersMap, (stringStringMap, stringStringMap2) -> {
                Map<String, String> mergedMap = new HashMap<>(stringStringMap);
                mergedMap.putAll(stringStringMap2);
                return mergedMap;
            });
        }
    }


    /**
     * Returns a list of the --urlParams supplied. The list will contain strings in the {@code name:value} pairs.
     *
     * @return a name:value list of url params to be replaced when calling the service
     */
    public List<String> getUrlParamsList() {
        return Optional.ofNullable(this.params).orElse(Collections.emptyList());
    }

    /**
     * Replaces the current URL parameters with the {@code --urlParams} arguments supplied.
     * The URL parameters are expected to be included in curly brackets.
     *
     * @param startingUrl the base url; http://localhost:8080/{petId}
     * @return the initial URL with the parameters replaced with --urlParams supplied values
     */
    public String replacePathWithUrlParams(String startingUrl) {
        for (String line : this.getUrlParamsList()) {
            String[] urlParam = line.split(":");
            String pathVar = "{" + urlParam[0] + "}";
            if (startingUrl.contains(pathVar)) {
                startingUrl = startingUrl.replace(pathVar, urlParam[1]);
            }
        }
        return startingUrl;
    }

    /**
     * Returns the header values supplied in the --headers argument.
     * <p>
     * The map keys are the contract paths or {@code all} if headers are applied to all paths.
     *
     * @return a Map representation of the --headers file with paths being the Map keys
     */
    public Map<String, Map<String, String>> getHeaders() {
        return this.headers;
    }

    /**
     * Returns the reference data from the --refData file supplied as argument.
     * <p>
     * It returns the reference data for both the given path and the {@code all} key.
     *
     * @param currentPath the current API path
     * @return a Map with the supplied --refData
     */
    public Map<String, String> getRefData(String currentPath) {
        Map<String, String> currentPathRefData = Optional.ofNullable(this.refData.get(currentPath)).orElse(Maps.newHashMap());
        Map<String, String> allValues = Optional.ofNullable(this.refData.get(ALL)).orElse(Collections.emptyMap());

        currentPathRefData.putAll(allValues);
        return currentPathRefData;
    }

    /**
     * Returns a map with key-value pairs for all additional query parameters corresponding
     * to the given path as well as the ALL entry.
     *
     * @param path the given path
     * @return a key-value map with all additional query params
     */
    public Map<String, String> getAdditionalQueryParamsForPath(String path) {
        return queryParams.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(path) || entry.getKey().equalsIgnoreCase(CatsDSLWords.ALL))
                .map(Map.Entry::getValue).collect(HashMap::new, Map::putAll, Map::putAll);
    }

    /**
     * Returns a Map representation of the --customFuzzer. Map keys are test cases IDs.
     *
     * @return a Map representation of the --customFuzzer
     */
    public Map<String, Map<String, Object>> getCustomFuzzerDetails() {
        return this.customFuzzerDetails;
    }

    /**
     * Returns a Map representation of the --securityFuzzer. Map keys are test cases IDs.
     *
     * @return a Map representation of the --securityFuzzer
     */
    public Map<String, Map<String, Object>> getSecurityFuzzerDetails() {
        return this.securityFuzzerDetails;
    }

}

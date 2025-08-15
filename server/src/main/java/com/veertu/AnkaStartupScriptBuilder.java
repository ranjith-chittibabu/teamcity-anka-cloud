package com.veertu;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.log.Loggers;
import com.veertu.common.AnkaConstants;

public final class AnkaStartupScriptBuilder {

    private final String agentPath;
    private final String propertiesFilePath;
    private final String loadScriptPath;
    private String serverUrl;
    private String controllerUrl;

    private static final Logger LOG = Logger.getInstance(Loggers.CLOUD_CATEGORY_ROOT);

    private static final String TEMPLATE_RESOURCE = "/buildServerResources/agent_bootstrap_startup_script.template";
    private static final String SCRIPT_TEMPLATE = loadTemplateOnce();

    public AnkaStartupScriptBuilder(String agentPath, String serverUrl,String mgmtURL) {
        this.serverUrl = serverUrl;
        this.agentPath = agentPath;
        this.controllerUrl = mgmtURL;
        this.propertiesFilePath = String.format("%s/conf/buildAgent.properties", this.agentPath);
        this.loadScriptPath = String.format("%s/bin/mac.launchd.sh", this.agentPath);
    }

    public String buildStartupScript(Map<String, String> properties) {
        String propertiesBlock = buildPropertiesBlock(properties);
        return SCRIPT_TEMPLATE
            .replace("${AGENT_PATH}", agentPath)
            .replace("${PROP_FILE}", propertiesFilePath)
            .replace("${SERVER_URL}", serverUrl)
            .replace("${CONTROLLER_URL}", controllerUrl)
            .replace("${LOAD_SCRIPT}", loadScriptPath)
            .replace("{{PROPERTIES_BLOCK}}", propertiesBlock)
            .replace("${ENV_AGENT_NAME_KEY}", AnkaConstants.ENV_AGENT_NAME_KEY)
            .replace("${ENV_INSTANCE_ID_KEY}", AnkaConstants.ENV_INSTANCE_ID_KEY);
    }

    private String buildPropertiesBlock(Map<String,String> properties) {
        StringBuilder propertiesBlockBuilder = new StringBuilder();
        for (Map.Entry<String,String> e : properties.entrySet()) {
            String k = e.getKey();
            String v = e.getValue() == null ? "" : e.getValue();
            propertiesBlockBuilder.append("printf '%s=%s\\n' '")
            .append(escapeSingleQuote(k))
            .append("' '")
            .append(escapeSingleQuote(v))
            .append("' >> \"$PROP_FILE\"\n");
        }
        return propertiesBlockBuilder.toString();
    }

    private String escapeSingleQuote(String s) {
        return s.replace("'", "'\"'\"'");
    }

     private static String loadTemplateOnce() {
        try (InputStream in = AnkaStartupScriptBuilder.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Template resource not found: " + TEMPLATE_RESOURCE);
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return r.lines().collect(Collectors.joining("\n")) + "\n";
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load startup script template: " + e.getMessage(), e);
        }
    }
}
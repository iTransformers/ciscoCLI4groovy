/*
 * Expect4GroovyScriptLauncher.java
 *
 * Copyright 2016  iTransformers Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.itransformers.ciscocli4groovy;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import net.itransformers.expect4groovy.Expect4Groovy;import net.itransformers.expect4java.cliconnection.CLIConnection;
import net.itransformers.expect4java.cliconnection.impl.EchoCLIConnection;
import net.itransformers.expect4java.cliconnection.impl.RawSocketCLIConnection;
import net.itransformers.expect4java.cliconnection.impl.SshCLIConnection;
import net.itransformers.expect4java.cliconnection.impl.TelnetCLIConnection;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.Object;import java.lang.RuntimeException;import java.lang.String;import java.lang.System;import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

public class Expect4GroovyScriptLauncher {
    Binding binding;
    CLIConnection connection;
    GroovyScriptEngine gse;
    static Logger logger = Logger.getLogger(Expect4GroovyScriptLauncher.class);
    private static Status status;


    public static void main(String[] args) throws IOException, ResourceException, ScriptException,RuntimeException {

        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("StrictHostKeyChecking", "no");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("protocol", "telnet");
        params.put("username", "nbu");
        params.put("password", "nbu321!");
        params.put("enable-password", "nbu321!");
        params.put("address", "193.19.175.129");
        params.put("port", 23);
        params.put("timeout",30000);
        params.put("config",config);
        params.put("LOGGING_LEVEL","DEBUG");
        params.put("prompt",">");
        params.put("defaultTerminator","\r");
        params.put("powerUserPrompt","#");
        params.put("retries",2);

        Expect4GroovyScriptLauncher launcher = new Expect4GroovyScriptLauncher();

        Map<String, Object> loginResult = launcher.open(new String[]{"scripts/groovy/cisco/ios" + File.separator}, "cisco_login.groovy", params);


        if (loginResult.get("status").equals(2)) {
            logger.debug(loginResult);
        } else {
            Map<String, Object> cmdParams = new LinkedHashMap<String, Object>();
            cmdParams.put("evalScript", null);
            cmdParams.put("configCommand","ip route 10.200.1.0 255.255.255.0 192.0.2.1");
            cmdParams.put("mode", loginResult.get("mode"));
            cmdParams.put("hostname", loginResult.get("hostname"));
            cmdParams.putAll(params);
            Map<String, Object> result = null;

            result = launcher.sendCommand("cisco_sendConfigCommand.groovy", cmdParams);

            if(result.get("status").equals(1)){
                System.out.println(result.get("data"));
            }else{
                System.out.println(result.get("data"));
            }
            cmdParams.put("mode", result.get("mode"));
            cmdParams.put("hostname", loginResult.get("hostname"));

            String configTemplate = "interface loopback 101\n" +
                    "ip address 127.0.0.101 255.255.255.255\n" +
                    "description useless loopback\n"+
                    "no shutdown\n";


            cmdParams.put("configTemplate", configTemplate);
            cmdParams.remove("configCommand");

            result = launcher.sendCommand("cisco_sendConfigCommand.groovy",cmdParams);

            if(result.get("status").equals(1)){
                System.out.println(result.get("data"));
            }else{
                System.out.println("Config Template Failure: "+result.get("data"));
            }

            cmdParams.remove("configTempate");
            cmdParams.put("command", "show running-config");
            cmdParams.put("mode", result.get("mode"));

            cmdParams.put("evalScript","scripts/groovy/cisco/ios/cisco_config_eval.groovy");

            result = launcher.sendCommand("cisco_sendCommand.groovy",cmdParams);
            if(result.get("status").equals(1)){
                System.out.println(result.get("data"));
            }else{
                System.out.println("Command has not been executed sucessfully!!!: "+result.get("data"));
                System.out.println("ReportResult"+result.get("reportResult"));
                System.out.println("EvalResult"+result.get("evalResult"));

            }

            params.put("mode", result.get("mode"));




            result = launcher.close("cisco_logout.groovy", params);


             System.out.println(result.get("data"));
        }
    }


    public Object launch(String[] roots, String scriptName, Map<String, Object> params) throws IOException, ResourceException, ScriptException {
        CLIConnection conn = createCliConnection(params);
        try {
            conn.connect(params);
            Binding binding = new Binding();
            Expect4Groovy.createBindings(conn, binding, true);
            binding.setProperty("params", params);
            GroovyScriptEngine gse = new GroovyScriptEngine(roots);
            return gse.run(scriptName, binding);
        } finally {
            conn.disconnect();
        }
    }

    public Map<String, Object> sendCommand(String scriptName, Map<String, Object> params) throws ResourceException, ScriptException {
        Map<String, Object> allParams = (Map<String, Object>) binding.getProperty("params");
        allParams.putAll(params); //merge params with the one obtained from the other commands
        binding.setProperty("params", allParams);
        Map<String, Object> result = (Map<String, Object>) gse.run(scriptName, binding);
        return result;

    }

    public Map<String, Object> open(String[] roots, String scriptName, Map<String, Object> params) throws ResourceException, ScriptException {
        connection = createCliConnection(params);
        Map<String, Object> result = null;
        try {
            connection.connect(params);
            binding = new Binding();
            Expect4Groovy.createBindings(connection, binding, true);
            binding.setProperty("params", params);
            gse = new GroovyScriptEngine(roots);
            result = (Map<String, Object>) gse.run(scriptName, binding);
            if (result.get("status").equals("1")) {
                return result;
            } else {
                return result;
            }

        } catch (IOException ioe) {
            logger.info(ioe);
        }
        return result;
    }

    public Map<String, Object> close(String scriptName,Map<String, Object> params) throws ResourceException, ScriptException {
        try {
            Map<String, Object> allParams = (Map<String, Object>) binding.getProperty("params");

            binding.setProperty("params", allParams);
            Map<String, Object> result = (Map<String, Object>) gse.run(scriptName, binding);
            return result;
        } finally {
            try {
                connection.disconnect();
            } catch (IOException e) {
                logger.info(e);
            }
        }
    }

    private CLIConnection createCliConnection(Map<String, Object> params) {
        CLIConnection conn;
        if ("telnet".equals(params.get("protocol"))) {
            conn = new TelnetCLIConnection();
        } else if ("raw".equals(params.get("protocol"))) {
            conn = new RawSocketCLIConnection();
        } else if ("echo".equals(params.get("protocol"))) {
            conn = new EchoCLIConnection();
        } else {
            conn = new SshCLIConnection();
        }
        return conn;
    }
}
/*
 * TestExpect4Groovy.groovy
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






import net.itransformers.expect4groovy.Expect4GroovyScriptLauncher
import net.itransformers.expect4java.cliconnection.CLIConnection
import net.itransformers.expect4java.cliconnection.impl.CrossPipedCLIConnection
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

class TestExpect4Groovy {

    String path="src/test/groovy"

    @DataProvider(name = "providerSingle")
    public Object[][] provideAll() throws Exception {
        return [["test_regexp_nested_states.groovy"]];
    }

    @DataProvider(name = "providerAll")
    public Object[][] provide() throws Exception {

        String[] files = new File(path).list(new FilenameFilter(){
            @Override
            boolean accept(File dir, String name) {
                return name.endsWith(".groovy")
            }
        })
        Object[][] result = new Object[files.size()][1]
        files.eachWithIndex { el, i -> result[i][0] = el}
        return result;
    }

//    @Test(dataProvider = "providerAll")
//    public void doTestAll(String script) {
//        doTest(script)
//    }

//    @Test(dataProvider = "providerSingle")
//    public void doTestSingle(String script) {
//        doTest(script)
//    }

//    @Test
//    public void doTestInterfaceParser() {
//        doTest("interface_parser.groovy")
//    }
    @Test
    public void doTestGlob2() {
        doTest("../../../scripts/groovy/cisco/ios/cisco_login.groovy")
    }

    private void doTest(String script) {
        initLogger();
        CLIConnection simulatorConnection = new CrossPipedCLIConnection();
        CLIConnection scriptUnderTestConnection = new CrossPipedCLIConnection();
        Hashtable<String, String> config = new Hashtable<String, String>();
        config.put("StrictHostKeyChecking", "no");

        def params = ["input": simulatorConnection.inputStream()]
        params.put("username", "nbu");
        params.put("password", "nbu321!");
        params.put("enable-password", "nbu321!");
        params.put("address", "193.19.175.129");
        params.put("config",config);
        params.put("port", 22);
        params.put("timeout",30000);
        params.put("LOGGING_LEVEL","DEBUG");
        params.put("prompt",">");
        params.put("defaultTerminator","\r");
        params.put("powerUserPrompt","#");
        params.put("retries",2);

        def simulatorParams = ["input": scriptUnderTestConnection.inputStream()]
        simulatorParams.putAll(params);
        simulatorParams.put("input",scriptUnderTestConnection.inputStream());
        String[] roots = new String[1];
        roots[0] = path
        Object result1;
        new Thread(new Runnable() {
            @Override
            void run() {
                result1 = new Expect4GroovyScriptLauncher().launch(roots, "simulator.groovy", simulatorParams);
            }
        }).start()
            Object result = new Expect4GroovyScriptLauncher().launch(roots, script, params);

        Assert.assertTrue((Boolean) result, "The script is not executed successful")
    }

    private static void initLogger() {
        Logger logger = Logger.getLogger("")
        logger.setLevel(Level.ALL)
        ConsoleHandler handler = new ConsoleHandler() {
            {
                setOutputStream(System.out)
            }
        }
        handler.setLevel(Level.ALL)
        handler.setFormatter(new SimpleFormatter())
        logger.addHandler(handler)
    }
}

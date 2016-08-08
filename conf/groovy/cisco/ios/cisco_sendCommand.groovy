/*
 * cisco_sendCommand.groovy
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

/**
 * Created with IntelliJ IDEA.
 * User: niau
 * Date: 1/23/14
 * Time: 3:35 PM
 * To change this template use File | Settings | File Templates.
 */



status = ["success": 1, "failure": 2,"timeout":3]
modes = ["notLogedIn": 0, "logedIn": 1, "logedInPrivilege15Mode": 2, "logedInConfigMode": 3]
def mode = params.get("mode");


prompt = params.get("prompt");
powerUserPrompt = params.get("powerUserPrompt");
defaultTerminator = params.get("defaultTerminator");

hostname = params.get("hostname");

command=params.get("command");
def evalScript=params.get("evalScript");

def result;
if (command!=null && command !="") {
    if (mode == modes["logedInConfigMode"]){
        def privilegeModeResult
        if (cleanTerminal().get("status")==status["success"]){
            privilegeModeResult = goFromConfigToPrivilegedUserMode();

        }   else{
            result = ["status": status["failure"], "data": "Can't enter in privilege mode from config mode","mode": mode];
            return result;
        }

        if (privilegeModeResult["status"]== status["success"]){
            result = sendCommand(command,evalScript);

        } else{

            println("Can't enter in privilege mode from config mode");
            result = ["status": status["failure"], "data": "Can't enter in privilege mode from config mode","mode": mode];
            return result;
        }
    }

}


return result

def sendCommand(command,evalScript) {
    def returnStatus = 2
    def result = null

    long timeout=params.get("timeout");
    int retries=params.get("retries");

    int i = 0;

    def commandResult = null;
    def evalResult = null;
    while (i < retries && returnStatus!=status["success"]) {
        i++;


        send(command + defaultTerminator)
        expect([
                _timeout(timeout){
                    commandResult=commandResult+it.getBuffer();
                    returnStatus=status["timeout"];
                    it.exp_continue();
                },
                        _re(command + defaultTerminator+"\n"){
                            try{
                            if (evalScript != null) {
                                evalResult = evaluate(new File(evalScript))
                                commandResult=evalResult["commandResult"];

                            }
                            }catch (Exception ex){
                               ex.printStackTrace();
                            }

                },
                _re(params["hostname"] + powerUserPrompt) {
                    returnStatus = status["success"];
                    //commandResult = commandResult+it.getBuffer()
                }
                ]


        );




//        expect([
//                _timeout(timeout){
//                    commandResult=commandResult+it.getBuffer();
//                    returnStatus=status["timeout"];
//                },
//                _re(command + defaultTerminator+"\n"){
//                 it.exp_continue();
//
//                },
//                _re(params["hostname"] + powerUserPrompt) {
//                    returnStatus = status["success"]
//                    commandResult = commandResult+it.getBuffer()
//                }
//
//
//        ]);




//        expect([
//                _re(params["hostname"] + powerUserPrompt + "\$") {
//                    returnStatus = status["success"]
//                    commandResult = it.getBuffer()
//                }
//
//        ]);

        timeout=timeout+100;

    }
    if (evalResult!=null){
        return ["status": returnStatus, "data": commandResult,"mode":mode,"reportResult":evalResult["reportResult"],"evalResult":evalResult["commandResult"]]
    }else{
        return ["status": returnStatus, "data": commandResult,"mode":mode];
    }

}


def goFromConfigToPrivilegedUserMode() {
    def returnStatus = status["failure"];
    ciscoDeviceMode=modes["logedInConfigMode"];

    long timeout=params.get("timeout");
    int retries=params.get("retries");

    int i = 0;

    if (mode == modes["logedInConfigMode"]) {

        while (i < retries && returnStatus!=status["success"]) {
            i++;

            send "end" + defaultTerminator ;

            expect ([
                    _re("end"+defaultTerminator){
                        it.exp_continue();
                    },
                    _timeout(timeout){
                        returnStatus=status["timeout"];
                    },
                    _re(hostname + powerUserPrompt) {
                        ciscoDeviceMode = modes["logedInPrivilege15Mode"];
                        mode =  modes["logedInPrivilege15Mode"];
                        returnStatus = status["success"];
                     }
            ]);
            timeout=timeout+100;

        }

    } else {
        println("----------Error Not logged in Privilege User Mode!----------")

    }
    return ["status": returnStatus, "mode": ciscoDeviceMode];
}


def sendPrivilegeModeCommand(command) {
    def returnFlag = status["failure"];
    String result = null

    if (cleanTerminal() == status["success"]) {


        if (mode == modes["logedInPrivilege15Mode"]) {
//            expect([
//                    _re(".*") {
//                        println("----------Buffer Before Config: " + it.getBuffer());
//                    }, _timeout(1000) {
//
//            }]);

            send command + defaultTerminator;


            expect([
                    _re(command+defaultTerminator+"\n"){
                        result= result.concat(it.getBuffer());
                        println("----------Match echo of the command: " + command + "----------" );
                        it.exp_continue();
                    },

                    _re(hostname + powerUserPrompt) {
                        returnFlag = status["success"]
                        //  println("----------Commnd Output: " + it.getBuffer() + "----------")
                        result = it.getBuffer();
                    },

            ]);

        } else {
            println("----------Error Not logged in Privilege User Mode!----------")

        }
    }
    return ["status": returnFlag, "data": result]


}

def sendUnprivilegedModeCommand(command) {
//Send verification command
    def returnFlag = status["failure"];

    String result = null
    if (mode == modes["logedIn"]) {
        //    send "show run" + "\r"
        send command + defaultTerminator

        expect _re(hostname + prompt + "\$") {
            returnFlag = status["success"]
            result = it.getBuffer()
        }

    } else {
        println("----------Error Not logged in  User Mode!----------")

    }
    return ["status": returnFlag, "data": result]


}

def cleanTerminal() {
    def returnFlag = status["failure"];
    println("----------Sending \\r to the terminal----------")
    def result;
    send defaultTerminator;
    expect ([
            _re(defaultTerminator){
                println("----------Matching the echo of \\r----------");
                it.exp_continue();
            },
            _re(hostname+"\\(config\\)"+powerUserPrompt) {
                println("----------Matching the "+hostname+"(config)"+powerUserPrompt + "----------");

                returnFlag = status["success"];
            }
    ]);

    return ["status": returnFlag]
}


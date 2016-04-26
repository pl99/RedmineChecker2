package ru.advantum.rmchecker
/**
 * Created by Kotin on 17.02.2016.
 */

import groovy.json.JsonSlurper


class CfgReader {
    def ConfigObject config
    private final String fileName="redminechecker.json"

    ConfigObject readConfiguration(){
// get the properties from the config file
        def URL urlFile = new File(fileName).toURI().toURL()
        config = new JsonSlurper().parse(urlFile)
        return config
    }

    static void main(String... args) {
        new CfgReader().readConfiguration()
    }

}
/**
 *  Meater Probe
 *
 *  Copyright\u00A9 2021 Justin Leonard
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 * v0.1.0 - Initial Beta
**/

import java.text.SimpleDateFormat
import groovy.transform.Field
import groovy.time.TimeCategory
import groovy.json.*
import groovy.json.JsonBuilder

metadata
{
    definition(name: "Meater Probe", namespace: "lnjustin", author: "Justin Leonard", importUrl: "")
    {       
        capability "Switch"
        
        attribute "cookName", "string"
        attribute "cookState", "string"        
        attribute "cookTargetTemp", "number"
        attribute "cookPeakTemp", "number"
        attribute "cookTimeElapsed", "number"
        attribute "cookTimeElapsedStr", "string"
        attribute "cookTimeRemaining", "number"
        attribute "cookTimeRemainingStr", "string"
        
        command "setInternalTemp", ["number"]
        command "setAmbientTemp", ["number"]
    }
}

def uninstalled()
{
    deleteChildren()
}

def deleteChildren()
{
    deleteChildDevice(device.deviceNetworkId + "Internal") 
    deleteChildDevice(device.deviceNetworkId + "Ambient") 
}

def createChildren() {
    def child = getChildDevice(device.deviceNetworkId + "Internal")    
    if (!child) {
       String childNetworkID = device.deviceNetworkId + "Internal"
       String name = "Internal Temperature Sensor"
       def newChild = addChildDevice("hubitat", "Virtual Temperature Sensor", childNetworkID, [label:name, isComponent:true, name:name])
    } 
    child = getChildDevice(device.deviceNetworkId + "Ambient")    
    if (!child) {
       String childNetworkID = device.deviceNetworkId + "Ambient"
       String name = "Ambient Temperature Sensor"
       def newChild = addChildDevice("hubitat", "Virtual Temperature Sensor", childNetworkID, [label:name, isComponent:true, name:name])
    } 
}


def on() {
    sendEvent(name: "switch", value: "on")
}

def off() {
    sendEvent(name: "switch", value: "off")
}

def offline() {
    setInternalTemp(0)
    setAmbientTemp(0)
    sendEvent(name: "cookName", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookState", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookTargetTemp", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookPeakTemp", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookTimeElapsed", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookTimeRemaining", value: "Device Offline", isStateChange: true)     
    sendEvent(name: "cookTimeElapsedStr", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookTimeRemainingStr", value: "Device Offline", isStateChange: true)  
}

def setInternalTemp(temp) {
    def child = getChildDevice(device.deviceNetworkId + "Internal")    
    if (child) {   
        child.setTemperature(temp)
    }
    else log.error "No internal sensor device found."
}

def setAmbientTemp(temp) {
    def child = getChildDevice(device.deviceNetworkId + "Ambient")    
    if (child) {    
        child.setTemperature(temp)
    }
    else log.error "No ambient sensor device found."    
}

/**
 *  Meater Parent Device
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
    definition(name: "Meater Parent Device", namespace: "lnjustin", author: "Justin Leonard", importUrl: "")
    {
        capability "Refresh"
        capability "Initialize"
        capability "Switch"
    }
}

preferences
{
    section
    {
        
        input name: "email", type: "String", title: "Meater Account Email", required: true
        input name: "password", type: "String", title: "Meater Account Password", required: true
        input name: "activeRefreshInterval", type: "Number", title: "Refresh Interval (Secs) While Cooking", required: true, defaultValue: 30
        input name: "inactiveRefreshInterval", type: "Number", title: "Refresh Interval (Secs) While Not Cooking", required: true, defaultValue: 120
        input name: "units", type: "enum", options:["Celcius", "Fahrenheit"], title: "Units", defaultValue: "Fahrenheit"
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def getInactiveRefreshSetting() {
    return inactiveRefreshInterval != null ? inactiveRefreshInterval : 120    
}

def getActiveRefreshSetting() {
    return activeRefreshInterval != null ? activeRefreshInterval : 60    
}
def installed() {
    logDebug("installed()")
    
    initialize()
}

def updated()
{
    configure()
}

def configure()
{    
    refresh()
}

def initialize() {
    logDebug("initialize()")
    unschedule()
    if (email != null && password != null) {
        login()
        getDevices()
    }
}


def uninstalled()
{
    deleteChildren()
}

def login() {
    def body = new JsonOutput().toJson([email:email, password:password])
    def response = sendApiRequest("login", body, "post")  
    if (response != null) {
        if (response.data.data != null && response.data.data.token != null) {
            state.token = response.data.data.token
        }
        if (response.data.data != null && response.data.data.userId != null) {
            state.userId = response.data.data.userId
        }
    }
}

def getDevices() {
    def response = sendApiRequest("devices")  
    logDebug("Devices response: ${response.data}")
    
    if (response != null) {
        if (response.data.data != null) {
            def devices = response.data.data.devices
            def deviceCount = 0
            def deviceIDs = [] // list of devices that are online
            for (deviceData in devices) {
                deviceCount++
                def deviceId = deviceData.id
                 deviceIDs.add("MeaterDevice${deviceId}")
                 def child = getChildDevice("MeaterDevice${deviceId}")    
                 if (!child) {
                     String childNetworkID = "MeaterDevice${deviceId}"
                     String name = "Meater Device ${deviceCount}"
                     def newChild = addChildDevice("lnjustin", "Meater Child Device", childNetworkID, [label:name, isComponent:true, name:name])
                     updateDevice(newChild, deviceData)
                 } 
                else updateDevice(child, deviceData)
            }
            
            def anyChildOn = false
            for (child in getChildDevices()) {
                String childID = child.deviceNetworkId
                if (deviceIDs.count(childID) >= 1) {
                    child.on()
                    anyChildOn = true
                }
                else {
                    child.off()
                    child.offline()
                }
            }
            // set on/off status of parent device to reflect whether any child is online
            if (anyChildOn) {
                on()
                runIn(getActiveRefreshSetting(), getDevices)
            }
            else {
                off()
                runIn(getInactiveRefreshSetting(), getDevices)
            }
        }
    }
}

def updateDevice(device, deviceData) {
    device.sendEvent(name: "internalTemp", value: convertReceivedUnits(deviceData.temperature.internal), isStateChange: true)
    device.sendEvent(name: "ambientTemp", value: convertReceivedUnits(deviceData.temperature.ambient), isStateChange: true)
    device.sendEvent(name: "cookName", value: (deviceData.cook != null) ? deviceData.cook.name : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookState", value: (deviceData.cook != null) ? deviceData.cook.state : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookTargetTemp", value: (deviceData.cook != null) ? convertReceivedUnits(deviceData.cook.temperature.target) : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookPeakTemp", value: (deviceData.cook != null) ? convertReceivedUnits(deviceData.cook.temperature.peak) : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookTimeElapsed", value: (deviceData.cook != null) ? deviceData.cook.time.elapsed : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookTimeRemaining", value: (deviceData.cook != null) ? deviceData.cook.time.remaining : "No Cook Data", isStateChange: true)    
}

def convertReceivedUnits(value) {
    if (units == "Fahrenheit") {
        return Math.round((value * (9/5)) +32)
    }
    else if (units == "Celcius") {
        return value
    }
}

def deleteChild(deviceId)
{
    deleteChildDevice("MeaterDevice${deviceId}")
}

def deleteChildren()
{
    for(child in getChildDevices())
    {
        deleteChildDevice(child.deviceNetworkId)
    }
}

def on() {
    sendEvent(name: "switch", value: "on")
}

def off() {
    sendEvent(name: "switch", value: "off")
}


def sendApiRequest(path, body = null, type = "get")
{
    def params = [
        uri: "https://public-api.cloud.meater.com/v1/",
        path: path,
    ]
    
    if (body != null)  params.body = body
    if (type == "get" && state.token != null) params.headers = ["Content-Type": "application/json", "Authorization": "Bearer ${state.token}"]
    else params.headers = ["Content-Type": "application/json"]

    def response = null
    logDebug("Api Call: ${params}")
    try
    {
        if(type == "post") {
            httpPostJson(params) { resp ->
                response = resp
            }
        }
        else if(type == "get") {
            httpGet(params) { resp ->
                response = resp
            }
        }                
    }
    catch (Exception e)
    {
        log.warn "sendApiRequest() failed: ${e.message}"
        return "Error: ${e.message}"
    }   
    return response
}


def refresh()
{

}

def logDebug(msg) 
{
    if (logEnable)
    {
        log.debug(msg)
    }
}  


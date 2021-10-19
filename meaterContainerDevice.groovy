/**
 *  Meater Container Device
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
    definition(name: "Meater Container Device", namespace: "lnjustin", author: "Justin Leonard", importUrl: "")
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
    
    def devices = null
    def deviceCount = 0
    def deviceIDs = [] // list of devices that are online
    
    if (response != null) {
        if (response.data.data != null) {
            devices = response.data.data.devices
            for (deviceData in devices) {
                deviceCount++
                def deviceId = deviceData.id
                deviceIDs.add("MeaterProbe${deviceId}")
                def child = getChildDevice("MeaterProbe${deviceId}")    
                if (!child) {
                     String childNetworkID = "MeaterProbe${deviceId}"
                     String name = "Meater Probe ${deviceCount}"
                     def newChild = addChildDevice("lnjustin", "Meater Probe", childNetworkID, [label:name, isComponent:true, name:name])
                     updateDevice(newChild, deviceData)
                } 
                else updateDevice(child, deviceData)
            }
        }
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

def updateDevice(device, deviceData) {
    device.createChildren()
    if (deviceData != null && deviceData.temperature != null) {
        device.setInternalTemp(convertReceivedUnits(deviceData.temperature.internal))
        device.setAmbientTemp(convertReceivedUnits(deviceData.temperature.ambient))
    }
    else {
        device.setInternalTemp(0)
        device.setAmbientTemp(0)
    }    
    device.sendEvent(name: "cookName", value: (deviceData != null && deviceData.cook != null) ? deviceData.cook.name : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookState", value: (deviceData != null && deviceData.cook != null) ? deviceData.cook.state : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookTargetTemp", value: (deviceData != null && deviceData.cook != null) ? convertReceivedUnits(deviceData.cook.temperature.target) : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookPeakTemp", value: (deviceData != null && deviceData.cook != null) ? convertReceivedUnits(deviceData.cook.temperature.peak) : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookTimeElapsed", value: (deviceData != null && deviceData.cook != null) ? deviceData.cook.time.elapsed : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookTimeRemaining", value: (deviceData != null && deviceData.cook != null) ? deviceData.cook.time.remaining : "No Cook Data", isStateChange: true)    
    device.sendEvent(name: "cookTimeElapsedStr", value: (deviceData != null && deviceData.cook != null) ? formatTimeMins(deviceData.cook.time.elapsed) : "No Cook Data", isStateChange: true)
    device.sendEvent(name: "cookTimeRemainingStr", value: (deviceData != null && deviceData.cook != null) ? formatTimeMins(deviceData.cook.time.remaining) : "No Cook Data", isStateChange: true)    
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
    deleteChildDevice("MeaterProbe${deviceId}")
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

def formatTimeMins(duration) {
    def hours = (duration / 3600).intValue()
    def mins = ((duration % 3600) / 60).intValue()
    return String.format("%01d:%02d", hours, mins)
}

def logDebug(msg) 
{
    if (logEnable)
    {
        log.debug(msg)
    }
}  

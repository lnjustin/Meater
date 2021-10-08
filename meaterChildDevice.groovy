/**
 *  Meater Child Device
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
    definition(name: "Meater Child Device", namespace: "lnjustin", author: "Justin Leonard", importUrl: "")
    {       
        capability "Switch"
        
        attribute "internalTemp", "number"
        attribute "ambientTemp", "number"
        
        attribute "cookName", "string"
        attribute "cookState", "string"        
        attribute "cookTargetTemp", "number"
        attribute "cookPeakTemp", "number"
        attribute "cookTimeElapsed", "number"
        attribute "cookTimeRemaining", "number"
    }
}


def on() {
    sendEvent(name: "switch", value: "on")
}

def off() {
    sendEvent(name: "switch", value: "off")
}

def offline() {
    sendEvent(name: "internalTemp", value: "Device Offline", isStateChange: true)
    sendEvent(name: "ambientTemp", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookName", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookState", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookTargetTemp", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookPeakTemp", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookTimeElapsed", value: "Device Offline", isStateChange: true)
    sendEvent(name: "cookTimeRemaining", value: "Device Offline", isStateChange: true)     
}


/*
* Copyright 2015 Mike Wilson
* // https://community.smartthings.com/t/linear-z-wave-siren-device-type-needed/19700/38
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
*/
metadata {
definition (name: "Linear Z-Wave Siren v3", namespace: "miketx", author: "Mike Wilson") {
capability "Actuator"
capability "Alarm"
capability "Battery"
capability "Polling"
capability "Refresh"
capability "Sensor"
capability "Switch"
command "setstrobe"
command "setsiren"
command "setboth"
//Supported Command Classes
//0x20-Basic ,0x25-Binary Switch ,0x70-Configuration , 0x72-Manufacturer Specific ,0x86-Version
// fingerprint inClusters: "0x20,0x25,0x70,0x72,0x86"
fingerprint deviceId:"0x1000", inClusters: "0x25,0x70,0x72,0x80,0x86"
// 0 0 0x1000 0 0 0 4 0x25 0x70 0x72 0x86
}
simulator {
// reply messages
reply "2001FF,2002": "command: 2002, payload: FF"
reply "200100,2002": "command: 2002, payload: 00"
reply "200121,2002": "command: 2002, payload: 21"
reply "200142,2002": "command: 2002, payload: 42"
reply "2001FF,delay 3000,200100,2002": "command: 2002, payload: 00"
}
tiles {
standardTile("alarm", "device.alarm", width: 2, height: 2) {
log.debug "Adjusting alarm state"

    state "both", label:'alarm!', action:"setsiren", icon:"st.alarm.alarm.alarm", backgroundColor:"#e86d13"
    state "siren", label:'siren!', action:"setstrobe", icon:"st.alarm.alarm.alarm", backgroundColor:"#e86d13"
    state "strobe", label:'strobe!', action:"setboth", icon:"st.alarm.alarm.alarm", backgroundColor:"#e86d13"						
}
    standardTile("off", "device.alarm", inactiveLabel: false, decoration: "flat") {
		state "default", label:'Off', action:"off"
    } 
    standardTile("on", "device.alarm", inactiveLabel: false, decoration: "flat") {
		state "default", label:'On', action:"on"
    } 
    valueTile("battery", "device.battery", inactiveLabel: false, decoration: "flat") { 
		state "battery", label:'${currentValue}% battery', unit:""
	}
    standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
		state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
	}

main "alarm"
details(["alarm","off","on","battery","refresh"])
}

preferences {
input "autoStopTime", "enum", title: "Disarm Time",required:true,displayDuringSetup:true, options: ["30","60","120","Infinite"],default:'30'
}
}
def updated() {
def autoStopTimeParameter = 0
if (autoStopTime == '30') {
autoStopTimeParameter = 0
} else if( autoStopTime == '60'){
autoStopTimeParameter = 1
} else if (autoStopTime == '120') {
autoStopTimeParameter = 2
} else if (autoStopTime == 'Infinite') {
autoStopTimeParameter = 3
}
log.debug "AutoStopTime - ${autoStopTimeParameter}"
zwave.configurationV1.configurationSet(parameterNumber: 1, size: 1, configurationValue: [autoStopTimeParameter])
}
//println org.codehaus.groovy.runtime.InvokerHelper.getVersion()

def setstrobe() {
log.debug "Setting alarm to strobe."
state.LastAlarmtype = 2
sendEvent(name: "alarm", value: "strobe")
zwave.configurationV1.configurationSet(parameterNumber: 0, size: 1, configurationValue: [2]).format()

}
def setsiren() {
log.debug "Setting alarm to siren."
state.LastAlarmtype = 1
sendEvent(name: "alarm", value: "siren")
zwave.configurationV1.configurationSet(parameterNumber: 0, size: 1, configurationValue: [1]).format()

}
def setboth() {
log.debug "Setting alarm to both."
state.LastAlarmtype = 0
sendEvent(name: "alarm", value: "both")
zwave.configurationV1.configurationSet(parameterNumber: 0, size: 1, configurationValue: [0]).format()

}

def off() {
log.debug "sending off"
[
zwave.basicV1.basicSet(value: 0x00).format(),
zwave.basicV1.basicGet().format()
]
}
def on() {
log.debug "sending on"
[
zwave.basicV1.basicSet(value: 0xff).format(),
zwave.basicV1.basicGet().format()
]
}

def both() {
log.debug "sending alarm on via both"
[
zwave.basicV1.basicSet(value: 0xff).format(),
zwave.basicV1.basicGet().format()
]
}

def parse(String description) {
def result = null
def cmd = zwave.parse(description)
log.debug "parse($description) - command is $cmd"
if (cmd) {
result = createEvents(cmd)
}
log.debug "Parse returned ${result?.descriptionText}"
return result
}
def createEvents(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
log.debug "createEvents with cmd value {$cmd.value}, LastAlarmtype: $state.LastAlarmtype"
def switchValue = cmd.value ? "on" : "off"
def alarmValue 
if (state.LastAlarmtype == 0) {
alarmValue = "both"
}
else if (state.LastAlarmtype == 2) {
alarmValue = "strobe"
}
else if (state.LastAlarmtype == 1) {
alarmValue = "siren"
}
else {
alarmValue = "off"
}
[
createEvent([name: "switch", value: switchValue, type: "digital", displayed: false]),
createEvent([name: "alarm", value: alarmValue, type: "digital"])
]
}
def createEvents(physicalgraph.zwave.Command cmd) {
log.warn "UNEXPECTED COMMAND: $cmd"
}
def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd)
{
def result = [createEvent(descriptionText: "${device.displayName} woke up", isStateChange: false)]
if (!state.lastbat || (new Date().time) - state.lastbat > 53*60*60*1000) {
result << response(zwave.batteryV1.batteryGet())
result << response("delay 1200")
}
result << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
result
}
def createEvents(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
def map = [ name: "battery", unit: "%" ]
if (cmd.batteryLevel == 0xFF) {
map.value = 1
map.descriptionText = "$device.displayName has a low battery"
map.isStateChange = true
} else {
map.value = cmd.batteryLevel
}
state.lastbatt = new Date().time
[createEvent(map), response(zwave.wakeUpV1.wakeUpNoMoreInformation())]
}
def createEvents(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd)
{
log.debug "CONFIGURATIONREPORT"
}
def poll() {
if (secondsPast(state.lastbatt, 36*60*60)) {
return zwave.batteryV1.batteryGet().format()
} else {
return null
}
}
private Boolean secondsPast(timestamp, seconds) {
if (!(timestamp instanceof Number)) {
if (timestamp instanceof Date) {
timestamp = timestamp.time
} else if ((timestamp instanceof String) && timestamp.isNumber()) {
timestamp = timestamp.toLong()
} else {
return true
}
}
return (new Date().time - timestamp) > (seconds * 1000)
}
def refresh() {
log.debug "sending battery refresh command"
zwave.batteryV1.batteryGet().format()
}

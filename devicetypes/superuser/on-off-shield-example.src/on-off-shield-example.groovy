/*
 *  On/Off Shield (example)
 *
 * This work with Hello On Off. Listed at the end of this
 *  Author: urman
 *  Date: 2013-03-14
 *  Revision: 2014-07-10
 *  Capabilities:
 *   Switch
 *  Custom Attributes:
 *   greeting
 *  Custom Commands
 *   hello
 *   goodbye
 */
 
 
metadata {
	definition (name: "On/Off Shield (example)", author: "urman") {
    		capability "Switch"
        	command "hello"
        	command "goodbye"
        	attribute "greeting","string"
	}

	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
		standardTile("greeting", "device.greeting", width: 1, height: 1, canChangeIcon: true, canChangeBackground: true) {
			state "default", label: 'hello', action: "hello", icon: "st.switches.switch.off", backgroundColor: "#ccccff"
		}     
		valueTile("message", "device.message", inactiveLabel: false) {
			state "greeting", label:'${currentValue}', unit:""    
		}
        
		main "switch"
		details(["switch","greeting","message"])
	}
    simulator {
        status "on":  "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
        status "off": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"
    
        // reply messages
        reply "raw 0x0 { 00 00 0a 0a 6f 6e }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
        reply "raw 0x0 { 00 00 0a 0a 6f 66 66 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"
    }
}

Map parse(String description) {

	def value = zigbee.parse(description)?.text
	def linkText = getLinkText(device)
	def descriptionText = getDescriptionText(description, linkText, value)
	def handlerName = value
	def isStateChange = value != "ping"
	def displayed = value && isStateChange

	def result = [
		value: value,
		name: value in ["on","off"] ? "switch" : (value && value != "ping" ? "greeting" : null),
		handlerName: handlerName,
		linkText: linkText,
		descriptionText: descriptionText,
		isStateChange: isStateChange,
		displayed: displayed
	]

	log.debug result.descriptionText
	result
}

def on() {
    zigbee.smartShield(text: "on").format()
}

def off() {
	zigbee.smartShield(text: "off").format()
}

def hello() {
	log.debug "Hello World!"
	zigbee.smartShield(text: "hello").format()
}

def goodbye() {
	log.debug "Bye Bye!"
	zigbee.smartShield(text: "goodbye").format()
}

def parse(String description) {
	log.debug "Parsing '${description}'"
    def arduinoMsg = zigbee.parse(description)?.text //helper function returns original message from Arduino
    log.debug "Arduino Msg ${arduinoMsg}" 
    sendEvent(name:"message", value:arduinoMsg)
      
	// TODO: handle 'switch' attribute
	// TODO: handle 'greeting' attribute

}


/*
Working Sketch for my code above. 

//*****************************************************************************
/// @file
/// @brief
///   Arduino SmartThings Shield LED Example 
//*****************************************************************************
#include <SoftwareSerial.h>   //TODO need to set due to some weird wire language linker, should we absorb this whole library into smartthings
#include <SmartThings.h>


#define PIN_THING_RX    3
#define PIN_THING_TX    2


SmartThingsCallout_t messageCallout;    // call out function forward decalaration
SmartThings smartthing(PIN_THING_RX, PIN_THING_TX, messageCallout);  // constructor


int ledPin = 8;
bool isDebugEnabled;    // enable or disable debug in this example
int stateLED;           // state to track last set value of LED

void setup()
{
  // setup default state of global variables
  isDebugEnabled = true;
  stateLED = 0;                 // matches state of hardware pin set below
  
  // setup hardware pins 
  pinMode(ledPin, OUTPUT);     // define PIN_LED as an output
  digitalWrite(ledPin, LOW);   // set value to LOW (off) to match stateLED=0

  if (isDebugEnabled)
  { // setup debug serial port
    Serial.begin(9600);         // setup serial with a baud rate of 9600
    Serial.println("setup..");  // print out 'setup..' on start
  }
}


void loop()
{
  // run smartthing logic
  smartthing.run();
}

void on()
{
  stateLED = 1;                 // save state as 1 (on)
  digitalWrite(ledPin, HIGH);  // turn LED on
  smartthing.shieldSetLED(0, 0, 1);
  smartthing.send("on");        // send message to cloud
  Serial.println("on");
}

void off()
{
  stateLED = 0;                 // set state to 0 (off)
  digitalWrite(ledPin, LOW);   // turn LED off
  smartthing.shieldSetLED(0, 0, 0);
  smartthing.send("off");       // send message to cloud
  Serial.println("off");
}

void hello()
{
  Serial.println("brobasaur");
  smartthing.send("colors!");
  smartthing.shieldSetLED(1, 0, 0);
  delay(200);
  smartthing.shieldSetLED(0, 1, 0);
  delay(200);
  smartthing.shieldSetLED(0, 0, 1);
  delay(200);
  smartthing.shieldSetLED(1, 1, 0);
  delay(200);
  smartthing.shieldSetLED(1, 1, 1);
  delay(200);
  smartthing.shieldSetLED(1, 0, 1);
  delay(200);
  smartthing.shieldSetLED(0, 1, 1);
  delay(200);
  smartthing.shieldSetLED(3, 2, 1);
  delay(200);
  smartthing.shieldSetLED(1, 2, 3);
  delay(200);
  smartthing.shieldSetLED(2, 2, 4);
  delay(200);
  smartthing.shieldSetLED(4, 3, 1);
     delay(200);
  smartthing.shieldSetLED(0, 0, 0);
  smartthing.send("fancy");
  
}


void messageCallout(String message)
{
  // if debug is enabled print out the received message
  if (isDebugEnabled)
  {
    Serial.print("Received message: '");
    Serial.print(message);
    Serial.println("' ");
  }

  // if message contents equals to 'on' then call on() function
  // else if message contents equals to 'off' then call off() function
  if (message.equals("on"))
  {
    on();
  }
  else if (message.equals("off"))
  {
    off();
  }
  else if (message.equals("hello"))
  {
    hello();
  }
}

*/


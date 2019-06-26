#include <DallasTemperature.h>
#include <ESP8266WebServer.h>
#include <ESP8266WiFi.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <OneWire.h>

// pin where the temp sensor's oneWire connects
const int DATA_PIN = 4;
// pin that toggle the IR LED
const int IR_LED = 5;

OneWire oneWire(DATA_PIN);
DallasTemperature sensor(&oneWire);

const char* ssid = "esp-testing";
const char* password = "webserver-testing";
ESP8266WebServer server(80);

IRsend irsend(IR_LED);

uint64_t ac_ir_power = 0x7F20DF;
uint64_t ac_ir_temp_down = 0x7F609F;
uint64_t ac_ir_temp_up = 0x7FA05F;
uint64_t ac_ir_mode = 0x7F00FF;
uint64_t ac_ir_fan_speed = 0x7FE01F;

bool ac_init_done = false;
byte ac_status = 0;
byte ac_fan_speed = 1;

void setup() {
    Serial.begin(9600);
    while (!Serial) delay(1000);

    Serial.print("Connecting to ");
    Serial.print(ssid);
    WiFi.hostname(F("esp-ac-control"));
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println("");
    Serial.println("Connected!");

    Serial.print("IP: ");
    Serial.println(WiFi.localIP());

    server.on("/", handle_root);
    server.on("/ac", handle_ac_status);
    server.on("/ac-toggle", HTTP_POST, handle_ac_toggle);
    server.on("/ac-toggle-speed", HTTP_POST, handle_ac_speed_toggle);
    server.on("/ac-init", HTTP_POST, handle_ac_init);

    server.onNotFound(handle404);

    server.begin();
    sensor.begin();
    irsend.begin();
}

void loop() {
    server.handleClient();
}

void handle_root() {
    server.send(200, "text/plain", "Hello there. Looks like I am working. :)");
}

void handle_ac_status() {
    String json = "{\n \"ac-status\": ";
    json.concat(ac_status);
    json.concat(",\n \"ac-fan-speed\": ");
    json.concat(ac_fan_speed);
    json.concat(",\n \"room-temperature\": ");
    json.concat(getTemperature());
    json.concat("\n}");

    server.send(200, "application/json", json);
}

void handle_ac_init() {
    initAC();
    server.send(200, "text/plain", String(ac_status));
}

void handle_ac_toggle() {
    if (server.hasArg("noinit")) {
        ac_init_done = true;
    }
    toggleAC();
    server.send(200, "text/plain", String(ac_status));
}

void handle_ac_speed_toggle() {
    toggleACFanSpeed();
    server.send(200, "text/plain", String(ac_fan_speed));
}

void handle404() {
    server.send(404, "text/plain", "404: Not found");
}

float getTemperature() {
    sensor.requestTemperatures();
    float tempC = sensor.getTempCByIndex(0);
    tempC -= 3;
    return tempC;
}

// this sets the AC to the parameters
// we want it to be at after a loss of power
void initAC() {
    toggleAC();
    irsend.sendNEC(ac_ir_mode);
    delay(100);
    toggleACFanSpeed();
    for (int i = 0; i < 10; i++) {
        irsend.sendNEC(ac_ir_temp_down);
        delay(100);
    }
}

int toggleAC() {
    if (!ac_init_done) {
        ac_init_done = true;
        initAC();
    }
    else {
        irsend.sendNEC(ac_ir_power);

        if (!ac_status)
            ac_status = 1;
        else
            ac_status = 0;

        delay(100);
    }
    return ac_status;
}

int toggleACFanSpeed() {
    irsend.sendNEC(ac_ir_fan_speed);
    if (ac_fan_speed == 1)
        ac_fan_speed = 2;
    else
        ac_fan_speed = 1;

    delay(100);
    return ac_fan_speed;
}

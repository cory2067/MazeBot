#include <Wire.h>
#include <Adafruit_MotorShield.h>
#include "utility/Adafruit_PWMServoDriver.h"
#include <Servo.h> 

Adafruit_MotorShield AFMS = Adafruit_MotorShield(); 

Adafruit_StepperMotor *right = AFMS.getStepper(200, 1);
Adafruit_StepperMotor *left = AFMS.getStepper(200, 2);
Servo servo;

//int leftSpeed, rightSpeed;
unsigned long time, leftDelay, rightDelay, leftLast, rightLast;
int leftMode = SINGLE, rightMode = SINGLE;
boolean penLifted = false;
byte steps[128];
int arrayLoc = 128, stepL = 0, stepR = 0;
boolean arrayEmpty = true;

void setup() 
{
  servo.attach(10);
  setLeftSpeed(0);
  setRightSpeed(0);
  Serial.begin(115200);
  AFMS.begin();
  time = micros();
  leftLast = time;
  rightLast = time;
  left->onestep(FORWARD, SINGLE);
  right->onestep(FORWARD, SINGLE);
  delay(100);
}

long count = 0;
void loop() 
{  
  if(arrayEmpty)
  {
    Serial.println("data");
    int aPos = 0;
    while(arrayEmpty)
    {
      if(Serial.available() > 0)
      {
        steps[aPos] = Serial.read();
        aPos++;
        if(aPos == 128)
        {
          arrayEmpty = false;
          Serial.println("done");
          arrayLoc = 0;
        }
      }
    }
    
    /*for(int a = 0; a < 128; a++)
    {
      Serial.println(steps[a]);
      delay(20);
    }*/
  }
  
  if(penLifted)
    servo.write(170);
  else
    servo.write(130);
    
  if(stepL == 0 && stepR == 0)
  {
    if(arrayLoc == 128)
    {
      setLeftSpeed(0);
      setRightSpeed(0);
      arrayEmpty = true;
    }
    stepL = steps[arrayLoc] / 10;
    stepR = steps[arrayLoc] % 10;
    int m = max(stepL, stepR);
    if(m == 0)
    {
      setLeftSpeed(0);
      setRightSpeed(0);
    }
    else
    {
      setLeftSpeed(300 * stepL / m);
      setRightSpeed(300 * stepR / m); 
    }
    arrayLoc++;
  }
  
  time = micros();
  if(leftDelay != -1)
  {
    if(time - leftLast > leftDelay && stepL > 0)
    {
      left->onestep(FORWARD, leftMode);
      stepL--;
      leftLast = time;
    } 
  }
  else
  {
    leftLast = time;
  }
  
  if(rightDelay != -1)
  {
    if(time - rightLast > rightDelay && stepR > 0)
    {
      right->onestep(FORWARD, rightMode); 
      stepR--;
      rightLast = time;
    }
  }
  else
  {
    rightLast = time;
  }
  
  delayMicroseconds(250);
}

void setLeftSpeed(int spd)
{
  leftDelay = calcDelay(spd);
  if(spd > 70 && spd < 250)
    leftMode = DOUBLE;
  else
    leftMode = DOUBLE;
}

void setRightSpeed(int spd)
{
  rightDelay = calcDelay(spd);
  if(spd > 70 && spd < 250)
    leftMode = DOUBLE;
  else
    leftMode = DOUBLE;
 }

long calcDelay(int spd)
{
  if(spd == 0)
    return -1;
  
  long micros = (1000000.0/spd);
  return micros;
}

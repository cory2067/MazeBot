#include <Wire.h>
#include <Adafruit_MotorShield.h>
#include <Servo.h> 

Adafruit_MotorShield AFMS = Adafruit_MotorShield(); 

Adafruit_StepperMotor *right = AFMS.getStepper(200, 1);
Adafruit_StepperMotor *left = AFMS.getStepper(200, 2);
Servo servo;

#define BATCH_SIZE 256
#define SPEED 150

unsigned long time, leftDelay, rightDelay, leftLast, rightLast;
int leftMode = SINGLE, rightMode = SINGLE;
int leftDir = FORWARD, rightDir = FORWARD;
boolean penLifted = true;
byte steps[BATCH_SIZE];
int arrayLoc = BATCH_SIZE, stepL = 0, stepR = 0;
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
  left->onestep(FORWARD, DOUBLE);
  right->onestep(FORWARD, DOUBLE);
  servo.write(165);
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
        if(aPos == BATCH_SIZE)
        {
          arrayEmpty = false;
          Serial.println("done");
          arrayLoc = 0;
        }
      }
    }
    
    /*for(int a = 0; a < BATCH_SIZE; a++)
    {
      Serial.println(steps[a]);
      delay(20);
    }*/
  }
    
  if(stepL == 0 && stepR == 0)
  {
    if(arrayLoc == BATCH_SIZE)
    {
      setLeftSpeed(0);
      setRightSpeed(0);
      arrayEmpty = true;
    }
    else
    {
      stepL = abs(steps[arrayLoc] / 10 - 4);
      stepR = abs(steps[arrayLoc] % 10 - 4);
      if(stepL == 5)
      {
        if(stepR == 4)
          penLifted = false;
        else if(stepR == 3)
          penLifted = true;
        setLeftSpeed(0);
        setRightSpeed(0); 
        delay(250);
        stepL = 0;
        stepR = 0;
      }
      else
      {
        int m = max(stepL, stepR);
        if(m == 0)
        {
          setLeftSpeed(0);
          setRightSpeed(0);
        }
        else
        {
          setLeftSpeed(SPEED * (steps[arrayLoc] / 10 - 4) / m);
          setRightSpeed(SPEED * (steps[arrayLoc] % 10 - 4) / m); 
        }
      }
      arrayLoc++;
    }
  }
  
  time = micros();
  if(leftDelay != -1)
  {
    if(time - leftLast > leftDelay && stepL > 0)
    {
      left->onestep(leftDir, leftMode);
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
      right->onestep(rightDir, rightMode); 
      stepR--;
      rightLast = time;
    }
  }
  else
  {
    rightLast = time;
  }
  
  if(penLifted)
    servo.write(165);
  else
    servo.write(120);
  delayMicroseconds(128);
}

void setLeftSpeed(int spd)
{
  if(spd < 0)
    leftDir = BACKWARD;
  else
    leftDir = FORWARD;
  leftDelay = calcDelay(abs(spd));
  leftMode = DOUBLE;
}

void setRightSpeed(int spd)
{
  if(spd < 0)
    rightDir = FORWARD;
  else
    rightDir = BACKWARD;
  rightDelay = calcDelay(abs(spd));
  //if(spd > 70 && spd < 250)
  rightMode = DOUBLE;
 }

long calcDelay(int spd)
{
  if(spd == 0)
    return -1;
  
  long micros = (1000000.0/spd);
  return micros;
}

# Bluetooth SMS App

## 📱 프로젝트 소개
이 앱은 HC-06 블루투스 모듈과 연동하여 특정 신호를 받으면 자동으로 SMS를 전송하는 안드로이드 애플리케이션입니다. 블루투스 통신을 백그라운드에서 유지하며, 사용자가 지정한 전화번호로 긴급 메시지를 전송할 수 있습니다.

## ⚙️ 주요 기능
- HC-06 블루투스 모듈과 자동 연결
- 백그라운드에서 블루투스 연결 유지
- 특정 신호("1") 수신 시 SMS 자동 발송
- 사용자 지정 전화번호 저장
- 연결 상태 실시간 확인
- Foreground 서비스 알림

## 🔧 기술 스택
- Language: Kotlin
- Platform: Android
- Minimum SDK: API 24 (Android 7.0)
- Target SDK: API 34 (Android 14)
- Bluetooth: HC-06 모듈 지원

## 📋 필요 권한
- Bluetooth
- Bluetooth Admin
- Bluetooth Connect
- Bluetooth Scan
- SMS 발송
- Foreground 서비스
- 위치 정보

'''
#include <SoftwareSerial.h>

// HC-06 모듈 연결 설정 (RX, TX)
SoftwareSerial bluetooth(0, 1); // RX=0, TX=1

const int buttonPin = 2;  // 버튼을 디지털 2번 핀에 연결
boolean lastButton = HIGH;  // 버튼의 이전 상태
boolean currentButton = HIGH;  // 버튼의 현재 상태

void setup() {
  // 버튼 핀을 입력으로 설정하고 내부 풀업 저항 사용
  pinMode(buttonPin, INPUT_PULLUP);
  
  // 블루투스 통신 시작 (HC-06 기본 통신속도는 9600)
  bluetooth.begin(9600);
  // 시리얼 모니터링을 위한 설정
  Serial.begin(9600);
}

void loop() {
  // 버튼 상태 읽기
  currentButton = digitalRead(buttonPin);

  // 버튼이 눌렸을 때 (LOW -> HIGH)
  if(lastButton == HIGH && currentButton == LOW) {
    Serial.println("1");
    // 블루투스로 '1' 전송
    bluetooth.println("1");
    delay(50);  // 디바운싱
  }

  // 현재 버튼 상태를 이전 상태로 저장
  lastButton = currentButton;
}
'''


## 💻 설치 방법
1. 프로젝트를 클론합니다
2. Android Studio에서 프로젝트를 엽니다
3. HC-06 모듈과 페어링된 안드로이드 기기에서 실행합니다

## 🚀 사용 방법
1. 앱을 실행하면 자동으로 HC-06 모듈과 연결을 시도합니다
2. SMS를 발송할 전화번호를 입력하고 저장합니다
3. 테스트 버튼으로 SMS 발송을 테스트할 수 있습니다
4. 앱이 백그라운드에서 실행되어도 블루투스 연결이 유지됩니다
5. HC-06 모듈로부터 "1" 신호를 받으면 저장된 번호로 SMS가 발송됩니다

## 📝 주의사항
- HC-06 모듈과 사전 페어링이 필요합니다
- 필요한 권한을 모두 허용해야 합니다
- SMS 발송 요금이 부과될 수 있습니다

## 🔄 버전 정보
- Version 1.0
  - 최초 릴리즈
  - 기본 기능 구현

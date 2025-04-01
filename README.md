# 법장사 홈페이지

## 페이지 주소
https://bjs-temple-dev.p-e.kr
## 기술 스택
- Java 17
- Next.js 14
- SpringBoot
- JPA
- MariaDB
- JWT
## 서버 환경
운영환경 (AWS 영역)
- AWS EC2
  - Nginx
  - Docker
    - backend 이미지
    - frontend 이미지
    - Portainer (컨테이너 관리용)
- AWS RDS (MariaDB)

배포환경 (배포서버 영역)
- ubuntu 22.04
  - Docker
    - Registry
    - Jenkins
    - backend 개발 이미지
    - frontend 개발 이미지
    - MariaDB
    - Portainer

## 구성도 이미지
![Pasted image 20250228232327.png](images/Pasted%20image%2020250228232327.png)

## 구성 방식
1. 개발 소스 수정 후 github에 push
2. push 후 jenkins를 통해 해당 코드를 Docker 컨테이너로 변환
3. 변환 후 만들어 둔 사설 Repository에 이미지를 올림.
4. 실제 배포할 서버에서는 해당 컨테이너만 가져갈 수 있도록 설정
   - 사설 repo를 등록해두어 해당 repo에서 컨테이너를 찾을 수 있도록 처리
   - 이를 통해 서버 환경이 변경되더라도 변경된 환경에 Docker만 설치하면 컨테이너를 통해 일관된 배포를 처리.
5. nginx를 통해 등록한 도메인 접근 시 해당 서버 포트를 찾아갈 수 있도록 설정
   - SSL
   - 리버스프록시

## 백엔드 모듈 구성
### bha-api
  - 사용자의 API 요청/응답에 대해 처리하는 모듈 
  - 컨트롤러, 전체 예외에 대한 응답 처리 
  - 모듈들의 서비스 로직을 주입받아 데이터 처리 
  - Infra, Core, Common 모듈 의존
### bha-core
  - 실제 DB와 연동하여 데이터의 조작/조회를 처리하는 모듈
  - MariaDB 데이터 처리
  - DB Entity 관리
  - Dto 데이터의 DB 처리
  - Common 모듈 의존
### bha-common
  - 전체 모듈의 공통 클래스 모음
  - Exception, Type, Constant 관리
  - 의존모듈 없음(독립적)
### bha-infra
 - 제반 관리 모듈
 - security, 외부 연동등의 처리
 - Common 모듈 의존

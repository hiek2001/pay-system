## 의사 결정 및 구현할 기능 정리
1. 소셜을 활용한 회원가입, 로그인, 로그아웃 
   - `SoicalUser` 테이블에 로그인 시도하는 회원이 이미 가입되어 있다면 로그인됨
   - 미가입된 회원이라면 회원가입됨

     → Kakao에서 받은 인가 코드로 액세스 코드 요청

     → 액세스 토큰으로 사용자 정보 조회 및 처리

     → 회원 가입 또는 로그인 처리 후 JWT 발급 및 반환
   
2. 동영상의 재생 상태 관리
    - 동영상 페이지 API 최초 호출하면 동영상 자동 재생

      → `Video` 테이블 videoViews(조회수) 1 증가

      → `UserVideoHistory` 테이블에 회원의 동영상 재생 내역을 최초 저장(save)

    - 사용자 측면에서 재생과 정지 버튼은 동일하다고 전제함
    - 동영상이 “재생” 중인 상태에서 사용자가 재생 버튼 클릭 시, `동영상 상태`가 “정지”로 변경
    - 동영상이 “정지” 중인 상태에서 사용자가 재생 버튼 클릭 시, `동영상 상태`가 “재생”으로 변경

      → 동영상 상태 (정지, 재생, 시청완료) - (PAUSE, PLAY, END)

      → 사용자가 재생 버튼을 클릭할 때마다 `UserVideoHistory` 테이블에 pausedTime과 playTime, status가 변경(update)

      → 트랜잭션의 경우 정지, 재생, 영상이 끝났을 경우에만 짧게 처리할 것

      e.g 회원이 동영상을 시청할 때, 한번도 정지하지 않고 끝까지 봤을 경우를 가정

      → 트랜잭션이 완료되면 `UserVideoHistory` 테이블에 pausedTime을 `Video` 테이블의 duration과 동일하게, status는 END로 변경(update)


    -  사용자 측면에서 동영상을 마우스로 클릭하여 넘어갈 수도 있다고 전제함.
        
        e.g 영상 길이가 10분이라면 회원이 4분 50초까지만 보고, 5분 10초로 클릭해서 넘어가는 경우를 가정, 5분 단위로 나오는 광고를 피하기 위함
        
        → 5의 배수로 시청 시간이 지나면 광고가 트리거 형식으로 나오게 구현 (트리거 로직 어떻게 구현할지 고민해보기 : CompletableFuture )
        
        e.g 영상 길이가 15분일 때, 10분 클릭하고 10초만 보고 나가는것을 가정
        
        → `UserVideoHistory` 테이블에 playTime 컬럼을 추가하여 Long 타입으로 받아 접속 시간 계산하기

3. 광고 삽입
    - 동영상 길이가 5의 배수일 때마다 1개씩 자동 등록함
        - 5분 초과 동영상 → 광고 1개 자동 등록
            - 시청 시간 0분에 광고 시작
        - 10분 초과 동영상 → 광고 2개 자동 등록
            - 시청 시간 0분, 5분에 광고 시작

      → 동영상 1개 당 광고 갯수를 (동영상 길이/5) 개로 삽입되는지 계산

      → 동영상 재생 시점이 광고 삽입된 시점(특정 시간)을 지나면 랜덤으로 광고를 트리거 로직으로 구현 (광고 실시간성 유지)

      → `VideoAdsLink` 테이블에 랜덤 광고 데이터 추가 (save)

      → 해당 광고(adsId)에 맞는 `Ads` 테이블에 adsViews (광고 조회수) 1 증가

      → 실시간으로 관찰하는 부분은 트리거 로직 구현하면서 볼 것.


4. 에러 처리
- 네트워크 문제로 동영상이 중간에 끊길 경우 어떻게 처리할건지 고민하기
- 에러 발생 시, 서버에 어떤 응답을 줄 것인지 고민하기
    - HandlerException

동시성 처리 예시

- 충돌 문제

  여러 사용자가 동시에 같은 동영상을 정지 또는 재생할 경우

- 중복 업데이트

  두 개 이상의 트랜잭션이 동시에 실행되면서 마지막으로 커밋된 데이터가 예상한 것과 달라질 수 있음


→ 이런 경우 cors 패턴 사용하면 해결됨.
# MoneyLog - 개발 TODO 리스트

> **최종 업데이트**: 2026-04-13
> **디자인 시스템**: "The Sovereign Ledger" (High-End Editorial)

---

## Phase 1: 기획/설계 (1주) ✅ 완료

- [x] 요구사항 분석 (기능 요구사항 문서 작성)
- [x] DB 모델링 (ERD, 테이블 정의)
- [x] 시스템 아키텍처 설계
- [x] UI 화면 설계 (와이어프레임)
- [x] 기술 스택 선정
- [x] 데이터 레이어 설계 (DAO, Repository)
- [x] 개발환경 설정 가이드 작성
- [x] UI 디자인 시스템 확정 — "The Sovereign Ledger" (stitch/indigo_ledger/DESIGN.md)
- [x] 화면별 디자인 목업 완성 (stitch/ — 8개 화면 HTML/이미지)
- [ ] Android Studio 프로젝트 초기 생성 (`com.moneylog`)
- [ ] `gradle/libs.versions.toml` 버전 카탈로그 구성
- [ ] `app/build.gradle.kts` 의존성 설정 (Compose, Room, Hilt, Vico 등)
- [ ] 프로젝트 구조 생성 (data/ui/di/util 패키지)
- [ ] Git 저장소 초기화 및 `.gitignore` 설정

---

## Phase 2: 코어 기능 — 거래 CRUD, 카테고리, Room DB (2주) 📋 예정

### 2-1. Room 데이터베이스 설정

- [ ] `AppDatabase.kt` — Room Database 클래스 생성 (version=1)
- [ ] `TransactionEntity.kt` — 거래 엔티티 정의
- [ ] `CategoryEntity.kt` — 카테고리 엔티티 정의
- [ ] `BudgetEntity.kt` — 예산 엔티티 정의
- [ ] `RecurringEntity.kt` — 반복 거래 엔티티 정의
- [ ] 인덱스 설정 (`idx_tx_date`, `idx_tx_category`, `idx_tx_type`, `idx_tx_deleted`)
- [ ] `uq_budget_category_month` 유니크 인덱스 설정

### 2-2. DAO 구현

- [ ] `TransactionDao.kt` — 거래 CRUD + 월별 요약 + 카테고리별 집계 쿼리
- [ ] `CategoryDao.kt` — 카테고리 CRUD + 타입별 조회
- [ ] `BudgetDao.kt` — 예산 CRUD + 월별 조회
- [ ] `RecurringDao.kt` — 반복 거래 CRUD + 미실행 조회

### 2-3. Repository 구현

- [ ] `TransactionRepository.kt` — 월별 조회, 생성, 수정, 소프트 삭제
- [ ] `CategoryRepository.kt` — 카테고리 관리 + 기본 카테고리 시드
- [ ] `BudgetRepository.kt` — 예산 관리
- [ ] `RecurringRepository.kt` — 반복 거래 관리 + 자동 실행 로직

### 2-4. DI (Hilt) 설정

- [ ] `AppModule.kt` — Room DB, DAO, Repository Hilt 모듈 정의
- [ ] `Application` 클래스에 `@HiltAndroidApp` 적용

### 2-5. 카테고리 기본 데이터

- [ ] 기본 지출 카테고리 10개 시드 — **Material Symbols 아이콘명** 사용 (restaurant, directions_bus, home, sports_esports, checkroom, local_hospital, menu_book, redeem, local_cafe, inventory_2)
- [ ] 기본 수입 카테고리 5개 시드 — Material Symbols 아이콘명 (payments, account_balance_wallet, savings, trending_up, inventory_2)
- [ ] `RoomDatabase.Callback`으로 첫 실행 시 자동 삽입

### 2-6. 디자인 시스템 & 테마 구현

> **참조**: stitch/indigo_ledger/DESIGN.md — "The Sovereign Ledger"

#### 색상 팔레트 (Material 3 Surface Hierarchy)

- [ ] `Color.kt` — 전체 색상 토큰 정의
  - Primary: `#3525CD` / Primary Container: `#4F46E5`
  - Surface: `#F8F9FA` / Surface-Container-Low: `#F3F4F5` / Surface-Container-Lowest: `#FFFFFF`
  - Surface-Container: `#EDEEEF` / Surface-Container-High: `#E7E8E9`
  - On-Surface: `#191C1D` (순수 검정 #000000 사용 금지)
  - Error: `#BA1A1A` / Error-Container: `#FFDAD6`
  - Secondary: `#00687A` / Secondary-Container: `#57DFFE` (AI Frosted Cyan용)
  - Tertiary: `#005338` / Tertiary-Container: `#006E4B`
  - Outline: `#777587` / Outline-Variant: `#C7C4D8`
  - Income 계열: Tertiary `#005338`~`#6FFBBE`
  - Expense 계열: Error `#BA1A1A`~`#FFDAD6`

#### 타이포그래피 (듀얼 폰트)

- [ ] `Type.kt` — Manrope + Inter 타이포그래피 스케일
  - Display-LG: **Manrope** 3.5rem/700 — 대형 잔액 표시
  - Headline-MD: **Manrope** 1.75rem/600 — 페이지 제목, 섹션 헤더
  - Title-MD: **Inter** 1.125rem/500 — 카드 제목
  - Body-LG: **Inter** 1rem/400 — 거래 설명
  - Label-MD: **Inter** 0.75rem/600 — 오버라인, 카테고리 태그
- [ ] `fonts/` 디렉토리에 Manrope, Inter 폰트 파일 추가

#### 테마 & 디자인 원칙

- [ ] `Theme.kt` — Material 3 커스텀 ColorScheme (위 색상 토큰 기반)
  - 라이트/다크 모드 각각 정의
  - Dynamic Color 비활성 (고정 브랜드 컬러 사용)
- [ ] **"No-Line" 정책 적용**: 1px 디바이더 사용 금지
  - 리스트 아이템: 16dp 수직 여백으로 분리
  - 섹션 구분: 배경색 변화 (surface ↔ surface-container-low)
- [ ] **Tonal Layering**: 카드 = surface-container-lowest(#FFFFFF) on surface-container-low(#F3F4F5)
- [ ] **카드 스타일**: xxl (1.5rem = 24dp) 라운드 코너, 드롭 섀도 없음
- [ ] **Ambient Shadow**: FAB/Modal용 — blur 32px, opacity 4%, Indigo 틴트
- [ ] **버튼 스타일**:
  - Primary: Indigo 그라디언트 (`#3525CD` → `#4F46E5`), full(9999px) 라운딩
  - Secondary: Glassmorphic Cyan (AI 액션) 또는 surface-container-high
  - Press 상태: scale 96%
- [ ] **입력 필드 스타일**: "Plinth" — 테두리 없음, surface-container-highest 배경, md(12dp) 라운딩
  - Focus: primary_fixed 배경 + Ghost Border (outline-variant 15% opacity)

#### 공통 컴포넌트

- [ ] `NavGraph.kt` — 네비게이션 그래프 (하단 5탭: 홈, 내역, 등록, 통계, 설정)
- [ ] 하단 `NavigationBar` — Material Symbols Outlined 아이콘 (home, receipt_long, add_circle, bar_chart, settings)
- [ ] `TopAppBar` 공통 컴포넌트 — Glassmorphism (surface_bright 80% opacity + backdrop-blur 16px)
- [ ] `AiInsightCard.kt` — AI Insight 카드 공통 컴포넌트
  - secondary_fixed_dim 배경 + noise 텍스쳐 오버레이 + backdrop-blur
  - sparkle 아이콘 (auto_awesome) + 시안 그라디언트
- [ ] `CategoryIcon.kt` — Material Symbols Outlined 기반 카테고리 아이콘 (이모지 대신)
  - 원형 배경 (surface-container-low) + 아이콘
- [ ] `OverlineLabel.kt` — 오버라인 텍스트 (Label-MD, 대문자, primary 색상)
  - "FINANCIAL OVERVIEW", "PREFERENCES", "FINANCIAL INSIGHT" 등
- [ ] `EditorialMargin` — 좌우 패딩: `max(1.5rem, 5vw)` 대응값
- [ ] `GradientButton.kt` — Primary CTA 그라디언트 버튼 (primary → primary-container)

### 2-7. 거래 관련 UI (F02)

- [ ] `TransactionViewModel.kt` — 거래 목록 ViewModel
- [ ] `TransactionScreen.kt` — 거래 내역 화면 (디자인 참조: stitch/history/)
  - 일별 그룹 헤더 (Indigo 색상 날짜 + "TODAY" 오른쪽 정렬)
  - 거래 카드: surface-container-lowest 배경, xxl 라운드, Material Symbol 카테고리 아이콘
  - `Auto` 배지: primary-container 배경, 자동 등록 항목 표시
  - 필터 Chips: All / Expense / Income (full 라운딩, 선택 시 primary-container 채움)
  - 카테고리 드롭다운 ("All Categories")
  - FAB (+) 버튼: primary-container, Ambient Shadow
  - LazyColumn 무한 스크롤
- [ ] `TransactionFormScreen.kt` — 거래 등록/수정 화면 (디자인 참조: stitch/add_transaction/)
  - Expense / Income 토글탭 (full 라운딩 pill)
  - 금액 Display-LG (Manrope 700) + "KRW" 단위 표시
  - 날짜 Plinth 입력 (캘린더 아이콘 + 날짜 텍스트)
  - 메모 Plinth 입력 + **AI Recommend 칩** (sparkle 아이콘 + 시안 배경)
  - 카테고리 그리드 (Material Symbols Outlined 아이콘 + 라벨, 선택 시 primary-container 하이라이트)
  - 결제 수단 Chips (Card / Cash / Transfer / Other, outline 스타일)
  - 예산 경고 카드 (하단): "Logging this will put your 'Cafe' spending at 85% of your monthly budget."
  - "Save Transaction" 그라디언트 CTA 버튼
- [ ] `TransactionList.kt` — 거래 목록 컴포넌트 (No-Line, 여백으로 구분)
- [ ] `CategoryBadge.kt` — Material Symbols 기반 카테고리 배지 (원형 배경 + 아이콘)
- [ ] 거래 상세 조회 (개별 거래 모든 필드 확인)
- [ ] 거래 수정 기능
- [ ] 거래 소프트 삭제 (확인 다이얼로그)
- [ ] 거래 검색 (메모 키워드, 금액 범위, 카테고리 필터) — P1

### 2-8. 카테고리 관리 UI (F04)

- [ ] 카테고리 목록 화면 (지출/수입 구분, Material Symbols 아이콘)
- [ ] 커스텀 카테고리 추가 — P1
- [ ] 카테고리 수정·삭제 (아이콘, 색상, 이름) — P1
- [ ] 카테고리 드래그 앤 드롭 정렬 — P2

---

## Phase 3: 자동화 — 고정 수입·지출, WorkManager 반복 거래 (1주) 📋 예정

### 3-1. 반복 거래 UI (F03)

> **디자인 참조**: stitch/fixed_transactions/

- [ ] `RecurringViewModel.kt` — 반복 거래 ViewModel
- [ ] `RecurringScreen.kt` — 고정 수입·지출 관리 화면
  - "MONTHLY COMMITMENT" 헤더 카드 — primary 그라디언트 배경, 총 고정 지출 금액 (Display-LG), "N items scheduled this month"
  - "Fixed Expenses" / "Fixed Income" 섹션 (Headline, 우측 "N ITEMS" 카운터)
  - 반복 거래 카드: Material Symbol 아이콘 + 이름 + "Every month on the Nth" + 금액 + DEBIT/CREDIT/INACTIVE 라벨 + 활성/비활성 토글 스위치
  - "+ Add Recurring Transaction" 그라디언트 CTA 버튼 (full 라운딩)
  - 하단 **AI Insight 카드**: 고정 지출 후 남은 여유 예산 분석
- [ ] `RecurringList.kt` — 반복 거래 목록 컴포넌트 (No-Line, 카드 기반)
- [ ] 반복 거래 등록 폼 (금액, 카테고리, 매월 N일, 메모, 결제수단)
- [ ] 반복 거래 수정 — P1
- [ ] 반복 거래 비활성화/활성화 토글 — P1

### 3-2. WorkManager 자동 실행

- [ ] `RecurringWorker.kt` — PeriodicWorkRequest (매일 1회)
- [ ] 앱 실행 시 즉시 미처리 반복 거래 실행
- [ ] 밀린 달 한꺼번에 처리 로직 (`nextDate.plusMonths(1)` 루프)
- [ ] 자동 등록된 거래에 `is_auto = true` 표시
- [ ] 대시보드 "Pending Updates" 카드에 자동 등록 알림 + "Confirm Now" 버튼

---

## Phase 4: 대시보드·통계 — 차트, 예산, 월별 분석 (1주) 📋 예정

### 4-1. 대시보드 (메인 화면)

> **디자인 참조**: stitch/dashboard/

- [ ] `DashboardViewModel.kt` — 월별 수입/지출/잔액 + 최근 거래
- [ ] `DashboardScreen.kt` — 대시보드 레이아웃
  - "FINANCIAL OVERVIEW" 오버라인 + "April 2026" Headline (에디토리얼 좌측 정렬)
  - 월 네비게이션: "< This Month >" 화살표
  - **Total Balance 히어로 카드**: primary 그라디언트 배경, 잔액 Display-LG (흰색), MONTHLY INCOME / MONTHLY EXPENSE 하단 표시
  - **Pending Updates 카드**: 자동 등록 알림 — "N Auto-registered transactions detected: 항목명", "Confirm Now" 버튼
  - **AI Summary 카드**: secondary-fixed-dim 글래스모피즘, sparkle 아이콘, 한줄 요약 + "View Details →"
  - **Spending Distribution**: 도넛 차트 (Vico) + 중앙에 "Top Category: Dining" + 범례 리스트
  - **Recent Transactions**: 최근 3건 + "See All" 링크, 카드별 Material Symbol 아이콘
  - 하단 NavigationBar (5탭)

### 4-2. 통계·분석 (F06)

> **디자인 참조**: stitch/statistics/

- [ ] `StatisticsViewModel.kt` — 통계 ViewModel
- [ ] `StatisticsScreen.kt` — 통계 화면
  - "FINANCIAL INSIGHT" 오버라인 + "Statistics" Headline + "April 2026" 날짜
  - **AI Smart Analysis 카드**: 글래스모피즘 + sparkle 아이콘, 소비 분석 요약 텍스트, 절약 조언
  - **전월 대비 메트릭 카드 2개**: "MONTHLY TOTAL +5.2%" / "DINING FOCUS -3.1%" (추이 아이콘 + 색상 바)
  - **Spending Trend 차트**: 라인 차트 (Vico, "Last 6 Months" 칩), 월별 축 (Nov~Apr)
  - **Category Breakdown**: 카테고리별 수평 바 차트 + Material Symbol 아이콘 + 퍼센트
  - FAB (+) 하단 배치
- [ ] `MonthlyChart.kt` — 월별 지출 추이 라인 차트 (Vico) — P1
- [ ] 카테고리별 지출 비율 수평 바 차트 + 도넛 차트 (Vico) — P1
- [ ] 전월 대비 카테고리별 증감 메트릭 카드 — P2
- [ ] 일별 지출 히트맵 (캘린더 뷰) — P2

### 4-3. 예산 관리 (F05)

> **디자인 참조**: stitch/budget_management/

- [ ] `BudgetViewModel.kt` — 예산 ViewModel
- [ ] `BudgetScreen.kt` — 예산 관리 화면
  - "CURRENT OVERVIEW" 오버라인 + "Financial Health" Headline
  - **전체 예산 카드**: 총 예산 금액 + "Remaining" 잔여액 (Tertiary 색상) + Usage Progress 바 + 퍼센트
  - "Category Breakdown" 섹션 + "+ Add Category Budget" 그라디언트 버튼
  - **카테고리 예산 카드**: Material Symbol 아이콘 + 카테고리명 + 태그 뱃지 (Essential/Utility/Warning)
    - 예산 금액 + 소진율 바 (primary/tertiary/error 색상 구분) + "N used" 사용 금액
    - Warning 상태: 예산 바 error 색상 + "Warning" 뱃지
  - **Editorial Insight 카드** (하단): AI 기반 예산 조언 — "Your 'Cafe' spending is approaching the limit..." (sparkle 아이콘)
- [ ] `BudgetProgress.kt` — 예산 소진율 프로그레스 바 컴포넌트 (색상 단계: primary → tertiary → error)
- [ ] 월별 총 예산 설정 — P1
- [ ] 카테고리별 예산 설정 (식비 30만, 교통 10만 등) — P1
- [ ] 카테고리 태그 분류 (Essential / Utility / Warning) — P1
- [ ] 예산 초과 알림 (80%/100%) — Android Notification — P2

---

## Phase 5: 백업 & 배포 — Google Drive 백업·복원 (1주) 📋 예정

### 5-1. Google 인증

- [ ] Google Cloud Console 프로젝트 생성 + OAuth 2.0 설정
- [ ] Google Sign-In (Credential Manager) 연동 — `drive.appdata` scope
- [ ] SHA-1 지문 등록 (debug + release)

### 5-2. Google Drive 백업·복원 (F07)

- [ ] `BackupRepository.kt` — DB 파일 백업/복원 로직
  - [ ] WAL checkpoint → DB 파일 복사
  - [ ] Google Drive `appDataFolder`에 업로드
  - [ ] 백업 파일명: `moneylog_yyyy-MM-dd_HHmmss.db`
- [ ] 수동 백업 버튼 ("Backup Now" 그라디언트 버튼) — P1
- [ ] 백업 목록 조회 (날짜·크기 확인) — P1
- [ ] 복원 버튼 ("Restore" 아웃라인 버튼) — P1
- [ ] 자동 백업 (WorkManager 주기적 실행: 매일/매주) — P2
- [ ] 백업 파일 AES 암호화 — P2

### 5-3. 앱 잠금 (F01)

> **디자인 참조**: stitch/pin_lock/

- [ ] `PinLockScreen.kt` — PIN 입력/설정 화면
  - 에디토리얼 블러 배경 (secondary-container/10 + primary-container/5 원형 그라디언트)
  - 브랜드 앵커: 자물쇠 아이콘 (primary-container 라운드 배경) + "MoneyLog" (Manrope 700) + "THE SOVEREIGN LEDGER" 서브타이틀
  - "Enter your PIN" + "Please enter your 4-digit code"
  - PIN 인디케이터 (4개 원형 dot, 입력 시 primary 색상 채움)
  - 숫자 패드 (1~9, 0, 백스페이스) — 각 키에 ABC/DEF 등 레터 서브텍스트
  - "Unlock with Fingerprint" 버튼 (surface-container-high 배경, 지문 아이콘)
  - "FORGOT PIN?" 링크 (primary 색상)
- [ ] `CryptoUtils.kt` — SHA-256 해시 유틸리티
- [ ] PIN 4~6자리 설정 (EncryptedSharedPreferences + Android Keystore) — P0
- [ ] PIN 잠금 해제 — P0
- [ ] 생체인증 (AndroidX Biometric) — P1
- [ ] 자동 잠금 (5분 비활성) — P1
- [ ] PIN 변경 (기존 PIN 확인 후) — P1
- [ ] PIN 초기화 (Google 계정 인증) — P1

### 5-4. 설정 화면 (F10)

> **디자인 참조**: stitch/settings/

- [ ] `SettingsViewModel.kt` — 설정 ViewModel
- [ ] `SettingsScreen.kt` — 설정 화면 레이아웃
  - "PREFERENCES" 오버라인 + "Control your financial footprint" Headline
  - **Data Management 섹션**:
    - "Fixed Transactions" → RecurringScreen 연결 (calendar_month 아이콘 + "Manage your recurring bills" 설명)
    - "Category Management" → 카테고리 관리 (category 아이콘 + "Edit icons and colors" 설명)
  - **Backup & Sync 섹션**:
    - Google Drive 연결 상태 표시 ("Google Drive Connected" + 마지막 백업 날짜)
    - "Backup Now" 그라디언트 버튼 + "Restore" 아웃라인 버튼
  - **AI Insights 섹션**:
    - Gemini Nano Status 카드 (글래스모피즘) + "SUPPORTED"/"UNSUPPORTED" 뱃지
    - "On-device processing active for enhanced privacy." 설명
  - **Device Preferences 섹션**:
    - "Change PIN" → PIN 변경 화면
    - "Biometrics" 토글 (Enabled/Disabled)
    - "Appearance" 드롭다운 (System default / Light / Dark)
  - **위험 영역**:
    - "Reset All Data" 빨간 버튼 (error-container 배경)
    - "This action is permanent and cannot be undone." 경고 텍스트
  - CSV 내보내기 (Android Share Intent) — P1
  - 알림 설정 on/off — P2

### 5-5. 배포 준비

- [ ] 릴리스 서명 키 생성
- [ ] `app/build.gradle.kts` release 빌드 설정
- [ ] ProGuard/R8 난독화 규칙
- [ ] Google Play Console 앱 등록
- [ ] 스크린샷 / 스토어 설명 준비
- [ ] GitHub Actions CI/CD (main → 자동 빌드 → Play Store 업로드)

---

## Phase 6: AI 요약 — Gemini Nano 온디바이스 소비 분석 (1주) 📋 예정

### 6-1. Gemini Nano 연동 (F08)

- [ ] Google AI Edge SDK 의존성 추가
- [ ] `AndroidManifest.xml`에 AICore 메타데이터 추가
- [ ] `AiSummaryRepository.kt` — Gemini Nano 호출 + 폴백 로직
- [ ] 기기 호환성 체크 (`isAvailable()`) — 미지원 시 AI 기능 숨김 — P0

### 6-2. AI 요약 UI

- [ ] `AiSummaryViewModel.kt` — AI 요약 ViewModel
- [ ] `AiSummaryScreen.kt` — AI 요약 상세 화면
- [ ] `AiInsightCard.kt` — 공통 AI 카드 컴포넌트 (글래스모피즘 + noise 텍스쳐)
  - 대시보드: "AI Summary" 한줄 요약 + "View Details →"
  - 통계: "AI Smart Analysis" 심층 분석 + 절약 조언
  - 예산: "Editorial Insight" 예산 초과 알림 + 리밸런싱 제안
  - 반복거래: 고정 지출 후 여유 예산 분석
- [ ] 월간 소비 요약 (2~3문장 자연어) — P1
- [ ] 절약 조언 (지출 기반 절약 항목 제안) — P1
- [ ] 카테고리 자동 추천 (거래 메모 → AI Recommend 칩) — P2
- [ ] 전월 대비 분석 요약 — P2
- [ ] "다시 분석" 버튼

---

## Phase 7: 수익화 — Google AdMob 광고 통합 (추후) 📋 예정

### 7-1. AdMob 연동 (F09)

- [ ] Google AdMob 계정 + 앱 등록
- [ ] `AdBanner.kt` — 하단 네비 바 위 320x50 배너 광고 컴포넌트 — P2
- [ ] 인터스티셜 광고 (통계 페이지 진입 시) — P2
- [ ] 광고 제거 옵션 (향후 유료 기능) — P2

---

## 유틸리티 & 공통

- [ ] `DateUtils.kt` — 날짜 포맷 유틸리티 (yyyy-MM-dd, 월 시작/끝일 계산)
- [ ] `CryptoUtils.kt` — PIN SHA-256 해시 유틸
- [ ] `FormatUtils.kt` — 금액 포맷 (1,000 단위 콤마 + "KRW" 단위 표시)

---

## 테스트

- [ ] `TransactionDao` 단위 테스트 (Room in-memory DB)
- [ ] `CategoryDao` 단위 테스트
- [ ] `BudgetDao` 단위 테스트
- [ ] `RecurringDao` 단위 테스트
- [ ] `TransactionRepository` 단위 테스트 (MockK)
- [ ] `RecurringRepository` — 자동 실행 로직 테스트 (밀린 달 처리)
- [ ] `BackupRepository` — 백업 & 복원 테스트
- [ ] Compose UI 테스트 (`ui-test-junit4`)
- [ ] Room 마이그레이션 테스트 (`room-testing`)

---

## 디자인 참조 파일 (stitch/)

| 화면 | 파일 | 설명 |
|---|---|---|
| 디자인 시스템 | `stitch/indigo_ledger/DESIGN.md` | "The Sovereign Ledger" 전체 디자인 규칙 |
| 전체 명세 | `stitch/moneylog_ui_design_specification.html` | UI 설계 명세 HTML |
| PIN 잠금 | `stitch/pin_lock/code.html` | PIN 입력 화면 |
| 대시보드 | `stitch/dashboard/code.html` | 메인 대시보드 |
| 거래 등록 | `stitch/add_transaction/code.html` | 거래 등록 폼 |
| 거래 내역 | `stitch/history/code.html` | 거래 목록 |
| 통계 | `stitch/statistics/code.html` | 통계 + AI 분석 |
| 예산 관리 | `stitch/budget_management/code.html` | 예산 관리 |
| 반복 거래 | `stitch/fixed_transactions/code.html` | 고정 수입·지출 |
| 설정 | `stitch/settings/code.html` | 설정 화면 |

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
- [x] Android Studio 프로젝트 초기 생성 (`com.moneylog`) — `settings.gradle.kts`, `build.gradle.kts`
- [x] `gradle/libs.versions.toml` 버전 카탈로그 구성
- [x] `app/build.gradle.kts` 의존성 설정 (Compose, Room, Hilt, Vico 등)
- [x] 프로젝트 구조 생성 (data/db/dao, data/db/entity, data/repository, data/worker, ui/screen, ui/viewmodel, ui/component, ui/navigation, ui/theme, di, util)
- [x] Git 저장소 초기화 및 `.gitignore` 설정

---

## Phase 2: 코어 기능 — 거래 CRUD, 카테고리, Room DB (2주) ✅ 완료

### 2-1. Room 데이터베이스 설정

- [x] `AppDatabase.kt` — Room Database 클래스 생성 (version=1)
- [x] `TransactionEntity.kt` — 거래 엔티티 정의
- [x] `CategoryEntity.kt` — 카테고리 엔티티 정의
- [x] `BudgetEntity.kt` — 예산 엔티티 정의
- [x] `RecurringEntity.kt` — 반복 거래 엔티티 정의
- [x] 인덱스 설정 (`idx_tx_date`, `idx_tx_category`, `idx_tx_type`, `idx_tx_deleted`)
- [x] `uq_budget_category_month` 유니크 인덱스 설정

### 2-2. DAO 구현

- [x] `TransactionDao.kt` — 거래 CRUD + 월별 요약 + 카테고리별 집계 쿼리
- [x] `CategoryDao.kt` — 카테고리 CRUD + 타입별 조회
- [x] `BudgetDao.kt` — 예산 CRUD + 월별 조회
- [x] `RecurringDao.kt` — 반복 거래 CRUD + 미실행 조회

### 2-3. Repository 구현

- [x] `TransactionRepository.kt` — 월별 조회, 생성, 수정, 소프트 삭제
- [x] `CategoryRepository.kt` — 카테고리 관리 + 기본 카테고리 시드
- [x] `BudgetRepository.kt` — 예산 관리
- [x] `RecurringRepository.kt` — 반복 거래 관리 + 자동 실행 로직

### 2-4. DI (Hilt) 설정

- [x] `AppModule.kt` — Room DB, DAO, Repository Hilt 모듈 정의
- [x] `Application` 클래스에 `@HiltAndroidApp` 적용

### 2-5. 카테고리 기본 데이터

- [x] 기본 지출 카테고리 10개 시드 — **Material Symbols 아이콘명** 사용 (restaurant, directions_bus, home, sports_esports, checkroom, local_hospital, menu_book, redeem, local_cafe, inventory_2)
- [x] 기본 수입 카테고리 5개 시드 — Material Symbols 아이콘명 (payments, account_balance_wallet, savings, trending_up, inventory_2)
- [x] `RoomDatabase.Callback`으로 첫 실행 시 자동 삽입

### 2-6. 디자인 시스템 & 테마 구현

> **참조**: stitch/indigo_ledger/DESIGN.md — "The Sovereign Ledger"

#### 색상 팔레트 (Material 3 Surface Hierarchy)

- [x] `Color.kt` — 전체 색상 토큰 정의
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

- [x] `Type.kt` — Manrope + Pretendard 타이포그래피 스케일
- [x] `fonts/` 디렉토리에 Manrope(3종), Pretendard(4종) TTF 파일 추가
  - Manrope v20: manrope_semibold.ttf(600), manrope_bold.ttf(700), manrope_extrabold.ttf(800) — Google Fonts CDN
  - Pretendard v1.3.9: pretendard_regular.ttf(400), pretendard_medium.ttf(500), pretendard_semibold.ttf(600), pretendard_bold.ttf(700) — MIT License

#### 테마 & 디자인 원칙

- [x] `Theme.kt` — Material 3 커스텀 ColorScheme (위 색상 토큰 기반)
  - 라이트/다크 모드 각각 정의
  - Dynamic Color 비활성 (고정 브랜드 컬러 사용)
- [x] **"No-Line" 정책 적용**: 1px 디바이더 사용 금지
  - 리스트 아이템: 16dp 수직 여백으로 분리
  - 섹션 구분: 배경색 변화 (surface ↔ surface-container-low)
- [x] **Tonal Layering**: 카드 = surface-container-lowest(#FFFFFF) on surface-container-low(#F3F4F5)
- [x] **카드 스타일**: xxl (1.5rem = 24dp) 라운드 코너, 드롭 섀도 없음 (`Widget.MoneyLog.Card`)
- [ ] **Ambient Shadow**: FAB/Modal용 — blur 32px, opacity 4%, Indigo 틴트
- [x] **버튼 스타일**:
  - Primary: `Widget.MoneyLog.Button.Primary` (full 999dp 라운딩)
  - Secondary: `Widget.MoneyLog.Button.Outlined`
  - Press 상태: scale 96%
- [x] **입력 필드 스타일**: "Plinth" — `Widget.MoneyLog.TextInputLayout` (테두리 없음, surface-container-highest 배경, 12dp 라운딩)

#### 공통 컴포넌트

- [x] `NavGraph.kt` — 네비게이션 그래프 (하단 5탭: 홈, 내역, 등록, 통계, 설정)
- [x] 하단 `NavigationBar` (`BottomBar.kt`) — Material Symbols Outlined 아이콘
- [x] `AiInsightCard.kt` — AI Insight 카드 공통 컴포넌트
- [x] `CategoryIcon.kt` — Material Symbols Outlined 기반 카테고리 아이콘
- [x] `OverlineLabel.kt` — 오버라인 텍스트 (Label-MD, 대문자, primary 색상)
- [x] `GradientButton.kt` — Primary CTA 그라디언트 버튼 (primary → primary-container)
- [ ] `TopAppBar` 공통 컴포넌트 — Glassmorphism (surface_bright 80% opacity + backdrop-blur 16px)
- [ ] `EditorialMargin` — 좌우 패딩: `max(1.5rem, 5vw)` 대응값

#### 유틸리티

- [x] `DateUtils.java` — 날짜 포맷, 월 계산, yearMonth 헬퍼
- [x] `FormatUtils.java` — 금액 콤마 포맷, 퍼센트 포맷, 입력 파싱
- [x] `CryptoUtils.java` — PBKDF2WithHmacSHA256 PIN 해시/검증 (salt + 120k iterations + timing-safe 비교)

### 2-7. 거래 관련 UI (F02)

- [x] `TransactionViewModel.java` — 거래 목록 ViewModel (월별 조회, 타입 필터, CRUD)
- [x] `TransactionFragment.java` + `fragment_transaction.xml` — 거래 내역 화면
- [x] `TransactionFormFragment.java` + `fragment_transaction_form.xml` — 거래 등록/수정 폼
- [x] `TransactionAdapter.java` — 거래 목록 RecyclerView 어댑터
- [x] `MainActivity.java` — NavController + BottomNavigationView 연결
- [ ] 거래 검색 (메모 키워드, 금액 범위, 카테고리 필터) — P1

### 2-8. 카테고리 관리 UI (F04)

- [x] 카테고리 목록 화면 (`CategoryFragment.java` + `fragment_category.xml`)
- [x] `CategoryViewModel.java` — 카테고리 CRUD ViewModel
- [x] `CategoryAdapter.java` — RecyclerView 어댑터
- [x] 커스텀 카테고리 추가 — BottomSheetDialog (`bottom_sheet_category.xml`)
- [x] 카테고리 수정·삭제 (아이콘, 이름)
- [ ] 카테고리 드래그 앤 드롭 정렬 — P2

---

## Phase 3: 자동화 — 고정 수입·지출, WorkManager 반복 거래 (1주) ✅ 완료

### 3-1. 반복 거래 UI (F03)

> **디자인 참조**: stitch/fixed_transactions/

- [x] `RecurringViewModel.java` — 반복 거래 ViewModel
- [x] `RecurringFragment.java` — 고정 수입·지출 관리 화면
  - "MONTHLY COMMITMENT" 헤더 카드 — primary 그라디언트 배경, 총 고정 지출 금액, "N items scheduled this month"
  - "Fixed Expenses" / "Fixed Income" 섹션 (Headline, 우측 "N ITEMS" 카운터)
  - 반복 거래 카드: 이름 + "매월 N일" + DEBIT/CREDIT 라벨 + 활성/비활성 토글 스위치
  - 하단 **AI Insight 카드**: 고정 지출 후 남은 여유 예산 분석
- [x] `RecurringFormFragment.java` — 반복 거래 등록/수정 폼 (금액, 카테고리, 매월 N일, 메모, 결제수단)
- [x] `item_recurring.xml` — 반복 거래 카드 레이아웃
- [x] `fragment_recurring.xml` — 반복 거래 목록 레이아웃
- [x] `fragment_recurring_form.xml` — 반복 거래 폼 레이아웃
- [x] 반복 거래 수정 — 편집 모드 (loadById)
- [x] 반복 거래 비활성화/활성화 토글

### 3-2. WorkManager 자동 실행

- [x] `RecurringWorker.java` — PeriodicWorkRequest (매일 1회), `@HiltWorker`
- [x] 앱 실행 시 즉시 미처리 반복 거래 실행 (`ExistingPeriodicWorkPolicy.KEEP`)
- [x] 밀린 달 한꺼번에 처리 로직 (`nextDate.plusMonths(1)` 루프)
- [x] 자동 등록된 거래에 `is_auto = true` 표시
- [x] `MoneyLogApplication.java` — HiltWorkerFactory 통합 + WorkManager 스케줄링
- [x] AndroidManifest.xml — WorkManager 자동 초기화 비활성화 (Hilt 수동 초기화)
- [ ] 대시보드 "Pending Updates" 카드에 자동 등록 알림 + "Confirm Now" 버튼 — Phase 4

---

## Phase 4: 대시보드·통계 — 차트, 예산, 월별 분석 (1주) ✅ 완료

### 4-1. 대시보드 (메인 화면)

> **디자인 참조**: stitch/dashboard/

- [x] `DashboardViewModel.java` — 월별 수입/지출/잔액 + 최근 거래
- [x] `DashboardFragment.java` + `fragment_dashboard.xml` — 대시보드 레이아웃
- [x] `StatisticsViewModel.java` + `StatisticsFragment.java` + `fragment_statistics.xml`
- [x] `BudgetViewModel.java` + `BudgetFragment.java` + `fragment_budget.xml`
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

## Phase 5: 백업 & 배포 — Google Drive 백업·복원 (1주) ⚙️ 일부 완료

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

- [x] `PinLockFragment.java` + `fragment_pin_lock.xml` — PIN 입력/설정 화면
- [x] PIN 4자리 설정 (EncryptedSharedPreferences + Android Keystore)
- [x] PBKDF2WithHmacSHA256 해시 (salt + 120k iterations + timing-safe 비교)
- [x] 브루트포스 보호 (5회 실패 → 5분 잠금)
- [x] `SettingsFragment.java` + `fragment_settings.xml` — 설정 화면
- [x] `BackupRepository.java` — stub 구현
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

## Phase 6: AI 요약 — Gemini Nano 온디바이스 소비 분석 (1주) ⚙️ Scaffold 완료

### 6-1. Gemini Nano 연동 (F08)

- [x] `AiSummaryRepository.java` — scaffold + isAvailable() 체크
- [x] `AiSummaryViewModel.java` + `AiSummaryFragment.java` + `fragment_ai_summary.xml`
- [ ] Google AI Edge SDK 의존성 추가 (`libs.versions.toml` 주석 해제)
- [ ] `AndroidManifest.xml`에 AICore 메타데이터 추가
- [ ] `AiSummaryRepository.java` — Gemini Nano 호출 + 폴백 로직
- [ ] 기기 호환성 체크 (`isAvailable()`) — 미지원 시 AI 기능 숨김

### 6-2. AI 요약 UI

- [x] AI 요약 상세 화면 (scaffold)
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

- [x] `DateUtils.java` — 날짜 포맷 유틸리티 (yyyy-MM-dd, 월 시작/끝일 계산)
- [x] `CryptoUtils.java` — PBKDF2WithHmacSHA256 PIN 해시 (salt + 120k iterations + timing-safe)
- [x] `FormatUtils.java` — 금액 포맷 (1,000 단위 콤마 + "원" 단위 표시)

---

## 보안 강화 (OWASP Mobile Top 10 대응) ✅ 완료

- [x] **CRITICAL**: `CryptoUtils.java` — SHA-256 raw hash → PBKDF2WithHmacSHA256 + salt(16B) + 120k iterations
- [x] **CRITICAL**: `CryptoUtils.verifyPin()` — `String.equals()` → `MessageDigest.isEqual()` (timing-safe)
- [x] **HIGH**: `PinLockFragment.java` — 브루트포스 보호 (5회 실패 → 5분 잠금, EncryptedSharedPreferences 에 저장)
- [x] **HIGH**: `RecurringWorker.java` — 민감 재무 데이터 Log.d/Log.e 제거
- [x] **양호**: `backup_rules.xml` + `data_extraction_rules.xml` — DB/SharedPrefs 백업 제외
- [x] **양호**: Room @Query + Prepared statements — SQL Injection 방어
- [x] **양호**: Release `isMinifyEnabled=true` + R8 난독화
- [x] **양호**: WebView 미사용 — XSS 위험 없음
- [x] **양호**: 하드코드된 시크릿 없음

---

## 테스트

- [ ] `TransactionDao` 단위 테스트 (Room in-memory DB)
- [ ] `CategoryDao` 단위 테스트
- [ ] `BudgetDao` 단위 테스트
- [ ] `RecurringDao` 단위 테스트
- [ ] `TransactionRepository` 단위 테스트
- [ ] `RecurringRepository` — 자동 실행 로직 테스트 (밀린 달 처리)
- [ ] `BackupRepository` — 백업 & 복원 테스트
- [ ] `CryptoUtils` — PBKDF2 해시/검증 테스트
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

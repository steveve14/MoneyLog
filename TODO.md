# MoneyLog - 개발 TODO 리스트

> **최종 업데이트**: 2026-04-16
> **디자인 시스템**: "The Sovereign Ledger" (High-End Editorial)
> **기술 스택**: Java 17 + Material 3 XML + ViewBinding (Compose 미사용)
> **DB 버전**: 4 (v3→v4: recurring_transactions에 sort_order 추가)

---

## Phase 1: 기획/설계 (1주) ✅ 완료

- [x] 요구사항 분석 (기능 요구사항 문서 작성)
- [x] DB 모델링 (ERD, 테이블 정의)
- [x] 시스템 아키텍처 설계
- [x] UI 화면 설계 (와이어프레임)
- [x] 기술 스택 선정
- [x] 데이터 레이어 설계 (DAO, Repository)
- [x] 개발환경 설정 가이드 작성
- [x] UI 디자인 시스템 확정 — "The Sovereign Ledger" (stitch/serene_ledger/DESIGN.md)
- [x] 화면별 디자인 목업 완성 (stitch/ — 8개 화면 HTML/이미지)
- [x] Android Studio 프로젝트 초기 생성 (`com.moneylog`) — `settings.gradle.kts`, `build.gradle.kts`
- [x] `gradle/libs.versions.toml` 버전 카탈로그 구성
- [x] `app/build.gradle.kts` 의존성 설정 (Material 3, Room, Hilt, WorkManager 등)
- [x] 프로젝트 구조 생성 (data/db/dao, data/db/entity, data/repository, data/worker, ui/fragment, ui/viewmodel, ui/adapter, ui/widget, di, util)
- [x] Git 저장소 초기화 및 `.gitignore` 설정

---

## Phase 2: 코어 기능 — 거래 CRUD, 카테고리, Room DB (2주) ✅ 완료

### 2-1. Room 데이터베이스 설정

- [x] `AppDatabase.java` — Room Database 클래스 (현재 version=3, 마이그레이션 1→2→3 포함)
- [x] `TransactionEntity.java` — 거래 엔티티 정의 (recurring_id, is_auto 필드 포함)
- [x] `CategoryEntity.java` — 카테고리 엔티티 정의 (icon_name, is_default, sort_order)
- [x] ~~`BudgetEntity.java`~~ — ❌ DB v3에서 budgets 테이블 제거 (마이그레이션 2→3)
- [x] `RecurringEntity.java` — 반복 거래 엔티티 정의 (interval_type: DAILY/WEEKLY/MONTHLY/YEARLY)
- [x] 인덱스 설정 (`idx_tx_date`, `idx_tx_category`, `idx_tx_type`, `idx_tx_deleted`)
- [x] ~~`uq_budget_category_month` 유니크 인덱스~~ — budgets 테이블과 함께 제거됨

### 2-2. DAO 구현

- [x] `TransactionDao.java` — 거래 CRUD + 월별 요약 + 카테고리별 집계 + 일별 집계 쿼리
- [x] `CategoryDao.java` — 카테고리 CRUD + 타입별 조회
- [x] ~~`BudgetDao.java`~~ — ❌ budgets 테이블 제거로 삭제됨
- [x] `RecurringDao.java` — 반복 거래 CRUD + 활성/비활성 조회 + WorkManager용 동기 조회
- [x] `MonthlySummary.java` — 월별 합계 POJO
- [x] `CategorySummary.java` — 카테고리별 집계 POJO
- [x] `DailySummary.java` — 일별 합계 POJO

### 2-3. Repository 구현

- [x] `TransactionRepository.java` — 월별 조회, 생성, 수정, 소프트 삭제
- [x] `CategoryRepository.java` — 카테고리 관리 + 기본 카테고리 시드
- [x] ~~`BudgetRepository.java`~~ — ❌ budgets 테이블 제거로 삭제됨
- [x] `RecurringRepository.java` — 반복 거래 관리 + 자동 실행 로직

### 2-4. DI (Hilt) 설정

- [x] `AppModule.java` — Room DB, DAO(Transaction/Category/Recurring), Repository Hilt 모듈 정의
- [x] `MoneyLogApplication.java` — `@HiltAndroidApp` 적용 + HiltWorkerFactory 통합

### 2-5. 카테고리 기본 데이터

- [x] 기본 지출 카테고리 10개 시드 — **Material Symbols 아이콘명** 사용 (restaurant, directions_bus, home, sports_esports, checkroom, local_hospital, menu_book, redeem, local_cafe, inventory_2)
- [x] 기본 수입 카테고리 5개 시드 — Material Symbols 아이콘명 (payments, account_balance_wallet, savings, trending_up, inventory_2)
- [x] `RoomDatabase.Callback`으로 첫 실행 시 자동 삽입

### 2-6. 디자인 시스템 & 테마 구현

> **참조**: stitch/serene_ledger/DESIGN.md

#### 색상 팔레트 (Material 3 Surface Hierarchy)

- [x] `colors.xml` — 전체 색상 토큰 정의 (XML 리소스)
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

- [x] `themes.xml` — Manrope + Pretendard 타이포그래피 스케일 (XML fontFamily 적용)
- [x] `fonts/` 디렉토리에 Manrope(3종), Pretendard(4종) TTF 파일 추가
  - Manrope v20: manrope_semibold.ttf(600), manrope_bold.ttf(700), manrope_extrabold.ttf(800) — Google Fonts CDN
  - Pretendard v1.3.9: pretendard_regular.ttf(400), pretendard_medium.ttf(500), pretendard_semibold.ttf(600), pretendard_bold.ttf(700) — MIT License

#### 테마 & 디자인 원칙

- [x] `themes.xml` — Material 3 커스텀 테마 (`Theme.MoneyLog` + Widget 스타일)
  - 라이트 모드 정의 (다크 모드 미구현)
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

- [x] `nav_graph.xml` — 네비게이션 그래프 (8개 destination: Onboarding, Dashboard, Transaction, TransactionForm, Statistics, Category, Settings, AiSummary)
- [x] 하단 `NavigationBar` (`bottom_nav_menu.xml` + `activity_main.xml`) — 5탭: 홈/거래/추가/통계/AI
- [x] `PieChartView.java` — 커스텀 원형 차트 위젯 (카테고리별 지출 시각화)
- [x] `IconHelper.java` — Material Symbols Outlined 기반 카테고리 아이콘 매핑
- [x] `IconPickerAdapter.java` — 아이콘 선택 그리드 RecyclerView 어댑터
- [x] XML 스타일 기반 오버라인/그라디언트 버튼 (`Widget.MoneyLog.Button.Primary`, `bg_primary_gradient`)
- [ ] `TopAppBar` 공통 컴포넌트 — Glassmorphism (surface_bright 80% opacity + backdrop-blur 16px)
- [ ] `EditorialMargin` — 좌우 패딩: `max(1.5rem, 5vw)` 대응값

#### 유틸리티

- [x] `DateUtils.java` — 날짜 포맷, 월 계산, yearMonth 헬퍼
- [x] `FormatUtils.java` — 금액 콤마 포맷, 퍼센트 포맷, 입력 파싱
- [x] `IconHelper.java` — 카테고리 아이콘 헬퍼
- [x] `YearMonthPickerDialog.java` — 년/월 선택 다이얼로그
- [x] `DataManagementHelper.java` — CSV 내보내기/가져오기 헬퍼

### 2-7. 거래 관련 UI (F02)

- [x] `TransactionViewModel.java` — 거래 목록 ViewModel (월별 조회, 타입 필터, CRUD)
- [x] `TransactionFragment.java` + `fragment_transaction.xml` — 거래 내역 화면
- [x] `TransactionFormFragment.java` + `fragment_transaction_form.xml` — 거래 등록/수정 폼
- [x] `TransactionAdapter.java` — 거래 목록 RecyclerView 어댑터
- [x] `MainActivity.java` — NavController + BottomNavigationView 연결
- [x] 거래 검색 (메모 키워드, 금액 범위, 카테고리 필터) — `TransactionFragment`에서 메모/금액/카테고리명 필터링 구현

### 2-8. 카테고리 관리 UI (F04)

- [x] 카테고리 목록 화면 (`CategoryFragment.java` + `fragment_category.xml`)
- [x] `CategoryViewModel.java` — 카테고리 CRUD ViewModel
- [x] `CategoryAdapter.java` — RecyclerView 어댑터
- [x] 커스텀 카테고리 추가 — BottomSheetDialog (`bottom_sheet_category.xml`)
- [x] 카테고리 수정·삭제 (아이콘, 이름)
- [x] 지출/수입 세그먼트 토글 배경색 분리 (`surface_container`로 구분)
- [x] 카테고리 드래그 앤 드롭 정렬 — `ItemTouchHelper` + `CategoryViewModel.updateSortOrders()` 구현

---

## Phase 3: 자동화 — 고정 수익·지출, WorkManager 반복 거래 (1주) ✅ 완료

### 3-1. 반복 거래 데이터 레이어 ✅

- [x] `RecurringViewModel.java` — 반복 거래 ViewModel (CRUD + loadById + setActive 토글)
- [x] `RecurringDao.java` — 전체 조회, 타입별 조회, 활성화/비활성화, WorkManager용 동기 조회
- [x] `RecurringRepository.java` — 반복 거래 관리 + 자동 실행 로직

### 3-2. 반복 거래 UI (F03) ✅

> **디자인 참조**: stitch/fixed_transactions_serene/

- [x] `RecurringFragment.java` + `fragment_recurring.xml` — 반복 거래 목록 화면 (활성/비활성 토글, 드래그 앤 드롭 정렬)
- [x] `RecurringAdapter.java` — 반복 거래 목록 RecyclerView 어댑터 (드래그 핸들, ItemTouchHelper)
- [x] ~~`RecurringFormFragment.java`~~ — 별도 폼 없이 `TransactionFormFragment`에서 고정거래 등록/수정 통합 처리
  - `recurringId` NavArg로 편집 모드 진입
  - 반복 주기 선택: DAILY / WEEKLY / MONTHLY / YEARLY 칩 그룹
  - 스케줄 입력: 요일 칩(WEEKLY), 일(MONTHLY), 월+일(YEARLY)
  - 고정거래 토글 시 날짜 입력 비활성화 (alpha 0.4)
  - 기존 거래 수정 시 고정거래 스위치 비활성화
- [x] `nav_graph.xml`에 `recurringFragment` destination 추가
- [x] 설정 화면에서 "고정거래 관리" 네비게이션 링크 추가
- [x] RecurringViewModel에 `updateSortOrders()` — 드래그 후 DB sort_order 동기화

### 3-3. WorkManager 자동 실행 ✅

- [x] `RecurringWorker.java` — PeriodicWorkRequest (매일 1회), `@HiltWorker`
- [x] 앱 실행 시 즉시 미처리 반복 거래 실행 (`ExistingPeriodicWorkPolicy.KEEP`)
- [x] 밀린 달 한꺼번에 처리 로직 (`nextDate.plusMonths(1)` 루프)
- [x] 자동 등록된 거래에 `is_auto = true` 표시
- [x] `MoneyLogApplication.java` — HiltWorkerFactory 통합 + WorkManager 스케줄링
- [x] AndroidManifest.xml — WorkManager 자동 초기화 비활성화 (Hilt 수동 초기화)
- [ ] 대시보드 "Pending Updates" 카드에 자동 등록 알림 + "Confirm Now" 버튼 — Phase 4

---

## Phase 4: 대시보드·통계 — 차트, 예산, 월별 분석 (1주) ⚙️ 일부 완료

### 4-1. 대시보드 (메인 화면) ✅

> **디자인 참조**: stitch/dashboard_serene/

- [x] `DashboardViewModel.java` — 월별 수입/지출/잔액 + 카테고리별 지출 + 최근 거래
- [x] `DashboardFragment.java` + `fragment_dashboard.xml` — 대시보드 레이아웃
  - 월 네비게이션 (이전/다음 달 + 년월 피커)
  - 잔액/수입/지출 표시
  - 카테고리별 막대 그래프 (Top 8) + 원형 차트 (`PieChartView`)
  - 최근 거래 3건 표시
- [x] `StatisticsViewModel.java` + `StatisticsFragment.java` + `fragment_statistics.xml` — 통계 화면 (Java/XML)

#### 대시보드 디자인 리뉴얼 (미적용)

- [ ] "FINANCIAL OVERVIEW" 오버라인 + "April 2026" Headline (에디토리얼 좌측 정렬)
- [ ] **Total Balance 히어로 카드**: primary 그라디언트 배경, 잔액 Display-LG (흰색)
- [ ] **Pending Updates 카드**: 자동 등록 알림 — "N Auto-registered transactions detected", "Confirm Now" 버튼
- [ ] **AI Summary 카드**: secondary-fixed-dim 글래스모피즘, sparkle 아이콘
- [ ] **Spending Distribution**: 도넛 차트 + 중앙 "Top Category" + 범례 리스트

### 4-2. 통계·분석 (F06) ✅ 기본 + 확장 구현 완료

> **디자인 참조**: stitch/statistics_serene/

- [x] `StatisticsViewModel.java` — 월별 합계 + 카테고리별 지출/수익 집계 (type 필드 동적 필터)
- [x] `StatisticsFragment.java` + `fragment_statistics.xml` — 원형 차트 + 카테고리 분류 리스트 + 월 선택
- [x] `item_statistics_category.xml` — 카테고리별 진행 바 + 금액/비율 표시
- [x] 지출/수익 토글 — 월 네비게이션 하단 세그먼트 버튼 (`btnStatExpense` / `btnStatIncome`)
- [x] 토글에 따른 동적 레이블 변경 ("지출 추이" ↔ "수익 추이", "지출" ↔ "수익")
- [x] MPAndroidChart 도넛 차트 클릭 시 중앙 금액 표시 (Dashboard와 동일 패턴)
- [x] 전월 대비 비교 카드 (증감액, 증감률)
- [x] 헤더 숨김 — 스크롤 시 `llHeader` 숨김/표시 (ViewTreeObserver)

#### 통계 디자인 리뉴얼 (미적용)

- [ ] **AI Smart Analysis 카드**: 글래스모피즘 + sparkle 아이콘, 소비 분석 요약
- [ ] **전월 대비 메트릭 카드 2개**: "MONTHLY TOTAL +5.2%" / "DINING FOCUS -3.1%"
- [ ] **Spending Trend 차트**: 라인 차트 (월별 추이, "Last 6 Months") — P1
- [ ] 전월 대비 카테고리별 증감 메트릭 카드 — P2
- [ ] 일별 지출 히트맵 (캘린더 뷰) — P2

### 4-3. 예산 관리 (F05) ❌ 미구현 (DB에서 제거됨)

> **디자인 참조**: stitch/budget_management_serene/
> ⚠️ DB v3 마이그레이션에서 budgets 테이블이 제거됨. 재구현 시 DB v4 마이그레이션 필요.

- [ ] `BudgetEntity.java` — 예산 엔티티 재정의 + DB v4 마이그레이션
- [ ] `BudgetDao.java` — 예산 CRUD + 월별 조회
- [ ] `BudgetRepository.java` — 예산 관리 로직
- [ ] `BudgetViewModel.java` — 예산 ViewModel
- [ ] `BudgetFragment.java` + `fragment_budget.xml` — 예산 관리 화면
- [ ] 예산 소진율 프로그레스 바 컴포넌트 (색상 단계: primary → tertiary → error)
- [ ] 월별 총 예산 설정 — P1
- [ ] 카테고리별 예산 설정 (식비 30만, 교통 10만 등) — P1
- [ ] 카테고리 태그 분류 (Essential / Utility / Warning) — P1
- [ ] 예산 초과 알림 (80%/100%) — Android Notification — P2

---

## Phase 5: 백업 & 배포 — Google Drive 백업·복원 (1주) ⚙️ 일부 완료

### 5-1. Google 인증

- [x] Google Sign-In 연동 — `drive.appdata` scope (Play Services Auth)
- [ ] Google Cloud Console OAuth 2.0 클라이언트 ID 등록
- [ ] SHA-1 지문 등록 (debug + release)

### 5-2. Google Drive 백업·복원 (F07)

- [x] `BackupRepository.java` — DB 파일 백업/복원 로직
  - [x] WAL checkpoint → DB 파일 복사
  - [x] Google Drive `appDataFolder`에 업로드/업데이트
  - [x] 복원 시 DB 교체 + 앱 재시작
- [x] Google Drive 연동 상태 표시 (미연동: "Google Drive 연동", 연동: 이메일 표시)
- [x] 수동 백업 버튼 — P1
- [x] 복원 버튼 + 확인 다이얼로그 — P1
- [x] 연동 해제 기능
- [ ] 백업 목록 조회 (날짜·크기 확인) — P2
- [ ] 자동 백업 (WorkManager 주기적 실행: 매일/매주) — P2
- [ ] 백업 파일 AES 암호화 — P2

### 5-3. 설정 화면 ✅

- [x] `SettingsFragment.java` + `fragment_settings.xml` — 설정 화면
- [x] `BackupRepository.java` — Google Drive 백업/복원 구현
- [x] 언어 선택 (한국어/English/日本語) — `LocaleHelper.java`
- [x] 금액 텍스트 표시 모드 토글
- [x] 카테고리 관리 네비게이션 (`categoryFragment`로 이동)
- [x] 데이터 관리 (CSV 내보내기/가져오기/정리) — `DataManagementHelper.java`
- [x] MaterialAlertDialogBuilder (Serene Ledger 28dp 라운드) 적용
- [x] 온보딩 화면에서 하단 네비게이션 숨김
- [x] AI Insights 섹션 (Gemini Nano 지원 상태 표시)
- [x] 카테고리 전체 삭제 / 기본값 리셋
- [x] 전체 데이터 리셋 기능
- [x] 고정거래 관리 네비게이션 링크 추가

### 5-4. 설정 화면 디자인 리뉴얼 (미적용)

> **디자인 참조**: stitch/settings_serene/

- [ ] `SettingsViewModel.kt` — 설정 ViewModel
- [ ] 설정 화면 디자인 리뉴얼 (에디토리얼 스타일)
  - "PREFERENCES" 오버라인 + "Control your financial footprint" Headline
  - **Data Management 섹션**:
    - "Fixed Transactions" → 반복 거래 관리 연결
    - "Category Management" → 카테고리 관리 연결
  - **Backup & Sync 섹션**:
    - "Backup Now" 그라디언트 버튼 + "Restore" 아웃라인 버튼
  - **AI Insights 섹션**:
    - Gemini Nano Status 카드 (글래스모피즘)
  - **위험 영역**:
    - "Reset All Data" 빨간 버튼 (error-container 배경)
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

## Phase 6: AI 요약 — 온디바이스 소비 분석 (1주) ✅ 완료

### 6-1. AI 분석 엔진

- [x] `AiSummaryRepository.java` — 로컬 규칙 기반 분석 엔진 구현 (수입/지출 합계, 저축률, 카테고리 Top5, 일별 패턴, 절약 조언)
- [x] `AiSummaryViewModel.java` — 월 선택 + analyze() API 연동
- [x] `AiSummaryFragment.java` + `fragment_ai_summary.xml` — 월 네비게이션 + 분석 결과 UI
- [x] Gemini Nano 호환성 체크 (`isGeminiAvailable()`) — 미지원 시 로컬 분석 폴백
- [x] 다국어 문자열 추가 (`ai_local_analysis`, `ai_no_data`, `ai_analysis_failed`)

### 6-2. AI 요약 UI

- [x] AI 요약 상세 화면
- [x] 월간 소비 요약 (수입/지출/저축률 + 이모지 피드백)
- [x] 절약 조언 (지출 기반 절약 항목 제안)
- [x] 카테고리별 지출 비중 분석 (Top 5)
- [x] 일별 지출 패턴 (평균 + 최대 지출일)
- [x] "분석 시작" 버튼

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
- [x] `FormatUtils.java` — 금액 포맷 (1,000 단위 콤마 + "원" 단위 표시)
- [x] `IconHelper.java` — 카테고리 아이콘 리소스 매핑
- [x] `YearMonthPickerDialog.java` — 년/월 선택 다이얼로그 (NumberPicker 기반)
- [x] `LocaleHelper.java` — 다국어 지원 (한국어/영어/일본어) + 온보딩 완료 여부 관리
- [x] `DataManagementHelper.java` — CSV 내보내기/가져오기 + 데이터 정리
  - [x] CSV 내보내기에 `[RECURRING]` 섹션 포함 (intervalType/dayOfMonth/monthOfYear/isActive/sortOrder)
  - [x] CSV 가져오기에서 `[RECURRING]` 섹션 파싱 (구 형식 isAuto 기반 수정 호환)

---

## 보안 강화 (OWASP Mobile Top 10 대응) ✅ 완료

- [x] **HIGH**: `RecurringWorker.java` — 민감 재무 데이터 Log.d/Log.e 제거
- [x] **양호**: `backup_rules.xml` + `data_extraction_rules.xml` — DB/SharedPrefs 백업 제외
- [x] **양호**: Room @Query + Prepared statements — SQL Injection 방어
- [x] **양호**: Release `isMinifyEnabled=true` + R8 난독화
- [x] **양호**: WebView 미사용 — XSS 위험 없음
- [x] **양호**: 하드코드된 시크릿 없음

> ❌ PIN 잠금 기능 제거됨 (2026-04-13) — CryptoUtils.java, PinLockFragment 관련 코드 전체 삭제

---

## 테스트

- [ ] `TransactionDao` 단위 테스트 (Room in-memory DB)
- [ ] `CategoryDao` 단위 테스트
- [ ] `RecurringDao` 단위 테스트
- [ ] `TransactionRepository` 단위 테스트
- [ ] `RecurringRepository` — 자동 실행 로직 테스트 (밀린 달 처리)
- [ ] `BackupRepository` — 백업 & 복원 테스트
- [ ] Room 마이그레이션 테스트 (`room-testing`, v1→v2→v3→v4)

---

## 디자인 참조 파일 (stitch/)

| 화면 | 파일 | 설명 |
|---|---|---|
| 디자인 시스템 | `stitch/serene_ledger/DESIGN.md` | "The Sovereign Ledger" 전체 디자인 규칙 |
| 대시보드 | `stitch/dashboard_serene/code.html` | 메인 대시보드 |
| 거래 등록 | `stitch/add_transaction_serene/code.html` | 거래 등록 폼 |
| 거래 내역 | `stitch/history_serene/code.html` | 거래 목록 |
| 통계 | `stitch/statistics_serene/code.html` | 통계 + AI 분석 |
| 예산 관리 | `stitch/budget_management_serene/code.html` | 예산 관리 |
| 반복 거래 | `stitch/fixed_transactions_serene/code.html` | 고정 수입·지출 |
| 설정 | `stitch/settings_serene/code.html` | 설정 화면 |

---

## UX 개선 ✅ 완료

### 용어 통일

- [x] "수입" → "수익" 전체 리네이밍 (ko/en/ja strings.xml 포함)
  - `transaction_income`, `dashboard_monthly_income`, `recurring_fixed_income`, `ai_total_income` 등

### 차트 개선

- [x] MPAndroidChart 도넛 차트 범례: 색상 + 카테고리명만 표시 (금액/비율 제거)
- [x] 차트 범례 하단 마진 추가
- [x] 대시보드 ↔ 통계 도넛 차트 클릭 동작 통일 (center text 금액 표시)

### 스크롤 UX

- [x] 글로벌 스크롤 탑 FAB — `activity_main.xml` + `MainActivity.java`
  - 모든 메인 탭에서 동일 위치에 표시 (BottomNav 상단 24dp)
  - ScrollView / RecyclerView 자동 감지
  - 400px 이상 스크롤 시 fade-in, 돌아갈 때 fade-out
  - 페이지 전환 시 리스너 자동 재연결
- [x] 통계 화면 헤더 숨김 — 스크롤 시 `llHeader` 숨김/표시
- [x] 거래내역 추가(fab_add) FAB 제거 (하단 네비 "추가" 탭으로 통합)

### 거래 폼 개선

- [x] "거래일" → "날짜" 레이블 변경
- [x] 고정거래 토글 시 날짜 입력 비활성화 (alpha 0.4, 클릭 불가)
- [x] 거래 수정 모드에서 고정거래 스위치 비활성화 (switchRecurring.setEnabled(false))
- [x] 중복 거래 등록 방지 로직

---

## 미완료 목록

> 전체 TODO에서 완료되지 않은 항목을 Phase/섹션별로 정리

### Phase 2: 코어 기능

#### 디자인 시스템 & 테마

- [ ] **Ambient Shadow**: FAB/Modal용 — blur 32px, opacity 4%, Indigo 틴트
- [ ] `TopAppBar` 공통 컴포넌트 — Glassmorphism (surface_bright 80% opacity + backdrop-blur 16px)
- [ ] `EditorialMargin` — 좌우 패딩: `max(1.5rem, 5vw)` 대응값

### Phase 3: 자동화

- [ ] 대시보드 "Pending Updates" 카드에 자동 등록 알림 + "Confirm Now" 버튼 — Phase 4

### Phase 4: 대시보드·통계

#### 대시보드 디자인 리뉴얼 (미적용)

- [ ] "FINANCIAL OVERVIEW" 오버라인 + "April 2026" Headline (에디토리얼 좌측 정렬)
- [ ] **Total Balance 히어로 카드**: primary 그라디언트 배경, 잔액 Display-LG (흰색)
- [ ] **Pending Updates 카드**: 자동 등록 알림 — "N Auto-registered transactions detected", "Confirm Now" 버튼
- [ ] **AI Summary 카드**: secondary-fixed-dim 글래스모피즘, sparkle 아이콘
- [ ] **Spending Distribution**: 도넛 차트 + 중앙 "Top Category" + 범례 리스트

#### 통계 디자인 리뉴얼 (미적용)

- [ ] **AI Smart Analysis 카드**: 글래스모피즘 + sparkle 아이콘, 소비 분석 요약
- [ ] **전월 대비 메트릭 카드 2개**: "MONTHLY TOTAL +5.2%" / "DINING FOCUS -3.1%"
- [ ] **Spending Trend 차트**: 라인 차트 (월별 추이, "Last 6 Months") — P1
- [ ] 전월 대비 카테고리별 증감 메트릭 카드 — P2
- [ ] 일별 지출 히트맵 (캘린더 뷰) — P2

#### 예산 관리 (F05) ❌ 미구현

- [ ] `BudgetEntity.java` — 예산 엔티티 재정의 + DB v4 마이그레이션
- [ ] `BudgetDao.java` — 예산 CRUD + 월별 조회
- [ ] `BudgetRepository.java` — 예산 관리 로직
- [ ] `BudgetViewModel.java` — 예산 ViewModel
- [ ] `BudgetFragment.java` + `fragment_budget.xml` — 예산 관리 화면
- [ ] 예산 소진율 프로그레스 바 컴포넌트 (색상 단계: primary → tertiary → error)
- [ ] 월별 총 예산 설정 — P1
- [ ] 카테고리별 예산 설정 (식비 30만, 교통 10만 등) — P1
- [ ] 카테고리 태그 분류 (Essential / Utility / Warning) — P1
- [ ] 예산 초과 알림 (80%/100%) — Android Notification — P2

### Phase 5: 백업 & 배포

#### Google 인증

- [ ] Google Cloud Console OAuth 2.0 클라이언트 ID 등록
- [ ] SHA-1 지문 등록 (debug + release)

#### Google Drive 백업·복원 (F07)

- [ ] 백업 목록 조회 (날짜·크기 확인) — P2
- [ ] 자동 백업 (WorkManager 주기적 실행: 매일/매주) — P2
- [ ] 백업 파일 AES 암호화 — P2

#### 설정 화면 디자인 리뉴얼 (미적용)

- [ ] `SettingsViewModel.kt` — 설정 ViewModel
- [ ] 설정 화면 디자인 리뉴얼 (에디토리얼 스타일)

#### 배포 준비

- [ ] 릴리스 서명 키 생성
- [ ] `app/build.gradle.kts` release 빌드 설정
- [ ] ProGuard/R8 난독화 규칙
- [ ] Google Play Console 앱 등록
- [ ] 스크린샷 / 스토어 설명 준비
- [ ] GitHub Actions CI/CD (main → 자동 빌드 → Play Store 업로드)

### Phase 7: 수익화 — AdMob

- [ ] Google AdMob 계정 + 앱 등록
- [ ] `AdBanner.kt` — 하단 네비 바 위 320x50 배너 광고 컴포넌트 — P2
- [ ] 인터스티셜 광고 (통계 페이지 진입 시) — P2
- [ ] 광고 제거 옵션 (향후 유료 기능) — P2

### 테스트

- [ ] `TransactionDao` 단위 테스트 (Room in-memory DB)
- [ ] `CategoryDao` 단위 테스트
- [ ] `RecurringDao` 단위 테스트
- [ ] `TransactionRepository` 단위 테스트
- [ ] `RecurringRepository` — 자동 실행 로직 테스트 (밀린 달 처리)
- [ ] `BackupRepository` — 백업 & 복원 테스트
- [ ] Room 마이그레이션 테스트 (`room-testing`, v1→v2→v3→v4)

---

## 현재 프로젝트 요약

### 구현된 소스 파일 (45개)

| 분류 | 파일 | 개수 |
|---|---|---|
| Activity | `MainActivity.java` | 1 |
| Application | `MoneyLogApplication.java` | 1 |
| Fragment | Onboarding, Dashboard, Transaction, TransactionForm, Statistics, Category, Settings, AiSummary, Recurring | 9 |
| ViewModel | Dashboard, Transaction, Statistics, Category, Recurring, AiSummary | 6 |
| Repository | Transaction, Category, Recurring, Backup, AiSummary | 5 |
| Entity | Transaction, Category, Recurring | 3 |
| DAO + POJO | TransactionDao, CategoryDao, RecurringDao, MonthlySummary, CategorySummary, DailySummary | 6 |
| Adapter | TransactionAdapter, CategoryAdapter, IconPickerAdapter, RecurringAdapter | 4 |
| Widget | PieChartView | 1 |
| Worker | RecurringWorker | 1 |
| DI | AppModule | 1 |
| Utility | DateUtils, FormatUtils, IconHelper, LocaleHelper, YearMonthPickerDialog, DataManagementHelper | 6 |
| Layout XML | 21개 (Fragment 9 + Item/Component 9 + Other 3) | 21 |

### 하단 네비게이션 탭 (5개)

| # | ID | 레이블 | 아이콘 |
|---|---|---|---|
| 1 | dashboardFragment | 홈 | ic_home_24 |
| 2 | transactionFragment | 거래 | ic_receipt_24 |
| 3 | transactionFormFragment | 추가 | ic_add_24 |
| 4 | statisticsFragment | 통계 | ic_bar_chart_24 |
| 5 | aiSummaryFragment | AI | ic_ai_24 |

> ⚠️ 설정(Settings)은 하단 탭이 아닌 앱 내부 네비게이션으로 접근

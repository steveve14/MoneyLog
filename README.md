# MoneyLog — 안드로이드 가계부 앱

오프라인 퍼스트 개인 가계부 앱. 모든 데이터는 사용자 기기에 저장되며, 선택적으로 Google Drive에 백업할 수 있습니다.

## 스크린샷

> _준비 중_

## 주요 기능

| 기능 | 설명 |
|---|---|
| **거래 관리** | 수익·지출 등록, 수정, 삭제, 검색 (카테고리·메모·결제수단) |
| **반복 거래** | WorkManager 기반 자동 등록 (DAILY/WEEKLY/MONTHLY/YEARLY), 드래그 정렬 |
| **카테고리** | 기본 15종 (지출 10 + 수익 5), 커스텀 추가 |
| **통계** | 월별 요약, 도넛 차트, 카테고리 비중, 지출/수익 토글, 전월 대비 |
| **AI 요약** | 온디바이스 규칙 기반 분석 (저축률, 패턴, 절약 팁), Gemini Nano 확장 예정 |
| **Google Drive 백업** | 수동·자동 백업/복원, 암호화 |
| **다국어** | 한국어(기본), English, 日本語 |
| **다크 모드** | 시스템 설정 연동 |
| **CSV 내보내기** | 거래 + 반복거래 섹션 포함 내보내기/가져오기 |
| **스크롤 UX** | 글로벌 스크롤 탑 FAB, 헤더 숨김, 부드러운 애니메이션 |

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 | Java 17 |
| UI | XML + ViewBinding + Material Design 3 |
| 아키텍처 | Single-Activity, Fragment Navigation, ViewModel + LiveData |
| DI | Hilt 2.56 |
| DB | Room 2.7.1 (SQLite), v4 |
| 차트 | MPAndroidChart 3.1.0 |
| 비동기 | Executor + LiveData |
| 백업 | Google Drive API v3 |
| 인증 | Google Sign-In (Credential Manager) |
| 빌드 | Gradle Kotlin DSL, AGP 8.9.0 |

## 디자인 시스템

**Serene Ledger** — "The Financial Sanctuary"

- Primary: `#506356` (Sage)
- Surface: `#FAF9F6` (Warm Off-White)
- Typography: Manrope
- Glass & Gradient, No-Line Rule, Tone Layering

## 빌드 방법

### 필수 환경

- Android Studio Ladybug 이상
- JDK 17
- Android SDK 35

### 빌드

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (서명 설정 필요)
./gradlew assembleRelease
```

빌드 산출물: `app/build/outputs/apk/`

## 프로젝트 구조

```
app/src/main/java/com/moneylog/
├── data/
│   ├── db/           # Room Database, Entity, DAO, Migration (v1→v4)
│   └── repository/   # Repository (AI, Category, Transaction, Recurring, Backup)
├── di/               # Hilt AppModule
├── ui/
│   ├── adapter/      # RecyclerView Adapters (Transaction, Category, Recurring, IconPicker)
│   ├── fragment/     # UI Fragments (Dashboard, Transaction, TransactionForm, Statistics, Recurring, Category, Settings, AiSummary, Onboarding)
│   └── viewmodel/    # ViewModels
├── util/             # FormatUtils, DateUtils, IconHelper, DataManagementHelper 등
└── worker/           # WorkManager Workers

app/src/main/res/
├── layout/           # 21개 레이아웃 XML
├── values/           # 한국어 (기본)
├── values-en/        # English
└── values-ja/        # 日本語
```

## 라이선스

> _추후 결정_

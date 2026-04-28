# pay-server DevOps 가이드라인

> Spring Boot 3.2.2 / Java 17 기반 pay-server를 대상으로  
> GitHub → GitHub Actions → EKS (ap-northeast-2) 파이프라인 구성 가이드

---

## 목차

1. [GitHub Private Repository 구성](#1-github-private-repository-구성)
2. [GitHub Actions CI Pipeline](#2-github-actions-ci-pipeline)

---

## 1. GitHub Private Repository 구성

### 1-1. 레포지토리 생성

1. GitHub → **New repository**
2. 설정값:
   - **Repository name**: `pay-server`
   - **Visibility**: `Private`
   - **Initialize with README**: 체크 해제 (기존 코드 push 예정)
3. **Create repository** 클릭

### 1-2. 로컬 프로젝트 연결

```bash
cd /path/to/pay-server

git init
git remote add origin git@github.com:<your-org>/pay-server.git

# .gitignore 확인 (빌드 산출물, 환경변수 파일 제외)
cat .gitignore

git add .
git commit -m "feat: initial commit"
git push -u origin main
```

### 1-3. 브랜치 전략 (권장)

```
main       ← 운영 배포 트리거
develop    ← 개발 통합 브랜치
feature/*  ← 기능 개발
hotfix/*   ← 긴급 패치
```

**Branch Protection Rules** (Settings → Branches → main):
- Require pull request reviews before merging: **1명 이상**
- Require status checks to pass before merging: **CI 워크플로 지정**
- Do not allow bypassing the above settings: **체크**

### 1-4. Secrets 등록

Settings → **Secrets and variables → Actions**에 아래 항목 등록:

| Secret 이름 | 값 |
|---|---|
| `HARBOR_USERNAME` | Harbor 계정명 |
| `HARBOR_PASSWORD` | Harbor 비밀번호 |
| `GITOPS_TOKEN` | GitOps 레포 쓰기 권한 PAT |

---

## 2. GitHub Actions CI Pipeline

### 2-1. 워크플로 파일 생성

`.github/workflows/ci.yaml`

```yaml
name: CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

env:
  IMAGE_NAME: amdp-registry.skala-ai.com/skala26a-cloud/pay-server

jobs:
  build-and-push:
    runs-on: ubuntu-latest

    steps:
      - name: 01. 소스코드 체크아웃
        uses: actions/checkout@v4

      - name: 02. JDK 17 설정
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: 03. Gradlew 실행 권한 부여
        run: chmod +x ./gradlew

      - name: 04. Gradle 빌드 (테스트 포함)
        run: ./gradlew clean build

      # PR은 여기까지만 실행 (이미지 빌드/푸시 제외)
      - name: 05. Docker Buildx 설정
        if: github.event_name == 'push'
        uses: docker/setup-buildx-action@v3

      - name: 06. Harbor 로그인
        if: github.event_name == 'push'
        uses: docker/login-action@v3
        with:
          registry: amdp-registry.skala-ai.com
          username: ${{ secrets.HARBOR_USERNAME }}
          password: ${{ secrets.HARBOR_PASSWORD }}

      - name: 07. 이미지 태그 생성
        if: github.event_name == 'push'
        id: meta
        run: |
          SHORT_SHA=$(echo ${{ github.sha }} | cut -c1-7)
          echo "tag=${SHORT_SHA}" >> $GITHUB_OUTPUT

      - name: 08. Docker 이미지 빌드 & Push
        if: github.event_name == 'push'
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.IMAGE_NAME }}:${{ steps.meta.outputs.tag }}
            ${{ env.IMAGE_NAME }}:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

```

### 2-2. 주요 동작 흐름

```
Pull Request  →  빌드 + 테스트 (이미지 푸시 없음)
Push to main  →  빌드 + 테스트 + 이미지 빌드/푸시
```

### 2-3. 빌드 캐시 최적화

- `actions/setup-java`의 `cache: 'gradle'` 옵션으로 Gradle 캐시 활용
- `docker/build-push-action`의 `cache-from/to: type=gha`로 레이어 캐시 활용
- 빌드 시간 단축 효과: 첫 실행 대비 약 40~60% 단축

---

## 참고 링크

- [GitHub Actions 공식 문서](https://docs.github.com/actions)

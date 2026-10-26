# 電子哨 · Electronic Whistle

原生 Android 電子哨 App，可使用大型螢幕按鈕或手機側邊的實體音量鍵操作，支援 Android 8.0（API 26）以上。目前版本為 1.1.0。

[下載 APK v1.1.0（開發測試版）](https://github.com/moon200702/electronic_whistle_app/raw/refs/heads/main/apk/electronic-whistle-v1.1.0-debug.apk)。編譯好的安裝檔與 SHA-256 校驗碼放在 [`apk/`](apk/) 目錄，可直接下載到 Android 手機安裝。

[查看介面截圖](docs/screenshot.png)。小螢幕會使用精簡配置；往下捲動可調整發聲方式與音量。

[查看實體音量鍵開關](docs/volume-keys.png)。

## 功能

- **四種哨音**：標準哨、雙短哨、顫音哨、低音哨。
- **按住發聲**：按下持續發聲，放開或拖出按鈕範圍立即停止。
- **點按播放**：點一下播放約 1.2 秒，再點一下可提早停止。
- **實體音量鍵**：預設開啟。App 在前景時，按住音量＋或音量－播放目前選擇的哨音，放開即停；長按不會反覆重新播放，兩鍵同時按住時需都放開才停止。音量鍵一律採按住操作，不受螢幕點按模式影響。
- **恢復音量控制**：往下捲動關閉「實體音量鍵發聲」，音量鍵便恢復調整手機媒體音量。開關設定會保留；開啟時不會同時改變手機音量。
- **音量調整**：App 內的音量搭配手機「媒體音量」使用，初始為 45%。
- **記住設定**：保留音色、發聲方式、音量與實體音量鍵開關；重新開啟不會自動發聲。
- **離線使用**：以 PCM 合成哨音，無網路、錄音或帳號需求，無廣告。
- 離開 App、切換模式、切換音色或失去音訊焦點時停止發聲。
- 音量鍵發聲僅在 App 畫面內使用；切到其他 App、鎖定螢幕或失去視窗焦點後停止，不會攔截其他 App 的音量鍵。
- 大型觸控目標與 TalkBack 支援；輔助操作可用中央按鈕播放短哨。

手機的媒體音量、靜音狀態與目前輸出裝置都會影響實際聲音；藍牙裝置也可能增加延遲。實際音量取決於手機喇叭，無法保證與實體電子哨相同。建議先以低音量試聽，勿貼近耳朵播放。

## Android Studio 開發

1. 在 Android Studio 開啟本目錄，使用 JDK 17 或以上（雲端驗證使用 JDK 21）。
2. 安裝 Android SDK Platform 35、Build Tools 35.0.0、Platform Tools。
3. 等待 Gradle 同步完成，選取 Android 8.0 以上的手機或模擬器，執行 `app`。

使用專案提供的 Gradle Wrapper（8.11.1），不需要另外安裝全域 Gradle。

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest  # 需要已連接的手機或模擬器
```

可安裝的測試 APK：`app/build/outputs/apk/debug/app-debug.apk`。這是開發用簽章，正式上架前需另設正式簽章與發行設定。

## Codex 雲端開發

使用現有 `/workspace/electronic_whistle_app` checkout；每個雲端任務已有隔離環境，不需建立 Git worktree。

```bash
cd /workspace/electronic_whistle_app
bash scripts/setup-cloud.sh                 # 安裝、建置、單元測試、Lint
bash scripts/setup-cloud.sh --with-emulator # 額外安裝 API 30 模擬器映像
bash scripts/gradle-cloud.sh assembleDebug testDebugUnitTest lintDebug
```

工具及快取保存在 `/workspace/toolchains`；不寫入系統目錄。`cloud-java.py` 沿用環境的網路代理並匯入平台提供的 CA 至本機 Java truststore，維持 TLS 驗證。Gradle 發行檔有 SHA-256 驗證；Android CLI 工具有 Google 官方儲存庫提供的固定 SHA-1 驗證，後續 SDK 套件由 SDK Manager 驗證。

模擬器測試的啟動方式見 [開發驗證紀錄](docs/validation.md)。手機端音量、聽感與藍牙延遲仍需實機確認。

## 程式結構

- `MainActivity.java`：繁體中文介面、觸控、實體音量鍵及設定。
- `WhistlePlayer.java`：音訊焦點、PCM 播放及釋放。
- `WhistleSynthesizer.java`：四種聲音的本機合成及音頭／音尾淡化。
- `app/src/test`：聲音長度、能量、音高、間隔與差異測試。
- `app/src/androidTest`：實際 Android 按鈕操作、停止行為與設定保留測試。

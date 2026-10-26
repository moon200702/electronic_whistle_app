# 開發驗證紀錄

驗證平台：Linux x86_64、Temurin JDK 21.0.13+9、Gradle 8.11.1、Android Gradle Plugin 8.9.3、Android SDK 35／Build Tools 35.0.0。

## 已完成

- `assembleDebug`：成功產生可安裝的開發用 APK。
- `testDebugUnitTest`：7 項通過，0 失敗、0 跳過。
- `connectedDebugAndroidTest`：Android 11 / API 30 模擬器執行 13 項，全部通過，0 跳過。
- `apksigner verify`：APK 簽章驗證通過。
- `lintDebug`：通過，無錯誤或警告。
- 重複執行雲端安裝腳本，確認既有工具、SDK、授權及快取可以重用。

聲音測試檢查 PCM 的長度、非靜音、振幅餘裕、音頭音尾淡化、四種聲音差異、雙短哨間隔、音高與顫音調變。

Android 操作測試涵蓋：預設四種音色與音量、按住開始／放開停止、取消觸控及拖出按鈕停止、點按開始／再次點按停止、切換音色停止與保留設定，以及離開 App 停止發聲。這些測試實際建立 Android `AudioTrack`，並確認播放狀態，不只檢查畫面文字。

1.1.0 新增七項實體音量鍵測試：音量＋與音量－的長按和放開、跨越短哨播放時間後持續發聲、按鍵重複事件、雙鍵同按及取消、與觸控同時使用、關閉開關後停止及保留設定、背景／失去焦點後清除狀態，以及切換音色後不受舊按鍵事件干擾。另透過 Android 輸入系統注入音量鍵事件，驗證開啟時發聲且媒體音量不變，關閉後實際改變手機媒體音量。這是模擬器按鍵事件驗證，仍需實體手機確認硬體按鍵。

1.1.0 APK 的版本代碼為 2，與 1.0.0 使用相同開發簽章，可覆蓋安裝更新。

## 重現

```bash
cd /workspace/electronic_whistle_app
bash scripts/setup-cloud.sh --with-emulator
bash scripts/start-emulator.sh
bash scripts/gradle-cloud.sh --no-daemon connectedDebugAndroidTest
```

模擬器沒有 KVM 硬體加速時使用軟體模擬，第一次開機可能需要數分鐘；啟動腳本最多等待 600 秒。`cloud-java.py` 將 ddmlib 裝置資訊讀取期限設為 30 秒，避免軟體模擬超過預設兩秒限制。保留原有測試斷言，不略過測試。

模擬器及 adb 需要可使用的 `/home/agent/.android` 目錄；本次已建立。若新環境出現此目錄的權限錯誤，應透過平台要求該目錄權限，不改寫 `HOME`。模擬器 console token 無法寫入不會影響透過 adb 執行上述測試。

Gradle 的連線測試完成後會移除測試 App。若需手動操作，可重新安裝並啟動：

```bash
/workspace/toolchains/android-sdk/platform-tools/adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
/workspace/toolchains/android-sdk/platform-tools/adb -s emulator-5554 shell am start -n com.moon.electronicwhistle/.MainActivity
```

## 實機仍需確認

尚未以實體 Android 手機驗證硬體音量鍵或試聽。手機喇叭音量、音色主觀聽感、藍牙延遲及不同品牌的按鍵和音訊路由，需安裝 APK 後確認。本次模擬器以無主機音訊輸出模式執行，音訊測試證明播放及停止狀態，並不代表已用耳朵試聽。

本版本使用開發用簽章，尚未設定正式發行簽章，也未上架 Google Play。

# checkhelper

某位置ゲームのAndroid向けツールアプリのKotlin実装版  

現在値位置をバックグラウンドで収集し最近傍の駅の変化を通知などユーザを手助けする

## 使用データ
[station-database](https://github.com/Seo-4d696b75/station_database)のリポジトリで管理

## 開発セットアップ

[他リポジトリで管理しているソースを利用します](https://github.com/Seo-4d696b75/MyAndroidLibrary/tree/kotlin)

```bash
git clone https://github.com/Seo-4d696b75/MyAndroidLibrary.git
git checkout kotlin
```

`/library`ディレクトリ下にある2つのモジュールをこのプロジェクトにimportします

- widget : カスタムViewを利用
- diagram : 図形計算に使用

`settings.gradle`にimportするモジュールのパスを指定します
```groovy
include ':widget'
include ':diagram'
project(':widget').projectDir = new File(settingsDir, '${path2project}/library/widget')
project(':diagram').projectDir = new File(settingsDir, '${path2project}/library/diagram')
```

appモジュールの`build.gradle`に対象２モジュールを依存に追加します
```groovy
    implementation project(':widget')
    implementation project(':diagram')
```
# checkhelper

某位置ゲームのAndroid向けツールアプリのKotlin実装版  

現在値位置をバックグラウンドで収集し最近傍の駅の変化を通知などユーザを手助けする

## 使用データ
[station-database](https://github.com/Seo-4d696b75/station_database)のリポジトリで管理

## 開発セットアップ

### 依存の追加

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

### Build署名の用意

必要に応じてkeystoreファイルを生成します.
key alias は`key0`を指定します.

`app/gradle.properties`に署名情報を記述します.

**このファイルは.gitignoreに追加されています**

```shell
release_keystore_path=${path_to_your_keystore_file}
release_keystore_pwd=${keystore_password}
release_key_pwd=${key_password}
```

`app/build.gradle`に署名の設定を追加します

```groovy
android {
    signingConfigs {
        release {
            storeFile file(release_keystore_path)
            storePassword release_keystore_pwd
            keyAlias 'key0'
            keyPassword release_key_pwd
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

# 駅サガース Androidアプリ

某位置ゲームのAndroid向けツールアプリ

- ✅ 現在位置をバックグラウンドで監視して付近の駅が変化したら通知する
- ✅ 現在位置からレーダーでアクセスできる駅一覧の確認
- ✅ [最新の駅情報](https://github.com/Seo-4d696b75/station_database)への更新機能
- ✅ 現在位置や付近の駅の履歴を記録・閲覧

## 開発セットアップ

### GitHubの認証情報

一部の依存をGitHub Packageから取得するため、各自のGitHubアカウントの認証情報が必要です  
認証情報のファイルはgit管理せず、各自で`/github_credential.properties`を追加してください

```properties
username=${GitHubアカウント名}
token=${Githubのアクセストークン}
```

### Firebase認証情報

`app/google-services.json`を用意する

### リリース用の署名

詳細は`app/build.gradle.kts`の署名設定を参照してください

署名関連のファイルはgitで管理しないため各自で用意します

- keystoreファイルは`app/release.jks`
    - key alias: `key0`
- `app/gradle.properties`にパスワードを指定
    - release_keystore_pwd: keystoreのパスワード
    - release_key_pwd: key0のパスワード

### GitHubActionsの設定

一部の認証情報がCIでも必要になるため、対象のファイルをbase64でエンコードしてリポジトリのシークレットに登録します

| name                   | file                       |  
|------------------------|----------------------------|
| KEYSTORE_BASE64        | `app/release.jks`          |  
| PWD_BASE64             | `app/gradle.properties`    |  
| GOOGLE_SERVICES_BASE64 | `app/google-services.json` |

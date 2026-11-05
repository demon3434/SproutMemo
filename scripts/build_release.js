const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const androidDir = path.join(rootDir, 'sproutmemo-android');
const releasesDir = path.join(rootDir, 'releases');
const jbrPath = 'D:\\Program Files\\04.IDE\\Android Studio\\jbr';

console.log('========================================================');
console.log('🚀 正在构建 SproutMemo Android Release APK...');
console.log('========================================================');

if (!fs.existsSync(jbrPath)) {
  console.error('[!] 错误: 未找到 JBR 路径:', jbrPath);
  process.exit(1);
}

const env = {
  ...process.env,
  JAVA_HOME: jbrPath,
  PATH: `${path.join(jbrPath, 'bin')};${process.env.PATH}`
};

if (!fs.existsSync(releasesDir)) {
  fs.mkdirSync(releasesDir, { recursive: true });
}

console.log('[*] 正在执行 Gradle 构建: gradlew.bat assembleRelease...');
try {
  execSync('gradlew.bat assembleRelease', {
    cwd: androidDir,
    env,
    stdio: 'inherit'
  });
  console.log('\n[+] Gradle assembleRelease 构建成功！');
} catch (err) {
  console.error('\n[!] Gradle 构建失败:', err.message);
  process.exit(1);
}

const apkOutput = path.join(androidDir, 'app/build/outputs/apk/release/app-release.apk');
if (!fs.existsSync(apkOutput)) {
  console.error('[!] 错误: 未在输出目录找到 APK 文件:', apkOutput);
  process.exit(1);
}

const stat = fs.statSync(apkOutput);
const sizeMb = (stat.size / (1024 * 1024)).toFixed(2);

const gradleContent = fs.readFileSync(path.join(androidDir, 'app/build.gradle.kts'), 'utf8');
const verMatch = gradleContent.match(/versionName\s*=\s*"([^"]+)"/);
const appVersion = verMatch ? verMatch[1] : '1.0';

const targetVersionApk = path.join(releasesDir, `SproutMemo-v${appVersion}-release.apk`);
const targetLatestApk = path.join(releasesDir, 'SproutMemo-latest-release.apk');
const targetGenericApk = path.join(releasesDir, 'SproutMemo-release.apk');
const rootGenericApk = path.join(rootDir, 'SproutMemo-release.apk');

fs.copyFileSync(apkOutput, targetVersionApk);
fs.copyFileSync(apkOutput, targetLatestApk);
fs.copyFileSync(apkOutput, targetGenericApk);
fs.copyFileSync(apkOutput, rootGenericApk);

console.log('\n========================================================');
console.log(`🎉 Release APK v${appVersion} 打包发布成功！`);
console.log(`APK 文件大小: ${sizeMb} MB`);
console.log(`根目录归档: SproutMemo-release.apk`);
console.log(`版本库归档: releases/SproutMemo-v${appVersion}-release.apk`);
console.log(`最新滚动版: releases/SproutMemo-latest-release.apk`);
console.log('========================================================\n');

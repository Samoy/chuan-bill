/**
 * 复制构建产物到 Android 资源目录
 *
 * 读取 manifest.config.ts 中的 appId，
 * 将 dist/build/app 产物复制到
 * chuan-bill-android/app/src/main/assets/apps/{appId}/www/
 */
/// <reference types="node" />

import path from 'node:path'
import process from 'node:process'
import fs from 'fs-extra'

const ROOT = path.resolve(import.meta.dirname, '..')
const ANDROID_ROOT = path.resolve(ROOT, '..', 'chuan-bill-android')

async function getAppId() {
  const manifest = fs.readFileSync(path.join(ROOT, 'src/manifest.json'), 'utf-8')
  const content = JSON.parse(manifest)
  const appid = content.appid
  if (!appid) {
    throw new Error('Failed to get appId from manifest.json')
  }
  return appid
}

async function main() {
  const appId = await getAppId()

  const srcDir = path.join(ROOT, 'dist', 'build', 'app')
  const destWww = path.join(ANDROID_ROOT, 'app', 'src', 'main', 'assets', 'apps', appId, 'www')

  if (!await fs.pathExists(srcDir)) {
    console.error(`❌ Dist not found: ${srcDir}`)
    process.exit(1)
  }

  // 清空并重建 www 目录，然后复制
  console.log(`📦 Coping assets...`)
  await fs.emptyDir(destWww)
  await fs.copy(srcDir, destWww)

  console.log(`✅ Copied success!`)
}

main().catch((err) => {
  console.error('❌ Copy failed:', err)
  process.exit(1)
})

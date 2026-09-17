/**
 * 幂等键工具 — 为创建类请求生成唯一的 Idempotency-Key。
 * 使用 crypto.randomUUID() 生成 UUID v4。
 */
export function generateIdempotencyKey() {
  return crypto.randomUUID()
}

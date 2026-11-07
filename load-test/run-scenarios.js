// 시나리오 일괄 실행: 30 / 90 / 180 / 300명 x (한 방에 몰림 | 3개 방 분산)
//   node run-scenarios.js [--duration 60] [--rate 5] [--url http://localhost:8080] [--only 180]
// 결과: results/<시나리오>.json, results/summary.md, results/summary.json
import { spawnSync } from 'node:child_process';
import { readFileSync, writeFileSync, mkdirSync, existsSync, unlinkSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const get = (k, d) => { const i = args.indexOf('--' + k); return i >= 0 ? args[i + 1] : d; };
const duration = get('duration', '60');
const rate = get('rate', '5');
const url = get('url', 'http://localhost:8080');
const only = get('only', null);

const CRITERIA = { p95: 200, cpu: 70 };
const BOT_COUNTS = [30, 90, 180, 300].filter((n) => !only || String(n) === only);
const LAYOUTS = [{ rooms: 1, label: '한 방에 몰림' }, { rooms: 3, label: '3개 방 분산' }];

const resultsDir = join(here, 'results');
mkdirSync(resultsDir, { recursive: true });
const rows = [];

for (const bots of BOT_COUNTS) {
  for (const layout of LAYOUTS) {
    const name = `${bots}bots-${layout.rooms}room`;
    const jsonPath = join(resultsDir, `${name}.json`);
    console.log(`\n========== ${bots}명 · ${layout.label} ==========`);
    // Windows 에서 부하 테스트 Node 프로세스가 시작 직후 0xC0000409 로 드물게 죽는 문제가 있어 (README 참고)
    // 결과를 못 남기고 죽은 경우에만 최대 3회까지 다시 실행한다. 측정 구간 데이터는 재시도와 섞이지 않는다.
    let status = null;
    for (let attempt = 1; attempt <= 3; attempt++) {
      if (existsSync(jsonPath)) unlinkSync(jsonPath);
      const r = spawnSync(process.execPath, [join(here, 'load-test.js'),
        '--bots', String(bots), '--rooms', String(layout.rooms), '--rate', rate, '--duration', duration,
        '--url', url, '--json', jsonPath], { stdio: 'inherit' });
      status = r.status;
      if (status === 0) {
        const saved = JSON.parse(readFileSync(jsonPath, 'utf8'));
        writeFileSync(jsonPath, JSON.stringify({ ...saved, attempts: attempt }, null, 2));
        break;
      }
      console.error(`scenario ${name} attempt ${attempt} crashed (exit ${status}${status === 3221226505 ? ' = 0xC0000409' : ''})`);
      await new Promise((res) => setTimeout(res, 5000));
    }
    if (status !== 0) { console.error(`scenario ${name} failed after 3 attempts`); continue; }
    // 서버가 퇴장 처리/GC 할 시간
    await new Promise((res) => setTimeout(res, 5000));
  }
}

// 요약은 results/ 에 있는 전체 시나리오 결과로 만든다 (--only 로 일부만 다시 돌려도 표가 유지되게)
for (const bots of [30, 90, 180, 300]) {
  for (const layout of LAYOUTS) {
    const jsonPath = join(resultsDir, `${bots}bots-${layout.rooms}room.json`);
    if (existsSync(jsonPath)) rows.push({ label: layout.label, ...JSON.parse(readFileSync(jsonPath, 'utf8')) });
  }
}

const verdict = (r) => {
  const fails = [];
  if (r.connections.fail > 0 || r.connections.droppedDuringTest > 0) fails.push('연결');
  if (r.latencyMs.p95 > CRITERIA.p95) fails.push('p95');
  if (r.server.cpuPct.max > CRITERIA.cpu) fails.push('CPU');
  const retry = r.attempts > 1 ? ` (재시도 ${r.attempts - 1})` : '';
  return (fails.length ? `❌ ${fails.join(',')}` : '✅') + retry;
};
const header = '| 인원 | 배치 | 연결 성공/실패 | p50 | p95 | p99 | max | 수신 msg/s (봇당) | 수신 KB/s | CPU avg / max | JVM mem avg / max (MB) | heap 시작→끝 (MB) | 판정 |\n'
  + '|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|:---:|';
const lines = rows.map((r) => `| ${r.scenario.bots} | ${r.label} | ${r.connections.ok} / ${r.connections.fail} | ${r.latencyMs.p50} | ${r.latencyMs.p95} | ${r.latencyMs.p99} | ${r.latencyMs.max} | ${r.received.perSec} (${r.received.perBotPerSec}) | ${r.received.kbPerSec} | ${r.server.cpuPct.avg}% / ${r.server.cpuPct.max}% | ${r.server.memoryUsedMB.avg} / ${r.server.memoryUsedMB.max} | ${r.server.heapUsedMB.first10pctAvg} → ${r.server.heapUsedMB.last10pctAvg} | ${verdict(r)} |`);
const md = `${header}\n${lines.join('\n')}\n\n지연 단위 ms · 기준: p95 ≤ ${CRITERIA.p95}ms, CPU ≤ ${CRITERIA.cpu}% · move ${rate}회/s · 측정 ${duration}s\n`;
writeFileSync(join(resultsDir, 'summary.md'), md);
writeFileSync(join(resultsDir, 'summary.json'), JSON.stringify(rows, null, 2));
console.log('\n' + md);

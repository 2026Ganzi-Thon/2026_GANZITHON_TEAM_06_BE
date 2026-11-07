// 대기실 실시간 부하 테스트
//   node load-test.js --bots 180 --rooms 1 --rate 5 --duration 60
// 옵션
//   --bots      봇 수 (기본 30)
//   --rooms     방 개수, 봇을 room-1..room-N 에 균등 분배 (기본 1)
//   --rate      봇당 초당 move 전송 횟수 (기본 5)
//   --duration  측정 시간(초), 전원 접속 완료 후부터 (기본 60)
//   --ramp      접속을 나눠 거는 시간(ms) (기본 3000)
//   --workers   봇을 나눠 돌릴 worker thread 수 (기본: 봇 40명당 1개, 최대 코어/2)
//   --url       서버 주소 (기본 http://localhost:8080)
//   --json      결과를 JSON 파일로도 저장할 경로
import { Worker, isMainThread, parentPort, workerData } from 'node:worker_threads';
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import os from 'node:os';
import { Client } from '@stomp/stompjs';
import WebSocket from 'ws';

const SITUATION = {
  'room-1': ['SUBWAY_DELAY', 'SUBWAY_CROWDED', 'SUBWAY_TRANSFER'],
  'room-2': ['HOSPITAL_WAITING', 'HOSPITAL_RESULT', 'HOSPITAL_PHARMACY'],
  'room-3': ['BUS_LATE', 'BUS_RAIN', 'BUS_LAST'],
};
const STATE = ['TIRED', 'BORED', 'HAPPY', 'HUNGRY'];
const BROADCAST_CHANCE_PER_SEC = 1 / 90;   // "가끔 방송"
const REACTION_CHANCE = 0.3;               // 방송 수신 시 리액션할 확률

if (isMainThread) {
  await main();
} else {
  runWorker(workerData);
}

// ───────────────────────────── main thread ─────────────────────────────
function parseArgs() {
  const a = process.argv.slice(2);
  const get = (k, d) => { const i = a.indexOf('--' + k); return i >= 0 ? a[i + 1] : d; };
  const bots = +get('bots', 30);
  return {
    bots,
    rooms: +get('rooms', 1),
    rate: +get('rate', 5),
    duration: +get('duration', 60),
    ramp: +get('ramp', 3000),
    workers: +get('workers', Math.max(1, Math.min(Math.floor(os.cpus().length / 2), Math.ceil(bots / 40)))),
    url: get('url', 'http://localhost:8080'),
    json: get('json', null),
  };
}

async function main() {
  const opt = parseArgs();
  const wsUrl = opt.url.replace(/^http/, 'ws') + '/ws';
  console.log(`[load] bots=${opt.bots} rooms=${opt.rooms} rate=${opt.rate}/s duration=${opt.duration}s workers=${opt.workers}`);

  // 봇 배정: i 번째 봇 → room-(i % rooms + 1), 워커에 라운드로빈
  const assignments = Array.from({ length: opt.workers }, () => []);
  for (let i = 0; i < opt.bots; i++) assignments[i % opt.workers].push({ idx: i, room: `room-${(i % opt.rooms) + 1}` });

  const workers = assignments.map((bots) => new Worker(new URL(import.meta.url), {
    workerData: { bots, wsUrl, rate: opt.rate, ramp: opt.ramp },
  }));
  const waitAll = (type) => Promise.all(workers.map((w) => new Promise((res) => {
    const h = (m) => { if (m.type === type) { w.off('message', h); res(m); } };
    w.on('message', h);
  })));

  // 1) 접속
  const t0 = Date.now();
  const connected = await waitAll('connected');
  const connOk = connected.reduce((s, m) => s + m.ok, 0);
  const connFail = connected.reduce((s, m) => s + m.fail, 0);
  console.log(`[load] connected ${connOk}/${opt.bots} (fail ${connFail}) in ${Date.now() - t0}ms`);

  // 2) 측정
  const sampler = startActuatorSampler(opt.url);
  workers.forEach((w) => w.postMessage({ type: 'measure' }));
  const measureStart = Date.now();
  const progress = process.stdout.isTTY
    ? setInterval(() => process.stdout.write(`\r[load] ${Math.round((Date.now() - measureStart) / 1000)}s / ${opt.duration}s`), 1000)
    : null;
  await new Promise((r) => setTimeout(r, opt.duration * 1000));
  clearInterval(progress);
  const measuredSec = (Date.now() - measureStart) / 1000;
  const server = await sampler.stop();
  if (progress) process.stdout.write('\n');

  // 3) 수집
  workers.forEach((w) => w.postMessage({ type: 'stop' }));
  const reports = await waitAll('report');
  await Promise.all(workers.map((w) => w.terminate()));

  const lat = reports.flatMap((r) => r.latencies).sort((a, b) => a - b);
  const sum = (k) => reports.reduce((s, r) => s + r[k], 0);
  const byType = {};
  reports.forEach((r) => Object.entries(r.received).forEach(([t, n]) => { byType[t] = (byType[t] || 0) + n; }));
  const totalRecv = Object.values(byType).reduce((s, n) => s + n, 0);

  const result = {
    scenario: { bots: opt.bots, rooms: opt.rooms, rate: opt.rate, duration: opt.duration },
    connections: { ok: connOk, fail: connFail, droppedDuringTest: sum('dropped') },
    latencyMs: {
      samples: lat.length,
      p50: pct(lat, 50), p95: pct(lat, 95), p99: pct(lat, 99), max: lat.at(-1) ?? null,
      coalescedSeqs: sum('coalesced'),          // 같은 틱 안에서 최신 seq 로 덮인 move 수 (정상 동작)
    },
    sent: { moves: sum('movesSent'), broadcasts: sum('broadcastsSent'), reactions: sum('reactionsSent') },
    received: {
      perSec: round(totalRecv / measuredSec),
      perBotPerSec: round(totalRecv / measuredSec / Math.max(1, connOk)),
      kbPerSec: round(sum('bytes') / 1024 / measuredSec),
      byType,
    },
    server,
  };
  printResult(result);
  if (opt.json) {
    mkdirSync(dirname(opt.json), { recursive: true });
    writeFileSync(opt.json, JSON.stringify(result, null, 2));
  }
  process.exit(0);
}

function startActuatorSampler(baseUrl) {
  const samples = [];
  const metric = async (name, tag) => {
    const res = await fetch(`${baseUrl}/actuator/metrics/${name}${tag ? `?tag=${tag}` : ''}`);
    return (await res.json()).measurements[0].value;
  };
  const timer = setInterval(async () => {
    try {
      const [cpu, mem, heap] = await Promise.all([
        metric('process.cpu.usage'), metric('jvm.memory.used'), metric('jvm.memory.used', 'area:heap'),
      ]);
      samples.push({ cpu, mem, heap });
    } catch { /* 샘플 하나 누락은 무시 */ }
  }, 1000);
  return {
    async stop() {
      clearInterval(timer);
      const MB = 1024 * 1024;
      const stat = (k, scale) => {
        const v = samples.map((s) => s[k] * scale);
        return { avg: round(v.reduce((a, b) => a + b, 0) / Math.max(1, v.length)), max: round(Math.max(...v)) };
      };
      const tenth = Math.max(1, Math.floor(samples.length / 10));
      const avgOf = (arr) => round(arr.reduce((a, s) => a + s.heap / MB, 0) / Math.max(1, arr.length));
      return {
        samples: samples.length,
        cores: os.cpus().length,
        cpuPct: stat('cpu', 100),                 // process.cpu.usage: 전체 코어 대비 비율
        memoryUsedMB: stat('mem', 1 / MB),        // jvm.memory.used (heap + non-heap)
        heapUsedMB: { ...stat('heap', 1 / MB), first10pctAvg: avgOf(samples.slice(0, tenth)), last10pctAvg: avgOf(samples.slice(-tenth)) },
      };
    },
  };
}

function printResult(r) {
  const s = r.server;
  console.log('──────────────── result ────────────────');
  console.log(`scenario     : ${r.scenario.bots} bots / ${r.scenario.rooms} room(s) / ${r.scenario.rate} moves/s / ${r.scenario.duration}s`);
  console.log(`connections  : ok ${r.connections.ok}, fail ${r.connections.fail}, dropped ${r.connections.droppedDuringTest}`);
  console.log(`latency (ms) : p50 ${r.latencyMs.p50}  p95 ${r.latencyMs.p95}  p99 ${r.latencyMs.p99}  max ${r.latencyMs.max}  (n=${r.latencyMs.samples})`);
  console.log(`received     : ${r.received.perSec} msg/s total, ${r.received.perBotPerSec} msg/s per bot, ${r.received.kbPerSec} KB/s`);
  console.log(`by type      : ${JSON.stringify(r.received.byType)}`);
  console.log(`sent         : ${JSON.stringify(r.sent)}`);
  console.log(`server cpu   : avg ${s.cpuPct.avg}%  max ${s.cpuPct.max}%  (of ${s.cores} logical cores)`);
  console.log(`server mem   : jvm.memory.used avg ${s.memoryUsedMB.avg}MB  max ${s.memoryUsedMB.max}MB | heap first ${s.heapUsedMB.first10pctAvg}MB -> last ${s.heapUsedMB.last10pctAvg}MB`);
}

function pct(sorted, p) {
  if (!sorted.length) return null;
  return sorted[Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)];
}
function round(v) { return Math.round(v * 10) / 10; }

// ───────────────────────────── worker thread ─────────────────────────────
async function runWorker({ bots, wsUrl, rate, ramp }) {

  const stats = { latencies: [], received: {}, bytes: 0, movesSent: 0, broadcastsSent: 0, reactionsSent: 0, coalesced: 0, dropped: 0 };
  let measuring = false;
  let stopping = false;
  const clients = [];

  const connectResults = await Promise.all(bots.map((b, i) => new Promise((resolve) => {
    setTimeout(() => startBot(b, resolve), (ramp * i) / Math.max(1, bots.length));
  })));
  parentPort.postMessage({ type: 'connected', ok: connectResults.filter(Boolean).length, fail: connectResults.filter((x) => !x).length });

  parentPort.on('message', async (m) => {
    if (m.type === 'measure') measuring = true;
    if (m.type === 'stop') {
      measuring = false;
      stopping = true;
      clients.forEach((c) => clearInterval(c.timer));
      await new Promise((r) => setTimeout(r, 300));
      await Promise.all(clients.map((c) => c.client.deactivate().catch(() => {})));
      parentPort.postMessage({ type: 'report', ...stats });
    }
  });

  function startBot({ room }, resolve) {
    const bot = {
      client: null, timer: null, selfId: null, seq: 0,
      x: 400, y: 300, tx: rand(800), ty: rand(600),
      sentAt: new Map(), cooldownUntil: 0,
    };
    let settled = false;
    const settle = (ok) => { if (!settled) { settled = true; clearTimeout(timeout); resolve(ok); } };
    const timeout = setTimeout(() => settle(false), 15000);

    const client = new Client({
      webSocketFactory: () => new WebSocket(wsUrl),
      reconnectDelay: 0, heartbeatIncoming: 0, heartbeatOutgoing: 0, debug: () => {},
    });
    bot.client = client;
    const publish = (action, body) => {
      if (client.connected) client.publish({ destination: `/app/rooms/${room}/${action}`, body: JSON.stringify(body) });
    };

    const onMessage = (frame) => {
      if (measuring) stats.bytes += frame.body.length;
      const msg = JSON.parse(frame.body);
      if (measuring) stats.received[msg.type] = (stats.received[msg.type] || 0) + 1;
      if (msg.type === 'SNAPSHOT') {
        if (msg.full) {
          bot.selfId = msg.selfId;
          const mine = msg.players.find((p) => p.id === bot.selfId);
          if (mine) { bot.x = mine.x; bot.y = mine.y; }
          clients.push(bot);
          bot.timer = setInterval(() => step(bot, publish), 1000 / rate);
          settle(true);
          return;
        }
        const now = Date.now();
        for (const p of msg.players) {
          if (p.id !== bot.selfId) continue;
          const sent = bot.sentAt.get(p.seq);
          if (sent !== undefined && measuring && sent.measured) stats.latencies.push(now - sent.at);
          // seq 이하 기록 정리 (덮여서 따로 오지 않는 seq 는 coalesced 로 집계)
          for (const [s, v] of bot.sentAt) {
            if (s > p.seq) break;
            if (s < p.seq && v.measured) stats.coalesced++;
            bot.sentAt.delete(s);
          }
          break;
        }
      } else if (msg.type === 'BROADCAST' && !stopping && Math.random() < REACTION_CHANCE) {
        setTimeout(() => {
          if (stopping) return;
          publish('reaction', { broadcastId: msg.broadcastId, type: Math.random() < 0.5 ? 'NOD' : 'AWW' });
          if (measuring) stats.reactionsSent++;
        }, rand(1000));
      } else if (msg.type === 'ERROR' && msg.retryAfterMs) {
        bot.cooldownUntil = Date.now() + msg.retryAfterMs;
      }
    };

    client.onConnect = () => {
      client.subscribe(`/topic/rooms/${room}`, onMessage);
      client.subscribe('/user/queue/events', onMessage);
      publish('join', {});
    };
    client.onWebSocketClose = () => {
      if (settled && !stopping) stats.dropped++;
      settle(false);
    };
    client.onStompError = () => settle(false);
    client.activate();

    // 방송 시도: 1초마다 확률적으로
    bot.broadcast = () => {
      if (stopping || Date.now() < bot.cooldownUntil || Math.random() >= BROADCAST_CHANCE_PER_SEC) return;
      const chips = SITUATION[room];
      publish('broadcast', { situationChipId: pick(chips), stateChipId: pick(STATE) });
      bot.cooldownUntil = Date.now() + 31000;
      if (measuring) stats.broadcastsSent++;
    };
    bot.lastBroadcastCheck = Date.now();
  }

  // 목표 지점으로 걸어가다 도착하면 새 목표. 매 호출마다 move 1회 전송.
  function step(bot, publish) {
    const speed = 120 / rate;   // 초당 120px
    const dx = bot.tx - bot.x, dy = bot.ty - bot.y, d = Math.hypot(dx, dy);
    if (d <= speed) { bot.x = bot.tx; bot.y = bot.ty; bot.tx = rand(800); bot.ty = rand(600); }
    else { bot.x += (dx / d) * speed; bot.y += (dy / d) * speed; }
    const seq = ++bot.seq;
    bot.sentAt.set(seq, { at: Date.now(), measured: measuring });
    publish('move', { x: Math.round(bot.x), y: Math.round(bot.y), seq });
    if (measuring) stats.movesSent++;
    if (Date.now() - bot.lastBroadcastCheck >= 1000) { bot.lastBroadcastCheck = Date.now(); bot.broadcast(); }
  }
}

function rand(n) { return Math.random() * n; }
function pick(arr) { return arr[Math.floor(Math.random() * arr.length)]; }

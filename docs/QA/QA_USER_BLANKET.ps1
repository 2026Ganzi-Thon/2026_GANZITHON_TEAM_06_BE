# user · blanket 도메인 QA에 실제로 사용한 PowerShell 명령 모음 (2026-11-19, 배포 서버)
#
# - 아래 명령은 한 번에 돌리는 용도가 아니라, 블록 단위로 순서대로 붙여넣어 실행한 기록입니다.
# - 보안 정보는 * 로 가렸습니다. 다시 실행하려면 아래 값을 채워서 쓰세요.
#     QA********  : 테스트 계정 닉네임(메인)
#     QA***       : 테스트 계정 닉네임(보조, 남의 조각 403 확인용)
#     "****"      : 테스트 계정 PIN (숫자 4자리)
#   틀린 PIN으로 쓴 "0000", "9999", "12ab" 같은 값은 테스트 입력값이라 그대로 두었습니다.
# - 테스트 계정과 담요 조각이 대상 서버 DB에 남습니다. 시연 직전에는 실행하지 마세요.
# - 토큰(accessToken, guestToken)은 응답에서 변수($tokA, $tok 등)로 받아 쓰며 파일에 기록하지 않습니다.

# ===== 0. 준비 (한 번만 실행) =====
$B = "https://api.wait-station.online/api/v1"
function Api($method, $path, $body, $token, $guest) {
  $headers = @{}
  if ($token) { $headers["Authorization"] = "Bearer $token" }
  if ($guest) { $headers["X-Guest-Token"] = $guest }
  $p = @{ Uri = "$B$path"; Method = $method; Headers = $headers; UseBasicParsing = $true }
  if ($body) { $p["Body"] = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Compress)); $p["ContentType"] = "application/json" }
  try {
    $r = Invoke-WebRequest @p
    $status = [int]$r.StatusCode
    $text = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
  } catch {
    $resp = $_.Exception.Response
    if ($resp) {
      $status = [int]$resp.StatusCode
      $text = (New-Object IO.StreamReader($resp.GetResponseStream(), [Text.Encoding]::UTF8)).ReadToEnd()
    } else { $status = 0; $text = $_.Exception.Message }
  }
  $short = $text.Substring(0, [Math]::Min(260, $text.Length))
  Write-Host "$method $path -> $status  $short"
  $json = $null
  try { $json = $text | ConvertFrom-Json } catch {}
  return [pscustomobject]@{ Status = $status; Json = $json }
}
$n  = "QA********"                      # 메인 테스트 계정 (실제 값은 가림)
$n2 = "qb" + (Get-Random -Maximum 9999) # 잠금 테스트 전용
$n3 = "qc" + (Get-Random -Maximum 9999) # 실패 횟수 초기화 테스트 전용
$n4 = "qd" + (Get-Random -Maximum 9999) # 남의 조각 확인용 다른 사용자

# ===== 1. 가입 =====
$x = Api POST "/users" @{nickname=$n; pin="****"; pinConfirm="****"}       # 201 + 토큰
$x = Api POST "/users" @{nickname=$n; pin="****"; pinConfirm="****"}       # 409 USER_NICKNAME_DUPLICATED
$x = Api POST "/users" @{nickname="zz$n"; pin="****"; pinConfirm="9999"}   # 400 (pinConfirm 오류)
$x = Api POST "/users" @{nickname="a b!"; pin="12ab"; pinConfirm="12ab"}   # 400 (닉네임, PIN 형식)
$x = Api POST "/users" @{nickname="QAnobody1"; pin="123"; pinConfirm="123"}                 # 400 (PIN 3자리)
$x = Api POST "/users" @{nickname="QAnobody12345"; pin="****"; pinConfirm="****"}          # 400 (닉네임 13자)

# ===== 2. 로그인 / 5회 잠금 =====
$x = Api POST "/users/login" @{nickname=$n; pin="****"}                    # 200 + 토큰
$tokA = $x.Json.data.accessToken
$x = Api POST "/users/login" @{nickname=$n; pin="0000"}                    # 401 USER_LOGIN_FAILED
$x = Api POST "/users/login" @{nickname="nobody$n"; pin="****"}            # 401 (틀린 PIN과 같은 코드)

# 잠금 테스트 (전용 계정 $n2)
$x = Api POST "/users" @{nickname=$n2; pin="****"; pinConfirm="****"}
1..5 | ForEach-Object { $x = Api POST "/users/login" @{nickname=$n2; pin="0000"} }   # 401 x5
$x = Api POST "/users/login" @{nickname=$n2; pin="****"}                   # 429 USER_LOGIN_LOCKED (정답이어도 거부)

# 실패 횟수 초기화 테스트 (전용 계정 $n3)
$x = Api POST "/users" @{nickname=$n3; pin="****"; pinConfirm="****"}
1..4 | ForEach-Object { $x = Api POST "/users/login" @{nickname=$n3; pin="0000"} }   # 401 x4
$x = Api POST "/users/login" @{nickname=$n3; pin="****"}                   # 200 (성공하면 횟수 초기화)
1..4 | ForEach-Object { $x = Api POST "/users/login" @{nickname=$n3; pin="0000"} }   # 401 x4 (잠기지 않음)
$x = Api POST "/users/login" @{nickname=$n3; pin="****"}                   # 200

# 잠금 5분 뒤 풀림 (시간이 지난 뒤 실행)
$x = Api POST "/users/login" @{nickname=$n2; pin="****"}                   # 5분 뒤 200

# ===== 3. 내 정보 / 공개 설정 =====
$x = Api GET "/users/me"                                                    # 401 AUTH_LOGIN_REQUIRED
$x = Api GET "/users/me" $null $tokA                                        # 200, isPublic=false
$x = Api PATCH "/users/me" @{isPublic=$true} $tokA                          # 200, isPublic=true
$x = Api GET "/users/me" $null $tokA                                        # 200, isPublic=true (반영 확인)
$x = Api PATCH "/users/me" @{isPublic=$null} $tokA                          # 400
$x = Api PATCH "/users/me" @{isPublic="abc"} $tokA                          # 400
$x = Api PATCH "/users/me" @{isPublic=$false} $tokA                         # 200, isPublic=false (끄기)

# ===== 4-0. 담요 조각 QA 준비 (한 번만 실행) =====
# 응답 한글이 깨지지 않도록 성공 응답을 UTF-8로 직접 디코딩하는 버전
function Req($method, $path, $obj, $token, $guest) {
  $h = @{ "Content-Type" = "application/json" }
  if ($token) { $h["Authorization"] = "Bearer $token" }
  if ($guest) { $h["X-Guest-Token"] = $guest }
  $p = @{ Method = $method; Uri = "$B$path"; Headers = $h; UseBasicParsing = $true }
  if ($obj -ne $null) { $p["Body"] = ($obj | ConvertTo-Json -Compress) }
  try { $r = Invoke-WebRequest @p; $code = [int]$r.StatusCode; $raw = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) }
  catch {
    $resp = $_.Exception.Response
    if ($resp) { $code = [int]$resp.StatusCode; $raw = (New-Object IO.StreamReader($resp.GetResponseStream())).ReadToEnd() }
    else { $code = 0; $raw = $_.Exception.Message }
  }
  $j = $null; try { $j = $raw | ConvertFrom-Json } catch {}
  [pscustomobject]@{ Status = $code; Raw = $raw; Json = $j }
}
function Chk($label, $res, $expect) {
  $mark = if ($res.Status -eq $expect) { "OK" } else { "NG" }
  "$mark  [$label] 상태=$($res.Status) (기대 $expect)  에러코드=$($res.Json.error.code)"
}
$lg = Req POST "/users/login" @{ nickname = $n; pin = "****" }
Chk "로그인" $lg 200
$tok = $lg.Json.data.accessToken

# ===== 4-A. 종료 시 조각 자동 저장 + 열차 수 (약 35초) =====
$c = Req POST "/tickets" @{ roomCode = "RESULT" } $tok
Chk "승차권 발급(로그인)" $c 201
$tid = $c.Json.data.ticketId
$iv  = [long]$c.Json.data.trainIntervalMs
"   ticketId=$tid  번호=$($c.Json.data.ticketNumber)  열차간격=${iv}ms  guestToken=[$($c.Json.data.guestToken)]  (guestToken은 비어 있어야 함)"
"35초 기다립니다..."
Start-Sleep -Seconds 35
$e = Req POST "/tickets/$tid/end" @{ endReason = "ANSWERED" } $tok
Chk "기다림 종료" $e 200
$d = $e.Json.data
"   savedToAccount=$($d.savedToAccount)  (기대: True)"
"   도안=$($d.patternCode)  번호=$($d.ticketNumber)  기다림이름=$($d.waitingName)  종료표식=$($d.endReason)"
$ci = [Globalization.CultureInfo]::InvariantCulture
$st = [DateTimeOffset]::Parse(([regex]::Match($e.Raw,'"startedAt":"([^"]+)"').Groups[1].Value), $ci)
$en = [DateTimeOffset]::Parse(([regex]::Match($e.Raw,'"endedAt":"([^"]+)"').Groups[1].Value), $ci)
$shared = [math]::Floor($en.ToUnixTimeMilliseconds()/$iv) - [math]::Floor($st.ToUnixTimeMilliseconds()/$iv)
"   기다린 시간=$([math]::Round(($en-$st).TotalSeconds))초  실제 trainCount=$($d.trainCount)  계산한 기대값≈$(1+$shared) (±1 허용)"

# ===== 4-B. 목록과 상세 =====
$l = Req GET "/blanket-pieces" $null $tok
Chk "내 담요 목록" $l 200
$items = @($l.Json.data.blanketPieces)
"   조각 수=$($items.Count) (기대: 지금은 1개, 최대 9)"
$items | ForEach-Object { "   id=$($_.blanketPieceId)  도안=$($_.patternCode)  만든시각=$($_.createdAt)" }
$pieceId = $items[0].blanketPieceId
$firstPiece = $pieceId
$dt = Req GET "/blanket-pieces/$pieceId" $null $tok
Chk "조각 상세" $dt 200
$p = $dt.Json.data
"   [앞면] 도안=$($p.patternCode)"
"   [뒷면] 번호=$($p.ticketNumber)  기다림이름=$($p.waitingName)  열차수=$($p.trainCount)  종료표식=$($p.endReason)"
"   조각ID=$($p.blanketPieceId) 승차권ID=$($p.ticketId) (승차권ID가 4-A의 $tid 와 같아야 함)"

# ===== 4-C. 남의 조각 403, 없는 조각 404 =====
$s = Req POST "/users" @{ nickname = "QA***"; pin = "****"; pinConfirm = "****" }   # 보조 계정 (이미 있으면 409)
"   보조 계정 가입 상태=$($s.Status) (201 또는 이미 있으면 409)"
$sl = Req POST "/users/login" @{ nickname = "QA***"; pin = "****" }
$tok2 = $sl.Json.data.accessToken
$r403 = Req GET "/blanket-pieces/$pieceId" $null $tok2
Chk "남의 조각 상세" $r403 403
$r404 = Req GET "/blanket-pieces/99999999" $null $tok
Chk "없는 조각" $r404 404

# ===== 4-D. 조각 10개 이상 → 최신 9개만 (조각 11개가 됨) =====
1..10 | ForEach-Object {
  $t = Req POST "/tickets" @{ roomCode = "RESULT" } $tok
  $x = Req POST "/tickets/$($t.Json.data.ticketId)/end" @{ endReason = "LET_GO" } $tok
  "   ${_}번째: 발급=$($t.Status) 종료=$($x.Status) 저장=$($x.Json.data.savedToAccount)"
}
$l = Req GET "/blanket-pieces" $null $tok
Chk "목록(조각 11개일 때)" $l 200
$ids = @($l.Json.data.blanketPieces.blanketPieceId)
"   조각 수=$($ids.Count) (기대: 9)"
"   id 순서=$($ids -join ', ')  (큰 번호부터 내려가야 최신순)"
"   가장 오래된 조각($firstPiece)이 목록에 있나? $($ids -contains $firstPiece) (기대: False)"
$old = Req GET "/blanket-pieces/$firstPiece" $null $tok
Chk "오래된 조각 상세(삭제 안 됐는지)" $old 200

# ===== 4-E. 비로그인 종료 후 계정으로 옮기기 (약 25초) =====
$g = Req POST "/tickets" @{ roomCode = "RESULT" }
Chk "비로그인 발급" $g 201
$gid = $g.Json.data.ticketId
$gt  = $g.Json.data.guestToken
"   guestToken 길이=$($gt.Length) (기대: 43)"
"25초 기다립니다..."
Start-Sleep -Seconds 25
$ge = Req POST "/tickets/$gid/end" @{ endReason = "ANSWERED" } $null $gt
Chk "비로그인 종료" $ge 200
"   savedToAccount=$($ge.Json.data.savedToAccount) (기대: False)  종료 때 trainCount=$($ge.Json.data.trainCount)"
$cl = Req POST "/tickets/claim" @{ ticketIds = @($gid) } $tok $gt
Chk "계정으로 옮기기" $cl 200
$cp = @($cl.Json.data.blanketPieces)
"   옮겨진 조각 수=$($cp.Count) (기대: 1)  옮긴 뒤 trainCount=$($cp[0].trainCount) (종료 때 값과 같아야 함)"
$cl2 = Req POST "/tickets/claim" @{ ticketIds = @($gid) } $tok $gt
Chk "같은 조각 다시 옮기기" $cl2 409

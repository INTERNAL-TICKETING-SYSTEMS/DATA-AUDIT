$url = "http://localhost:8081/api/v1/audit/events"
$totalRequisicoes = 100
$sucessos = 0
$falhas = 0

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "INICIANDO TESTE DE CARGA E ESTRESSE (ATV-008)" -ForegroundColor Cyan
Write-Host "Alvo: $url" -ForegroundColor Cyan
Write-Host "Volume planejado: $totalRequisicoes requisicoes" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$cronometroTotal = [System.Diagnostics.Stopwatch]::StartNew()
$temposMs = @()

for ($i = 1; $i -le $totalRequisicoes; $i++) {
    $timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
    $payload = '{"origem":"BENCHMARK-STI","entidade":"TICKET","idEntidade":"' + $i + '","tipoOperacao":"ATUALIZACAO_CARGA","autor":"carga.stress","dataHoraEvento":"' + $timestamp + '","estadoAnterior":null,"estadoAtual":"{\"contador\":' + $i + ',\"status\":\"PROCESSADO\"}","metadados":"{\"lote\":1}"}'

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $resp = Invoke-RestMethod -Uri $url -Method Post -Body $payload -ContentType "application/json"
        $sw.Stop()
        $sucessos++
        $temposMs += $sw.ElapsedMilliseconds
    } catch {
        $sw.Stop()
        $falhas++
    }

    if ($i % 20 -eq 0) {
        Write-Host " -> Progresso: $i / $totalRequisicoes concluidas..." -ForegroundColor Yellow
    }
}

$cronometroTotal.Stop()

$tempoTotalSegundos = [Math]::Round($cronometroTotal.Elapsed.TotalSeconds, 2)
$mediaMs = [Math]::Round(($temposMs | Measure-Object -Average).Average, 2)
$maxMs = ($temposMs | Measure-Object -Maximum).Maximum
$minMs = ($temposMs | Measure-Object -Minimum).Minimum
$tps = [Math]::Round($sucessos / $tempoTotalSegundos, 2)

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "RELATORIO DE CONFIABILIDADE E DESEMPENHO" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Requisicoes Totais:     $totalRequisicoes" -ForegroundColor Cyan
Write-Host "Sucessos (HTTP 201):    $sucessos" -ForegroundColor Green
Write-Host "Falhas:                 $falhas" -ForegroundColor $(if ($falhas -eq 0) { "Green" } else { "Red" })
Write-Host "Tempo Total:            $tempoTotalSegundos s" -ForegroundColor Cyan
Write-Host "Taxa (Req/segundo):     $tps req/s" -ForegroundColor Cyan
Write-Host "Latencia Media:         $mediaMs ms" -ForegroundColor Cyan
Write-Host "Latencia Minima:        $minMs ms" -ForegroundColor Cyan
Write-Host "Latencia Maxima:        $maxMs ms" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Green

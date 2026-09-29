Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "INICIANDO ESTEIRA DE INTEGRACAO STI -> AUDITORIA DE DADOS" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. CLIENTE
Write-Host "`n[1. CLIENTE] Solicitando abertura de chamado #9001..." -ForegroundColor Yellow
$corpo1 = '{"origem":"STI-TICKETS","entidade":"TICKET","idEntidade":"9001","tipoOperacao":"CRIACAO","autor":"usuario.aluno","dataHoraEvento":"2026-09-29T10:45:00","estadoAnterior":null,"estadoAtual":"{\"status\":\"ABERTO\",\"titulo\":\"Computador do Laboratorio sem rede\"}","metadados":"{\"bloco\":\"Bloco C\",\"sala\":\"Laboratorio 02\",\"perfil\":\"CLIENTE\"}"}'
$resp1 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/audit/events" -Method Post -Body $corpo1 -ContentType "application/json"
$idCriado =$resp1.id
Write-Host " -> Evento registrado com ID: $idCriado | Hash: $($resp1.hashIntegridade.Substring(0, 16))..." -ForegroundColor Green

# 2. TECNICO
Write-Host "`n[2. TECNICO] Atribuindo chamado e finalizando com parecer tecnico valido..." -ForegroundColor Yellow
$corpo2 = '{"origem":"STI-TICKETS","entidade":"TICKET","idEntidade":"9001","tipoOperacao":"UPDATE_STATUS","autor":"tecnico.suporte","dataHoraEvento":"2026-09-29T11:00:00","estadoAnterior":"{\"status\":\"EM_ANDAMENTO\"}","estadoAtual":"{\"status\":\"CONCLUIDO\"}","metadados":"{\"resolucao\":\"Conector RJ-45 crimpado novamente e sinal reestabelecido.\",\"perfil\":\"TECNICO\"}"}'
$resp2 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/audit/events" -Method Post -Body $corpo2 -ContentType "application/json"
Write-Host " -> Fechamento regular registrado. ID: $($resp2.id)" -ForegroundColor Green

# 3. GERENTE
Write-Host "`n[3. GERENTE] Simulando encerramento irregular no chamado #9002 (sem parecer)..." -ForegroundColor Yellow
$corpo3 = '{"origem":"STI-TICKETS","entidade":"TICKET","idEntidade":"9002","tipoOperacao":"UPDATE_STATUS","autor":"estagiario.teste","dataHoraEvento":"2026-09-29T11:15:00","estadoAnterior":"{\"status\":\"EM_ANDAMENTO\"}","estadoAtual":"{\"status\":\"CONCLUIDO\"}","metadados":"{\"observacao\":\"sem informacoes tecnicas\"}"}'
$resp3 = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/audit/events" -Method Post -Body $corpo3 -ContentType "application/json"
Write-Host " -> Evento persistido (ID: $($resp3.id)). Verificando violacao REG-001..." -ForegroundColor Yellow

Start-Sleep -Seconds 1
$todasViolacoes = (Invoke-RestMethod -Uri "http://localhost:8081/api/v1/audit/violations" -Method Get).content
$violacao =$todasViolacoes | Where-Object { $_.idEntidadeAfetada -eq "9002" -and $_.statusResolucao -eq "PENDENTE" } | Select-Object -First 1

if ($violacao) {
    Write-Host " -> Inconformidade detectada! ID: $($violacao.id) | Severidade: $($violacao.severidade)" -ForegroundColor Red
    Write-Host " -> Gerente tratando e encerrando ocorrencia..." -ForegroundColor Yellow
    $resolvido = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/audit/violations/$($violacao.id)/resolver" -Method Patch
    Write-Host " -> Status atualizado: $($resolvido.statusResolucao)" -ForegroundColor Green
}

# 4. DIRETOR
Write-Host "`n[4. DIRETOR/AUDITOR] Validando integridade forense do historico auditado..." -ForegroundColor Yellow
$validacao = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/audit/events/$idCriado/verificar-integridade" -Method Get
Write-Host " -> ID Auditado:          $($validacao.eventoId)" -ForegroundColor Cyan
Write-Host " -> Status Criptografico: $($validacao.statusIntegridade)" -ForegroundColor Cyan
Write-Host " -> Hash no Banco:        $($validacao.hashArmazenado)" -ForegroundColor Cyan
Write-Host " -> Hash Recalculado:     $($validacao.hashRecalculado)" -ForegroundColor Cyan

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "SUCESSO: INTEGRACAO ENTRE STI E SUBSISTEMA HOMOLOGADA!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green

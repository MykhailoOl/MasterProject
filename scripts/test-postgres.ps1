param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin',
    [int]$Port = 55439
)
$ErrorActionPreference = 'Stop'
$workspace = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$runPath = Join-Path $workspace ('build\postgres-verification-' + [Guid]::NewGuid().ToString('N'))
$dataPath = Join-Path $runPath 'data'
New-Item -ItemType Directory -Path $runPath -Force | Out-Null
$previousTestUrl = $env:INTERVIEW_TEST_DATABASE_URL
$started = $false
try {
    & (Join-Path $PostgresBin 'initdb.exe') -D $dataPath -U postgres -A trust -E UTF8 --no-locale > (Join-Path $runPath 'initdb.log') 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'Test database initialization failed.' }
    & (Join-Path $PostgresBin 'pg_ctl.exe') -D $dataPath -l (Join-Path $runPath 'postgres.log') -o "-h 127.0.0.1 -p $Port" -w start
    if ($LASTEXITCODE -ne 0) { throw 'Test database startup failed.' }
    $started = $true
    & (Join-Path $PostgresBin 'createdb.exe') -h 127.0.0.1 -p $Port -U postgres interview_verification
    if ($LASTEXITCODE -ne 0) { throw 'Test database creation failed.' }
    $env:INTERVIEW_TEST_DATABASE_URL = "jdbc:postgresql://127.0.0.1:$Port/interview_verification"
    Push-Location $workspace
    try {
        & .\gradlew.bat test --tests '*InterviewFlowIntegrationTests' --tests '*PostgresMigrationTests' --rerun-tasks --console=plain 2>&1 | Tee-Object -FilePath (Join-Path $runPath 'verification.log')
        $verificationExit = $LASTEXITCODE
        Copy-Item -LiteralPath (Join-Path $workspace 'build\reports\tests\test') -Destination (Join-Path $runPath 'report') -Recurse
        Copy-Item -LiteralPath (Join-Path $workspace 'build\test-results\test') -Destination (Join-Path $runPath 'test-results') -Recurse
        if ($verificationExit -ne 0) { throw 'PostgreSQL verification failed.' }
    } finally { Pop-Location }
} finally {
    $env:INTERVIEW_TEST_DATABASE_URL = $previousTestUrl
    if ($started) {
        & (Join-Path $PostgresBin 'pg_ctl.exe') -D $dataPath -m fast -w stop
        if ($LASTEXITCODE -ne 0) { Write-Warning 'The isolated test database needs manual shutdown.' }
    }
    Write-Output "Verification files: $runPath"
}

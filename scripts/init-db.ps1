param (
    [string]$PostgresPassword,
    [string]$PostgresUser = "postgres",
    [string]$AppDb = "thesis_app",
    [string]$AppUser = "thesis",
    [string]$AppPassword = "thesis",
    [string]$PsqlPath = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
)

if (-not (Test-Path $PsqlPath)) {
    Write-Error "psql not found at $PsqlPath"
    exit 1
}

if (-not $PostgresPassword) {
    $secure = Read-Host "Enter password for PostgreSQL user '$PostgresUser'" -AsSecureString
    $PostgresPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    )
}

$env:PGPASSWORD = $PostgresPassword

Write-Host "Creating role and database if missing..."

& $PsqlPath -U $PostgresUser -d postgres -h localhost -v ON_ERROR_STOP=1 -c @"
DO `$`$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$AppUser') THEN
        CREATE ROLE $AppUser LOGIN PASSWORD '$AppPassword';
    ELSE
        ALTER ROLE $AppUser WITH LOGIN PASSWORD '$AppPassword';
    END IF;
END
`$`$;
"@

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create/update role. Check the postgres password."
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    exit 1
}

$dbExists = & $PsqlPath -U $PostgresUser -d postgres -h localhost -tAc "SELECT 1 FROM pg_database WHERE datname = '$AppDb';"
if ($dbExists.Trim() -ne "1") {
    & $PsqlPath -U $PostgresUser -d postgres -h localhost -v ON_ERROR_STOP=1 -c "CREATE DATABASE $AppDb OWNER $AppUser;"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to create database $AppDb"
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
        exit 1
    }
}

& $PsqlPath -U $PostgresUser -d $AppDb -h localhost -v ON_ERROR_STOP=1 -c @"
GRANT ALL ON SCHEMA public TO $AppUser;
ALTER SCHEMA public OWNER TO $AppUser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $AppUser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $AppUser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $AppUser;
"@

$env:PGPASSWORD = $AppPassword
& $PsqlPath -U $AppUser -d $AppDb -h localhost -c "SELECT current_user, current_database();"

Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue

if ($LASTEXITCODE -eq 0) {
    Write-Host "Done. Database '$AppDb' is ready for user '$AppUser'."
} else {
    Write-Error "Role/database created, but login as $AppUser failed."
    exit 1
}

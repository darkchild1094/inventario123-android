# =============================================================================
#  crear-keystore.ps1
#  Genera el keystore de SUBIDA para Google Play y escribe keystore.properties
#  para que Gradle firme el release automaticamente.
#
#  Uso:   powershell -ExecutionPolicy Bypass -File .\crear-keystore.ps1
# =============================================================================

$ErrorActionPreference = 'Stop'

# --- Rutas ---------------------------------------------------------------------
$ProjectDir   = "C:\Users\darkchild1094\AndroidStudioProjects\inventario123-android"
$KeystoreDir  = "C:\Users\darkchild1094\keystores"
$KeystorePath = Join-Path $KeystoreDir "inventario123-upload.jks"
$PropsPath    = Join-Path $ProjectDir  "keystore.properties"
$Alias        = "upload"

# --- Localizar keytool ------------------------------------------------------------
$keytool = $null
$cmd = Get-Command keytool -ErrorAction SilentlyContinue
if ($cmd) { $keytool = $cmd.Source }
if (-not $keytool) {
    $candidatos = @(
        "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
        "$env:JAVA_HOME\bin\keytool.exe"
    )
    foreach ($c in $candidatos) { if ($c -and (Test-Path $c)) { $keytool = $c; break } }
}
if (-not $keytool) { throw "No se encontro keytool. Instala un JDK o Android Studio." }
Write-Host "keytool: $keytool"

# --- Comprobaciones -------------------------------------------------------------
if (Test-Path $KeystorePath) {
    throw "Ya existe $KeystorePath . Borralo o renombralo si quieres regenerarlo."
}
New-Item -ItemType Directory -Force -Path $KeystoreDir | Out-Null

# --- Contrasena --------------------------------------------------------------------
$p1 = Read-Host "Contrasena para el keystore (minimo 6 caracteres)" -AsSecureString
$p2 = Read-Host "Repite la contrasena" -AsSecureString
$s1 = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($p1))
$s2 = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($p2))
if ($s1 -ne $s2)      { throw "Las contrasenas no coinciden." }
if ($s1.Length -lt 6) { throw "La contrasena debe tener al menos 6 caracteres." }

# --- Datos del certificado (edita si quieres) --------------------------------
$dname = "CN=kernel94, OU=Inventario123, O=kernel94, L=NA, ST=NA, C=MX"

# --- Generar keystore --------------------------------------------------------
& $keytool -genkeypair -v `
    -keystore $KeystorePath `
    -storetype PKCS12 `
    -alias $Alias `
    -keyalg RSA -keysize 2048 -validity 10000 `
    -storepass $s1 -keypass $s1 `
    -dname $dname
if ($LASTEXITCODE -ne 0) { throw "keytool fallo (codigo $LASTEXITCODE)." }

# --- Escribir keystore.properties ------------------------------------------------
$storeFileProp = $KeystorePath -replace '\\', '/'
$propsContent = @"
storeFile=$storeFileProp
storePassword=$s1
keyAlias=$Alias
keyPassword=$s1
"@
# Sin BOM: java.util.Properties no tolera el BOM UTF-8 en la primera clave.
[System.IO.File]::WriteAllText($PropsPath, $propsContent, (New-Object System.Text.UTF8Encoding($false)))

Write-Host ""
Write-Host "======================================================================"
Write-Host " OK"
Write-Host "  Keystore : $KeystorePath"
Write-Host "  Alias    : $Alias"
Write-Host "  Props    : $PropsPath   (ignorado por git)"
Write-Host "  storePassword y keyPassword son la MISMA."
Write-Host ""
Write-Host " RESPALDA el archivo .jks y la contrasena en un lugar seguro."
Write-Host " Si los pierdes NO podras volver a actualizar la app en Play."
Write-Host "======================================================================"
Write-Host ""
Write-Host " Siguiente paso:  .\gradlew.bat bundleRelease"

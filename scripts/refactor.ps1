$ErrorActionPreference = "Stop"

$basePath = "C:\Users\andre\Documents\samba-web-ui\src"
$mainPath = "$basePath\main\java\mari\samba"
$testPath = "$basePath\test\java\mari\samba"

Write-Host "1. Creating new package directories..."
$dirs = @("core", "infra", "ad", "auth", "smbconfig", "filesystem", "group", "share", "user", "monitoring")
foreach ($dir in $dirs) {
    if (-not (Test-Path "$mainPath\$dir")) { New-Item -ItemType Directory -Force -Path "$mainPath\$dir" | Out-Null }
    if (-not (Test-Path "$testPath\$dir")) { New-Item -ItemType Directory -Force -Path "$testPath\$dir" | Out-Null }
}

Write-Host "2. Moving files..."
$moves = @(
    @("$mainPath\dto\common\*", "$mainPath\core\"),
    @("$mainPath\exception\*", "$mainPath\core\"),
    @("$mainPath\controller\advice\*", "$mainPath\core\"),
    @("$testPath\controller\advice\*", "$testPath\core\"),

    @("$mainPath\service\infra\*", "$mainPath\infra\"),
    @("$mainPath\service\parser\*", "$mainPath\infra\"),
    @("$testPath\service\infra\*", "$testPath\infra\"),
    @("$testPath\service\parser\*", "$testPath\infra\"),

    @("$mainPath\dto\ad\*", "$mainPath\ad\"),
    @("$mainPath\service\ad\*", "$mainPath\ad\"),
    @("$mainPath\controller\api\AdApiController.java", "$mainPath\ad\"),
    @("$testPath\controller\api\AdApiControllerTest.java", "$testPath\ad\"),

    @("$mainPath\dto\auth\*", "$mainPath\auth\"),
    @("$mainPath\controller\api\AuthApiController.java", "$mainPath\auth\"),
    @("$mainPath\service\AuthService*.java", "$mainPath\auth\"),
    @("$mainPath\service\BruteForce*.java", "$mainPath\auth\"),
    @("$testPath\controller\api\AuthApiControllerTest.java", "$testPath\auth\"),
    @("$testPath\service\AuthServiceTest.java", "$testPath\auth\"),

    @("$mainPath\dto\config\*", "$mainPath\smbconfig\"),
    @("$mainPath\controller\api\ConfigApiController.java", "$mainPath\smbconfig\"),
    @("$mainPath\service\SambaConfigService.java", "$mainPath\smbconfig\"),
    @("$testPath\controller\api\ConfigApiControllerTest.java", "$testPath\smbconfig\"),
    @("$testPath\service\SambaConfigServiceTest.java", "$testPath\smbconfig\"),

    @("$mainPath\dto\fs\*", "$mainPath\filesystem\"),
    @("$mainPath\controller\api\FileSystemApiController.java", "$mainPath\filesystem\"),
    @("$mainPath\service\FileSystemService.java", "$mainPath\filesystem\"),
    @("$testPath\controller\api\FileSystemApiControllerTest.java", "$testPath\filesystem\"),
    @("$testPath\service\FileSystemServiceTest.java", "$testPath\filesystem\"),

    @("$mainPath\dto\group\*", "$mainPath\group\"),
    @("$mainPath\controller\api\GroupApiController.java", "$mainPath\group\"),
    @("$mainPath\service\SambaGroupService.java", "$mainPath\group\"),
    @("$mainPath\model\SambaGroup.java", "$mainPath\group\"),
    @("$testPath\controller\api\GroupApiControllerTest.java", "$testPath\group\"),
    @("$testPath\service\SambaGroupServiceTest.java", "$testPath\group\"),

    @("$mainPath\dto\share\*", "$mainPath\share\"),
    @("$mainPath\controller\api\ShareApiController.java", "$mainPath\share\"),
    @("$mainPath\service\SambaShareService.java", "$mainPath\share\"),
    @("$mainPath\model\SambaShare.java", "$mainPath\share\"),
    @("$testPath\controller\api\ShareApiControllerTest.java", "$testPath\share\"),
    @("$testPath\service\SambaShareServiceTest.java", "$testPath\share\"),

    @("$mainPath\dto\user\*", "$mainPath\user\"),
    @("$mainPath\controller\api\UserApiController.java", "$mainPath\user\"),
    @("$mainPath\service\SambaUserService.java", "$mainPath\user\"),
    @("$mainPath\model\SambaUser.java", "$mainPath\user\"),
    @("$testPath\controller\api\UserApiControllerTest.java", "$testPath\user\"),
    @("$testPath\service\SambaUserServiceTest.java", "$testPath\user\"),

    @("$mainPath\dto\monitoring\*", "$mainPath\monitoring\"),
    @("$mainPath\controller\api\MonitoringApiController.java", "$mainPath\monitoring\"),
    @("$mainPath\controller\api\LogApiController.java", "$mainPath\monitoring\"),
    @("$mainPath\service\SambaMonitoringService.java", "$mainPath\monitoring\"),
    @("$mainPath\service\SambaLogService.java", "$mainPath\monitoring\"),
    @("$testPath\controller\api\MonitoringApiControllerTest.java", "$testPath\monitoring\")
)

foreach ($move in $moves) {
    $src = $move[0]
    $dest = $move[1]
    if (Test-Path $(Split-Path $src)) {
        Get-ChildItem $src -ErrorAction SilentlyContinue | Foreach-Object {
            Move-Item -Path $_.FullName -Destination $dest -Force
        }
    }
}

Write-Host "3. Cleaning empty packages..."
# Remove empty folders
$folders = Get-ChildItem -Path $basePath -Recurse -Directory | Sort-Object -Property @{Expression={$_.FullName.Length};Descending=$true}
foreach ($folder in $folders) {
    if (-not (Test-Path $folder.FullName)) { continue }
    if ((Get-ChildItem -Path $folder.FullName -Force).Count -eq 0) {
        Remove-Item -Path $folder.FullName -Force
    }
}

Write-Host "4. Updating Java imports and statements..."
$files = Get-ChildItem -Path $basePath -Filter *.java -Recurse
foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Raw

    # 4.1 Automate 'package' headers based on new location
    $relativePath = $file.DirectoryName -replace "^.*?\\src\\(main|test)\\java\\", ""
    $expectedPackage = $relativePath -replace "\\", "."
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "(?m)^package\s+.*?;", "package $expectedPackage;")

    # 4.2 Bulk replace imports and fully qualified names
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.dto\.ad\.([A-Za-z0-9_]+)\b", 'mari.samba.ad.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.dto\.auth\.([A-Za-z0-9_]+)\b", 'mari.samba.auth.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.dto\.common\.([A-Za-z0-9_]+)\b", 'mari.samba.core.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.dto\.config\.([A-Za-z0-9_]+)\b", 'mari.samba.smbconfig.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.dto\.fs\.([A-Za-z0-9_]+)\b", 'mari.samba.filesystem.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.dto\.group\.([A-Za-z0-9_]+)\b", 'mari.samba.group.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.dto\.share\.([A-Za-z0-9_]+)\b", 'mari.samba.share.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.dto\.user\.([A-Za-z0-9_]+)\b", 'mari.samba.user.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.dto\.monitoring\.([A-Za-z0-9_]+)\b", 'mari.samba.monitoring.$1')
    
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.exception\.([A-Za-z0-9_]+)\b", 'mari.samba.core.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.advice\.([A-Za-z0-9_]+)\b", 'mari.samba.core.$1')
    
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.infra\.([A-Za-z0-9_]+)\b", 'mari.samba.infra.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.parser\.([A-Za-z0-9_]+)\b", 'mari.samba.infra.$1')
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.ad\.([A-Za-z0-9_]+)\b", 'mari.samba.ad.$1')
    
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.api\.AdApiController\b", "mari.samba.ad.AdApiController")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.api\.AuthApiController\b", "mari.samba.auth.AuthApiController")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.api\.ConfigApiController\b", "mari.samba.smbconfig.ConfigApiController")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.api\.FileSystemApiController\b", "mari.samba.filesystem.FileSystemApiController")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.api\.GroupApiController\b", "mari.samba.group.GroupApiController")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.api\.ShareApiController\b", "mari.samba.share.ShareApiController")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.api\.UserApiController\b", "mari.samba.user.UserApiController")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.api\.MonitoringApiController\b", "mari.samba.monitoring.MonitoringApiController")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.controller\.api\.LogApiController\b", "mari.samba.monitoring.LogApiController")

    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.AuthService\b", "mari.samba.auth.AuthService")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.AuthServiceImpl\b", "mari.samba.auth.AuthServiceImpl")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.BruteForceProtectionService\b", "mari.samba.auth.BruteForceProtectionService")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.BruteForceProtectionServiceImpl\b", "mari.samba.auth.BruteForceProtectionServiceImpl")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.SambaConfigService\b", "mari.samba.smbconfig.SambaConfigService")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.FileSystemService\b", "mari.samba.filesystem.FileSystemService")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.SambaGroupService\b", "mari.samba.group.SambaGroupService")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.SambaShareService\b", "mari.samba.share.SambaShareService")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.SambaUserService\b", "mari.samba.user.SambaUserService")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.SambaMonitoringService\b", "mari.samba.monitoring.SambaMonitoringService")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.service\.SambaLogService\b", "mari.samba.monitoring.SambaLogService")

    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.model\.SambaGroup\b", "mari.samba.group.SambaGroup")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.model\.SambaShare\b", "mari.samba.share.SambaShare")
    $content = [System.Text.RegularExpressions.Regex]::Replace($content, "\bmari\.samba\.model\.SambaUser\b", "mari.samba.user.SambaUser")

    Set-Content -Path $file.FullName -Value $content -NoNewline
}

Write-Host "Migration script completed successfully."

param([string]$ProjectRoot = (Join-Path $PSScriptRoot '..\..'))
$ErrorActionPreference = 'Stop'
$taskBuild = [IO.Path]::GetFullPath($ProjectRoot)
$taskCache = Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1'
$jsonJar = Get-ChildItem "$taskCache\org.json\json\20240303" -Recurse -Filter '*.jar' | Select-Object -First 1 -ExpandProperty FullName
$kotlinJar = Get-ChildItem "$taskCache\org.jetbrains.kotlin\kotlin-stdlib\2.0.21" -Recurse -Filter '*.jar' | Select-Object -First 1 -ExpandProperty FullName
$androidJar = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platforms\android-36\android.jar'
$testClasses = Join-Path $PSScriptRoot 'classes'
New-Item -ItemType Directory -Path $testClasses -Force | Out-Null
$appClasses = Join-Path $taskBuild 'app\build\tmp\kotlin-classes\debug'
$cp = "$testClasses;$appClasses;$jsonJar;$kotlinJar;$androidJar"
& javac -cp $cp -d $testClasses (Join-Path $PSScriptRoot 'android\content\Context.java') (Join-Path $PSScriptRoot 'BackupRegression.java')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$expectedMetadata = Get-Content -LiteralPath (Join-Path $taskBuild 'app\build\outputs\apk\debug\output-metadata.json') -Raw | ConvertFrom-Json
$expectedVersion = $expectedMetadata.elements[0].versionName
$expectedBuild = $expectedMetadata.elements[0].versionCode
& java "-DexpectedVersion=$expectedVersion" "-DexpectedBuild=$expectedBuild" -cp $cp BackupRegression
exit $LASTEXITCODE

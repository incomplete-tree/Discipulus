[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BaselineDirectory,

    [Parameter(Mandatory)]
    [string]$FixedDirectory,

    [ValidateRange(1, 120)]
    [int]$TimeoutSeconds = 20
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;

public sealed class TopLevelWindow {
    public IntPtr Handle { get; set; }
    public bool Visible { get; set; }
}

public static class WindowProbe {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool EnumWindows(EnumWindowsProc callback, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);

    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool IsWindowVisible(IntPtr hWnd);

    public static TopLevelWindow[] ForProcess(uint processId) {
        var windows = new List<TopLevelWindow>();
        EnumWindows((hWnd, lParam) => {
            uint ownerProcessId;
            GetWindowThreadProcessId(hWnd, out ownerProcessId);
            if (ownerProcessId == processId) {
                windows.Add(new TopLevelWindow {
                    Handle = hWnd,
                    Visible = IsWindowVisible(hWnd)
                });
            }
            return true;
        }, IntPtr.Zero);
        return windows.ToArray();
    }
}
'@

function Invoke-LaunchProbe {
    param(
        [Parameter(Mandatory)]
        [string]$Name,

        [Parameter(Mandatory)]
        [string]$Directory
    )

    $executable = Join-Path $Directory 'discipulus.exe'
    if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
        throw "$Name executable is missing: $executable"
    }

    $process = Start-Process -FilePath $executable -WorkingDirectory $Directory -PassThru
    $windows = @()

    try {
        $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
        do {
            Start-Sleep -Milliseconds 500
            $process.Refresh()
            if ($process.HasExited) {
                break
            }
            $windows = [WindowProbe]::ForProcess([uint32]$process.Id)
        } while ((Get-Date) -lt $deadline -and (@($windows | Where-Object Visible).Count -eq 0))

        $process.Refresh()
        [pscustomobject]@{
            name = $Name
            pid = $process.Id
            exited = $process.HasExited
            exitCode = if ($process.HasExited) { $process.ExitCode } else { $null }
            topLevelWindows = @($windows).Count
            visibleTopLevelWindows = @($windows | Where-Object Visible).Count
        }
    }
    finally {
        if (-not $process.HasExited) {
            Stop-Process -Id $process.Id -Force
            $process.WaitForExit(5000) | Out-Null
        }
    }
}

$baseline = Invoke-LaunchProbe -Name 'baseline v0.2.1' -Directory $BaselineDirectory
$fixed = Invoke-LaunchProbe -Name 'fixed build' -Directory $FixedDirectory

$baseline, $fixed | Format-Table -AutoSize | Out-String | Write-Host

if ($baseline.exited -or $baseline.visibleTopLevelWindows -ne 0) {
    throw 'Baseline did not reproduce the expected running-but-hidden window state.'
}

if ($fixed.exited -or $fixed.visibleTopLevelWindows -eq 0) {
    throw 'Fixed build did not create a visible top-level window.'
}

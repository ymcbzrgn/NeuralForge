/**
 * NeuralForge - Electron Main Process
 * 
 * This spawns the Java backend and establishes IPC communication.
 * NO MOCKS - Real process spawn with stdin/stdout communication.
 */

const { app, BrowserWindow } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const readline = require('readline');

let mainWindow = null;
let backendProcess = null;
let backendReady = false;

/**
 * Start Java backend process
 * REAL implementation - spawns actual Java process
 */
function startBackend() {
    console.log('[Main] Starting Java backend...');
    
    const jarPath = path.join(__dirname, '..', 'backend', 'build', 'libs', 'neuralforge-backend.jar');
    
    // Spawn Java process with IPC via stdin/stdout
    backendProcess = spawn('java', [
        '-Xmx2G',  // Max 2GB RAM for backend
        '-jar',
        jarPath
    ], {
        stdio: ['pipe', 'pipe', 'pipe']  // stdin, stdout, stderr
    });
    
    // Setup readline for line-delimited JSON from stdout
    const rl = readline.createInterface({
        input: backendProcess.stdout,
        crlfDelay: Infinity
    });
    
    // Handle responses from backend (REAL IPC!)
    rl.on('line', (line) => {
        try {
            const response = JSON.parse(line);
            console.log('[Main] Backend response:', response);
            
            // Forward to renderer if window exists
            if (mainWindow && !mainWindow.isDestroyed()) {
                mainWindow.webContents.send('backend-message', response);
            }
            
            // Mark backend as ready on first pong
            if (response.type === 'pong' && !backendReady) {
                backendReady = true;
                console.log('[Main] Backend is ready!');
            }
        } catch (err) {
            console.error('[Main] Failed to parse backend response:', err.message);
        }
    });
    
    // Log backend stderr (not IPC, just logs)
    backendProcess.stderr.on('data', (data) => {
        console.log('[Backend]', data.toString().trim());
    });
    
    // Handle backend exit
    backendProcess.on('exit', (code) => {
        console.log(`[Main] Backend exited with code ${code}`);
        backendReady = false;
    });
    
    // Send ping to verify connection
    setTimeout(() => {
        sendToBackend({ type: 'ping', id: 'startup-ping' });
    }, 2000);
}

/**
 * Send message to backend via stdin
 * REAL IPC - writes JSON to Java process stdin
 */
function sendToBackend(message) {
    if (!backendProcess || backendProcess.killed) {
        console.error('[Main] Backend process not running');
        return;
    }
    
    const json = JSON.stringify(message) + '\n';
    console.log('[Main] Sending to backend:', message);
    backendProcess.stdin.write(json);
}

/**
 * Create main window
 */
function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1200,
        height: 800,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false
        }
    });
    
    // Load simple HTML (VS Code integration comes later)
    mainWindow.loadFile(path.join(__dirname, 'index.html'));
    
    // Open DevTools in development
    mainWindow.webContents.openDevTools();
    
    mainWindow.on('closed', () => {
        mainWindow = null;
    });
    
    console.log('[Main] Window created');
}

/**
 * App lifecycle
 */
app.on('ready', () => {
    console.log('[Main] App ready, starting backend...');
    startBackend();
    
    // Create window after short delay to let backend start
    setTimeout(createWindow, 1000);
});

app.on('window-all-closed', () => {
    // Quit app when windows closed
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

app.on('before-quit', () => {
    console.log('[Main] Shutting down backend...');
    if (backendProcess && !backendProcess.killed) {
        backendProcess.kill('SIGTERM');
    }
});

app.on('activate', () => {
    if (mainWindow === null) {
        createWindow();
    }
});

// Export for testing
module.exports = { sendToBackend };

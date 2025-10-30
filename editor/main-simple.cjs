const { app, BrowserWindow } = require('electron');

app.on('ready', () => {
    console.log('App ready!');
    const win = new BrowserWindow({ width: 800, height: 600 });
    win.loadFile('index.html');
});

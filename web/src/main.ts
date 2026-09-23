import './style.css';
import { preload } from './ui/art';
import { App } from './ui/app';

preload();
const app = new App(document.getElementById('table')!);
void app.start();
// Handy for poking at the game from the console while developing.
if (import.meta.env.DEV) Object.assign(window, { app });

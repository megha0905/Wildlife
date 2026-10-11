// Entry point: restore the saved login (if any), then start the app.
import { startApp } from './App.js';
import { restoreSession } from './auth/auth.js';
import { h, mount } from './components/dom.js';

const root = document.getElementById('root');
mount(root, h('div', { class: 'loading' }, 'Restoring your session…'));
restoreSession().then(() => startApp(root));

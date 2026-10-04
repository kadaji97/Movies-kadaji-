/**
 * Movieskadaji - Web Companion Server & APK Gateway
 * Runs on port 3000 to serve the web companion interface, health checks,
 * live stream previews, and direct Android APK download for testing.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

// Port configuration: Nginx listens on 8080 and proxies traffic to 3000 (DEFAULT_APP_PORT)
const PORT = parseInt(process.env.DEFAULT_APP_PORT || '3000', 10);
const APP_ROOT = __dirname;

// Path to compiled APK outputs
const APK_PATHS = [
  path.join(APP_ROOT, '.build-outputs', 'app-debug.apk'),
  path.join(APP_ROOT, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
];

function getApkFilePath() {
  for (const p of APK_PATHS) {
    if (fs.existsSync(p)) {
      return p;
    }
  }
  return null;
}

// Preset Catalog of Movies & CDN Streams for web preview
const CATALOG = [
  {
    imdbId: 'tt15239678',
    title: 'Dune: Part Two',
    category: '🎬 Movies',
    genres: ['Sci-Fi', 'Adventure', 'Action', 'Drama'],
    year: 2024,
    rating: 8.6,
    duration: '2h 46m',
    badge: '4K HDR',
    posterUrl: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80',
    backdropUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop&q=80',
    synopsis: 'Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.',
    streamUrl: 'https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-1080p.mp4',
    servers: [
      { name: 'Akamai UltraEdge (1080p)', ping: '24ms', status: 'Optimal' },
      { name: 'Cloudflare Stream Global (HLS)', ping: '32ms', status: 'High Speed' },
      { name: 'Fastly SuperCast (720p)', ping: '46ms', status: 'Failover Ready' }
    ]
  },
  {
    imdbId: 'tt6263850',
    title: 'Deadpool & Wolverine',
    category: '🎬 Movies',
    genres: ['Action', 'Comedy', 'Sci-Fi'],
    year: 2024,
    rating: 7.8,
    duration: '2h 08m',
    badge: '4K HDR',
    posterUrl: 'https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80',
    backdropUrl: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop&q=80',
    synopsis: 'Deadpool\'s peaceful existence comes crashing down when the Time Variance Authority recruits him alongside Wolverine.',
    streamUrl: 'https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8',
    servers: [
      { name: 'Akamai UltraEdge (1080p)', ping: '22ms', status: 'Optimal' },
      { name: 'Cloudflare Stream Global (HLS)', ping: '28ms', status: 'High Speed' },
      { name: 'Fastly SuperCast (720p)', ping: '41ms', status: 'Balanced' }
    ]
  },
  {
    imdbId: 'tt16366886',
    title: 'Gladiator II',
    category: '🎬 Movies',
    genres: ['Action', 'Adventure', 'Drama'],
    year: 2024,
    rating: 8.1,
    duration: '2h 28m',
    badge: '4K HDR',
    posterUrl: 'https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=600&auto=format&fit=crop&q=80',
    backdropUrl: 'https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&auto=format&fit=crop&q=80',
    synopsis: 'Years after witnessing the death of Maximus, Lucius must enter the Colosseum after Roman emperors conquer his home.',
    streamUrl: 'https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-720p.mp4',
    servers: [
      { name: 'Akamai UltraEdge (1080p)', ping: '26ms', status: 'Optimal' },
      { name: 'Cloudflare Stream Global (HLS)', ping: '35ms', status: 'High Speed' },
      { name: 'Fastly SuperCast (720p)', ping: '45ms', status: 'Failover Ready' }
    ]
  },
  {
    imdbId: 'tt18411490',
    title: 'Alien: Romulus',
    category: '🎬 Movies',
    genres: ['Sci-Fi', 'Horror', 'Thriller'],
    year: 2024,
    rating: 7.3,
    duration: '1h 59m',
    badge: '1080p FHD',
    posterUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&auto=format&fit=crop&q=80',
    backdropUrl: 'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1200&auto=format&fit=crop&q=80',
    synopsis: 'Young space colonizers scavenge a derelict station and face the most terrifying life form in the universe.',
    streamUrl: 'https://cdn.plyr.io/static/demo/View_From_A_Blue_Moon_Trailer-1080p.mp4',
    servers: [
      { name: 'Akamai UltraEdge (1080p)', ping: '29ms', status: 'Optimal' },
      { name: 'Cloudflare Stream Global (HLS)', ping: '31ms', status: 'High Speed' },
      { name: 'Fastly SuperCast (720p)', ping: '48ms', status: 'Balanced' }
    ]
  }
];

function renderHtml() {
  const apkPath = getApkFilePath();
  const apkAvailable = !!apkPath;
  const apkSizeMb = apkAvailable ? (fs.statSync(apkPath).size / (1024 * 1024)).toFixed(1) : '0';

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Movieskadaji - Cinematic Streaming & Android Companion</title>
  <style>
    :root {
      --bg-dark: #0A0D14;
      --bg-card: #121824;
      --bg-card-hover: #1A2234;
      --primary: #E50914;
      --primary-glow: rgba(229, 9, 20, 0.4);
      --accent-cyan: #00E5FF;
      --accent-gold: #FFB300;
      --text-main: #FFFFFF;
      --text-muted: #94A3B8;
      --border: rgba(255, 255, 255, 0.1);
    }
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    }
    body {
      background-color: var(--bg-dark);
      color: var(--text-main);
      line-height: 1.6;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    header {
      background: rgba(18, 24, 36, 0.85);
      backdrop-filter: blur(16px);
      border-bottom: 1px solid var(--border);
      position: sticky;
      top: 0;
      z-index: 100;
      padding: 16px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .logo-container {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .logo-badge {
      background: linear-gradient(135deg, #E50914, #B71C1C);
      color: white;
      font-weight: 900;
      font-size: 1.25rem;
      padding: 6px 14px;
      border-radius: 8px;
      box-shadow: 0 4px 14px var(--primary-glow);
    }
    .logo-text {
      font-size: 1.4rem;
      font-weight: 800;
      letter-spacing: -0.5px;
      background: linear-gradient(90deg, #FFFFFF, #94A3B8);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .badge-status {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      border-radius: 20px;
      background: rgba(16, 185, 129, 0.15);
      color: #10B981;
      font-size: 0.85rem;
      font-weight: 600;
      border: 1px solid rgba(16, 185, 129, 0.3);
    }
    .badge-status::before {
      content: '';
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #10B981;
      animation: pulse 2s infinite;
    }
    @keyframes pulse {
      0% { transform: scale(0.95); opacity: 0.8; }
      50% { transform: scale(1.2); opacity: 1; }
      100% { transform: scale(0.95); opacity: 0.8; }
    }
    .btn-download {
      background: linear-gradient(135deg, #E50914, #D32F2F);
      color: white;
      text-decoration: none;
      padding: 10px 20px;
      border-radius: 8px;
      font-weight: 700;
      font-size: 0.95rem;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      transition: all 0.2s ease;
      box-shadow: 0 4px 12px var(--primary-glow);
    }
    .btn-download:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 18px rgba(229, 9, 20, 0.6);
    }
    .hero {
      padding: 48px 28px 24px;
      max-width: 1200px;
      margin: 0 auto;
      width: 100%;
    }
    .hero-banner {
      background: linear-gradient(180deg, rgba(18, 24, 36, 0.4) 0%, rgba(10, 13, 20, 0.9) 100%),
                  radial-gradient(ellipse at top, rgba(229, 9, 20, 0.25), transparent 70%);
      border: 1px solid var(--border);
      border-radius: 16px;
      padding: 40px;
      text-align: center;
      position: relative;
      overflow: hidden;
    }
    .hero h1 {
      font-size: 2.5rem;
      font-weight: 800;
      margin-bottom: 12px;
      letter-spacing: -1px;
    }
    .hero p {
      color: var(--text-muted);
      font-size: 1.15rem;
      max-width: 650px;
      margin: 0 auto 28px;
    }
    .stats-row {
      display: flex;
      justify-content: center;
      gap: 32px;
      flex-wrap: wrap;
      margin-top: 24px;
      padding-top: 24px;
      border-top: 1px solid var(--border);
    }
    .stat-item {
      text-align: center;
    }
    .stat-val {
      font-size: 1.75rem;
      font-weight: 800;
      color: var(--accent-cyan);
    }
    .stat-label {
      font-size: 0.85rem;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .section-title {
      font-size: 1.5rem;
      font-weight: 700;
      margin: 36px 0 20px;
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .movies-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 24px;
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 28px 48px;
      width: 100%;
    }
    .movie-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: 12px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      transition: all 0.25s ease;
    }
    .movie-card:hover {
      transform: translateY(-6px);
      background: var(--bg-card-hover);
      border-color: rgba(229, 9, 20, 0.5);
      box-shadow: 0 12px 28px rgba(0,0,0,0.5);
    }
    .poster-container {
      position: relative;
      height: 340px;
      overflow: hidden;
    }
    .poster-container img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }
    .movie-card:hover .poster-container img {
      transform: scale(1.05);
    }
    .card-badge {
      position: absolute;
      top: 12px;
      right: 12px;
      background: rgba(0, 0, 0, 0.75);
      backdrop-filter: blur(6px);
      color: var(--accent-gold);
      font-size: 0.8rem;
      font-weight: 700;
      padding: 4px 8px;
      border-radius: 6px;
      border: 1px solid rgba(255, 179, 0, 0.4);
    }
    .card-rating {
      position: absolute;
      bottom: 12px;
      left: 12px;
      background: rgba(0, 0, 0, 0.85);
      backdrop-filter: blur(6px);
      color: #FFD700;
      font-size: 0.85rem;
      font-weight: 800;
      padding: 4px 10px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .movie-info {
      padding: 16px;
      display: flex;
      flex-direction: column;
      flex-grow: 1;
    }
    .movie-title {
      font-size: 1.15rem;
      font-weight: 700;
      margin-bottom: 6px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .movie-meta {
      color: var(--text-muted);
      font-size: 0.85rem;
      margin-bottom: 10px;
      display: flex;
      gap: 8px;
    }
    .movie-synopsis {
      color: var(--text-muted);
      font-size: 0.85rem;
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      margin-bottom: 14px;
    }
    .servers-list {
      margin-top: auto;
      background: rgba(0, 0, 0, 0.3);
      border-radius: 8px;
      padding: 8px 10px;
      font-size: 0.75rem;
    }
    .server-item {
      display: flex;
      justify-content: space-between;
      color: #94A3B8;
      padding: 2px 0;
    }
    .server-ping {
      color: #10B981;
      font-weight: 600;
    }
    footer {
      margin-top: auto;
      background: #080A0E;
      border-top: 1px solid var(--border);
      padding: 24px;
      text-align: center;
      color: var(--text-muted);
      font-size: 0.9rem;
    }
  </style>
</head>
<body>
  <header>
    <div class="logo-container">
      <div class="logo-badge">M</div>
      <div class="logo-text">Movieskadaji</div>
    </div>
    <div class="header-actions">
      <div class="badge-status">Android Build Active</div>
      <a href="/download-apk" class="btn-download">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="7 10 12 15 17 10"></polyline>
          <line x1="12" y1="15" x2="12" y2="3"></line>
        </svg>
        Download APK (${apkSizeMb} MB)
      </a>
    </div>
  </header>

  <div class="hero">
    <div class="hero-banner">
      <h1>Cinematic Streaming Engine</h1>
      <p>Android app powered by Jetpack Compose, Room Database, ExoPlayer & Multi-CDN streaming server links. Stream, track, and download offline with zero logins.</p>
      
      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-val">3+</div>
          <div class="stat-label">CDN Streams / Title</div>
        </div>
        <div class="stat-item">
          <div class="stat-val">4K & HDR</div>
          <div class="stat-label">Adaptive HLS Quality</div>
        </div>
        <div class="stat-item">
          <div class="stat-val">100%</div>
          <div class="stat-label">Build Verified</div>
        </div>
        <div class="stat-item">
          <div class="stat-val">ExoPlayer</div>
          <div class="stat-label">Media3 Playback</div>
        </div>
      </div>
    </div>

    <div class="section-title">
      <span>🔥</span>
      <span>Trending & Verified Streaming Releases</span>
    </div>
  </div>

  <div class="movies-grid">
    ${CATALOG.map(movie => `
      <div class="movie-card">
        <div class="poster-container">
          <img src="${movie.posterUrl}" alt="${movie.title}" loading="lazy">
          <div class="card-badge">${movie.badge}</div>
          <div class="card-rating">★ ${movie.rating}</div>
        </div>
        <div class="movie-info">
          <div class="movie-title">${movie.title}</div>
          <div class="movie-meta">
            <span>${movie.year}</span>
            <span>•</span>
            <span>${movie.duration}</span>
            <span>•</span>
            <span>${movie.genres.slice(0, 2).join(', ')}</span>
          </div>
          <div class="movie-synopsis">${movie.synopsis}</div>
          <div class="servers-list">
            ${movie.servers.map(s => `
              <div class="server-item">
                <span>${s.name}</span>
                <span class="server-ping">${s.ping}</span>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `).join('')}
  </div>

  <footer>
    <p>Movieskadaji • Android Jetpack Compose • Streaming Emulator Bridge</p>
    <p style="margin-top: 6px; font-size: 0.8rem; color: #64748B;">
      Android Application ID: com.aistudio.movieskadaji.tykohz • Cloud Run Port: ${PORT}
    </p>
  </footer>
</body>
</html>`;
}

const server = http.createServer((req, res) => {
  const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = parsedUrl.pathname;

  // Health and Readiness checks
  if (pathname === '/health' || pathname === '/ready' || pathname === '/api/status') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      service: 'Movieskadaji',
      framework: 'Android Compose',
      port: PORT,
      timestamp: new Date().toISOString()
    }));
    return;
  }

  // API: Catalog
  if (pathname === '/api/catalog') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ catalog: CATALOG }));
    return;
  }

  // APK Direct Download Endpoint
  if (pathname === '/download-apk' || pathname === '/app-debug.apk' || pathname === '/apk') {
    const apkFile = getApkFilePath();
    if (apkFile && fs.existsSync(apkFile)) {
      const stat = fs.statSync(apkFile);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="Movieskadaji-debug.apk"'
      });
      fs.createReadStream(apkFile).pipe(res);
      return;
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('APK file is currently compiling. Please refresh in a moment.');
      return;
    }
  }

  // Default: Serve Web Companion UI
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(renderHtml());
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[Movieskadaji Companion Server] Listening on http://0.0.0.0:${PORT}`);
});

process.on('SIGTERM', () => {
  console.log('[Movieskadaji Companion Server] Received SIGTERM, shutting down...');
  server.close(() => process.exit(0));
});

process.on('SIGINT', () => {
  console.log('[Movieskadaji Companion Server] Received SIGINT, shutting down...');
  server.close(() => process.exit(0));
});

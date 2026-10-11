// Leaflet map with OpenStreetMap tiles (no PostGIS needed: plain lat/lng columns).
// Leaflet itself is loaded by index.html, which gives us the global `L`.
import { h } from './dom.js';

/**
 * markers: [{ lat, lng, label, color, radius, onClick }]
 * path: optional [[lat, lng], ...] drawn as a dashed line (e.g. a patrol route)
 */
export function mapView({ markers = [], path, height = 360 }) {
  const host = h('div', { class: 'map', style: { height: `${height}px` } });

  // Leaflet needs the element to be on the page first, so wait until it is.
  const start = () => {
    if (!host.isConnected) { requestAnimationFrame(start); return; }
    if (typeof window.L === 'undefined') {
      host.textContent = 'Map unavailable (could not load the map library).';
      return;
    }
    const L = window.L;
    const map = L.map(host, { scrollWheelZoom: false }).setView([22.5, 82.5], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(map);

    const points = [];
    for (const mk of markers) {
      const c = L.circleMarker([mk.lat, mk.lng], {
        radius: mk.radius ?? 7,
        color: '#17302a',
        weight: 1.5,
        fillColor: mk.color ?? '#2c5246',
        fillOpacity: 0.9,
      }).bindTooltip(mk.label);
      if (mk.onClick) c.on('click', mk.onClick);
      c.addTo(map);
      points.push([mk.lat, mk.lng]);
    }
    if (path && path.length > 1) {
      L.polyline(path, { color: '#a8740f', weight: 3, dashArray: '6 4' }).addTo(map);
      points.push(...path);
    }
    if (points.length === 1) map.setView(points[0], 12);
    else if (points.length > 1) map.fitBounds(L.latLngBounds(points), { padding: [30, 30], maxZoom: 13 });
  };
  requestAnimationFrame(start);
  return host;
}

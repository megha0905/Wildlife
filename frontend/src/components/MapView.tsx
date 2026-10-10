// Leaflet map with OpenStreetMap tiles (no PostGIS needed: plain lat/lng columns).
import { useEffect, useRef } from 'react';
import L, { type LatLngExpression, type LayerGroup, type Map as LeafletMap } from 'leaflet';
import 'leaflet/dist/leaflet.css';

export interface MapMarker {
  id: string | number;
  lat: number;
  lng: number;
  label: string;
  color?: string;
  radius?: number;
  onClick?: () => void;
}

interface Props {
  markers: MapMarker[];
  /** Optional line through points, e.g. a patrol route. */
  path?: [number, number][];
  height?: number;
}

export function MapView({ markers, path, height = 360 }: Props) {
  const host = useRef<HTMLDivElement>(null);
  const map = useRef<LeafletMap | null>(null);
  const layer = useRef<LayerGroup | null>(null);

  useEffect(() => {
    if (!host.current || map.current) return;
    map.current = L.map(host.current, { scrollWheelZoom: false }).setView([22.5, 82.5], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(map.current);
    layer.current = L.layerGroup().addTo(map.current);
    return () => {
      map.current?.remove();
      map.current = null;
    };
  }, []);

  useEffect(() => {
    const m = map.current;
    const g = layer.current;
    if (!m || !g) return;
    g.clearLayers();
    const points: LatLngExpression[] = [];
    for (const mk of markers) {
      const c = L.circleMarker([mk.lat, mk.lng], {
        radius: mk.radius ?? 7,
        color: '#17302a',
        weight: 1.5,
        fillColor: mk.color ?? '#2c5246',
        fillOpacity: 0.9,
      }).bindTooltip(mk.label);
      if (mk.onClick) c.on('click', mk.onClick);
      c.addTo(g);
      points.push([mk.lat, mk.lng]);
    }
    if (path && path.length > 1) {
      L.polyline(path, { color: '#a8740f', weight: 3, dashArray: '6 4' }).addTo(g);
      points.push(...path);
    }
    if (points.length === 1) m.setView(points[0], 12);
    else if (points.length > 1) m.fitBounds(L.latLngBounds(points), { padding: [30, 30], maxZoom: 13 });
  }, [markers, path]);

  return <div ref={host} className="map" style={{ height }} />;
}

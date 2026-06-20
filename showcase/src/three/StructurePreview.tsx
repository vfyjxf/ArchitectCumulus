import { Canvas } from "@react-three/fiber";
import { Edges, Html, RoundedBox } from "@react-three/drei";
import { Suspense, useEffect, useMemo, useRef, useState } from "react";
import * as THREE from "three";
import { SceneEnv, GridFloor } from "./SceneEnv";
import type { ParsedStructure, JsonComponent } from "./dataDrivenStore";

// 每种 predicate 给一个稳定颜色
function colorForPredicate(p: string): string {
  if (!p) return "#666";
  if (p.startsWith("#")) {
    // tag
    const hash = hashStr(p);
    const palette = ["#fb7185", "#fb923c", "#fbbf24", "#a3e635", "#22d3ee", "#f0abfc"];
    return palette[hash % palette.length];
  }
  if (p.includes("controller")) return "#7c5cff";
  if (p.includes("casing") || p.includes("shell")) return "#fb7185";
  if (p.includes("energy")) return "#fbbf24";
  if (p.includes("item")) return "#22d3ee";
  if (p.includes("fluid") || p.includes("lava")) return "#a3e635";
  if (p.includes("air") || p.includes("door")) return "#666";
  return "#7c5cff";
}

function hashStr(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) {
    h = (h << 5) - h + s.charCodeAt(i);
    h |= 0;
  }
  return Math.abs(h);
}

interface Box3D {
  pos: [number, number, number];
  size: [number, number, number];
  color: string;
  label: string;
  isRegion?: boolean;
  isController?: boolean;
}

function componentToBoxes(c: JsonComponent): Box3D[] {
  const color = colorForPredicate(c.predicate);
  const label = c.name;
  const boxes: Box3D[] = [];

  if (c.kind === "point") {
    boxes.push({
      pos: c.from,
      size: [1, 1, 1],
      color,
      label,
      isController: c.capture === "controller" || c.name === "controller",
    });
    return boxes;
  }

  if (c.kind === "region") {
    // wireframe box for region
    const w = c.to[0] - c.from[0] + 1;
    const h = c.to[1] - c.from[1] + 1;
    const d = c.to[2] - c.from[2] + 1;
    boxes.push({
      pos: [
        (c.from[0] + c.to[0]) / 2,
        (c.from[1] + c.to[1]) / 2,
        (c.from[2] + c.to[2]) / 2,
      ],
      size: [w, h, d],
      color: "#fb923c",
      label: `${label} (region)`,
      isRegion: true,
    });
    return boxes;
  }

  // box / hollowBox: expand to cells
  for (let x = c.from[0]; x <= c.to[0]; x++) {
    for (let y = c.from[1]; y <= c.to[1]; y++) {
      for (let z = c.from[2]; z <= c.to[2]; z++) {
        const onShell =
          x === c.from[0] || x === c.to[0] ||
          y === c.from[1] || y === c.to[1] ||
          z === c.from[2] || z === c.to[2];
        if (c.kind === "hollowBox" && !onShell) continue;
        boxes.push({
          pos: [x, y, z],
          size: [0.95, 0.95, 0.95],
          color,
          label: onShell ? label : `${label} (interior)`,
        });
      }
    }
  }
  return boxes;
}

function PreviewBlock({ box, animateIn }: { box: Box3D; animateIn: boolean }) {
  const ref = useRef<THREE.Group>(null);
  const [hover, setHover] = useState(false);

  useEffect(() => {
    if (!animateIn || !ref.current) return;
    ref.current.scale.setScalar(0.01);
    let raf = 0;
    const start = performance.now();
    const dur = 400 + Math.random() * 200;
    const tick = () => {
      const t = Math.min(1, (performance.now() - start) / dur);
      const e = 1 - Math.pow(1 - t, 3);
      if (ref.current) ref.current.scale.setScalar(e);
      if (t < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [animateIn]);

  if (box.isRegion) {
    return (
      <group ref={ref} position={box.pos}>
        <mesh>
          <boxGeometry args={box.size} />
          <meshBasicMaterial color={box.color} wireframe transparent opacity={0.7} />
        </mesh>
        {hover && (
          <Html center distanceFactor={10} position={[0, box.size[1] / 2 + 0.5, 0]}>
            <div className="px-2 py-1 rounded bg-ink-950/90 text-amber2 text-[10px] font-mono border border-amber-400/30 whitespace-nowrap">
              {box.label}
            </div>
          </Html>
        )}
        <mesh
          onPointerEnter={(e) => { e.stopPropagation(); setHover(true); }}
          onPointerLeave={() => setHover(false)}
        >
          <boxGeometry args={box.size} />
          <meshBasicMaterial transparent opacity={0} />
        </mesh>
      </group>
    );
  }

  return (
    <group ref={ref} position={box.pos}>
      <RoundedBox
        args={box.size}
        radius={box.isController ? 0.16 : 0.06}
        smoothness={4}
        castShadow
        receiveShadow
        onPointerEnter={(e) => { e.stopPropagation(); setHover(true); }}
        onPointerLeave={() => setHover(false)}
      >
        <meshStandardMaterial
          color={box.color}
          metalness={box.isController ? 0.7 : 0.3}
          roughness={box.isController ? 0.2 : 0.55}
          emissive={box.color}
          emissiveIntensity={box.isController ? 0.5 : hover ? 0.3 : 0.1}
        />
        <Edges threshold={15} color="#ffffff" opacity={hover ? 0.5 : 0.2} transparent />
      </RoundedBox>
      {box.isController && (
        <mesh rotation={[0, performance.now() / 800, 0]}>
          <icosahedronGeometry args={[0.32, 0]} />
          <meshBasicMaterial color="#ffffff" wireframe />
        </mesh>
      )}
      {hover && (
        <Html center distanceFactor={10} position={[0, 0.8, 0]}>
          <div className="px-2 py-1 rounded bg-ink-950/90 text-[10px] font-mono border border-white/20 whitespace-nowrap">
            <span style={{ color: box.color }}>●</span>{" "}
            <span className="text-white/90">{box.label}</span>
            <span className="text-white/40 ml-1">@ {box.pos.join(",")}</span>
          </div>
        </Html>
      )}
    </group>
  );
}

function SceneImpl({ structure }: { structure: ParsedStructure | null }) {
  const boxes = useMemo<Box3D[]>(() => {
    if (!structure) return [];
    return structure.components.flatMap(componentToBoxes);
  }, [structure]);

  // auto-frame camera
  const center = useMemo(() => {
    if (boxes.length === 0) return { c: [0, 1, 0] as [number, number, number], dist: 8 };
    const xs = boxes.map((b) => b.pos[0]);
    const ys = boxes.map((b) => b.pos[1]);
    const zs = boxes.map((b) => b.pos[2]);
    const cx = (Math.min(...xs) + Math.max(...xs)) / 2;
    const cy = (Math.min(...ys) + Math.max(...ys)) / 2;
    const cz = (Math.min(...zs) + Math.max(...zs)) / 2;
    const dx = Math.max(...xs) - Math.min(...xs) + 2;
    const dy = Math.max(...ys) - Math.min(...ys) + 2;
    const dz = Math.max(...zs) - Math.min(...zs) + 2;
    const dist = Math.max(6, Math.sqrt(dx * dx + dy * dy + dz * dz) * 1.4);
    return { c: [cx, cy + 0.5, cz] as [number, number, number], dist };
  }, [boxes]);

  return (
    <>
      <SceneEnv
        cameraPosition={[center.dist, center.dist * 0.7, center.dist]}
        cameraTarget={center.c}
      />
      <GridFloor size={Math.max(20, center.dist * 2.5)} divisions={Math.max(20, center.dist * 2.5)} />
      {boxes.map((b, i) => (
        <PreviewBlock key={`${b.label}-${b.pos.join(",")}-${i}`} box={b} animateIn />
      ))}
    </>
  );
}

export function StructurePreview({ structure }: { structure: ParsedStructure | null }) {
  return (
    <Canvas shadows dpr={[1, 2]} camera={{ position: [8, 6, 8], fov: 42 }}>
      <Suspense fallback={null}>
        <SceneImpl structure={structure} />
      </Suspense>
    </Canvas>
  );
}

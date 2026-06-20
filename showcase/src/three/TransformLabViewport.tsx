import { Canvas } from "@react-three/fiber";
import { Edges, Html, RoundedBox } from "@react-three/drei";
import { Suspense, useState } from "react";
import { SceneEnv, GridFloor } from "./SceneEnv";
import {
  LAB_BLOCKS,
  WORLD_BLOCKS,
  TRANSFORMS,
  useTransformLab,
  type TransformId,
} from "./transformLab";

function WorldBlock({ pos, kind }: { pos: [number, number, number]; kind: string }) {
  const [hover, setHover] = useState(false);
  const color = kind === "controller" ? "#7c5cff" : kind === "port" ? "#fbbf24" : "#fb7185";
  return (
    <group position={pos}>
      <RoundedBox
        args={[0.92, 0.92, 0.92]}
        radius={0.04}
        smoothness={3}
        castShadow
        receiveShadow
        onPointerEnter={(e) => { e.stopPropagation(); setHover(true); }}
        onPointerLeave={() => setHover(false)}
      >
        <meshStandardMaterial
          color={color}
          metalness={0.25}
          roughness={0.55}
          emissive={color}
          emissiveIntensity={hover ? 0.35 : 0.08}
          transparent
          opacity={0.45}
        />
        <Edges threshold={15} color={color} opacity={0.7} transparent />
      </RoundedBox>
      {hover && (
        <Html center distanceFactor={10} position={[0, 0.7, 0]}>
          <div className="px-2 py-1 rounded bg-ink-950/90 text-[10px] font-mono border border-white/20 whitespace-nowrap">
            <span style={{ color }}>●</span> world {kind} @ {pos.join(",")}
          </div>
        </Html>
      )}
    </group>
  );
}

function PlacedBlock({
  world,
  color,
  label,
  kind,
  matched,
  index,
}: {
  world: [number, number, number];
  color: string;
  label: string;
  kind: string;
  matched: boolean;
  index: number;
}) {
  const [hover, setHover] = useState(false);
  return (
    <group position={world}>
      <RoundedBox
        args={[0.95, 0.95, 0.95]}
        radius={kind === "controller" ? 0.16 : 0.06}
        smoothness={4}
        castShadow
        receiveShadow
        onPointerEnter={(e) => { e.stopPropagation(); setHover(true); }}
        onPointerLeave={() => setHover(false)}
      >
        <meshStandardMaterial
          color={color}
          metalness={kind === "controller" ? 0.7 : 0.3}
          roughness={kind === "controller" ? 0.2 : 0.55}
          emissive={matched ? color : "#fb7185"}
          emissiveIntensity={matched ? (kind === "controller" ? 0.5 : 0.18) : 0.4}
        />
        <Edges
          threshold={15}
          color={matched ? "#ffffff" : "#fb7185"}
          opacity={hover ? 0.7 : matched ? 0.3 : 0.8}
          transparent
        />
      </RoundedBox>
      {!matched && (
        <Html center distanceFactor={10} position={[0, 1, 0]}>
          <div className="px-2 py-1 rounded bg-rose2 text-white text-[10px] font-mono border border-rose-300 whitespace-nowrap shadow-lg">
            ✗ #{index + 1} mismatch
          </div>
        </Html>
      )}
      {matched && hover && (
        <Html center distanceFactor={10} position={[0, 1, 0]}>
          <div className="px-2 py-1 rounded bg-lime-400/90 text-ink-950 text-[10px] font-mono border border-lime-200 whitespace-nowrap">
            ✓ #{index + 1} {label}
          </div>
        </Html>
      )}
    </group>
  );
}

function Anchor({ pos = [0, 0, 0] as [number, number, number] }) {
  return (
    <group position={pos}>
      <mesh position={[0, -0.4, 0]}>
        <cylinderGeometry args={[0.5, 0.5, 0.04, 32]} />
        <meshBasicMaterial color="#7c5cff" transparent opacity={0.4} />
      </mesh>
      <mesh position={[0, -0.4, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.4, 0.55, 32]} />
        <meshBasicMaterial color="#7c5cff" transparent opacity={0.8} />
      </mesh>
    </group>
  );
}

function SceneImpl() {
  const selected = useTransformLab((s) => s.selected);
  const transform = TRANSFORMS.find((t) => t.id === selected)!;
  const placedBlocks = LAB_BLOCKS.map((b) => {
    const world = transform.apply(b.rel);
    const worldBlock = WORLD_BLOCKS.find(
      (w) => w.pos[0] === world[0] && w.pos[1] === world[1] && w.pos[2] === world[2]
    );
    const matched = !!worldBlock && worldBlock.expectedKind === b.kind;
    return { ...b, world, matched };
  });

  return (
    <>
      <SceneEnv cameraPosition={[6, 6, 8]} cameraTarget={[1, 0, 1]} />
      <GridFloor size={20} divisions={20} />

      <Anchor />

      {/* ghosted world blocks (always visible) */}
      {WORLD_BLOCKS.map((w, i) => (
        <WorldBlock key={i} pos={w.pos} kind={w.expectedKind} />
      ))}

      {/* placed blocks on top */}
      {placedBlocks.map((b, i) => (
        <PlacedBlock
          key={i}
          world={b.world}
          color={b.color}
          label={b.label}
          kind={b.kind}
          matched={b.matched}
          index={i}
        />
      ))}
    </>
  );
}

export function TransformLabViewport() {
  return (
    <Canvas shadows dpr={[1, 2]} camera={{ position: [6, 6, 8], fov: 42 }}>
      <Suspense fallback={null}>
        <SceneImpl />
      </Suspense>
    </Canvas>
  );
}

import { Canvas } from "@react-three/fiber";
import { Edges, Html, RoundedBox } from "@react-three/drei";
import { Suspense, useEffect, useMemo, useRef, useState } from "react";
import * as THREE from "three";
import { SceneEnv, GridFloor } from "./SceneEnv";
import {
  PALETTE,
  useBuilder,
  generateIR,
  type PlacedBlock,
  type BlockKind,
} from "./builderStore";

const GRID_SIZE = 6; // -3..3 on x/z
const GRID_HEIGHT = 6; // 0..5 on y

function rotateY90(b: PlacedBlock): PlacedBlock {
  return { ...b, x: -b.z, z: b.x };
}
function rotateY180(b: PlacedBlock): PlacedBlock {
  return { ...b, x: -b.x, z: -b.z };
}
function rotateY270(b: PlacedBlock): PlacedBlock {
  return { ...b, x: b.z, z: -b.x };
}

function PlacedBlockMesh({
  block,
  rotateStep,
  selectedLoc,
  onSelect,
}: {
  block: PlacedBlock;
  rotateStep: number;
  selectedLoc: string | null;
  onSelect: (k: string) => void;
}) {
  const { kind, x, y, z } = block;
  const palette = PALETTE.find((p) => p.kind === kind)!;
  const isDiagnostic = useBuilder((s) =>
    s.diagnostics.some(
      (d) =>
        d.location[0] === block.x &&
        d.location[1] === block.y &&
        d.location[2] === block.z
    )
  );
  const isController = kind === "controller";
  const key = `${x},${y},${z}`;
  const isSelected = selectedLoc === key;

  // hover state — declared at top with other hooks
  const [hover, setHover] = useState(false);

  // apply rotation (visual only)
  let display = block;
  if (rotateStep === 1) display = rotateY90(block);
  else if (rotateStep === 2) display = rotateY180(block);
  else if (rotateStep === 3) display = rotateY270(block);

  // place-in animation
  const ref = useRef<THREE.Group>(null);
  useEffect(() => {
    if (!ref.current) return;
    ref.current.scale.setScalar(0.01);
    const start = performance.now();
    const dur = 350;
    let raf = 0;
    const tick = () => {
      const t = Math.min(1, (performance.now() - start) / dur);
      const e = 1 - Math.pow(1 - t, 3);
      if (ref.current) ref.current.scale.setScalar(0.4 + e * 0.6);
      if (t < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [key]);

  return (
    <group
      ref={ref}
      position={[display.x, display.y, display.z]}
      onPointerEnter={(e) => {
        e.stopPropagation();
        setHover(true);
      }}
      onPointerLeave={() => setHover(false)}
      onClick={(e) => {
        e.stopPropagation();
        onSelect(key);
      }}
    >
      <RoundedBox
        args={[0.95, 0.95, 0.95]}
        radius={isController ? 0.18 : 0.06}
        smoothness={5}
        castShadow
        receiveShadow
      >
        <meshStandardMaterial
          color={palette.color}
          metalness={isController ? 0.7 : 0.3}
          roughness={isController ? 0.2 : 0.55}
          emissive={palette.color}
          emissiveIntensity={
            isController ? 0.45 : hover || isSelected ? 0.35 : 0.1
          }
        />
        <Edges
          threshold={15}
          color={isSelected ? "#ffffff" : isDiagnostic ? "#fb7185" : "#ffffff"}
          opacity={
            isSelected ? 0.9 : isDiagnostic ? 0.7 : hover ? 0.4 : 0.2
          }
          transparent
        />
      </RoundedBox>

      {isController && (
        <mesh rotation={[0, performance.now() / 800, 0]}>
          <icosahedronGeometry args={[0.3, 0]} />
          <meshBasicMaterial color="#ffffff" wireframe />
        </mesh>
      )}

      {(hover || isSelected) && (
        <Html center distanceFactor={10} position={[0, 1, 0]}>
          <div className="px-2.5 py-1.5 rounded-lg bg-ink-950/90 backdrop-blur-md text-[10.5px] font-mono border border-white/15 whitespace-nowrap shadow-xl">
            <div className="flex items-center gap-1.5 mb-0.5">
              <span
                className="w-2 h-2 rounded-sm"
                style={{ background: palette.color }}
              />
              <span className="text-white/90 font-semibold">{palette.label}</span>
              {isController && (
                <span className="text-brand-300 text-[9px]">★ CTRL</span>
              )}
            </div>
            <div className="text-white/40">
              @ ({x}, {y}, {z})
              {rotateStep > 0 && (
                <span className="text-cyan2 ml-1">
                  → rot{rotateStep} ({display.x},{display.y},{display.z})
                </span>
              )}
            </div>
            <div className="text-white/50 text-[9.5px] mt-0.5">
              role: {palette.role} ·{" "}
              {palette.predicate.split("(")[0].slice(0, 30)}
            </div>
            {isDiagnostic && (
              <div className="text-rose2 text-[9.5px] mt-0.5">⚠ diagnostic</div>
            )}
          </div>
        </Html>
      )}

      {isDiagnostic && !hover && !isSelected && (
        <Html center distanceFactor={10} position={[0, 1, 0]}>
          <div className="px-1.5 py-0.5 rounded bg-rose2/90 text-white text-[9px] font-mono border border-rose-200/40 whitespace-nowrap shadow-lg">
            ⚠
          </div>
        </Html>
      )}
    </group>
  );
}

function GhostBox({
  position,
  size,
}: {
  position: [number, number, number];
  size: [number, number, number];
}) {
  return (
    <mesh position={position}>
      <boxGeometry args={size} />
      <meshBasicMaterial color="#7c5cff" transparent opacity={0.08} />
      <Edges color="#7c5cff" opacity={0.5} transparent />
    </mesh>
  );
}

function GroundTile({
  x,
  z,
  onPlace,
  hover,
  setHover,
}: {
  x: number;
  z: number;
  onPlace: (x: number, y: number, z: number) => void;
  hover: boolean;
  setHover: (b: boolean) => void;
}) {
  return (
    <mesh
      position={[x, -0.5, z]}
      rotation={[-Math.PI / 2, 0, 0]}
      onPointerEnter={(e) => {
        e.stopPropagation();
        setHover(true);
      }}
      onPointerLeave={() => setHover(false)}
      onClick={(e) => {
        e.stopPropagation();
        onPlace(x, 0, z);
      }}
      onContextMenu={(e) => {
        e.stopPropagation();
        onPlace(x, 0, z);
      }}
    >
      <planeGeometry args={[0.96, 0.96]} />
      <meshStandardMaterial
        color={hover ? "#3a3a72" : "#15152a"}
        emissive={hover ? "#7c5cff" : "#000000"}
        emissiveIntensity={hover ? 0.3 : 0}
        transparent
        opacity={0.7}
      />
      <Edges threshold={15} color={hover ? "#7c5cff" : "#2a2a4a"} />
    </mesh>
  );
}

function BuildCell({
  x,
  y,
  z,
  onPlace,
}: {
  x: number;
  y: number;
  z: number;
  onPlace: (x: number, y: number, z: number) => void;
}) {
  const [hover, setHover] = useState(false);
  return (
    <mesh
      position={[x, y, z]}
      onPointerEnter={(e) => {
        e.stopPropagation();
        setHover(true);
      }}
      onPointerLeave={() => setHover(false)}
      onClick={(e) => {
        e.stopPropagation();
        onPlace(x, y, z);
      }}
      visible={hover}
    >
      <boxGeometry args={[0.95, 0.95, 0.95]} />
      <meshBasicMaterial color="#7c5cff" transparent opacity={0.18} wireframe />
    </mesh>
  );
}

function AxisGizmo() {
  return (
    <group position={[-GRID_SIZE - 1.5, -0.5, -GRID_SIZE - 1.5]}>
      <mesh position={[0.6, 0, 0]}>
        <boxGeometry args={[1.2, 0.04, 0.04]} />
        <meshBasicMaterial color="#fb7185" />
      </mesh>
      <mesh position={[0, 0.6, 0]}>
        <boxGeometry args={[0.04, 1.2, 0.04]} />
        <meshBasicMaterial color="#a3e635" />
      </mesh>
      <mesh position={[0, 0, 0.6]}>
        <boxGeometry args={[0.04, 0.04, 1.2]} />
        <meshBasicMaterial color="#22d3ee" />
      </mesh>
    </group>
  );
}

function BuilderScene({
  rotateStep,
  selectedLoc,
  onSelect,
}: {
  rotateStep: number;
  selectedLoc: string | null;
  onSelect: (k: string) => void;
}) {
  const blocks = useBuilder((s) => s.blocks);
  const place = useBuilder((s) => s.place);
  const ghost = useBuilder((s) => s.ghost);

  const [hoverKey, setHoverKey] = useState<string | null>(null);

  const ghostBounds = useMemo(() => {
    if (blocks.length === 0) return null;
    const xs = blocks.map((b) => b.x);
    const ys = blocks.map((b) => b.y);
    const zs = blocks.map((b) => b.z);
    return {
      fx: Math.min(...xs) - 0.5,
      fy: Math.min(...ys) - 0.5,
      fz: Math.min(...zs) - 0.5,
      tx: Math.max(...xs) + 0.5,
      ty: Math.max(...ys) + 0.5,
      tz: Math.max(...zs) + 0.5,
    };
  }, [blocks]);

  return (
    <>
      <SceneEnv cameraPosition={[8, 8, 10]} cameraTarget={[0, 1, 0]} />
      <GridFloor size={GRID_SIZE * 2 + 4} divisions={GRID_SIZE * 2 + 4} />

      {/* ground tiles */}
      {Array.from({ length: GRID_SIZE * 2 + 1 }, (_, i) => i - GRID_SIZE).flatMap((x) =>
        Array.from({ length: GRID_SIZE * 2 + 1 }, (_, j) => j - GRID_SIZE).map((z) => {
          const k = `${x},${z}`;
          return (
            <GroundTile
              key={`g-${x}-${z}`}
              x={x}
              z={z}
              onPlace={place}
              hover={hoverKey === k}
              setHover={(b) => setHoverKey(b ? k : hoverKey === k ? null : hoverKey)}
            />
          );
        })
      )}

      {/* invisible build cells */}
      {Array.from({ length: GRID_SIZE * 2 + 1 }, (_, i) => i - GRID_SIZE).flatMap((x) =>
        Array.from({ length: GRID_HEIGHT }, (_, y) => y).flatMap((yy) =>
          Array.from({ length: GRID_SIZE * 2 + 1 }, (_, k) => k - GRID_SIZE).map((z) => (
            <BuildCell
              key={`c-${x}-${yy}-${z}`}
              x={x}
              y={yy}
              z={z}
              onPlace={place}
            />
          ))
        )
      )}

      {/* ghost bounding box */}
      {ghost && ghostBounds && (
        <GhostBox
          position={[
            (ghostBounds.fx + ghostBounds.tx) / 2,
            (ghostBounds.fy + ghostBounds.ty) / 2,
            (ghostBounds.fz + ghostBounds.tz) / 2,
          ]}
          size={[
            ghostBounds.tx - ghostBounds.fx + 1,
            ghostBounds.ty - ghostBounds.fy + 1,
            ghostBounds.tz - ghostBounds.fz + 1,
          ]}
        />
      )}

      {/* placed blocks */}
      {blocks.map((b, i) => (
        <PlacedBlockMesh
          key={`${b.x}-${b.y}-${b.z}-${i}`}
          block={b}
          rotateStep={rotateStep}
          selectedLoc={selectedLoc}
          onSelect={onSelect}
        />
      ))}

      <AxisGizmo />
    </>
  );
}

interface ViewportProps {
  rotateStep: number;
  selectedLoc: string | null;
  onSelect: (k: string) => void;
}

export function BuilderViewport({ rotateStep, selectedLoc, onSelect }: ViewportProps) {
  return (
    <Canvas shadows dpr={[1, 2]} camera={{ position: [8, 8, 10], fov: 42 }}>
      <Suspense fallback={null}>
        <BuilderScene
          rotateStep={rotateStep}
          selectedLoc={selectedLoc}
          onSelect={onSelect}
        />
      </Suspense>
    </Canvas>
  );
}

export { useBuilder, generateIR };
export type { BlockKind };

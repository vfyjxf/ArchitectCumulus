import { Canvas } from "@react-three/fiber";
import { Edges, Html, RoundedBox } from "@react-three/drei";
import { Suspense, useMemo, useRef, useState } from "react";
import * as THREE from "three";
import { SceneEnv, GridFloor } from "./SceneEnv";
import { ROLE_COLOR, type ScenarioBlock } from "@/data/scenarios";

interface ViewportBlock extends ScenarioBlock {
  color: string;
}

interface Props {
  blocks: ScenarioBlock[];
  /** 当前 hover 的 dslTag（外部控制） */
  activeTag?: string | null;
  /** 当 hover 方块时通知外部 */
  onHoverTag?: (tag: string | null) => void;
}

function Block3D({
  block,
  active,
  onHover,
}: {
  block: ViewportBlock;
  active: boolean;
  onHover: (tag: string | null) => void;
}) {
  const ref = useRef<THREE.Group>(null);
  const [hover, setHover] = useState(false);
  const size = block.size ?? [0.92, 0.92, 0.92];
  const isRegion = block.role === "region";
  const isController = block.role === "controller";
  const isContext = block.role === "context";

  const highlighted = active || hover;

  if (isRegion) {
    return (
      <group position={block.pos}>
        <mesh>
          <boxGeometry args={size} />
          <meshBasicMaterial
            color={block.color}
            wireframe
            transparent
            opacity={highlighted ? 0.9 : 0.5}
          />
        </mesh>
        {highlighted && (
          <Html center distanceFactor={10} position={[0, size[1] / 2 + 0.5, 0]}>
            <div className="px-2 py-1 rounded bg-ink-950/90 text-[10px] font-mono border border-amber-400/30 whitespace-nowrap">
              <span style={{ color: block.color }}>●</span>{" "}
              <span className="text-white/90">{block.label}</span>
            </div>
          </Html>
        )}
        <mesh
          onPointerEnter={(e) => { e.stopPropagation(); setHover(true); onHover(block.dslTag ?? null); }}
          onPointerLeave={() => { setHover(false); onHover(null); }}
        >
          <boxGeometry args={size} />
          <meshBasicMaterial transparent opacity={0} />
        </mesh>
      </group>
    );
  }

  return (
    <group ref={ref} position={block.pos}>
      {isContext ? (
        // context 方块用半透明球体表示（抽象派生，非真实方块）
        <mesh
          onPointerEnter={(e) => { e.stopPropagation(); setHover(true); onHover(block.dslTag ?? null); }}
          onPointerLeave={() => { setHover(false); onHover(null); }}
        >
          <sphereGeometry args={[0.35, 16, 16]} />
          <meshStandardMaterial
            color={block.color}
            transparent
            opacity={highlighted ? 0.7 : 0.35}
            emissive={block.color}
            emissiveIntensity={highlighted ? 0.6 : 0.3}
          />
        </mesh>
      ) : (
        <RoundedBox
          args={size}
          radius={isController ? 0.16 : 0.06}
          smoothness={4}
          castShadow
          receiveShadow
          onPointerEnter={(e) => { e.stopPropagation(); setHover(true); onHover(block.dslTag ?? null); }}
          onPointerLeave={() => { setHover(false); onHover(null); }}
        >
          <meshStandardMaterial
            color={block.color}
            metalness={isController ? 0.7 : 0.3}
            roughness={isController ? 0.2 : 0.55}
            emissive={block.color}
            emissiveIntensity={highlighted ? 0.5 : 0.1}
          />
          <Edges threshold={15} color="#ffffff" opacity={highlighted ? 0.6 : 0.15} transparent />
        </RoundedBox>
      )}
      {isController && (
        <mesh rotation={[0, Date.now() / 800, 0]}>
          <icosahedronGeometry args={[0.3, 0]} />
          <meshBasicMaterial color="#ffffff" wireframe />
        </mesh>
      )}
      {highlighted && (
        <Html center distanceFactor={10} position={[0, 0.8, 0]}>
          <div className="px-2 py-1 rounded bg-ink-950/90 text-[10px] font-mono border border-white/20 whitespace-nowrap max-w-[280px]">
            <div className="flex items-center gap-1">
              <span style={{ color: block.color }}>●</span>
              <span className="text-white/90 font-semibold">{block.label}</span>
              <span className="text-white/30">@ {block.pos.join(",")}</span>
            </div>
            {block.target && (
              <div className="text-white/50 mt-0.5">
                {block.pointId} → {block.target}
                {block.predicate ? ` · ${block.predicate}` : ""}
              </div>
            )}
            {block.note && (
              <div className="text-white/40 mt-0.5 text-[9px] leading-tight">{block.note}</div>
            )}
          </div>
        </Html>
      )}
    </group>
  );
}

function SceneImpl({ blocks, activeTag, onHoverTag }: Props) {
  const viewportBlocks: ViewportBlock[] = useMemo(
    () => blocks.map((b) => ({ ...b, color: ROLE_COLOR[b.role] })),
    [blocks]
  );

  const center = useMemo(() => {
    if (viewportBlocks.length === 0) return { c: [0, 1, 0] as [number, number, number], dist: 8 };
    const xs = viewportBlocks.map((b) => b.pos[0]);
    const ys = viewportBlocks.map((b) => b.pos[1]);
    const zs = viewportBlocks.map((b) => b.pos[2]);
    const cx = (Math.min(...xs) + Math.max(...xs)) / 2;
    const cy = (Math.min(...ys) + Math.max(...ys)) / 2;
    const cz = (Math.min(...zs) + Math.max(...zs)) / 2;
    const dx = Math.max(...xs) - Math.min(...xs) + 2;
    const dy = Math.max(...ys) - Math.min(...ys) + 2;
    const dz = Math.max(...zs) - Math.min(...zs) + 2;
    const dist = Math.max(6, Math.sqrt(dx * dx + dy * dy + dz * dz) * 1.5);
    return { c: [cx, cy + 0.5, cz] as [number, number, number], dist };
  }, [viewportBlocks]);

  return (
    <>
      <SceneEnv
        cameraPosition={[center.dist * 0.8, center.dist * 0.6, center.dist]}
        cameraTarget={center.c}
      />
      <GridFloor size={Math.max(20, center.dist * 2.5)} divisions={Math.max(20, center.dist * 2.5)} />
      {viewportBlocks.map((b, i) => (
        <Block3D
          key={`${b.label}-${b.pos.join(",")}-${i}`}
          block={b}
          active={activeTag != null && b.dslTag === activeTag}
          onHover={onHoverTag ?? (() => {})}
        />
      ))}
    </>
  );
}

export function ScenarioViewport({ blocks, activeTag, onHoverTag }: Props) {
  return (
    <Canvas shadows dpr={[1, 2]} camera={{ position: [8, 6, 8], fov: 42 }}>
      <Suspense fallback={null}>
        <SceneImpl blocks={blocks} activeTag={activeTag} onHoverTag={onHoverTag} />
      </Suspense>
    </Canvas>
  );
}

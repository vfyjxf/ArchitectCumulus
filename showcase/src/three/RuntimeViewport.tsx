import { Canvas } from "@react-three/fiber";
import { Html } from "@react-three/drei";
import { Suspense, useEffect, useRef } from "react";
import * as THREE from "three";
import { SceneEnv } from "./SceneEnv";
import { useRuntimeSim, type InstanceStatus } from "./runtimeSim";

const N = 8; // 8x8 sections

/** 视觉映射：InstanceStatus → 颜色 / 高度 / 发光强度 */
const VISUAL: Record<
  string,
  { color: string; height: number; emissive: number }
> = {
  valid: { color: "#a3e635", height: 0.18, emissive: 0.12 },
  invalid: { color: "#fb7185", height: 0.18, emissive: 0.2 },
  incomplete: { color: "#fbbf24", height: 0.4, emissive: 0.35 },
  stale: { color: "#7c5cff", height: 0.6, emissive: 0.45 },
};

const FLAT_LOADED = { color: "#1a1a3a", height: 0.06, emissive: 0.05 };
const FLAT_UNLOADED = { color: "#0a0a12", height: 0.04, emissive: 0.02 };

function Section({
  x,
  z,
  status,
  hasInstance,
  loaded,
  isCursor,
}: {
  x: number;
  z: number;
  status: InstanceStatus;
  hasInstance: boolean;
  loaded: boolean;
  isCursor: boolean;
}) {
  const ref = useRef<THREE.Mesh>(null);
  const v = hasInstance
    ? VISUAL[status]
    : loaded
    ? FLAT_LOADED
    : FLAT_UNLOADED;

  return (
    <group position={[x * 1.1 - (N - 1) * 0.55, 0, z * 1.1 - (N - 1) * 0.55]}>
      <mesh ref={ref} position={[0, v.height / 2, 0]} castShadow receiveShadow>
        <boxGeometry args={[1, v.height, 1]} />
        <meshStandardMaterial
          color={v.color}
          emissive={v.color}
          emissiveIntensity={v.emissive}
          metalness={0.2}
          roughness={0.6}
        />
      </mesh>
      {/* outline: cursor 高亮 */}
      <mesh position={[0, 0.001, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[1.02, 1.02]} />
        <meshBasicMaterial
          color={isCursor ? "#22d3ee" : status === "stale" ? "#7c5cff" : "#222250"}
          wireframe
          transparent
          opacity={isCursor || status === "stale" ? 1 : 0.4}
        />
      </mesh>
    </group>
  );
}

function Cursor({ x, z }: { x: number; z: number }) {
  const ref = useRef<THREE.Group>(null);
  return (
    <group ref={ref} position={[x * 1.1 - (N - 1) * 0.55, 0.7, z * 1.1 - (N - 1) * 0.55]}>
      <mesh>
        <coneGeometry args={[0.35, 0.5, 4]} />
        <meshBasicMaterial color="#22d3ee" wireframe />
      </mesh>
    </group>
  );
}

function SceneImpl() {
  const sections = useRuntimeSim((s) => s.sections);
  const cursor = useRuntimeSim((s) => s.cursor);
  const tick = useRuntimeSim((s) => s.tick);

  useEffect(() => {
    const id = setInterval(() => useRuntimeSim.getState().step(), 320);
    return () => clearInterval(id);
  }, []);

  return (
    <>
      <SceneEnv
        cameraPosition={[0, 11, 6]}
        cameraTarget={[0, 0, 0]}
        enablePan={false}
      />
      {Array.from({ length: N }, (_, x) =>
        Array.from({ length: N }, (_, z) => {
          const idx = x * N + z;
          const sec = sections[idx];
          const isCursor = cursor?.x === x && cursor?.z === z && tick > 0;
          return (
            <Section
              key={`${x}-${z}`}
              x={x}
              z={z}
              status={sec.status}
              hasInstance={sec.hasInstance}
              loaded={sec.loaded}
              isCursor={isCursor}
            />
          );
        })
      )}
      {tick > 0 && cursor && <Cursor x={cursor.x} z={cursor.z} />}
      <Html position={[0, 0, 0]} center>
        <div className="pointer-events-none" />
      </Html>
    </>
  );
}

export function RuntimeViewport() {
  return (
    <Canvas shadows dpr={[1, 2]} camera={{ position: [0, 11, 6], fov: 42 }}>
      <Suspense fallback={null}>
        <SceneImpl />
      </Suspense>
    </Canvas>
  );
}

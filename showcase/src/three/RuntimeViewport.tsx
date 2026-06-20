import { Canvas } from "@react-three/fiber";
import { Html } from "@react-three/drei";
import { Suspense, useEffect, useMemo, useRef, useState } from "react";
import * as THREE from "three";
import { SceneEnv } from "./SceneEnv";
import { useRuntimeSim, type SectionState } from "./runtimeSim";

const N = 8; // 8x8 sections

function Section({
  x,
  z,
  state,
  checked,
}: {
  x: number;
  z: number;
  state: SectionState;
  checked: boolean;
}) {
  const ref = useRef<THREE.Mesh>(null);
  const colorMap = {
    idle: "#0a0a18",
    loaded: "#1a1a3a",
    dirty: "#fb923c",
    queued: "#fbbf24",
    checking: "#22d3ee",
    valid: "#a3e635",
    invalid: "#fb7185",
    requested: "#7c5cff",
  };
  const heightMap = {
    idle: 0.06,
    loaded: 0.06,
    dirty: 0.45,
    queued: 0.32,
    checking: 0.6,
    valid: 0.18,
    invalid: 0.18,
    requested: 0.24,
  };
  const color = colorMap[state];
  const h = heightMap[state];

  return (
    <group position={[x * 1.1 - (N - 1) * 0.55, 0, z * 1.1 - (N - 1) * 0.55]}>
      <mesh ref={ref} position={[0, h / 2, 0]} castShadow receiveShadow>
        <boxGeometry args={[1, h, 1]} />
        <meshStandardMaterial
          color={color}
          emissive={color}
          emissiveIntensity={
            state === "checking"
              ? 0.6
              : state === "dirty" || state === "queued"
              ? 0.35
              : 0.1
          }
          metalness={0.2}
          roughness={0.6}
        />
      </mesh>
      {/* outline */}
      <mesh position={[0, 0.001, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[1.02, 1.02]} />
        <meshBasicMaterial
          color={
            checked
              ? "#a3e635"
              : state === "checking"
              ? "#22d3ee"
              : state === "dirty"
              ? "#fb923c"
              : "#222250"
          }
          wireframe
          transparent
          opacity={checked || state === "checking" || state === "dirty" ? 1 : 0.5}
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
      {/* sections */}
      {Array.from({ length: N }, (_, x) =>
        Array.from({ length: N }, (_, z) => {
          const idx = x * N + z;
          const sec = sections[idx];
          const checkedThisTick =
            cursor.x === x && cursor.z === z && tick > 0;
          return (
            <Section
              key={`${x}-${z}`}
              x={x}
              z={z}
              state={sec.state}
              checked={checkedThisTick}
            />
          );
        })
      )}
      {/* cursor */}
      {tick > 0 && <Cursor x={cursor.x} z={cursor.z} />}
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

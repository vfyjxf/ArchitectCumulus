import { Canvas } from "@react-three/fiber";
import { Float, RoundedBox } from "@react-three/drei";
import { Suspense } from "react";
import { SceneEnv } from "./SceneEnv";

function Block({
  position,
  color,
  delay,
}: {
  position: [number, number, number];
  color: string;
  delay: number;
}) {
  return (
    <Float
      speed={2}
      rotationIntensity={0.3}
      floatIntensity={0.4}
      floatingRange={[-0.05, 0.05]}
    >
      <RoundedBox
        args={[0.92, 0.92, 0.92]}
        radius={0.08}
        smoothness={4}
        position={position}
        castShadow
        receiveShadow
      >
        <meshStandardMaterial
          color={color}
          metalness={0.4}
          roughness={0.35}
          emissive={color}
          emissiveIntensity={0.18}
        />
      </RoundedBox>
    </Float>
  );
}

export function HeroCube() {
  const colors = ["#7c5cff", "#22d3ee", "#f0abfc", "#a3e635"];
  const cells: { p: [number, number, number]; c: string }[] = [];
  let i = 0;
  for (const x of [-1, 0, 1]) {
    for (const y of [-1, 0, 1]) {
      for (const z of [-1, 0, 1]) {
        if ((x + y + z) % 2 === 0 || Math.abs(x) + Math.abs(y) + Math.abs(z) > 1) {
          cells.push({
            p: [x, y, z],
            c: colors[i % colors.length],
          });
          i++;
        }
      }
    }
  }

  return (
    <Canvas shadows dpr={[1, 2]} camera={{ position: [4, 4, 6], fov: 42 }}>
      <Suspense fallback={null}>
        <SceneEnv cameraPosition={[4, 4, 6]} autoRotate />
        <group>
          {cells.map((cell, idx) => (
            <Block key={idx} position={cell.p} color={cell.c} delay={idx * 0.1} />
          ))}
        </group>
        <gridHelper args={[14, 14, "#1c1c3b", "#0e0e22"]} position={[0, -2, 0]} />
      </Suspense>
    </Canvas>
  );
}

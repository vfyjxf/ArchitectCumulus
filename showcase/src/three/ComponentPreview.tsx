import { Canvas } from "@react-three/fiber";
import { Edges, RoundedBox } from "@react-three/drei";
import { Suspense, useMemo } from "react";

interface Demo {
  from: [number, number, number];
  to: [number, number, number];
  hollow?: boolean;
}

function cellsFor(d: Demo): [number, number, number][] {
  const cells: [number, number, number][] = [];
  for (let x = d.from[0]; x <= d.to[0]; x++) {
    for (let y = d.from[1]; y <= d.to[1]; y++) {
      for (let z = d.from[2]; z <= d.to[2]; z++) {
        if (d.hollow) {
          const onSurface =
            x === d.from[0] || x === d.to[0] ||
            y === d.from[1] || y === d.to[1] ||
            z === d.from[2] || z === d.to[2];
          if (!onSurface) continue;
        }
        cells.push([x, y, z]);
      }
    }
  }
  return cells;
}

export function ComponentPreview({
  demo,
  color,
}: {
  demo: Demo;
  color: string;
}) {
  const cells = useMemo(() => cellsFor(demo), [demo]);
  // compute camera framing
  const fw = demo.to[0] - demo.from[0] + 1;
  const fh = demo.to[1] - demo.from[1] + 1;
  const fd = demo.to[2] - demo.from[2] + 1;
  const maxSize = Math.max(fw, fh, fd);
  const dist = Math.max(4, maxSize * 1.8);

  return (
    <Canvas
      shadows
      dpr={[1, 2]}
      camera={{ position: [dist, dist * 0.8, dist], fov: 38 }}
    >
      <Suspense fallback={null}>
        <ambientLight intensity={0.5} />
        <directionalLight position={[6, 8, 4]} intensity={1} />
        <directionalLight position={[-4, 4, -3]} intensity={0.3} color={color} />

        {/* if region or substructure, show region outline */}
        {cells.length === 1 && (
          <mesh position={[cells[0][0], cells[0][1], cells[0][2]]}>
            <boxGeometry args={[1.05, 1.05, 1.05]} />
            <meshBasicMaterial color={color} wireframe transparent opacity={0.4} />
          </mesh>
        )}

        {cells.map((p, i) => (
          <RoundedBox
            key={i}
            args={[0.9, 0.9, 0.9]}
            radius={0.06}
            smoothness={3}
            position={p}
            castShadow
            receiveShadow
          >
            <meshStandardMaterial
              color={color}
              metalness={0.35}
              roughness={0.45}
              emissive={color}
              emissiveIntensity={0.12}
            />
            <Edges threshold={15} color="#ffffff" opacity={0.18} transparent />
          </RoundedBox>
        ))}

        {/* for substructure, show region outline */}
        {cells.length === 0 && (
          <mesh>
            <boxGeometry args={[1.1, 1.1, 1.1]} />
            <meshBasicMaterial color={color} wireframe transparent opacity={0.6} />
          </mesh>
        )}

        {/* for region: outline the rectangle */}
        {(cells.length > 1 && demo.from[1] === demo.to[1] && demo.from[2] === demo.to[2] && demo.to[0] - demo.from[0] >= 1) && (
          <mesh
            position={[
              (demo.from[0] + demo.to[0]) / 2,
              demo.from[1],
              demo.from[2],
            ]}
          >
            <boxGeometry
              args={[
                demo.to[0] - demo.from[0] + 1.15,
                1.15,
                1.15,
              ]}
            />
            <meshBasicMaterial color={color} wireframe transparent opacity={0.4} />
          </mesh>
        )}
      </Suspense>
    </Canvas>
  );
}

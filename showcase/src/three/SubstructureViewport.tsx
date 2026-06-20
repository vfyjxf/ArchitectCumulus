import { Canvas } from "@react-three/fiber";
import { Edges, RoundedBox } from "@react-three/drei";
import { Suspense } from "react";
import * as THREE from "three";
import { SceneEnv } from "./SceneEnv";

interface ChildBox {
  pos: [number, number, number];
  size: [number, number, number];
  color: string;
  label: string;
  kind: "root" | "child" | "grandchild" | "leaf";
}

const BOXES: ChildBox[] = [
  {
    pos: [0, 1.4, 0],
    size: [6, 3, 4],
    color: "#7c5cff",
    label: "factory",
    kind: "root",
  },
  {
    pos: [0, 0.4, 0],
    size: [4, 0.6, 2],
    color: "#a3e635",
    label: "base",
    kind: "child",
  },
  {
    pos: [-2.2, 1.4, 0],
    size: [1.4, 1.4, 1.4],
    color: "#22d3ee",
    label: "left_arm",
    kind: "child",
  },
  {
    pos: [2.2, 1.4, 0],
    size: [1.4, 1.4, 1.4],
    color: "#22d3ee",
    label: "right_arm",
    kind: "child",
  },
  {
    pos: [-2.2, 2.4, 0],
    size: [0.6, 0.6, 0.6],
    color: "#f0abfc",
    label: "tool_a",
    kind: "grandchild",
  },
  {
    pos: [2.2, 2.4, 0],
    size: [0.6, 0.6, 0.6],
    color: "#f0abfc",
    label: "tool_b",
    kind: "grandchild",
  },
];

function Box({ box }: { box: ChildBox }) {
  return (
    <group position={box.pos}>
      {box.kind === "root" ? (
        <mesh>
          <boxGeometry args={box.size} />
          <meshBasicMaterial
            color={box.color}
            wireframe
            transparent
            opacity={0.4}
          />
        </mesh>
      ) : (
        <RoundedBox
          args={box.size}
          radius={0.06}
          smoothness={4}
          castShadow
          receiveShadow
        >
          <meshStandardMaterial
            color={box.color}
            metalness={0.4}
            roughness={0.4}
            emissive={box.color}
            emissiveIntensity={0.18}
            transparent
            opacity={0.85}
          />
          <Edges threshold={15} color="#ffffff" opacity={0.2} transparent />
        </RoundedBox>
      )}
    </group>
  );
}

function Connector({
  from,
  to,
}: {
  from: [number, number, number];
  to: [number, number, number];
}) {
  const geom = new THREE.BufferGeometry().setFromPoints([
    new THREE.Vector3(...from),
    new THREE.Vector3(...to),
  ]);
  return (
    // @ts-expect-error three fiber line element
    <line geometry={geom}>
      <lineBasicMaterial color="#7c5cff" transparent opacity={0.5} />
    </line>
  );
}

function SceneImpl() {
  return (
    <>
      <SceneEnv
        cameraPosition={[7, 5, 8]}
        cameraTarget={[0, 1, 0]}
        autoRotate
      />
      <gridHelper args={[18, 18, "#1c1c3b", "#0e0e22"]} position={[0, -0.01, 0]} />
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, -0.02, 0]}
        receiveShadow
      >
        <planeGeometry args={[18, 18]} />
        <meshStandardMaterial color="#080816" metalness={0.2} roughness={0.85} />
      </mesh>
      {BOXES.map((b, i) => (
        <Box key={i} box={b} />
      ))}
      <Connector from={[0, 1.4, 0]} to={[-2.2, 1.4, 0]} />
      <Connector from={[0, 1.4, 0]} to={[2.2, 1.4, 0]} />
      <Connector from={[-2.2, 1.4, 0]} to={[-2.2, 2.4, 0]} />
      <Connector from={[2.2, 1.4, 0]} to={[2.2, 2.4, 0]} />
      <Connector from={[0, 1.4, 0]} to={[0, 0.4, 0]} />
    </>
  );
}

export function SubstructureViewport() {
  return (
    <Canvas shadows dpr={[1, 2]} camera={{ position: [7, 5, 8], fov: 42 }}>
      <Suspense fallback={null}>
        <SceneImpl />
      </Suspense>
    </Canvas>
  );
}

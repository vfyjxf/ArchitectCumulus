import { OrbitControls } from "@react-three/drei";
import type { ReactNode } from "react";

interface SceneEnvProps {
  children?: ReactNode;
  cameraPosition?: [number, number, number];
  cameraTarget?: [number, number, number];
  enablePan?: boolean;
  autoRotate?: boolean;
}

export function SceneEnv({
  children,
  cameraPosition = [8, 8, 12],
  cameraTarget = [0, 0, 0],
  enablePan = true,
  autoRotate = false,
}: SceneEnvProps) {
  return (
    <>
      <ambientLight intensity={0.4} />
      <hemisphereLight args={["#a0a0ff", "#202040", 0.35]} />
      <directionalLight
        position={[10, 14, 8]}
        intensity={1.2}
        castShadow
        shadow-mapSize={[2048, 2048]}
      >
        <orthographicCamera
          attach="shadow-camera"
          args={[-12, 12, 12, -12, 0.1, 50]}
        />
      </directionalLight>
      <directionalLight position={[-8, 6, -4]} intensity={0.4} color="#7c5cff" />
      <OrbitControls
        enablePan={enablePan}
        target={cameraTarget}
        autoRotate={autoRotate}
        autoRotateSpeed={0.6}
        maxPolarAngle={Math.PI / 2.05}
        minDistance={4}
        maxDistance={40}
      />
      {children}
    </>
  );
}

export function GridFloor({
  size = 30,
  divisions = 30,
  color = "#222244",
}: {
  size?: number;
  divisions?: number;
  color?: string;
}) {
  return (
    <>
      <gridHelper
        args={[size, divisions, color, "#14142a"]}
        position={[0, -0.01, 0]}
      />
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, -0.02, 0]}
        receiveShadow
      >
        <planeGeometry args={[size, size]} />
        <meshStandardMaterial color="#080816" metalness={0.2} roughness={0.85} />
      </mesh>
    </>
  );
}

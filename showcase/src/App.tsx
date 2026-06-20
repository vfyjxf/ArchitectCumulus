import { TopNav } from "@/components/TopNav";
import { useScrollProgress } from "@/hooks/useScrollProgress";
import { Hero } from "@/sections/Hero";
import { Pipeline } from "@/sections/Pipeline";
import { Rationale } from "@/sections/Rationale";
import { Builder } from "@/sections/Builder";
import { TransformLab } from "@/sections/TransformLab";
import { Composer } from "@/sections/Composer";
import { Targets } from "@/sections/Targets";
import { PredicateComposer } from "@/sections/PredicateComposer";
import { Components } from "@/sections/Components";
import { Runtime } from "@/sections/Runtime";
import { Substructure } from "@/sections/Substructure";
import { LifecycleLab } from "@/sections/LifecycleLab";
import { Scenarios } from "@/sections/Scenarios";
import { CaseStudies } from "@/sections/CaseStudies";
import { DataDriven } from "@/sections/DataDriven";
import { ApiCookbook } from "@/sections/ApiCookbook";
import { Diagnostics } from "@/sections/Diagnostics";
import { Playground } from "@/sections/Playground";
import { Footer } from "@/sections/Footer";

export default function App() {
  const progress = useScrollProgress();
  return (
    <div className="relative min-h-screen">
      <TopNav progress={progress} />
      <main>
        <Hero />
        <Pipeline />
        <Rationale />
        <Builder />
        <TransformLab />
        <Composer />
        <Targets />
        <PredicateComposer />
        <Components />
        <Runtime />
        <Substructure />
        <LifecycleLab />
        <Scenarios />
        <CaseStudies />
        <DataDriven />
        <ApiCookbook />
        <Diagnostics />
        <Playground />
      </main>
      <Footer />
    </div>
  );
}

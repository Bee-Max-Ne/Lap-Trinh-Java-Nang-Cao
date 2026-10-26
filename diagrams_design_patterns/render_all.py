import os
import subprocess
import time
import shutil

html_dir = r"F:\Lap-Trinh-Java-Nang-Cao\diagrams_design_patterns\html_sources"
out_dir = r"F:\Lap-Trinh-Java-Nang-Cao\diagrams_design_patterns"
artifact_dir = r"C:\Users\Maxsys\.gemini\antigravity\brain\417c52ff-562b-4658-9d2d-d6c70b168cb1\diagrams_design_patterns"
os.makedirs(artifact_dir, exist_ok=True)

edge_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

files = [
    "01_facade_pattern",
    "02_builder_pattern",
    "03_observer_pattern",
    "04_strategy_pattern",
    "05_flyweight_pattern",
    "06_object_pool_pattern",
    "07_template_method_pattern",
    "08_chain_of_responsibility_pattern"
]

print("Starting batch rendering for all 8 diagrams...")
for name in files:
    html_file = os.path.join(html_dir, f"{name}.html")
    png_file = os.path.join(out_dir, f"{name}.png")
    art_png = os.path.join(artifact_dir, f"{name}.png")
    
    # We render with 1.5x device scale factor for high crispness while keeping manageable file size (~200KB)
    cmd = [
        edge_path,
        "--headless=new",
        "--force-device-scale-factor=1.5",
        f"--screenshot={png_file}",
        "--window-size=960,610",
        f"file:///{html_file.replace(os.sep, '/')}"
    ]
    
    subprocess.run(cmd, check=True)
    shutil.copyfile(png_file, art_png)
    print(f"Rendered: {name}.png")

print("All 8 diagrams successfully rendered and copied to artifacts!")

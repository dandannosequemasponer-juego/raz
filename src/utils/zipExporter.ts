import JSZip from 'jszip';
import { ALL_PLUGIN_FILES } from '../data/allSourceFiles';

export async function exportPluginZip(onProgress?: (percent: number) => void): Promise<Blob> {
  const zip = new JSZip();

  // Root files
  const rootFolder = zip.folder("CytricosRaces");
  if (!rootFolder) throw new Error("Could not create zip folder");

  for (const file of ALL_PLUGIN_FILES) {
    rootFolder.file(file.path, file.content);
  }

  // Also include a comprehensive README.md for GitHub
  const readmeContent = `# CytricosRaces (v7.0.0-RELEASE)
**Sistema de Razas Dinámico, Experiencia RPG, Cosméticos Independientes y Alas 3D para CYTRICOS RPG**

Desarrolladores Líderes: **Dandan_13** & **Wolfit12**  
Servidor: **CYTRICOS RPG** (\`play.cytricos.com\`)  
Compatibilidad: **Paper 26.2**  
Java: **Java 25**

---

## ⚡ Características Principales

1. **16 Razas con Escalado Físico Real en Vivo**:
   - Ajuste dinámico de escala y dimensiones de hitbox mediante \`Attribute.GENERIC_SCALE\`.
   - Enano (\`0.75\`), Duende (\`0.60\`), Hada (\`0.50\`), Gigante (\`1.35\`), Orco (\`1.20\`), Demonio/Dragonkin (\`1.10\`), Humano/Vampiro/Lobo (\`1.00\`).

2. **Experiencia RPG Completa (GUIs en Español)**:
   - Hub Principal (\`/raza\`), Perfil RPG, Libro de Habilidades con recargas, Árbol de Talentos (3 ramas), Misiones Raciales, Logros y Títulos, Enciclopedia y Ajustes Personales.

3. **Sistema Gratuito de Cosméticos, 8 Alas 3D y Objetos PDC**:
   - **100% Independiente**: NO requiere ItemsAdder, Oraxen ni mods de cliente (Forge/Fabric).
   - **8 Alas 3D**: Ángel, Demonio, Vampiro, Dragón, Hada, Fénix, Oscuras y Celestiales (renderizado por \`ItemDisplay\` + \`ELYTRA\` custom).
   - **Resource Pack propio** con modelos 3D Blockbench, texturas RGBA reales y formato dual (\`models/item\` + \`items/*.json\`).

---

## 🚀 Compilación con Maven

\`\`\`bash
cd CytricosRaces
mvn -DskipTests clean package
\`\`\`

El archivo compilado se generará en:  
\`target/CytricosRaces-7.0.0-RELEASE.jar\`

Copia el archivo \`.jar\` en la carpeta \`/plugins/\` de tu servidor Paper 26.2 (Java 25).
`;

  rootFolder.file("README.md", readmeContent);

  return await zip.generateAsync({
    type: "blob",
    compression: "DEFLATE",
    compressionOptions: { level: 9 }
  }, (metadata) => {
    if (onProgress) onProgress(metadata.percent);
  });
}

import fs from 'fs';
import path from 'path';

const rootDir = process.cwd();

function walkDir(dir, fileList = []) {
  if (!fs.existsSync(dir)) return fileList;
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    if (stat.isDirectory()) {
      walkDir(filePath, fileList);
    } else {
      fileList.push(filePath);
    }
  }
  return fileList;
}

const targetFiles = [
  path.join(rootDir, 'pom.xml'),
  ...walkDir(path.join(rootDir, 'src', 'main'))
];

function getCategory(relPath) {
  if (relPath.endsWith('pom.xml')) return 'maven';
  if (relPath.includes('resources')) return 'config';
  return 'source';
}

function getLanguage(relPath) {
  if (relPath.endsWith('.java')) return 'java';
  if (relPath.endsWith('.yml') || relPath.endsWith('.yaml')) return 'yaml';
  if (relPath.endsWith('.xml')) return 'xml';
  if (relPath.endsWith('.json') || relPath.endsWith('.mcmeta')) return 'json';
  return 'text';
}

function getDescription(fileName, relPath) {
  const map = {
    'pom.xml': 'Descriptor Maven con compatibilidad Java 25 y Paper 26.2',
    'CytricosRaces.java': 'Clase principal del plugin (JavaPlugin), arranque y ciclo de vida',
    'Main.java': 'Puente de compatibilidad para cargadores legados',
    'CommandManager.java': 'Gestor de comandos 100% en español con resolución multilingüe, RPG y diagnóstico',
    'ConfigManager.java': 'Gestor de configuración y persistencia de valores en config.yml',
    'CustomItemManager.java': 'Gestor PDC independiente de objetos custom, armas, reliquias y tokens',
    'CosmeticsManager.java': 'Gestor ligero de auras de partículas orbitales',
    'WingsManager.java': 'Gestor independiente de 16 alas 3D exclusivas por raza con anti-duplicación PDC',
    'HeadCosmeticManager.java': 'Gestor de cosméticos flotantes de cabeza desacoplados del casco',
    'ResourcePackManager.java': 'Gestor asíncrono de verificación SHA-1 y entrega automática del Resource Pack',
    'RelicManager.java': 'Gestor de reliquias raciales equipables en mano secundaria y drops de mobs',
    'TransformationManager.java': 'Gestor de metamorfosis de 6 segundos (Shift + Clic Derecho) y techo max-flight-y',
    'QuestManager.java': 'Gestor de misiones raciales temáticas y recompensas',
    'PlayerDataManager.java': 'Gestor de caché y ciclo de vida de datos de jugadores',
    'RaceManager.java': 'Carga, lógica, talentos, progresión y escalado físico de las 16 razas',
    'StorageManager.java': 'Persistencia dual UTF-8 en YAML, SQLite y MySQL HikariCP',
    'RelicTask.java': 'Tarea periódica de pasivas activas por reliquias en mano secundaria',
    'AdminGUI.java': 'Interfaz gráfica de administración interactiva para costos, reglas y slots',
    'MainMenuGUI.java': 'Hub Principal RPG profesional con acceso a todos los módulos',
    'ProfileGUI.java': 'Ficha de perfil RPG con barra de recurso, atributos y estadísticas',
    'AbilityBookGUI.java': 'Grimorio interactivo de habilidades pasivas, activas y recargas',
    'SkillTreeGUI.java': 'Árbol de talentos raciales en 3 ramas (Combate, Supervivencia, Utilidad)',
    'QuestsGUI.java': 'Interfaz de 54 casillas para misiones raciales con divisor de cristal en columna 4',
    'AchievementsGUI.java': 'Menú de logros y títulos cosméticos equipables',
    'CosmeticsGUI.java': 'Menú de auras, cosméticos de cabeza, 16 alas 3D exclusivas y vista en 1ra persona',
    'CustomItemsGUI.java': 'Catálogo interactivo de armas, reliquias, tokens, coronas y alas 3D',
    'SettingsGUI.java': 'Configuración personal persistente (partículas, sonidos, actionbar, alas)',
    'EncyclopediaGUI.java': 'Códice interactivo en libro escrito (WRITTEN_BOOK) para las 16 razas',
    'PlayerGUI.java': 'Menú interactivo de selección de razas de 45 casillas',
    'RaceChangeConfirmGUI.java': 'Menú de confirmación de cambio de raza con advertencia de reinicio total',
    'ChatListener.java': 'Formateador de chat con prefijo de raza sin alterar Tablist',
    'CustomItemListener.java': 'Acciones de reliquias, fragmentos de talento, protección de yunque y limpieza',
    'CustomMobDropListener.java': 'Drops configurables de reliquias y fragmentos en jefes y mobs desde relics.yml',
    'EnvironmentListener.java': 'Mecánicas ambientales: vulnerabilidad solar gradual Nv 1-6, agua e inmunidades',
    'GUIListener.java': 'Manejador central de clics, mochila persistente y seguridad para todas las GUIs RPG',
    'RaceAbilitiesListener.java': 'Habilidades activas/pasivas, hitos de kills, transformación y recursos raciales',
    'ProtocolLibHook.java': 'Ocultación selectiva de entidades cosméticas en primera persona mediante paquetes',
    'CytricosRacesPAPI.java': 'Expansión PlaceholderAPI completa en español con variables raciales y RPG',
    'PlaceholderAPIHook.java': 'Enlace de registro a PlaceholderAPI',
    'LibsDisguisesHook.java': 'Manejador de polimorfia y disfraces visuales',
    'WorldGuardHook.java': 'Protección de zonas seguras y banderas de WorldGuard',
    'ChangeMethod.java': 'Enumeración de métodos de pago de cambio de raza',
    'PlayerData.java': 'Modelo completo de datos RPG del jugador (talentos, misiones, alas, mochila, ajustes)',
    'Race.java': 'Modelo de raza con atributos, escalado físico y niveles',
    'RaceLevel.java': 'Modelo de nivel racial con recompensas y permisos',
    'ColorUtils.java': 'Utilidades de color Adventure API, hex y títulos',
    'plugin.yml': 'Descriptor de plugin Paper 26.2 con comandos y permisos',
    'config.yml': 'Configuración de economía, vuelos, cosméticos, Resource Pack y RPG en español',
    'races.yml': 'Definición completa en español de las 16 razas, escalas físicas y 6 niveles',
    'relics.yml': 'Configuración de reliquias raciales y probabilidades de drop por mob'
  };
  return map[fileName] || `Archivo ${fileName} de CytricosRaces`;
}

const fileEntries = [];

for (const fullPath of targetFiles) {
  const relPath = path.relative(rootDir, fullPath).replace(/\\/g, '/');
  const fileName = path.basename(fullPath);
  const content = fs.readFileSync(fullPath, 'utf-8');

  fileEntries.push({
    name: fileName,
    path: relPath,
    category: getCategory(relPath),
    language: getLanguage(relPath),
    description: getDescription(fileName, relPath),
    content: content
  });
}

// Ordenar: pom.xml primero, configs después, luego Java alfabético
fileEntries.sort((a, b) => {
  const order = { maven: 1, config: 2, source: 3, ci: 4 };
  const catDiff = (order[a.category] || 9) - (order[b.category] || 9);
  if (catDiff !== 0) return catDiff;
  return a.name.localeCompare(b.name);
});

const outputTs = `// Archivo autogenerado con el codigo fuente completo del plugin CytricosRaces v7.0.0
export interface SourceFile {
  name: string;
  path: string;
  category: "source" | "config" | "maven" | "ci";
  language: string;
  description: string;
  content: string;
}

export const ALL_PLUGIN_FILES: SourceFile[] = ${JSON.stringify(fileEntries, null, 2)};
`;

fs.writeFileSync(path.join(rootDir, 'src', 'data', 'allSourceFiles.ts'), outputTs, 'utf-8');
console.log(`Successfully synced ${fileEntries.length} plugin files to src/data/allSourceFiles.ts`);

import React, { useState } from 'react';
import { ALL_PLUGIN_FILES, SourceFile } from '../data/allSourceFiles';
import { exportPluginZip } from '../utils/zipExporter';
import { Code, Download, Copy, Check, FileText, FolderGit2, Search } from 'lucide-react';

export const CodeExplorer: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<SourceFile>(ALL_PLUGIN_FILES[0]);
  const [copied, setCopied] = useState(false);
  const [downloadingZip, setDownloadingZip] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeCategory, setActiveCategory] = useState<'all' | 'source' | 'config' | 'maven' | 'ci'>('all');

  const filteredFiles = ALL_PLUGIN_FILES.filter((f: SourceFile) => {
    const matchesSearch = f.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          f.path.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCat = activeCategory === 'all' || f.category === activeCategory;
    return matchesSearch && matchesCat;
  });

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadSingle = () => {
    const blob = new Blob([selectedFile.content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = selectedFile.name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleDownloadZip = async () => {
    try {
      setDownloadingZip(true);
      const zipBlob = await exportPluginZip();
      const url = URL.createObjectURL(zipBlob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `CytricosRaces-v7.0.0-SourceCode.zip`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
    } finally {
      setDownloadingZip(false);
    }
  };

  return (
    <div className="w-full max-w-6xl bg-slate-900 border border-slate-800 rounded-xl shadow-2xl overflow-hidden flex flex-col h-[650px]">
      {/* Explorer Top Toolbar */}
      <div className="bg-slate-950 p-4 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <FolderGit2 className="w-5 h-5 text-emerald-400" />
          <div>
            <h3 className="font-mc text-sm font-bold text-white">
              Explorador de Código Fuente · CytricosRaces (Java 25 · Paper 26.2)
            </h3>
            <p className="text-[11px] text-slate-400">
              {ALL_PLUGIN_FILES.length} archivos de producción listos para compilar con Maven y Resource Pack gratuito incluido
            </p>
          </div>
        </div>

        {/* Download Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <a
            href="/downloads/CytricosRaces-ResourcePack.zip"
            download="CytricosRaces-ResourcePack.zip"
            className="px-3.5 py-2 bg-purple-600 hover:bg-purple-500 text-white font-mc font-bold text-xs rounded-lg shadow-lg flex items-center gap-2 transition-all active:scale-95"
          >
            <Download className="w-4 h-4" />
            <span>Resource Pack 3D (.ZIP)</span>
          </a>

          <button
            onClick={handleDownloadZip}
            disabled={downloadingZip}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-700 text-white font-mc font-bold text-xs rounded-lg shadow-lg flex items-center gap-2 transition-all active:scale-95"
          >
            <Download className="w-4 h-4" />
            <span>{downloadingZip ? 'Empaquetando ZIP...' : 'Descargar Repositorio Completo (.ZIP)'}</span>
          </button>
        </div>
      </div>

      {/* Explorer Body: File List + Code Viewer */}
      <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
        {/* Left Sidebar: File Tree */}
        <div className="w-full md:w-80 bg-slate-950/70 border-r border-slate-800 flex flex-col">
          {/* Search & Filter */}
          <div className="p-3 border-b border-slate-800/80 space-y-2">
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                placeholder="Buscar archivo..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-md pl-8 pr-2.5 py-1 text-xs text-slate-200 placeholder:text-slate-600 focus:outline-none focus:border-cyan-500"
              />
            </div>

            {/* Category tabs */}
            <div className="flex items-center gap-1 overflow-x-auto text-[11px] font-mc pb-0.5">
              {(['all', 'source', 'config', 'maven', 'ci'] as const).map(cat => (
                <button
                  key={cat}
                  onClick={() => setActiveCategory(cat)}
                  className={`px-2 py-0.5 rounded capitalize transition-colors ${
                    activeCategory === cat
                      ? 'bg-emerald-950 text-emerald-300 border border-emerald-700 font-bold'
                      : 'text-slate-400 hover:text-white'
                  }`}
                >
                  {cat === 'all' ? 'Todos' : cat}
                </button>
              ))}
            </div>
          </div>

          {/* Files List */}
          <div className="flex-1 overflow-y-auto p-2 space-y-1">
            {filteredFiles.map((file: SourceFile) => {
              const isSelected = selectedFile.path === file.path;
              return (
                <button
                  key={file.path}
                  onClick={() => setSelectedFile(file)}
                  className={`w-full text-left p-2 rounded-lg font-mc text-xs flex items-center justify-between transition-colors ${
                    isSelected
                      ? 'bg-slate-800 text-cyan-300 font-semibold border border-slate-700'
                      : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
                  }`}
                >
                  <div className="flex items-center gap-2 truncate">
                    <FileText className={`w-3.5 h-3.5 shrink-0 ${
                      file.category === 'source' ? 'text-amber-400' :
                      file.category === 'config' ? 'text-emerald-400' :
                      file.category === 'maven' ? 'text-blue-400' : 'text-purple-400'
                    }`} />
                    <span className="truncate">{file.name}</span>
                  </div>
                  <span className="text-[9px] uppercase px-1 rounded bg-black/40 text-slate-500 shrink-0">
                    {file.language}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Right Pane: Code Viewer */}
        <div className="flex-1 flex flex-col bg-[#0a0c10] overflow-hidden">
          {/* File Header Details */}
          <div className="bg-slate-950/80 px-4 py-2.5 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2 truncate">
              <span className="font-mc text-xs text-cyan-400 font-bold">
                {selectedFile.path}
              </span>
              <span className="text-[11px] text-slate-500 hidden sm:inline">
                ({selectedFile.description})
              </span>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <button
                onClick={handleCopy}
                className="flex items-center gap-1 px-2.5 py-1 text-xs font-mc bg-slate-800 hover:bg-slate-700 text-slate-300 rounded border border-slate-700 transition-colors"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? '¡Copiado!' : 'Copiar'}</span>
              </button>

              <button
                onClick={handleDownloadSingle}
                className="flex items-center gap-1 px-2.5 py-1 text-xs font-mc bg-slate-800 hover:bg-slate-700 text-slate-300 rounded border border-slate-700 transition-colors"
                title="Descargar este archivo"
              >
                <Download className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Descargar</span>
              </button>
            </div>
          </div>

          {/* Code Viewer Container */}
          <div className="flex-1 overflow-y-auto p-4 font-mc text-xs text-slate-300 leading-relaxed select-text">
            <pre className="overflow-x-auto whitespace-pre font-mono">
              <code>{selectedFile.content}</code>
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};

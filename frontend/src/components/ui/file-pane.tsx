import React, { useState } from "react";
import { Folder, ArrowUp, FolderPlus, ChevronRight, File as FileIcon, Upload, MoveRight, Copy, Trash2, CheckSquare, Square } from "lucide-react";
import { useTranslation } from "../../lib/i18n";

export interface DirectoryItem {
  name: string;
  fullPath: string;
  type?: string;
  size?: number;
}

export interface BrowseResult {
  currentPath: string;
  parentPath: string | null;
  directories: DirectoryItem[];
}

interface FilePaneProps {
  path: string;
  onNavigate: (path: string) => void;
  browseData: BrowseResult | null;
  loading: boolean;
  selectedItems: Set<string>;
  onToggleSelect: (path: string) => void;
  onSelectAll: () => void;
  onCreateDir: (name: string) => void;
  onUploadFile: (file: File) => void;
  onDrop: (e: React.DragEvent) => void;
  otherPanePath: string;
  onAction: (action: 'copy' | 'move' | 'delete') => void;
  title: string;
}

function formatSize(bytes: number) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

export function FilePane({ onNavigate, browseData, loading, selectedItems, onToggleSelect, onSelectAll, onCreateDir, onUploadFile, onDrop, onAction }: FilePaneProps) {
  const { t } = useTranslation();
  const [isCreatingDir, setIsCreatingDir] = useState(false);
  const [newDirName, setNewDirName] = useState("");
  const [isDragOver, setIsDragOver] = useState(false);

  const handleCreateDir = (e: React.FormEvent) => {
    e.preventDefault();
    if (newDirName) {
      onCreateDir(newDirName);
      setNewDirName("");
      setIsCreatingDir(false);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };
  const handleDragLeave = () => setIsDragOver(false);
  const handleDropLocal = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    onDrop(e);
  };

  return (
    <div 
      className={`flex-1 flex flex-col min-w-0 bg-surface rounded-lg border shadow-sm-subtle overflow-hidden transition-colors ${isDragOver ? 'border-brand ring-2 ring-brand/20' : 'border-border'}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDropLocal}
    >
      <div className="px-3 py-2 border-b border-border bg-surface-hover/50 flex flex-wrap items-center justify-between gap-2 shrink-0">
        <div className="flex items-center gap-1.5 text-[12px] font-mono overflow-hidden">
          <button onClick={() => onNavigate("/")} className="text-foreground hover:text-brand px-1 py-0.5 rounded shrink-0">{t("common.root")}</button>
          {browseData?.currentPath.split("/").filter(Boolean).map((part, index, array) => {
            const p = "/" + array.slice(0, index + 1).join("/");
            return (
              <div key={p} className="flex items-center gap-1 shrink-0 min-w-0">
                <ChevronRight className="w-3 h-3 text-status-disabled shrink-0" />
                <button onClick={() => onNavigate(p)} className="text-foreground hover:text-brand px-1 py-0.5 rounded truncate max-w-[100px]">{part}</button>
              </div>
            );
          })}
        </div>

        <div className="flex items-center gap-2">
          {!isCreatingDir ? (
            <button 
              onClick={() => setIsCreatingDir(true)} 
              className="text-[11px] font-medium text-foreground bg-surface border border-border px-2 py-1 rounded hover:bg-surface-hover transition-colors flex items-center gap-1"
            >
              <FolderPlus className="w-3.5 h-3.5" />
            </button>
          ) : (
            <form onSubmit={handleCreateDir} className="flex gap-1">
              <input 
                autoFocus required pattern="[a-zA-Z0-9_.-]+"
                className="text-[11px] font-mono border border-border bg-surface px-2 py-1 rounded focus:outline-none focus:border-brand w-24"
                placeholder={t("dashboard.dirname")}
                value={newDirName}
                onChange={e => setNewDirName(e.target.value)}
              />
              <button type="submit" className="text-[11px] bg-brand text-brand-text px-2 py-1 rounded">{t("common.create")}</button>
              <button type="button" onClick={() => setIsCreatingDir(false)} className="text-[11px] bg-surface border border-border px-2 py-1 rounded">{t("common.cancel")}</button>
            </form>
          )}
          <label className="text-[11px] font-medium text-foreground bg-surface border border-border px-2 py-1 rounded hover:bg-surface-hover transition-colors flex items-center gap-1 cursor-pointer">
            <Upload className="w-3.5 h-3.5" />
            <input type="file" className="hidden" onChange={e => e.target.files?.[0] && onUploadFile(e.target.files[0])} />
          </label>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto custom-scrollbar bg-background">
        {loading ? (
          <div className="flex justify-center items-center h-32">
            <div className="w-5 h-5 border-2 border-border border-t-foreground rounded-full animate-spin"></div>
          </div>
        ) : (
          <table className="w-full text-left text-[12px] font-mono">
            <thead className="sticky top-0 bg-surface-hover border-b border-border z-10 shadow-sm-subtle">
              <tr>
                <th className="px-3 py-2 w-8">
                  <button onClick={onSelectAll}>
                    {browseData?.directories.length && selectedItems.size === browseData.directories.length ? <CheckSquare className="w-3.5 h-3.5" /> : <Square className="w-3.5 h-3.5 text-status-disabled" />}
                  </button>
                </th>
                <th className="px-2 py-2 font-semibold">Name</th>
                <th className="px-2 py-2 font-semibold text-right w-20">Size</th>
              </tr>
            </thead>
            <tbody>
              {browseData?.parentPath && (
                <tr className="hover:bg-surface-hover transition-colors group cursor-pointer border-b border-border/50" onClick={() => onNavigate(browseData.parentPath!)}>
                  <td className="px-3 py-2"></td>
                  <td className="px-2 py-2 flex items-center gap-2">
                    <ArrowUp className="w-3.5 h-3.5 text-status-disabled" />
                    <span className="font-medium text-foreground">..</span>
                  </td>
                  <td className="px-2 py-2"></td>
                </tr>
              )}
              {browseData?.directories.map(item => {
                const isSelected = selectedItems.has(item.fullPath);
                const isFolder = item.type === 'dir' || item.type === undefined;
                return (
                  <tr 
                    key={item.fullPath} 
                    className={`hover:bg-surface-hover transition-colors group border-b border-border/50 ${isSelected ? 'bg-brand/10' : ''}`}
                    onClick={(e) => {
                      if (e.target instanceof HTMLInputElement || e.target instanceof SVGElement || e.target instanceof HTMLButtonElement) return;
                      isFolder ? onNavigate(item.fullPath) : onToggleSelect(item.fullPath);
                    }}
                  >
                    <td className="px-3 py-2">
                      <button onClick={(e) => { e.stopPropagation(); onToggleSelect(item.fullPath); }}>
                        {isSelected ? <CheckSquare className="w-3.5 h-3.5 text-brand" /> : <Square className="w-3.5 h-3.5 text-status-disabled opacity-0 group-hover:opacity-100" />}
                      </button>
                    </td>
                    <td className="px-2 py-2 flex items-center gap-2 max-w-[200px] truncate" title={item.name}>
                      {isFolder ? <Folder className="w-3.5 h-3.5 text-status-disabled shrink-0" /> : <FileIcon className="w-3.5 h-3.5 text-status-disabled shrink-0" />}
                      <span className="truncate">{item.name}</span>
                    </td>
                    <td className="px-2 py-2 text-right text-status-disabled">
                      {!isFolder && item.size !== undefined ? formatSize(item.size) : ''}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {selectedItems.size > 0 && (
        <div className="px-3 py-2 border-t border-border bg-surface flex items-center justify-between shrink-0">
          <span className="text-[11px] font-medium text-status-disabled">{selectedItems.size} selected</span>
          <div className="flex gap-1.5">
            <button onClick={() => onAction('copy')} className="px-2 py-1 bg-surface border border-border rounded text-[11px] font-medium flex items-center gap-1 hover:bg-surface-hover">
              <Copy className="w-3 h-3" /> Copy
            </button>
            <button onClick={() => onAction('move')} className="px-2 py-1 bg-surface border border-border rounded text-[11px] font-medium flex items-center gap-1 hover:bg-surface-hover">
              <MoveRight className="w-3 h-3" /> Move
            </button>
            <button onClick={() => onAction('delete')} className="px-2 py-1 bg-status-error/10 text-status-error border border-status-error/20 rounded text-[11px] font-medium flex items-center gap-1 hover:bg-status-error/20">
              <Trash2 className="w-3 h-3" /> Delete
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

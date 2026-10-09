import React, { useState, useEffect } from "react";
import { FilePane, BrowseResult } from "./ui/file-pane";
import { api } from "../lib/api";
import { useToast } from "./ui/toast";

export function FileManager() {
  const [leftPath, setLeftPath] = useState("/");
  const [rightPath, setRightPath] = useState("/home");
  
  const [leftData, setLeftData] = useState<BrowseResult | null>(null);
  const [rightData, setRightData] = useState<BrowseResult | null>(null);
  
  const [leftLoading, setLeftLoading] = useState(false);
  const [rightLoading, setRightLoading] = useState(false);

  const [leftSelected, setLeftSelected] = useState<Set<string>>(new Set());
  const [rightSelected, setRightSelected] = useState<Set<string>>(new Set());

  const { error: toastError, success } = useToast();

  const fetchPane = async (path: string, side: 'left' | 'right') => {
    side === 'left' ? setLeftLoading(true) : setRightLoading(true);
    try {
      const res = await api.get<BrowseResult>(`/api/fs/browse?path=${encodeURIComponent(path)}`);
      if (side === 'left') {
        setLeftData(res);
        setLeftSelected(new Set());
      } else {
        setRightData(res);
        setRightSelected(new Set());
      }
    } catch (err: any) {
      toastError("FS Error", err.message);
    } finally {
      side === 'left' ? setLeftLoading(false) : setRightLoading(false);
    }
  };

  useEffect(() => { fetchPane(leftPath, 'left'); }, [leftPath]);
  useEffect(() => { fetchPane(rightPath, 'right'); }, [rightPath]);

  const toggleSelect = (path: string, side: 'left' | 'right') => {
    const setFn = side === 'left' ? setLeftSelected : setRightSelected;
    setFn(prev => {
      const next = new Set(prev);
      next.has(path) ? next.delete(path) : next.add(path);
      return next;
    });
  };

  const selectAll = (side: 'left' | 'right') => {
    const data = side === 'left' ? leftData : rightData;
    const setFn = side === 'left' ? setLeftSelected : setRightSelected;
    const selected = side === 'left' ? leftSelected : rightSelected;
    
    if (selected.size === data?.directories.length) {
      setFn(new Set());
    } else {
      setFn(new Set(data?.directories.map(d => d.fullPath) || []));
    }
  };

  const createDir = async (name: string, path: string, side: 'left' | 'right') => {
    try {
      await api.post("/api/fs/mkdir", { parentPath: path, name });
      fetchPane(path, side);
    } catch (err: any) {
      toastError("Error", err.message);
    }
  };

  const handleAction = async (action: 'copy' | 'move' | 'delete', side: 'left' | 'right') => {
    const selected = side === 'left' ? leftSelected : rightSelected;
    const destPath = side === 'left' ? rightPath : leftPath;
    const srcPath = side === 'left' ? leftPath : rightPath;
    
    if (selected.size === 0) return;
    
    if (action !== 'delete' && destPath === srcPath) {
      toastError("Error", "Source and destination are the same");
      return;
    }

    try {
      for (const item of selected) {
        if (action === 'delete') {
          await api.post("/api/fs/delete", { target: item });
        } else {
          await api.post(`/api/fs/${action}`, { source: item, destination: destPath });
        }
      }
      success("Success", `Items ${action}d successfully`);
      fetchPane(leftPath, 'left');
      fetchPane(rightPath, 'right');
    } catch (err: any) {
      toastError("Error", err.message);
    }
  };

  const uploadFile = async (file: File, path: string, side: 'left' | 'right') => {
    try {
      const reader = new FileReader();
      reader.onload = async () => {
                
        
        const formData = new FormData();
        formData.append("path", path);
        // We actually just send base64 in a multipart or something, wait.
        // FileSystemApiController expects `MultipartFile file`.
        // Let's create a File object and append it!
      };
      
      const formData = new FormData();
      formData.append("path", path);
      formData.append("file", file);
      
      // Use native fetch to bypass JSON stringify in `api.post` if needed, 
      // or check if `api.post` supports FormData
      const res = await fetch("/api/fs/upload", {
        method: "POST",
        body: formData
      });
      
      if (!res.ok) throw new Error(await res.text());
      success("Success", "File uploaded");
      fetchPane(path, side);
    } catch (err: any) {
      toastError("Upload Error", err.message);
    }
  };

  const handleDrop = (e: React.DragEvent, path: string, side: 'left' | 'right') => {
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      Array.from(e.dataTransfer.files).forEach(f => uploadFile(f, path, side));
    }
  };

  return (
    <div className="flex flex-col md:flex-row gap-4 h-[500px]">
      <FilePane 
        title="Left Pane"
        path={leftPath}
        onNavigate={setLeftPath}
        browseData={leftData}
        loading={leftLoading}
        selectedItems={leftSelected}
        onToggleSelect={(p) => toggleSelect(p, 'left')}
        onSelectAll={() => selectAll('left')}
        onCreateDir={(name) => createDir(name, leftPath, 'left')}
        onUploadFile={(f) => uploadFile(f, leftPath, 'left')}
        onDrop={(e) => handleDrop(e, leftPath, 'left')}
        otherPanePath={rightPath}
        onAction={(a) => handleAction(a, 'left')}
      />
      <FilePane 
        title="Right Pane"
        path={rightPath}
        onNavigate={setRightPath}
        browseData={rightData}
        loading={rightLoading}
        selectedItems={rightSelected}
        onToggleSelect={(p) => toggleSelect(p, 'right')}
        onSelectAll={() => selectAll('right')}
        onCreateDir={(name) => createDir(name, rightPath, 'right')}
        onUploadFile={(f) => uploadFile(f, rightPath, 'right')}
        onDrop={(e) => handleDrop(e, rightPath, 'right')}
        otherPanePath={leftPath}
        onAction={(a) => handleAction(a, 'right')}
      />
    </div>
  );
}

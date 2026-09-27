import React, { useEffect, useState } from 'react';
import { api } from '../../lib/api';
import { useToast } from '../../components/ui/toast';
import { useConfirm } from '../../components/ui/confirm';
import { Server, Shield, Unlock, Lock, AlertCircle, RefreshCw, Users } from 'lucide-react';

interface AdStatus {
  isJoined: boolean;
  statusMessage: string;
  domainName: string;
  dnsReachable: boolean;
  kerberosWorking: boolean;
  winbindWorking: boolean;
}

export default function ActiveDirectoryPage() {
    const { toast } = useToast();
  const { confirm } = useConfirm();
  const [status, setStatus] = useState<AdStatus | null>(null);
  const [loading, setLoading] = useState(true);

  // Form State
  const [domain, setDomain] = useState('');
  const [username, setUsername] = useState('Administrator');
  const [password, setPassword] = useState('');
  const [isJoining, setIsJoining] = useState(false);

  const fetchStatus = async () => {
    try {
      setLoading(true);
      const res = await api.get<AdStatus>('/api/ad/status');
      setStatus(res);
    } catch (err: any) {
      toast('error', 'Status Error', err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatus();
  }, []);

  const handleJoin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!domain || !username || !password) {
      toast('error', 'Validation Error', 'All fields are required.');
      return;
    }

    const ok = await confirm({
      title: 'Join Active Directory',
      message: `Are you sure you want to join the domain ${domain}? This will reconfigure Kerberos and Winbind.`,
      confirmText: 'Join Domain',
      cancelText: 'Cancel'
    });

    if (!ok) return;

    try {
      setIsJoining(true);
      const res = await api.post<AdStatus>('/api/ad/join', { domain, username, password });
      setStatus(res);
      toast('success', 'Joined', 'Successfully joined the domain');
      setPassword(''); // clear password
    } catch (err: any) {
      toast('error', 'Join Failed', err.message);
    } finally {
      setIsJoining(false);
    }
  };

  const handleLeave = async () => {
    const ok = await confirm({
      title: 'Leave Active Directory',
      message: 'Are you sure you want to leave the domain? Users will lose access to domain shares.',
      confirmText: 'Leave Domain',
      cancelText: 'Cancel'
    });

    if (!ok) return;

    if (!username || !password) {
      toast('error', 'Validation Error', 'Admin Username and Password are required to leave the domain safely.');
      return;
    }

    try {
      setIsJoining(true);
      const res = await api.post<AdStatus>('/api/ad/leave', { domain, username, password });
      setStatus(res);
      toast('success', 'Left Domain', 'Successfully disconnected from AD');
      setPassword('');
    } catch (err: any) {
      toast('error', 'Leave Failed', err.message);
    } finally {
      setIsJoining(false);
    }
  };

  if (loading && !status) {
    return <div className="p-8 text-center"><RefreshCw className="w-8 h-8 animate-spin mx-auto text-brand" /></div>;
  }

  return (
    <div className="space-y-8 animate-fade-in fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">Active Directory</h1>
        <button onClick={fetchStatus} className="bg-surface hover:bg-surface-hover border border-border px-4 py-2 rounded-md shadow-sm flex items-center text-sm font-medium transition-colors">
          <RefreshCw className="w-4 h-4 mr-2 text-brand" /> Refresh Status
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Status Panel */}
        <div className="bg-surface border border-border rounded-lg shadow-sm">
          <div className="p-5 border-b border-border/50">
            <h2 className="text-lg font-semibold flex items-center gap-2">
              <Server className="w-5 h-5 text-brand" /> Health & Status
            </h2>
            <p className="text-sm text-status-disabled mt-1">Current domain integration state</p>
          </div>
          
          <div className="p-5">
          {status ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 bg-surface-hover rounded-lg border border-border">
                <span className="font-medium text-foreground text-sm uppercase tracking-wider">Domain Join State</span>
                {status.isJoined ? (
                  <span className="flex items-center gap-1.5 px-3 py-1 bg-brand/10 text-brand rounded-full text-xs font-bold uppercase tracking-widest border border-brand/20">
                    <Shield className="w-3.5 h-3.5" /> Joined
                  </span>
                ) : (
                  <span className="flex items-center gap-1.5 px-3 py-1 bg-status-error/10 text-status-error rounded-full text-xs font-bold uppercase tracking-widest border border-status-error/20">
                    <AlertCircle className="w-3.5 h-3.5" /> Not Joined
                  </span>
                )}
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                 <div className="p-3 bg-surface-hover rounded-md bg-opacity-50 border border-border flex flex-col justify-center">
                   <div className="text-xs text-status-disabled uppercase tracking-widest mb-1.5 font-bold">Winbind Service</div>
                   <div className="font-semibold text-sm flex items-center gap-2">
                     <span className={`w-2 h-2 rounded-full ${status.winbindWorking ? 'bg-brand' : 'bg-status-error'}`}></span>
                     {status.winbindWorking ? 'Operational' : 'Failing'}
                   </div>
                 </div>
                 <div className="p-3 bg-surface-hover rounded-md bg-opacity-50 border border-border flex flex-col justify-center">
                   <div className="text-xs text-status-disabled uppercase tracking-widest mb-1.5 font-bold">Kerberos / DNS</div>
                   <div className="font-semibold text-sm flex items-center gap-2">
                     <span className={`w-2 h-2 rounded-full ${status.kerberosWorking ? 'bg-brand' : 'bg-status-error'}`}></span>
                     {status.kerberosWorking ? 'OK' : 'Errors Detected'}
                   </div>
                 </div>
              </div>
            </div>
          ) : (
             <div className="text-center p-8 text-status-disabled border border-dashed border-border rounded-lg text-sm">Status not available</div>
          )}
          </div>
        </div>

        {/* Action Panel */}
        <div className="bg-surface border border-border rounded-lg shadow-sm">
          <div className="p-5 border-b border-border/50">
            <h2 className="text-lg font-semibold flex items-center gap-2">
              <Lock className="w-5 h-5 text-brand" /> Join / Leave Domain
            </h2>
            <p className="text-sm text-status-disabled mt-1">Manage server membership</p>
          </div>

          <form onSubmit={handleJoin} className="p-5 space-y-4">
             <div className="space-y-1.5">
               <label className="text-sm font-medium">Target Domain (Realm)</label>
               <input type="text" className="w-full bg-surface-hover border border-border focus:border-border-strong px-3 py-2 text-sm rounded-md focus:outline-none focus:ring-2 focus:ring-brand/20 transition-all uppercase" placeholder="e.g. ad.company.com" value={domain} onChange={e => setDomain(e.target.value.toUpperCase())} required />
             </div>
             <div className="grid grid-cols-2 gap-4">
               <div className="space-y-1.5">
                 <label className="text-sm font-medium">Admin User</label>
                 <input type="text" className="w-full bg-surface-hover border border-border focus:border-border-strong px-3 py-2 text-sm rounded-md focus:outline-none focus:ring-2 focus:ring-brand/20 transition-all" value={username} onChange={e => setUsername(e.target.value)} required />
               </div>
               <div className="space-y-1.5">
                 <label className="text-sm font-medium">Admin Password</label>
                 <input type="password" className="w-full bg-surface-hover border border-border focus:border-border-strong px-3 py-2 text-sm rounded-md focus:outline-none focus:ring-2 focus:ring-brand/20 transition-all" value={password} onChange={e => setPassword(e.target.value)} required />
               </div>
             </div>
             
             <div className="pt-4 flex gap-3">
                <button type="submit" disabled={isJoining || status?.isJoined === true} className="flex-1 bg-brand text-brand-text hover:bg-brand-hover border flex justify-center items-center px-4 py-2 rounded-md text-sm font-bold shadow-sm transition-colors disabled:opacity-50">
                   {isJoining ? <RefreshCw className="w-4 h-4 mr-2 animate-spin" /> : <Shield className="w-4 h-4 mr-2" />}
                   Join Domain
                </button>
                {status?.isJoined && (
                  <button type="button" onClick={handleLeave} disabled={isJoining} className="flex-1 bg-surface hover:bg-status-error/10 border border-status-error/50 text-status-error justify-center flex items-center px-4 py-2 rounded-md text-sm font-bold transition-colors disabled:opacity-50">
                     <Unlock className="w-4 h-4 mr-2" /> Leave Domain
                  </button>
                )}
             </div>
          </form>
        </div>
      </div>
      
      {/* ID Mapping Visualization */}
      <div className="bg-surface border border-brand/20 rounded-lg shadow-sm p-5 border-l-4 border-l-brand">
         <h2 className="text-lg font-semibold flex items-center gap-2 mb-2">
            <Users className="w-5 h-5 text-brand" /> UID / GID Mapping Config
         </h2>
         <p className="text-sm text-status-disabled flex items-center gap-1.5">
           To configure specific range mappings, navigate to the <span className="text-foreground font-semibold px-2 py-0.5 bg-surface-hover border border-border rounded">Global Settings</span> tab. Samba will automatically allocate Unix UIDs for AD Users based on those configured IDMAP rules.
         </p>
      </div>

    </div>
  );
}

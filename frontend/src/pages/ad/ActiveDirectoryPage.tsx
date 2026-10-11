import React, { useEffect, useState } from 'react';
import { api } from '../../lib/api';
import { useToast } from '../../components/ui/toast';
import { useConfirm } from '../../components/ui/confirm';
import { Server, Shield, Unlock, Lock, AlertCircle, RefreshCw, Users } from 'lucide-react';
import { useTranslation } from '../../lib/i18n';

interface AdStatus {
  isJoined: boolean;
  statusMessage: string;
  domainName: string;
  dnsReachable: boolean;
  kerberosWorking: boolean;
  winbindWorking: boolean;
}

export default function ActiveDirectoryPage() {
  const { t } = useTranslation();
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
      title: t('ad.joinConfirmTitle'),
      message: t('ad.joinConfirmMsg').replace('{domain}', domain),
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
      title: t('ad.leaveConfirmTitle'),
      message: t('ad.leaveConfirmMsg'),
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
    return <div className="p-8 text-center"><RefreshCw className="w-8 h-8 animate-spin mx-auto text-accent-text" /></div>;
  }

  return (
    <div className="space-y-8 animate-fade-in fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold tracking-tight">{t('ad.title')}</h1>
        <button onClick={fetchStatus} className="bg-surface hover:bg-surface-2 border border-border-strong px-4 py-2 rounded-md flex items-center text-sm font-medium transition-colors">
          <RefreshCw className="w-4 h-4 mr-2 text-accent-text" /> {t('ad.refresh')}
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Status Panel */}
        <div className="bg-surface border border-border rounded-lg">
          <div className="p-5 border-b border-border">
            <h2 className="text-lg font-semibold flex items-center gap-2">
              <Server className="w-5 h-5 text-accent-text" /> {t('ad.healthStatus')}
            </h2>
            <p className="text-sm text-text-muted mt-1">{t('ad.healthDesc')}</p>
          </div>
          
          <div className="p-5">
          {status ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between p-4 bg-surface-2 rounded-lg border border-border">
                <span className="font-medium text-foreground text-sm uppercase tracking-wider">{t('ad.domainJoinState')}</span>
                {status.isJoined ? (
                  <span className="flex items-center gap-1.5 px-3 py-1 bg-surface-tint text-accent-text rounded-full text-xs font-bold uppercase tracking-widest border border-border">
                    <Shield className="w-3.5 h-3.5" /> {t('ad.joined')}
                  </span>
                ) : (
                  // TODO: status-error token
                  <span className="flex items-center gap-1.5 px-3 py-1 bg-status-error/10 text-status-error rounded-full text-xs font-bold uppercase tracking-widest border border-status-error/20">
                    <AlertCircle className="w-3.5 h-3.5" /> Not {t('ad.joined')}
                  </span>
                )}
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                 <div className="p-3 bg-surface-2 rounded-md border border-border flex flex-col justify-center">
                   <div className="text-xs text-text-muted uppercase tracking-widest mb-1.5 font-bold">{t('ad.winbindService')}</div>
                   <div className="font-semibold text-sm flex items-center gap-2">
                     {/* TODO: status-error / active token */}
                     <span className={`w-2 h-2 rounded-full ${status.winbindWorking ? 'bg-accent' : 'bg-status-error'}`}></span>
                     {status.winbindWorking ? t('ad.operational') : t('ad.failing')}
                   </div>
                 </div>
                 <div className="p-3 bg-surface-2 rounded-md border border-border flex flex-col justify-center">
                   <div className="text-xs text-text-muted uppercase tracking-widest mb-1.5 font-bold">{t('ad.kerberosDns')}</div>
                   <div className="font-semibold text-sm flex items-center gap-2">
                     {/* TODO: status-error / active token */}
                     <span className={`w-2 h-2 rounded-full ${status.kerberosWorking ? 'bg-accent' : 'bg-status-error'}`}></span>
                     {status.kerberosWorking ? t('ad.ok') : t('ad.errorsDetected')}
                   </div>
                 </div>
              </div>
            </div>
          ) : (
             <div className="text-center p-8 text-text-muted border border-dashed border-border rounded-lg text-sm">{t('ad.statusNotAvailable')}</div>
          )}
          </div>
        </div>

        {/* Action Panel */}
        <div className="bg-surface border border-border rounded-lg">
          <div className="p-5 border-b border-border">
            <h2 className="text-lg font-semibold flex items-center gap-2">
              <Lock className="w-5 h-5 text-accent-text" /> {t('ad.actionTitle')}
            </h2>
            <p className="text-sm text-text-muted mt-1">{t('ad.actionDesc')}</p>
          </div>

          <form onSubmit={handleJoin} className="p-5 space-y-4">
             <div className="space-y-1.5">
               <label className="text-sm font-medium">{t('ad.targetDomain')}</label>
               <input type="text" className="w-full bg-surface border border-border-strong focus:border-accent px-3 py-2 text-sm rounded-md focus:outline-none focus:ring-2 focus:ring-accent/20 transition-all uppercase" placeholder="e.g. ad.company.com" value={domain} onChange={e => setDomain(e.target.value.toUpperCase())} required />
             </div>
             <div className="grid grid-cols-2 gap-4">
               <div className="space-y-1.5">
                 <label className="text-sm font-medium">{t('ad.adminUser')}</label>
                 <input type="text" className="w-full bg-surface border border-border-strong focus:border-accent px-3 py-2 text-sm rounded-md focus:outline-none focus:ring-2 focus:ring-accent/20 transition-all" value={username} onChange={e => setUsername(e.target.value)} required />
               </div>
               <div className="space-y-1.5">
                 <label className="text-sm font-medium">{t('ad.adminPass')}</label>
                 <input type="password" className="w-full bg-surface border border-border-strong focus:border-accent px-3 py-2 text-sm rounded-md focus:outline-none focus:ring-2 focus:ring-accent/20 transition-all" value={password} onChange={e => setPassword(e.target.value)} required />
               </div>
             </div>
             
             <div className="pt-4 flex gap-3">
                <button type="submit" disabled={isJoining || status?.isJoined === true} className="flex-1 bg-accent text-on-accent hover:opacity-90 flex justify-center items-center px-4 py-2 rounded-md text-sm font-bold transition-opacity disabled:opacity-50">
                   {isJoining ? <RefreshCw className="w-4 h-4 mr-2 animate-spin" /> : <Shield className="w-4 h-4 mr-2" />}
                   {t('ad.joinDomain')}
                </button>
                {status?.isJoined && (
                  // TODO: status-error token
                  <button type="button" onClick={handleLeave} disabled={isJoining} className="flex-1 bg-surface hover:bg-status-error/10 border border-status-error/50 text-status-error justify-center flex items-center px-4 py-2 rounded-md text-sm font-bold transition-colors disabled:opacity-50">
                     <Unlock className="w-4 h-4 mr-2" /> {t('ad.leaveDomain')}
                  </button>
                )}
             </div>
          </form>
        </div>
      </div>
      
      {/* ID Mapping Visualization */}
      <div className="bg-surface border border-border rounded-lg p-5 border-l-4 border-l-accent">
         <h2 className="text-lg font-semibold flex items-center gap-2 mb-2">
            <Users className="w-5 h-5 text-accent-text" /> {t('ad.idmapTitle')}
         </h2>
         <p className="text-sm text-text-muted flex items-center gap-1.5">
           {t('ad.idmapDesc')} <span className="text-foreground font-semibold px-2 py-0.5 bg-surface-2 border border-border rounded">{t('ad.idmapTab')}</span> tab. Samba will automatically allocate Unix UIDs for AD Users based on those configured IDMAP rules.
         </p>
      </div>

    </div>
  );
}

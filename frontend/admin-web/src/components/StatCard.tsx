import React from 'react';
import { ArrowUpRight, LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string;
  subtitle: string;
  growth?: number;
  icon: LucideIcon;
  tone?: 'emerald' | 'blue' | 'amber' | 'rose';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  growth,
  icon: Icon,
  tone = 'emerald',
}) => {
  const toneClasses = {
    emerald: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    blue: 'bg-blue-50 text-blue-700 border-blue-200',
    amber: 'bg-amber-50 text-amber-700 border-amber-200',
    rose: 'bg-rose-50 text-rose-700 border-rose-200',
  }[tone];

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 flex flex-col justify-between gap-3 transition-colors duration-200 hover:border-slate-300">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-slate-600">{title}</span>
        <div className={`w-10 h-10 rounded-lg border flex items-center justify-center ${toneClasses}`}>
          <Icon className="w-5 h-5" />
        </div>
      </div>

      <div>
        <div className="text-2xl font-bold text-slate-900 tracking-tight">{value}</div>
        <div className="flex items-center gap-2 mt-1.5">
          {typeof growth === 'number' && (
            <span className="inline-flex items-center gap-0.5 text-xs font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
              <ArrowUpRight className="w-3.5 h-3.5" />+{growth}%
            </span>
          )}
          <span className="text-xs text-slate-500">{subtitle}</span>
        </div>
      </div>
    </div>
  );
};


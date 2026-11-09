import React, { useState } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { RevenueDataPoint, formatCurrencyVND } from '@shared/types';

interface RevenueChartProps {
  byDay: RevenueDataPoint[];
  byMonth: RevenueDataPoint[];
  byYear: RevenueDataPoint[];
}

export const RevenueChart: React.FC<RevenueChartProps> = ({
  byDay,
  byMonth,
  byYear,
}) => {
  const [period, setPeriod] = useState<'DAY' | 'MONTH' | 'YEAR'>('MONTH');

  const data =
    period === 'DAY' ? byDay : period === 'MONTH' ? byMonth : byYear;

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-5">
        <div>
          <h3 className="text-base font-bold text-slate-900">
            Biểu đồ Doanh thu & Lợi nhuận gộp
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Theo dõi biến động dòng tiền theo Ngày / Tháng / Năm (Đơn vị: VND)
          </p>
        </div>

        <div className="inline-flex rounded-lg bg-slate-100 p-1">
          {(
            [
              { key: 'DAY', label: '7 Ngày qua' },
              { key: 'MONTH', label: 'Theo Tháng' },
              { key: 'YEAR', label: 'Theo Năm' },
            ] as const
          ).map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => setPeriod(item.key)}
              className={`px-3 py-1.5 text-xs font-semibold rounded-md cursor-pointer transition-colors duration-200 ${
                period === item.key
                  ? 'bg-white text-emerald-700 shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {/* Responsive Container for 1366px to 1920px+ resolutions */}
      <div className="w-full h-80">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 16, left: 8, bottom: 0 }}>
            <defs>
              <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#047857" stopOpacity={0.25} />
                <stop offset="95%" stopColor="#047857" stopOpacity={0.02} />
              </linearGradient>
              <linearGradient id="profitGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#0284C7" stopOpacity={0.2} />
                <stop offset="95%" stopColor="#0284C7" stopOpacity={0.02} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" />
            <XAxis
              dataKey="label"
              tick={{ fontSize: 12, fill: '#475569' }}
              axisLine={{ stroke: '#CBD5E1' }}
            />
            <YAxis
              tickFormatter={(val: number) => `${Math.round(val / 1000000)}M`}
              tick={{ fontSize: 12, fill: '#475569' }}
              axisLine={{ stroke: '#CBD5E1' }}
            />
            <Tooltip
              formatter={(value: number, name: string) => [
                formatCurrencyVND(value),
                name === 'revenue' ? 'Doanh thu' : 'Lợi nhuận gộp',
              ]}
              contentStyle={{
                borderRadius: '10px',
                border: '1px solid #E2E8F0',
                fontSize: '12px',
              }}
            />
            <Area
              type="monotone"
              dataKey="revenue"
              stroke="#047857"
              strokeWidth={2.5}
              fillOpacity={1}
              fill="url(#revGrad)"
            />
            <Area
              type="monotone"
              dataKey="profit"
              stroke="#0284C7"
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#profitGrad)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};


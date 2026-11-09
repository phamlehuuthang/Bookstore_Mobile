import React from 'react';
import { Image as ImageIcon } from 'lucide-react';

const SAMPLE_COVERS = [
  'https://images.unsplash.com/photo-1532012197267-da84d127e765?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1554224155-6726b3ff858f?auto=format&fit=crop&w=600&q=80',
];

interface ImageUploaderProps {
  value: string;
  onChange: (url: string) => void;
}

export const ImageUploader: React.FC<ImageUploaderProps> = ({ value, onChange }) => {
  return (
    <div className="space-y-2">
      <label className="block text-xs font-semibold text-slate-700">
        Ảnh bìa sách (URL hoặc chọn mẫu bìa chuẩn)
      </label>
      <div className="flex items-center gap-3">
        <div className="w-16 h-20 rounded-lg border border-slate-200 bg-slate-100 overflow-hidden flex items-center justify-center shrink-0">
          {value ? (
            <img
              src={value}
              alt="Xem trước ảnh bìa"
              className="w-full h-full object-cover"
            />
          ) : (
            <ImageIcon className="w-6 h-6 text-slate-400" />
          )}
        </div>
        <div className="flex-1 space-y-2">
          <input
            type="url"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder="https://images.unsplash.com/..."
            className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 focus:outline-none focus:border-emerald-600"
          />
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-xs text-slate-500">Mẫu nhanh:</span>
            {SAMPLE_COVERS.map((url, idx) => (
              <button
                key={url}
                type="button"
                onClick={() => onChange(url)}
                className="px-2 py-0.5 text-xs rounded border border-slate-200 bg-slate-50 hover:bg-emerald-50 hover:border-emerald-300 text-slate-700 cursor-pointer"
              >
                Bìa #{idx + 1}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};


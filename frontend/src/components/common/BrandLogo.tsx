import React from 'react';

interface BrandLogoProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  variant?: 'light' | 'dark'; // 'light' is for light backgrounds, 'dark' is for dark backgrounds
  showTagline?: boolean;
  className?: string;
}

export const BrandLogo: React.FC<BrandLogoProps> = ({
  size = 'md',
  variant = 'light',
  showTagline = true,
  className = '',
}) => {
  const iconPixelSizes = {
    sm: { h: 'h-7', w: 'w-10' },
    md: { h: 'h-9', w: 'w-14' },
    lg: { h: 'h-12', w: 'w-18' },
    xl: { h: 'h-16', w: 'w-24' },
  };

  const textSizes = {
    sm: { title: 'text-base', subtitle: 'text-[9px] tracking-widest' },
    md: { title: 'text-xl', subtitle: 'text-[10px] tracking-widest' },
    lg: { title: 'text-2xl', subtitle: 'text-xs tracking-widest' },
    xl: { title: 'text-3xl', subtitle: 'text-sm tracking-widest' },
  };

  const currentIconSize = iconPixelSizes[size];
  const currentTextSize = textSizes[size];

  const isDark = variant === 'dark';
  const titleColor = isDark ? 'text-white' : 'text-slate-900';
  const subtitleColor = isDark ? 'text-purple-300' : 'text-purple-600';

  return (
    <div className={`inline-flex items-center space-x-3 select-none ${className}`}>
      {/* 3D Purple Infinity Loop Icon */}
      <div className="relative flex-shrink-0 flex items-center justify-center">
        <img
          src="/infinity-logo.png"
          alt="Brightloop Infinity Loop"
          className={`${currentIconSize.h} ${currentIconSize.w} object-contain transition-transform duration-200 hover:scale-105 drop-shadow-sm ${
            isDark ? 'brightness-110 contrast-125' : 'mix-blend-multiply'
          }`}
        />
      </div>

      {showTagline && (
        <div className="flex flex-col leading-none">
          <div className="flex items-center space-x-1.5">
            <span className={`font-black tracking-tight ${currentTextSize.title} ${titleColor}`}>
              Brightloop
            </span>
            <span className="px-1.5 py-0.5 bg-purple-600 text-white text-[9px] font-black rounded uppercase tracking-wider shadow-sm">
              Cloud
            </span>
          </div>
          <span className={`font-bold uppercase mt-1 ${currentTextSize.subtitle} ${subtitleColor}`}>
            Subscription Engine
          </span>
        </div>
      )}
    </div>
  );
};

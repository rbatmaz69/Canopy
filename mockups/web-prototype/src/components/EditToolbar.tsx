import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Check, LayoutGrid, Layers, Plus } from 'lucide-react';
import { TemplateKind } from '../layout/types';
import { triggerHaptic } from '../motion/haptics';
import { getSpringTransition, MotionSettings } from '../motion/springs';
import { ThemeTokens } from '../theme/themes';

interface EditToolbarProps {
  theme: ThemeTokens;
  motionSettings: MotionSettings;
  isAllAppsPage: boolean;
  onOpenCatalog: () => void;
  onApplyTemplate: (template: TemplateKind) => void;
  onOpenPages: () => void;
  onDone: () => void;
}

export const EditToolbar: React.FC<EditToolbarProps> = ({
  theme,
  motionSettings,
  isAllAppsPage,
  onOpenCatalog,
  onApplyTemplate,
  onOpenPages,
  onDone,
}) => {
  const [showTemplates, setShowTemplates] = useState(false);
  const spring = getSpringTransition('gapOpen', motionSettings);

  const templates: { id: TemplateKind; label: string; desc: string }[] = [
    { id: 'big-plus-3', label: 'Groß + 3', desc: '58 % Hero + 3 Kacheln' },
    { id: '2x2', label: '2 × 2', desc: '4 ausgewogene Felder' },
    { id: 'col-plus-grid', label: 'Spalte + Gitter', desc: 'Linke Spalte + 4 Felder' },
  ];

  return (
    <motion.div
      initial={{ y: 74, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      exit={{ y: 74, opacity: 0 }}
      transition={spring}
      onPointerDown={(e) => e.stopPropagation()}
      style={{
        position: 'absolute',
        left: 10,
        right: 10,
        bottom: 14,
        zIndex: 60,
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
      }}
    >
      <AnimatePresence>
        {showTemplates && (
          <motion.div
            initial={{ opacity: 0, y: 10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.95 }}
            transition={spring}
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(3, 1fr)',
              gap: 8,
              padding: 8,
              borderRadius: 20,
              background: theme.sheetBg,
              border: `1px solid ${theme.sheetBorder}`,
              boxShadow: '0 14px 32px rgba(0,0,0,0.28)',
            }}
          >
            {templates.map((tpl) => (
              <button
                key={tpl.id}
                type="button"
                onClick={() => {
                  triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
                  onApplyTemplate(tpl.id);
                  setShowTemplates(false);
                }}
                style={{
                  padding: '9px 8px',
                  borderRadius: 14,
                  border: `1px solid ${theme.sheetBorder}`,
                  background: 'rgba(125,125,145,0.1)',
                  color: theme.textPrimary,
                  cursor: 'pointer',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: 12.5, fontWeight: 700 }}>{tpl.label}</div>
                <div style={{ fontSize: 10, color: theme.textMuted, marginTop: 2 }}>{tpl.desc}</div>
              </button>
            ))}
          </motion.div>
        )}
      </AnimatePresence>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: 6,
          padding: 6,
          borderRadius: 22,
          background: theme.sheetBg,
          border: `1px solid ${theme.sheetBorder}`,
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          boxShadow: '0 14px 34px rgba(0,0,0,0.3)',
        }}
      >
        <button
          type="button"
          disabled={isAllAppsPage}
          onClick={() => {
            triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
            setShowTemplates(false);
            onOpenCatalog();
          }}
          style={toolBtnStyle(theme.textPrimary, false, isAllAppsPage)}
        >
          <Plus size={16} />
          <span>+ Kachel</span>
        </button>

        <button
          type="button"
          disabled={isAllAppsPage}
          onClick={() => {
            triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
            setShowTemplates((v) => !v);
          }}
          style={toolBtnStyle(theme.textPrimary, showTemplates, isAllAppsPage)}
        >
          <LayoutGrid size={16} />
          <span>Vorlage</span>
        </button>

        <button
          type="button"
          onClick={() => {
            triggerHaptic('press', { enabled: motionSettings.hapticsEnabled });
            setShowTemplates(false);
            onOpenPages();
          }}
          style={toolBtnStyle(theme.textPrimary, false, false)}
        >
          <Layers size={16} />
          <span>Seiten</span>
        </button>

        <button
          type="button"
          onClick={() => {
            triggerHaptic('snap', { enabled: motionSettings.hapticsEnabled });
            setShowTemplates(false);
            onDone();
          }}
          style={{
            ...toolBtnStyle('#FFFFFF', false, false),
            background: '#2563EB',
            color: '#FFFFFF',
          }}
        >
          <Check size={16} />
          <span>Fertig</span>
        </button>
      </div>
    </motion.div>
  );
};

function toolBtnStyle(color: string, active: boolean, disabled: boolean): React.CSSProperties {
  return {
    minHeight: 44,
    borderRadius: 16,
    border: 'none',
    background: active ? 'rgba(125,125,145,0.22)' : 'transparent',
    color,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 2,
    fontSize: 10.5,
    fontWeight: 700,
    cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.35 : 1,
  };
}
